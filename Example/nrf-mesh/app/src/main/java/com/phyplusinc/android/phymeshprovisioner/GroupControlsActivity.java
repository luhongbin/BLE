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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.ApplicationKey;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.models.SigModelParser;
import no.nordicsemi.android.meshprovisioner.models.VendorModel;
import no.nordicsemi.android.meshprovisioner.transport.Element;
import no.nordicsemi.android.meshprovisioner.transport.GenericLevelSetUnacknowledged;
import no.nordicsemi.android.meshprovisioner.transport.GenericOnOffSetUnacknowledged;
import no.nordicsemi.android.meshprovisioner.transport.LightHslGet;
import no.nordicsemi.android.meshprovisioner.transport.LightHslSetUnacknowledged;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.MeshModel;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked;
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus;
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageUnacked;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

import com.google.android.material.snackbar.Snackbar;
import com.phyplusinc.android.phymeshprovisioner.adapter.SubGroupAdapter;
import com.phyplusinc.android.phymeshprovisioner.ble.ScannerActivity;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentError;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetDetailsDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetLevelDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetLightHSLDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetOnOffDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetVendorDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.GroupControlsViewModel;

public class GroupControlsActivity extends AppCompatActivity implements Injectable,
        SubGroupAdapter.OnItemClickListener,
        BottomSheetOnOffDialogFragment.BottomSheetOnOffListener,
        BottomSheetLevelDialogFragment.BottomSheetLevelListener,
        BottomSheetLightHSLDialogFragment.BottomSheetHSLListener,
        BottomSheetVendorDialogFragment.BottomSheetVendorModelControlsListener,
        BottomSheetDetailsDialogFragment.BottomSheetDetailsListener {

    private static final String ON_OFF_FRAGMENT = "ON_OFF_FRAGMENT";
    private static final String LEVEL_FRAGMENT = "LEVEL_FRAGMENT";
    private static final String VENDOR_FRAGMENT = "VENDOR_FRAGMENT";
    private static final String DETAILS_FRAGMENT = "DETAILS_FRAGMENT";

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @BindView(R.id.container)
    CoordinatorLayout container;
    private GroupControlsViewModel mViewModel;
    private SubGroupAdapter groupAdapter;
    private boolean mIsConnected;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_groups);
        ButterKnife.bind(this);
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(GroupControlsViewModel.class);

        final Toolbar toolbar = findViewById(R.id.toolbar_info);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final View noModelsConfigured = findViewById(R.id.no_models_subscribed);
        final View noAppKeysBound = findViewById(R.id.no_app_keys);

        final RecyclerView recyclerViewSubGroups = findViewById(R.id.recycler_view_grouped_models);
        recyclerViewSubGroups.setLayoutManager(new LinearLayoutManager(this));
        groupAdapter = new SubGroupAdapter(this,
                mViewModel.getNetworkLiveData().getMeshNetwork(),
                mViewModel.getSelectedGroup());
        groupAdapter.setOnItemClickListener(this);
        recyclerViewSubGroups.setAdapter(groupAdapter);

        mViewModel.getSelectedGroup().observe(this, group -> {
            if (group != null) {
                getSupportActionBar().setTitle(group.getName());
                getSupportActionBar().setSubtitle(MeshAddress.formatAddress(group.getAddress(), true));
            }
        });

        mViewModel.getNetworkLiveData().observe(this, meshNetworkLiveData -> {
            if (groupAdapter.getModelCount() > 0) {
                noModelsConfigured.setVisibility(View.INVISIBLE);
                if (groupAdapter.getItemCount() > 0) {
                    noAppKeysBound.setVisibility(View.INVISIBLE);
                } else {
                    noAppKeysBound.setVisibility(View.VISIBLE);
                }
            } else {
                noModelsConfigured.setVisibility(View.VISIBLE);
                noAppKeysBound.setVisibility(View.INVISIBLE);
            }
            groupAdapter.updateAdapterData();
        });

        mViewModel.getSelectedModel().observe(this, meshModel -> {
            groupAdapter.updateAdapterData();
            final BottomSheetDetailsDialogFragment fragment = (BottomSheetDetailsDialogFragment) getSupportFragmentManager().findFragmentByTag(DETAILS_FRAGMENT);
            if (fragment != null) {
                final Group group = mViewModel.getSelectedGroup().getValue();
                final MeshNetwork meshNetwork = mViewModel.getNetworkLiveData().getMeshNetwork();
                final ArrayList<Element> elements = new ArrayList<>(meshNetwork.getElements(group));
                fragment.updateAdapter(group, elements);
            }
        });

        mViewModel.getMeshMessage().observe(this, meshMessage -> {
            if (meshMessage instanceof VendorModelMessageStatus) {
                final VendorModelMessageStatus status = (VendorModelMessageStatus) meshMessage;
                final BottomSheetVendorDialogFragment fragment = (BottomSheetVendorDialogFragment) getSupportFragmentManager().findFragmentByTag(VENDOR_FRAGMENT);
                if (fragment != null)
                    fragment.setReceivedMessage(status.getAccessPayload());
            }
        });

        mViewModel.isConnectedToProxy().observe(this, aBoolean -> {
            mIsConnected = aBoolean;
            groupAdapter.updateAdapterData();
            invalidateOptionsMenu();
        });

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (mViewModel.getNodes().getValue() != null && !mViewModel.getNodes().getValue().isEmpty()) {
            final Boolean isConnectedToNetwork = mViewModel.isConnectedToProxy().getValue();
            if (isConnectedToNetwork != null && isConnectedToNetwork) {
                getMenuInflater().inflate(R.menu.menu_group_controls_disconnect, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_group_controls_connect, menu);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_edit:
                editGroup();
                break;
            case R.id.action_connect:
                final Intent intent = new Intent(this, ScannerActivity.class);
                intent.putExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, false);
                startActivityForResult(intent, Utils.CONNECT_TO_NETWORK);
                return true;
            case R.id.action_disconnect:
                mViewModel.disconnect();
                return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSubGroupItemClick(final int appKeyIndex, final int modelId) {
        if(!isConnected())
            return;

        if (MeshParserUtils.isVendorModel(modelId)) {
            final BottomSheetVendorDialogFragment vendorFragment = BottomSheetVendorDialogFragment.getInstance(modelId, appKeyIndex);
            vendorFragment.show(getSupportFragmentManager(), VENDOR_FRAGMENT);
        } else {
            switch (modelId) {
                case SigModelParser.GENERIC_ON_OFF_SERVER:
                    final BottomSheetOnOffDialogFragment onOffFragment = BottomSheetOnOffDialogFragment.getInstance(appKeyIndex);
                    onOffFragment.show(getSupportFragmentManager(), ON_OFF_FRAGMENT);
                    break;
                case SigModelParser.GENERIC_LEVEL_SERVER:
                    final BottomSheetLevelDialogFragment levelFragment = BottomSheetLevelDialogFragment.getInstance(appKeyIndex);
                    levelFragment.show(getSupportFragmentManager(), LEVEL_FRAGMENT);
                    break;

                case SigModelParser.LIGHT_HSL_SERVER:
                    final BottomSheetLightHSLDialogFragment LightHSLFragment = BottomSheetLightHSLDialogFragment.getInstance(appKeyIndex);
                    LightHSLFragment.show(getSupportFragmentManager(), LEVEL_FRAGMENT);
                    break;
            }
        }
    }

    @Override
    public void toggle(final int appKeyIndex, final int modelId, final boolean isChecked) {
        if (!isConnected()) {
            return;
        }
        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null)
            return;

        final MeshMessage meshMessage;
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        final ApplicationKey applicationKey = network.getAppKey(appKeyIndex);
        final int tid = network.getSelectedProvisioner().getSequenceNumber();
        switch (modelId) {
            case SigModelParser.GENERIC_ON_OFF_SERVER:
                meshMessage = new GenericOnOffSetUnacknowledged(applicationKey, isChecked, tid);
                sendMessage(group.getAddress(), meshMessage);
                break;
            case SigModelParser.GENERIC_LEVEL_SERVER:
                meshMessage = new GenericLevelSetUnacknowledged(applicationKey, isChecked ? 32767 : -32768, tid);
                sendMessage(group.getAddress(), meshMessage);
                break;
        }
    }

    @Override
    public void toggle(final int keyIndex, final boolean state, final int transitionSteps, final int transitionStepResolution, final int delay) {
        if (!isConnected()) {
            return;
        }
        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null)
            return;

        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        final ApplicationKey applicationKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(keyIndex);
        final int tid = network.getSelectedProvisioner().getSequenceNumber();
        final MeshMessage meshMessage = new GenericOnOffSetUnacknowledged(applicationKey,
                state, tid, transitionSteps, transitionStepResolution, delay);
        sendMessage(group.getAddress(), meshMessage);
    }

    @Override
    public void toggleLevel(final int keyIndex, final int level, final int transitionSteps, final int transitionStepResolution, final int delay) {
        if (!isConnected()) {
            return;
        }
        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null)
            return;

        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (network != null) {
            final ApplicationKey applicationKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(keyIndex);
            final int tid = mViewModel.getNetworkLiveData().getMeshNetwork().getSelectedProvisioner().getSequenceNumber();
            final MeshMessage meshMessage = new GenericLevelSetUnacknowledged(applicationKey, transitionSteps, transitionStepResolution, delay, level, tid);
            sendMessage(group.getAddress(), meshMessage);
        }
    }

    @Override
    public void toggleLevel(int keyIndex, int hue, int sat, int lit) {
        if (!isConnected()) {
            return;
        }
        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null)
            return;

        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (network != null) {
            final ApplicationKey applicationKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(keyIndex);
            final int tid = mViewModel.getNetworkLiveData().getMeshNetwork().getSelectedProvisioner().getSequenceNumber();
            final MeshMessage meshMessage = new LightHslSetUnacknowledged(applicationKey, lit, hue, sat, tid);
            sendMessage(group.getAddress(), meshMessage);
        }
    }

    private void editGroup() {
        final Group group = mViewModel.getSelectedGroup().getValue();
        final MeshNetwork meshNetwork = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (meshNetwork != null) {
            final ArrayList<Element> elements = new ArrayList<>(meshNetwork.getElements(group));
            final BottomSheetDetailsDialogFragment onOffFragment = BottomSheetDetailsDialogFragment.getInstance(group, elements);
            onOffFragment.show(getSupportFragmentManager(), DETAILS_FRAGMENT);
        }
    }

    @Override
    public void editModelItem(@NonNull final Element element, @NonNull final MeshModel model) {
        final Boolean isConnectedToNetwork = mViewModel.isConnectedToProxy().getValue();
        if (isConnectedToNetwork != null && isConnectedToNetwork) {
            final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
            final ProvisionedMeshNode node = network.getNode(element.getElementAddress());
            if (node != null) {
                mViewModel.setSelectedMeshNode(node);
                mViewModel.setSelectedElement(element);
                mViewModel.setSelectedModel(model);
                mViewModel.navigateToModelActivity(this, model);
            }
        } else {
            Toast.makeText(this, R.string.disconnected_network_rationale, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGroupNameChanged(@NonNull final Group group) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        network.updateGroup(group);
    }

    @Override
    public void sendVendorModelMessage(final int modelId, final int keyIndex, final int opCode, final byte[] parameters, final boolean acknowledged) {
        final Group group = mViewModel.getSelectedGroup().getValue();
        if (group == null)
            return;

        final VendorModel model = getModel(modelId, keyIndex);
        if (model == null)
            return;

        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        if (network != null) {
            final ApplicationKey appKey = network.getAppKey(keyIndex);
            final MeshMessage message;
            if (acknowledged) {
                message = new VendorModelMessageAcked(appKey, modelId, model.getCompanyIdentifier(), opCode, parameters);
            } else {
                message = new VendorModelMessageUnacked(appKey, modelId, model.getCompanyIdentifier(), opCode, parameters);
            }
            sendMessage(group.getAddress(), message);
        }
    }

    private VendorModel getModel(final int modelId, final int appKeyIndex) {
        final List<MeshModel> models = groupAdapter.getModels();
        for (MeshModel model : models) {
            if (modelId == model.getModelId()) {
                if (model.getBoundAppKeyIndexes().contains(appKeyIndex)) {
                    return (VendorModel) model;
                }
            }
        }
        return null;
    }

    private void sendMessage(final int address, final MeshMessage meshMessage) {
        try {
            mViewModel.getMeshManagerApi().createMeshPdu(address, meshMessage);
        } catch (IllegalArgumentException ex) {
            final DialogFragmentError message = DialogFragmentError.
                    newInstance(getString(R.string.title_error), ex.getMessage());
            message.show(getSupportFragmentManager(), null);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isConnected() {
        if (!mIsConnected) {
            mViewModel.displaySnackBar(this, container, getString(R.string.please_connect_to_network), Snackbar.LENGTH_SHORT);
            return false;
        }
        return true;
    }
}
