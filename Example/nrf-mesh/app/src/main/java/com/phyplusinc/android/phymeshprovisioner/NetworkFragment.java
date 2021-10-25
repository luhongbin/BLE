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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel;
import no.nordicsemi.android.meshprovisioner.models.LightHslServer;
import no.nordicsemi.android.meshprovisioner.models.SensorServer;
import no.nordicsemi.android.meshprovisioner.models.VendorModel;
import no.nordicsemi.android.meshprovisioner.transport.ConfigNodeReset;
import no.nordicsemi.android.meshprovisioner.transport.Element;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.MeshModel;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;

import com.phyplusinc.android.phymeshprovisioner.ble.ScannerActivity;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentError;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentDeleteNode;
import com.phyplusinc.android.phymeshprovisioner.node.NodeConfigurationActivity;
import com.phyplusinc.android.phymeshprovisioner.node.VendorModelActivity;
import com.phyplusinc.android.phymeshprovisioner.node.adapter.NodeAdapter;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetOnOffDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.DialogFragmentResetNode;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SharedViewModel;
import com.phyplusinc.android.phymeshprovisioner.widgets.ItemTouchHelperAdapter;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableItemTouchHelperCallback;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

import static android.app.Activity.RESULT_OK;

public class NetworkFragment extends Fragment implements Injectable,
        NodeAdapter.OnItemClickListener,
        ItemTouchHelperAdapter,
        DialogFragmentDeleteNode.DialogFragmentDeleteNodeListener,
        DialogFragmentResetNode.DialogFragmentNodeResetListener {

    private SharedViewModel mViewModel;
    private boolean mIsConnected;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    @BindView(R.id.container)
    CoordinatorLayout container;
    @BindView(R.id.recycler_view_provisioned_nodes)
    RecyclerView mRecyclerViewNodes;
    private NodeAdapter mNodeAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = inflater.inflate(R.layout.fragment_network, null);
        mViewModel = new ViewModelProvider(requireActivity(), mViewModelFactory).get(SharedViewModel.class);
        ButterKnife.bind(this, rootView);

        final ExtendedFloatingActionButton fab = rootView.findViewById(R.id.fab_add_node);
        final View noNetworksConfiguredView = rootView.findViewById(R.id.no_networks_configured);

        // Configure the recycler view
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        mNodeAdapter = new NodeAdapter(requireContext(),network, mViewModel.getNodes());
        mNodeAdapter.setOnItemClickListener(this);
        mRecyclerViewNodes.setLayoutManager(new LinearLayoutManager(getContext()));
        final DividerItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        mRecyclerViewNodes.addItemDecoration(decoration);
        final ItemTouchHelper.Callback itemTouchHelperCallback = new RemovableItemTouchHelperCallback(this);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerViewNodes);
        mRecyclerViewNodes.setAdapter(mNodeAdapter);

        // Create view model containing utility methods for scanning 从存储里读节点数据
        mViewModel.getNodes().observe(getViewLifecycleOwner(), nodes -> {
            if (nodes != null && !nodes.isEmpty()) {
                noNetworksConfiguredView.setVisibility(View.GONE);
            } else {
                noNetworksConfiguredView.setVisibility(View.VISIBLE);
            }
            requireActivity().invalidateOptionsMenu();
        });

        mViewModel.isConnectedToProxy().observe(getViewLifecycleOwner(), isConnected -> {
            if (isConnected != null) {
                // >>>, PANDA, ADD, for node reset
                mIsConnected = isConnected;
                requireActivity().invalidateOptionsMenu();
            }
        });
         //上下滑动监听
        mRecyclerViewNodes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final LinearLayoutManager m = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (m != null) {
                    if (m.findFirstCompletelyVisibleItemPosition() == 0) {
                        fab.extend();
                    } else {
                        fab.shrink();
                    }
                }
            }
        });

        //创建按钮监听
        fab.setOnClickListener(v -> {
            final Intent intent = new Intent(requireContext(), ScannerActivity.class);
            intent.putExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, true);//带EXTRA_DATA_PROVISIONING_SERVICE给搜索页面
            startActivityForResult(intent, Utils.PROVISIONING_SUCCESS);//从搜索页面返回时带的数据
        });

        return rootView;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handleActivityResult(requestCode, resultCode, data);
    }

    // >>>, ADD, PANDA, single click -> show node vendor; long click -> show node configs
