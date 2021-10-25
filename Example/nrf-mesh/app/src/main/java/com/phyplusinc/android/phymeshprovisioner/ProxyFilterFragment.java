/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.phyplusinc.android.phymeshprovisioner;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.ProxyConfigAddAddressToFilter;
import no.nordicsemi.android.meshprovisioner.transport.ProxyConfigRemoveAddressFromFilter;
import no.nordicsemi.android.meshprovisioner.transport.ProxyConfigSetFilterType;
import no.nordicsemi.android.meshprovisioner.utils.AddressArray;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.ProxyFilter;
import no.nordicsemi.android.meshprovisioner.utils.ProxyFilterType;
import com.phyplusinc.android.phymeshprovisioner.adapter.FilterAddressAdapter;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentError;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentFilterAddAddress;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SharedViewModel;
import com.phyplusinc.android.phymeshprovisioner.widgets.ItemTouchHelperAdapter;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableItemTouchHelperCallback;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

public class ProxyFilterFragment extends Fragment implements Injectable,
        DialogFragmentFilterAddAddress.DialogFragmentFilterAddressListener,
        ItemTouchHelperAdapter {

    private static final String CLEAR_ADDRESS_PRESSED = "CLEAR_ADDRESS_PRESSED";
    private static final String PROXY_FILTER_DISABLED = "PROXY_FILTER_DISABLED";

    private SharedViewModel mViewModel;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @BindView(R.id.action_white_list)
    Button actionEnableWhiteList;
    @BindView(R.id.action_black_list)
    Button actionEnableBlackList;
    @BindView(R.id.action_disable)
    Button actionDisable;
    @BindView(R.id.action_add_address)
    Button actionAddFilterAddress;
    @BindView(R.id.action_clear_addresses)
    Button actionClearFilterAddress;
    @BindView(R.id.proxy_filter_address_card)
    CardView mProxyFilterCard;
    private ProxyFilter mFilter;
    private boolean clearAddressPressed;
    private boolean isProxyFilterDisabled;
    private FilterAddressAdapter addressAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = inflater.inflate(R.layout.fragment_proxy_filter, null);
        mViewModel = new ViewModelProvider(requireActivity(), mViewModelFactory).get(SharedViewModel.class);
        ButterKnife.bind(this, rootView);

        if (savedInstanceState != null) {
            clearAddressPressed = savedInstanceState.getBoolean(CLEAR_ADDRESS_PRESSED);
            isProxyFilterDisabled = savedInstanceState.getBoolean(PROXY_FILTER_DISABLED);
        }

        final TextView noAddressesAdded = rootView.findViewById(R.id.no_addresses);
        final RecyclerView recyclerViewAddresses = rootView.findViewById(R.id.recycler_view_filter_addresses);
        actionEnableWhiteList.setEnabled(false);
        actionEnableBlackList.setEnabled(false);
        actionDisable.setEnabled(false);

        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewAddresses.setItemAnimator(new DefaultItemAnimator());
        final ItemTouchHelper.Callback itemTouchHelperCallback = new RemovableItemTouchHelperCallback(this);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewAddresses);
        addressAdapter = new FilterAddressAdapter(requireContext());
        recyclerViewAddresses.setAdapter(addressAdapter);

        mViewModel.isConnectedToProxy().observe(getViewLifecycleOwner(), isConnected -> {
            if (!isConnected) {
                clearAddressPressed = false;
                final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
                if (network != null) {
                    mFilter = network.getProxyFilter();
                    if (mFilter == null) {
                        addressAdapter.clearData();
                        noAddressesAdded.setVisibility(View.VISIBLE);
                        recyclerViewAddresses.setVisibility(View.GONE);
                    }
                }

                actionEnableWhiteList.setSelected(isConnected);
                actionEnableBlackList.setSelected(isConnected);
                actionDisable.setSelected(isConnected);
                actionAddFilterAddress.setEnabled(isConnected);
                actionClearFilterAddress.setVisibility(View.GONE);
            }
            actionDisable.setEnabled(false);
            actionEnableWhiteList.setEnabled(isConnected);
            actionEnableBlackList.setEnabled(isConnected);
        });

        mViewModel.getNetworkLiveData().observe(getViewLifecycleOwner(), meshNetworkLiveData -> {
            final MeshNetwork network = meshNetworkLiveData.getMeshNetwork();
            if (network == null) {
                return;
            }

            final ProxyFilter filter = mFilter = network.getProxyFilter();
            if (filter == null) {
                addressAdapter.clearData();
                return;
            } else if (clearAddressPressed) {
                clearAddressPressed = false;
                return;
            } else if (isProxyFilterDisabled) {
                actionDisable.setSelected(true);
            }

            actionEnableWhiteList.setSelected(mFilter.getFilterType().getType() == ProxyFilterType.WHITE_LIST_FILTER && !actionDisable.isSelected());
            actionEnableBlackList.setSelected(mFilter.getFilterType().getType() == ProxyFilterType.BLACK_LIST_FILTER);

            if (!mFilter.getAddresses().isEmpty()) {
                noAddressesAdded.setVisibility(View.GONE);
                actionClearFilterAddress.setVisibility(View.VISIBLE);
            } else {
                noAddressesAdded.setVisibility(View.VISIBLE);
                actionClearFilterAddress.setVisibility(View.GONE);
            }
            actionAddFilterAddress.setEnabled(!actionDisable.isSelected());
            addressAdapter.updateData(filter);
        });

        actionEnableWhiteList.setOnClickListener(v -> {
            isProxyFilterDisabled = false;
            v.setSelected(true);
            actionEnableBlackList.setSelected(false);
            actionDisable.setSelected(false);
            actionDisable.setEnabled(true);
            setFilter(new ProxyFilterType(ProxyFilterType.WHITE_LIST_FILTER));
        });

        actionEnableBlackList.setOnClickListener(v -> {
            isProxyFilterDisabled = false;
            v.setSelected(true);
            actionEnableWhiteList.setSelected(false);
            actionDisable.setSelected(false);
            actionDisable.setEnabled(true);
            setFilter(new ProxyFilterType(ProxyFilterType.BLACK_LIST_FILTER));
        });

        actionDisable.setOnClickListener(v -> {
            v.setSelected(true);
            isProxyFilterDisabled = true;
            actionEnableWhiteList.setSelected(false);
            actionEnableBlackList.setSelected(false);
            addressAdapter.clearData();
            actionDisable.setEnabled(false);
            setFilter(new ProxyFilterType(ProxyFilterType.WHITE_LIST_FILTER));
        });

        actionAddFilterAddress.setOnClickListener(v -> {
            final ProxyFilterType filterType;
            if (mFilter == null) {
                filterType = new ProxyFilterType(ProxyFilterType.WHITE_LIST_FILTER);
            } else {
                filterType = mFilter.getFilterType();
            }
            final DialogFragmentFilterAddAddress filterAddAddress = DialogFragmentFilterAddAddress.newInstance(filterType);
            filterAddAddress.show(getChildFragmentManager(), null);
        });

        actionClearFilterAddress.setOnClickListener(v -> removeAddresses());

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CLEAR_ADDRESS_PRESSED, clearAddressPressed);
        outState.putBoolean(PROXY_FILTER_DISABLED, isProxyFilterDisabled);
    }

    @Override
    public void addAddresses(final List<AddressArray> addresses) {
        final ProxyConfigAddAddressToFilter addAddressToFilter = new ProxyConfigAddAddressToFilter(addresses);
        sendMessage(addAddressToFilter);
    }

    @Override
    public void onItemDismiss(final RemovableViewHolder viewHolder) {
        final int position = viewHolder.getAdapterPosition();
        if (viewHolder instanceof FilterAddressAdapter.ViewHolder) {
            removeAddress(position);
        }
    }

    @Override
    public void onItemDismissFailed(final RemovableViewHolder viewHolder) {

    }

    private void removeAddress(final int position) {
        final MeshNetwork meshNetwork = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (meshNetwork != null) {
            final ProxyFilter proxyFilter = meshNetwork.getProxyFilter();
            if (proxyFilter != null) {
                clearAddressPressed = true;
                final AddressArray addressArr = proxyFilter.getAddresses().get(position);
                final List<AddressArray> addresses = new ArrayList<>();
                addresses.add(addressArr);
                addressAdapter.clearRow(position);
                final ProxyConfigRemoveAddressFromFilter removeAddressFromFilter = new ProxyConfigRemoveAddressFromFilter(addresses);
                sendMessage(removeAddressFromFilter);
            }
        }
    }

    private void removeAddresses() {
        final MeshNetwork meshNetwork = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (meshNetwork != null) {
            final ProxyFilter proxyFilter = meshNetwork.getProxyFilter();
            if (proxyFilter != null) {
                if (!proxyFilter.getAddresses().isEmpty()) {
                    final ProxyConfigRemoveAddressFromFilter removeAddressFromFilter = new ProxyConfigRemoveAddressFromFilter(proxyFilter.getAddresses());
                    sendMessage(removeAddressFromFilter);
                }
            }
        }
    }

    private void setFilter(final ProxyFilterType filterType) {
        final ProxyConfigSetFilterType setFilterType = new ProxyConfigSetFilterType(filterType);
        sendMessage(setFilterType);
    }

    private void sendMessage(final MeshMessage meshMessage) {
        try {
            mViewModel.getMeshManagerApi().createMeshPdu(MeshAddress.UNASSIGNED_ADDRESS, meshMessage);
        } catch (IllegalArgumentException ex) {
            final DialogFragmentError message = DialogFragmentError.
                    newInstance(getString(R.string.title_error), ex.getMessage());
            message.show(getChildFragmentManager(), null);
        }
    }
}
