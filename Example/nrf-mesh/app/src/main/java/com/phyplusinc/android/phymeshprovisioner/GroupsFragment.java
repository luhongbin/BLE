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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.adapter.GroupAdapter;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentCreateGroup;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SharedViewModel;
import com.phyplusinc.android.phymeshprovisioner.widgets.ItemTouchHelperAdapter;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableItemTouchHelperCallback;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

public class GroupsFragment extends Fragment implements Injectable,
        ItemTouchHelperAdapter,
        GroupAdapter.OnItemClickListener,
        GroupCallbacks {

    private SharedViewModel mViewModel;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    @BindView(R.id.container)
    CoordinatorLayout container;
    @BindView(android.R.id.empty)
    View mEmptyView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = inflater.inflate(R.layout.fragment_groups, null);
        mViewModel = new ViewModelProvider(requireActivity(), mViewModelFactory).get(SharedViewModel.class);
        ButterKnife.bind(this, rootView);

        final ExtendedFloatingActionButton fab = rootView.findViewById(R.id.fab_add_group);

        // Configure the recycler view
        final RecyclerView recyclerViewGroups = rootView.findViewById(R.id.recycler_view_groups);
        recyclerViewGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerViewGroups.getContext(), DividerItemDecoration.VERTICAL);
        recyclerViewGroups.addItemDecoration(dividerItemDecoration);
        final ItemTouchHelper.Callback itemTouchHelperCallback = new RemovableItemTouchHelperCallback(this);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewGroups);
        final GroupAdapter adapter = new GroupAdapter(requireContext());//群组有一个数据适配器
        adapter.setOnItemClickListener(this);
        recyclerViewGroups.setAdapter(adapter);

        mViewModel.getNetworkLiveData().observe(getViewLifecycleOwner(), meshNetworkLiveData -> {
            if (meshNetworkLiveData != null) {
                if (meshNetworkLiveData.getMeshNetwork().getGroups().isEmpty()) {
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mEmptyView.setVisibility(View.INVISIBLE);
                }
            }
        });

        mViewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
            adapter.updateAdapter(network, groups);
        });

        //悬浮按钮监听
        fab.setOnClickListener(v -> {
            DialogFragmentCreateGroup fragmentCreateGroup = DialogFragmentCreateGroup.newInstance();
            fragmentCreateGroup.show(getChildFragmentManager(), null);
        });

        //列表上下滑动处理
        recyclerViewGroups.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final LinearLayoutManager m = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (m != null) {
                    if (m.findFirstCompletelyVisibleItemPosition() == 0) {
                        fab.extend();//悬浮按钮伸展
                    } else {
                        fab.shrink();//缩成一个圆
                    }
                }
            }
        });

        return rootView;

    }

    @Override
    public void onItemClick(final int address) {
        mViewModel.setSelectedGroup(address);
        startActivity(new Intent(requireContext(), GroupControlsActivity.class));
    }

    @Override
    public void onItemDismiss(final RemovableViewHolder viewHolder) {
        final int position = viewHolder.getAdapterPosition();
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        final Group group = network.getGroups().get(position);
        if (network.getModels(group).size() == 0) {
            network.removeGroup(group);
            displaySnackBar(group);
        }
    }

    @Override
    public void onItemDismissFailed(final RemovableViewHolder viewHolder) {
        final String message = getString(R.string.error_group_unsubscribe_to_delete);
        mViewModel.displaySnackBar(requireActivity(), container, message, Snackbar.LENGTH_LONG);
    }

    @Override
    public Group createGroup() {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.createGroup(network.getSelectedProvisioner(), "Mesh Group");
    }

    @Override
    public Group createGroup(@NonNull final String name) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.createGroup(network.getSelectedProvisioner(), name);
    }

    @Override
    public Group createGroup(@NonNull final UUID uuid, final String name) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.createGroup(uuid, null, name);
    }

    @Override
    public boolean onGroupAdded(@NonNull final String name, final int address) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        final Group group = network.createGroup(network.getSelectedProvisioner(), address, name);
        if (group != null) {
            return network.addGroup(group);
        }
        return false;
    }

    @Override
    public boolean onGroupAdded(@NonNull final Group group) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.addGroup(group);
    }

    //调用显示窗口显示消息
    private void displaySnackBar(final Group group) {
        final String message = getString(R.string.group_deleted, group.getName());
        Snackbar.make(container, message, Snackbar.LENGTH_LONG)//长时间显示
                .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                .setAction(R.string.undo, v -> {
                    mEmptyView.setVisibility(View.INVISIBLE);//群组空视图不显示
                    final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
                    if (network != null) {
                        network.addGroup(group);
                    }

                })
                .show();
    }
}