//    @Override
//    public void onDetailsClicked(final ProvisionedMeshNode node) {
//        /* single click */
//
////        mViewModel.setSelectedMeshNode(node);
////        final Intent meshDetailsIntent = new Intent(getActivity(), NodeDetailsActivity.class);
////        requireActivity().startActivity(meshDetailsIntent);
//
//        // >>>, ADD, PANDA, click -> show phy+ vendor model, through which we can use easy binding;
//        final Integer cid = node.getCompanyIdentifier();
//        if ( null != cid && 0x0504 == cid ) {
//            for (Element elem : node.getElements().values()) {
//                for (MeshModel model : elem.getMeshModels().values()) {
//                    if (model instanceof VendorModel) {
//                        mViewModel.setSelectedMeshNode(node);
//                        mViewModel.setSelectedElement(elem);
//                        mViewModel.setSelectedModel(model);
//                        final Intent meshCfgsIntent = new Intent(getActivity(), VendorModelActivity.class);
//                        requireActivity().startActivity(meshCfgsIntent);
//                        return;
//                    }
//                }
//            }
//        }
//
//        // >>>, ADD, PANDA, if Vendor Model not available, go NodeConfigurationActivity
//        {
//            mViewModel.setSelectedMeshNode(node);
//            final Intent meshConfigurationIntent = new Intent(getActivity(), NodeConfigurationActivity.class);
//            requireActivity().startActivity(meshConfigurationIntent);
//        }
//    }

    // >>>, ADD, PANDA, single click -> show node vendor; long click -> show node configs
//    @Override
//    public void onConfigureClicked(final ProvisionedMeshNode node) {
        /* long click */
//        mViewModel.setSelectedMeshNode(node);
//        final Intent meshConfigurationIntent = new Intent(getActivity(), NodeConfigurationActivity.class);
//        requireActivity().startActivity(meshConfigurationIntent);

//        final ArrayList<Element> elements = new ArrayList<>(node.getElements().values());
//        final BottomSheetModelsDialogFragment onOffFragment = BottomSheetModelsDialogFragment.getInstance(elements);
//        onOffFragment.show(getActivity().getSupportFragmentManager(), "xxxxxxxxxxxx");

//    }

    // >>>, ADD, PANDA, click -> show node cfgs through vendor model;
    @Override
    public void onCfgsClicked(final ProvisionedMeshNode node) {
        final Integer cid = node.getCompanyIdentifier();
        if ( null != cid && 0x0504 == cid ) {
            for (Element elem : node.getElements().values()) {
                for (MeshModel model : elem.getMeshModels().values()) {
                    if (model instanceof VendorModel) {
                        mViewModel.setSelectedMeshNode(node);
                        mViewModel.setSelectedElement(elem);
                        mViewModel.setSelectedModel(model);
                        mViewModel.navigateToModelActivity(getActivity(), model);
                        return;
                    }
                }
            }
        }

        // >>>, ADD, PANDA, if Vendor Model not available, go NodeConfigurationActivity
        {
            mViewModel.setSelectedMeshNode(node);
            final Intent meshConfigurationIntent = new Intent(getActivity(), NodeConfigurationActivity.class);
            requireActivity().startActivity(meshConfigurationIntent);
        }
    }

    @Override
    public void onUserClicked(View view, ProvisionedMeshNode node) {
        if (R.id.ib_node_cfgs == view.getId() ) {
            mViewModel.setSelectedMeshNode(node);
            final Intent meshConfigurationIntent = new Intent(getActivity(), NodeConfigurationActivity.class);
            requireActivity().startActivity(meshConfigurationIntent);
        } else
        if (R.id.ib_node_rset == view.getId() ) {//重置模块
            mViewModel.setSelectedMeshNode(node);
            final DialogFragmentResetNode resetNodeFragment =
                    DialogFragmentResetNode.newInstance(getString(R.string.title_reset_node), getString(R.string.reset_node_rationale_summary));
            resetNodeFragment.show(getChildFragmentManager(), null);
        } else
        /*if (R.id.ib_node_bulb == view.getId() ) {
            for (Element elem : node.getElements().values()) {
                for (MeshModel model : elem.getMeshModels().values()) {
                    if (model instanceof GenericOnOffServerModel ) {
                        mViewModel.setSelectedMeshNode(node);
                        mViewModel.setSelectedElement(elem);
                        mViewModel.setSelectedModel(model);
                        mViewModel.navigateToModelActivity(getActivity(), model);
                        return;
                    }
                }
            }
        } else
        if (R.id.ib_node_hsls == view.getId() ) {
            for (Element elem : node.getElements().values()) {
                for (MeshModel model : elem.getMeshModels().values()) {
                    if (model instanceof LightHslServer ) {
                        mViewModel.setSelectedMeshNode(node);
                        mViewModel.setSelectedElement(elem);
                        mViewModel.setSelectedModel(model);
                        mViewModel.navigateToModelActivity(getActivity(), model);
                        return;
                    }
                }
            }
        } else
        if (R.id.ib_node_snsr == view.getId() ) {
            for (Element elem : node.getElements().values()) {
                for (MeshModel model : elem.getMeshModels().values()) {
                    if ( model instanceof SensorServer ) {
                        mViewModel.setSelectedMeshNode(node);
                        mViewModel.setSelectedElement(elem);
                        mViewModel.setSelectedModel(model);
                        mViewModel.navigateToModelActivity(getActivity(), model);
                        return;
                    }
                }
            }
        } else
        if (R.id.ib_node_vndr == view.getId() ) {
            for (Element elem : node.getElements().values()) {
                for (MeshModel model : elem.getMeshModels().values()) {
                    if (model instanceof VendorModel ) {
                        mViewModel.setSelectedMeshNode(node);
                        mViewModel.setSelectedElement(elem);
                        mViewModel.setSelectedModel(model);
                        mViewModel.navigateToModelActivity(getActivity(), model);
                        return;
                    }
                }
            }
        } else */{
            for (Element elem : node.getElements().values()) {
                for (MeshModel model : elem.getMeshModels().values()) {
                    if ( R.id.ib_node_bulb == view.getId() && model instanceof GenericOnOffServerModel ||
                         R.id.ib_node_hsls == view.getId() && model instanceof LightHslServer ||
                         R.id.ib_node_snsr == view.getId() && model instanceof SensorServer ||
                         R.id.ib_node_vndr == view.getId() && model instanceof VendorModel ) {
                        mViewModel.setSelectedMeshNode(node);
                        mViewModel.setSelectedElement(elem);
                        mViewModel.setSelectedModel(model);
                        mViewModel.navigateToModelActivity(getActivity(), model);
                        return;
                    }
                }
            }
        }
    }

    // >>>, ADD, PANDA, click -> show node reset;
//    @Override
//    public void onRsetClicked(final ProvisionedMeshNode node) {
//        mViewModel.setSelectedMeshNode(node);
//        final DialogFragmentResetNode resetNodeFragment = DialogFragmentResetNode.
//                newInstance(getString(R.string.title_reset_node), getString(R.string.reset_node_rationale_summary));
//        resetNodeFragment.show(getChildFragmentManager(), null);
//    }

    @Override
    public void onItemDismiss(final RemovableViewHolder viewHolder) {//左右滑动删除节点
        final int position = viewHolder.getAdapterPosition();
        if (!mNodeAdapter.isEmpty()) {
            final DialogFragmentDeleteNode fragmentDeleteNode = DialogFragmentDeleteNode.newInstance(position);
            fragmentDeleteNode.show(getChildFragmentManager(), null);
        }
    }

    @Override
    public void onItemDismissFailed(final RemovableViewHolder viewHolder) {
        //Do nothing
    }

    @Override
    public void onNodeDeleteConfirmed(final int position) {//删除节点
       final ProvisionedMeshNode node = mNodeAdapter.getItem(position);
        //if (mViewModel.getNetworkLiveData().getMeshNetwork().deleteNode(node)) {
           // mViewModel.displaySnackBar(requireActivity(), container, getString(R.string.node_deleted), Snackbar.LENGTH_LONG);
        //}
    }

    @Override
    public void onNodeDeleteCancelled(final int position) {
        mNodeAdapter.notifyItemChanged(position);
    }

    @Override
    public void onNodeReset() {
        final ConfigNodeReset configNodeReset = new ConfigNodeReset();
        sendMessage(configNodeReset);
    }

    private void handleActivityResult(final int requestCode, final int resultCode, @NonNull final Intent data) {
        if (requestCode == Utils.PROVISIONING_SUCCESS) {
            if (resultCode == RESULT_OK) {
                final boolean provisioningSuccess = data.getBooleanExtra(Utils.PROVISIONING_COMPLETED, false);
                final DialogFragmentError fragmentConfigError;
                if (provisioningSuccess) {
                    final boolean provisionerUnassigned = data.getBooleanExtra(Utils.PROVISIONER_UNASSIGNED, false);
                    if (provisionerUnassigned) {
                        fragmentConfigError =
                                DialogFragmentError.newInstance(getString(R.string.title_init_config_error)
                                        , getString(R.string.provisioner_unassigned_msg));
                        fragmentConfigError.show(getChildFragmentManager(), null);
                    } else {
                        final boolean compositionDataReceived = data.getBooleanExtra(Utils.COMPOSITION_DATA_COMPLETED, false);
                        final boolean defaultTtlGetCompleted = data.getBooleanExtra(Utils.DEFAULT_GET_COMPLETED, false);
                        final boolean appKeyAddCompleted = data.getBooleanExtra(Utils.APP_KEY_ADD_COMPLETED, false);
                        final boolean networkRetransmitSetCompleted = data.getBooleanExtra(Utils.NETWORK_TRANSMIT_SET_COMPLETED, false);
                        final String title = getString(R.string.title_init_config_error);
                        final String message;
                        if (compositionDataReceived) {
                            if (appKeyAddCompleted) {
                                // >>>, TODO, PANDA, Add App Key to Models
                                if (!networkRetransmitSetCompleted) {
                                    fragmentConfigError =
                                            DialogFragmentError.newInstance(getString(R.string.title_init_config_error)
                                                    , getString(R.string.init_config_error_net_transmit_msg));
                                    fragmentConfigError.show(getChildFragmentManager(), null);
                                } else {
                                    // >>>, PANDA, ADD, for auto jump to publish/subscription activity
                                    final ProvisionedMeshNode node = data.getParcelableExtra(Utils.EXTRA_DATA);
                                    if ( null != node ) onCfgsClicked(node);
                                }
                            } else {
                                fragmentConfigError =
                                        DialogFragmentError.newInstance(getString(R.string.title_init_config_error)
                                                , getString(R.string.init_config_error_app_key_msg));
                                fragmentConfigError.show(getChildFragmentManager(), null);
                            }
                        } else {
                            fragmentConfigError =
                                    DialogFragmentError.newInstance(getString(R.string.title_init_config_error)
                                            , getString(R.string.init_config_error_all));
                            fragmentConfigError.show(getChildFragmentManager(), null);
                        }
                    }
                }
                requireActivity().invalidateOptionsMenu();
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected final boolean checkConnectivity() {
        if (!mIsConnected) {
            mViewModel.displayDisconnectedSnackBar(requireActivity(), container);
            return false;
        }
        return true;
    }

    private void sendMessage(final MeshMessage meshMessage) {
        try {
            if (!checkConnectivity())
                return;
            final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
            if (node != null) {
                mViewModel.getMeshManagerApi().createMeshPdu(node.getUnicastAddress(), meshMessage);
//                showProgressbar();
            }
        } catch (IllegalArgumentException ex) {
//            hideProgressBar();
            final DialogFragmentError message = DialogFragmentError.
                    newInstance(getString(R.string.title_error), ex.getMessage());
            message.show(getChildFragmentManager(), null);
        }
    }
}
