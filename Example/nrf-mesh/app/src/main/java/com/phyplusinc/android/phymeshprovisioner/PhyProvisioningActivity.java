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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.provisionerstates.ProvisioningCapabilities;
import no.nordicsemi.android.meshprovisioner.provisionerstates.ProvisioningFailedState;
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.utils.AuthenticationOOBMethods;
import no.nordicsemi.android.meshprovisioner.utils.InputOOBAction;
import no.nordicsemi.android.meshprovisioner.utils.OutputOOBAction;
import no.nordicsemi.android.meshprovisioner.utils.StaticOOBType;
import com.phyplusinc.android.phymeshprovisioner.adapter.ExtendedBluetoothDevice;
import com.phyplusinc.android.phymeshprovisioner.adapter.ProvisioningProgressAdapter;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentAuthenticationInput;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentConfigurationComplete;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentProvisioningFailedError;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentSelectOOBType;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentUnicastAddress;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.DialogFragmentNodeName;
import com.phyplusinc.android.phymeshprovisioner.utils.ProvisionerStates;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ProvisionerProgress;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ProvisioningViewModel;

public class PhyProvisioningActivity extends AppCompatActivity implements Injectable,
        DialogFragmentSelectOOBType.DialogFragmentSelectOOBTypeListener,
        DialogFragmentAuthenticationInput.ProvisionerInputFragmentListener,
        DialogFragmentNodeName.DialogFragmentNodeNameListener,
        DialogFragmentUnicastAddress.DialogFragmentUnicastAddressListener,
        DialogFragmentProvisioningFailedError.DialogFragmentProvisioningFailedErrorListener,
        DialogFragmentConfigurationComplete.ConfigurationCompleteListener {

    private static final String DIALOG_FRAGMENT_PROVISIONING_FAILED = "DIALOG_FRAGMENT_PROVISIONING_FAILED";
    private static final String DIALOG_FRAGMENT_AUTH_INPUT_TAG = "DIALOG_FRAGMENT_AUTH_INPUT_TAG";
    private static final String DIALOG_FRAGMENT_CONFIGURATION_STATUS = "DIALOG_FRAGMENT_CONFIGURATION_STATUS";

    private static final String TAG = PhyProvisioningActivity.class.getSimpleName();


    @BindView(R.id.container)
    CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.provisioning_progress_bar)
    ProgressBar mProvisioningProgressBar;
    @BindView(R.id.data_container)
    ScrollView container;
//    @BindView(R.id.capabilities_container)
//    View mCapabilitiesContainer;
    @BindView(R.id.info_provisioning_status_container)
    View provisioningStatusContainer;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private ProvisioningViewModel mViewModel;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesh_provisioner);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        final ExtendedBluetoothDevice device = intent.getParcelableExtra(Utils.EXTRA_DEVICE);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(ProvisioningViewModel.class);
        if (savedInstanceState == null)
            mViewModel.connect(this, device, false);

        // Set up views
        final LinearLayout connectivityProgressContainer = findViewById(R.id.connectivity_progress_container);
        final TextView connectionState = findViewById(R.id.connection_state);

        container.setVisibility(View.GONE);

        mViewModel.getConnectionState().observe(this, connectionState::setText);

        mViewModel.isConnected().observe(this, connected -> {
            final boolean isComplete = mViewModel.isProvisioningComplete();
            if (isComplete) {
                return;
            }

            if (connected != null && !connected)
                finish();
        });

        mViewModel.isDeviceReady().observe(this, deviceReady -> {
            if (mViewModel.getBleMeshManager().isDeviceReady()) {
                connectivityProgressContainer.setVisibility(View.GONE);
                final boolean isComplete = mViewModel.isProvisioningComplete();
                if (isComplete) {
                    mProvisioningProgressBar.setVisibility(View.VISIBLE);
                    provisioningStatusContainer.setVisibility(View.VISIBLE);
                    setupProvisionerStateObservers(provisioningStatusContainer);
                    return;
                }

                // >>>, ADD, PANDA, Send Invite
                final UnprovisionedMeshNode node = mViewModel.getUnprovisionedMeshNode().getValue();
                if (node == null) {
                    device.setName(mViewModel.getNetworkLiveData().getNodeName());
                    mViewModel.getNrfMeshRepository().identifyNode(device);
                    return;
                }
                // <<<,
            }
        });

        mViewModel.isReconnecting().observe(this, isReconnecting -> {
            if (isReconnecting != null && isReconnecting) {
                mViewModel.getUnprovisionedMeshNode().removeObservers(this);
                provisioningStatusContainer.setVisibility(View.GONE);
                connectivityProgressContainer.setVisibility(View.VISIBLE);
            } else {
                setResultIntent();
            }
        });

        mViewModel.getUnprovisionedMeshNode().observe(this, meshNode -> {
            if (meshNode != null) {
                final ProvisioningCapabilities capabilities = meshNode.getProvisioningCapabilities();
                if (capabilities != null) {
                    mProvisioningProgressBar.setVisibility(View.INVISIBLE);
                    final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
                    if (network != null) {
                        try {
                            final int elementCount = capabilities.getNumberOfElements();
                            final Provisioner provisioner = network.getSelectedProvisioner();
                            final int unicast = network.nextAvailableUnicastAddress(elementCount, provisioner);
                            network.assignUnicastAddress(unicast);
                        } catch (IllegalArgumentException ex) {
                            mViewModel.displaySnackBar(this, mCoordinatorLayout, ex.getMessage(), Snackbar.LENGTH_LONG);
                        }
                    }

                    // >>>, ADD, PANDA, OOB
                    if (capabilities.getAvailableOOBTypes().size() == 1 && capabilities.getAvailableOOBTypes().get(0) == AuthenticationOOBMethods.NO_OOB_AUTHENTICATION) {
                        onNoOOBSelected();
                    } else {
                        final DialogFragmentSelectOOBType fragmentSelectOOBType = DialogFragmentSelectOOBType.newInstance(meshNode.getProvisioningCapabilities());
                        fragmentSelectOOBType.show(getSupportFragmentManager(), null);
                    }

                    mViewModel.getUnprovisionedMeshNode().removeObservers(this);
                    // <<<,
                }
            }
        });

        if (savedInstanceState == null)
            mViewModel.getNetworkLiveData().resetSelectedAppKey();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //We disconnect from the device if the user presses the back button
        disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPinInputComplete(final String pin) {
        mViewModel.getMeshManagerApi().setProvisioningAuthentication(pin);
    }

    @Override
    public void onPinInputCanceled() {
        final String message = getString(R.string.provisioning_cancelled);
        final Snackbar snackbar = Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
        disconnect();
    }

    @Override
    public boolean onNodeNameUpdated(@NonNull final String nodeName) {
        mViewModel.getNetworkLiveData().setNodeName(nodeName);
        return true;
    }

    @Override
    public boolean setUnicastAddress(final int unicastAddress) {
        final MeshNetwork network = mViewModel.getMeshManagerApi().getMeshNetwork();
        if (network != null) {
            return network.assignUnicastAddress(unicastAddress);
        }
        return false;
    }

    @Override
    public int getNextUnicastAddress(final int elementCount) {
        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
        return network.nextAvailableUnicastAddress(elementCount, network.getSelectedProvisioner());
    }

    @Override
    public void onProvisioningFailed() {
        //Provisioning failed so now we go back to the scanner page.
        disconnect();
        setResultIntent();
    }

    private void disconnect() {
        mViewModel.getUnprovisionedMeshNode().removeObservers(this);
        mViewModel.disconnect();
    }

    public void setupProvisionerStateObservers(final View provisioningStatusContainer) {
        provisioningStatusContainer.setVisibility(View.VISIBLE);

        final RecyclerView recyclerView = provisioningStatusContainer.findViewById(R.id.recycler_view_provisioning_progress);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ProvisioningProgressAdapter adapter = new ProvisioningProgressAdapter(this, mViewModel.getProvisioningStatus());
        recyclerView.setAdapter(adapter);

        mViewModel.getProvisioningStatus().observe(this, provisioningStateLiveData -> {
            if (provisioningStateLiveData != null) {
                final ProvisionerProgress provisionerProgress = provisioningStateLiveData.getProvisionerProgress();
                adapter.refresh(provisioningStateLiveData.getStateList());
                if (provisionerProgress != null) {
                    final ProvisionerStates state = provisionerProgress.getState();
                    switch (state) {
                        case PROVISIONING_FAILED:
                            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_PROVISIONING_FAILED) == null) {
                                final String statusMessage = ProvisioningFailedState.parseProvisioningFailure(getApplicationContext(), provisionerProgress.getStatusReceived());
                                DialogFragmentProvisioningFailedError message = DialogFragmentProvisioningFailedError.newInstance(getString(R.string.title_error_provisioning_failed), statusMessage);
                                message.show(getSupportFragmentManager(), DIALOG_FRAGMENT_PROVISIONING_FAILED);
                            }
                            break;
                        case PROVISIONING_AUTHENTICATION_STATIC_OOB_WAITING:
                            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_AUTH_INPUT_TAG) == null) {
                                DialogFragmentAuthenticationInput dialogFragmentAuthenticationInput = DialogFragmentAuthenticationInput.
                                        newInstance(mViewModel.getUnprovisionedMeshNode().getValue());
                                dialogFragmentAuthenticationInput.show(getSupportFragmentManager(), DIALOG_FRAGMENT_AUTH_INPUT_TAG);
                            }
                            break;
                        case PROVISIONING_AUTHENTICATION_OUTPUT_OOB_WAITING:
                            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_AUTH_INPUT_TAG) == null) {
                                DialogFragmentAuthenticationInput dialogFragmentAuthenticationInput = DialogFragmentAuthenticationInput.
                                        newInstance(mViewModel.getUnprovisionedMeshNode().getValue());
                                dialogFragmentAuthenticationInput.show(getSupportFragmentManager(), DIALOG_FRAGMENT_AUTH_INPUT_TAG);
                            }
                            break;
                        case PROVISIONING_AUTHENTICATION_INPUT_OOB_WAITING:
                            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_AUTH_INPUT_TAG) == null) {
                                DialogFragmentAuthenticationInput dialogFragmentAuthenticationInput = DialogFragmentAuthenticationInput.
                                        newInstance(mViewModel.getUnprovisionedMeshNode().getValue());
                                dialogFragmentAuthenticationInput.show(getSupportFragmentManager(), DIALOG_FRAGMENT_AUTH_INPUT_TAG);
                            }
                            break;
                        case PROVISIONING_AUTHENTICATION_INPUT_ENTERED:
                            final DialogFragmentAuthenticationInput fragment = (DialogFragmentAuthenticationInput) getSupportFragmentManager().
                                    findFragmentByTag(DIALOG_FRAGMENT_AUTH_INPUT_TAG);
                            if (fragment != null)
                                fragment.dismiss();
                            break;
                        // >>>, ADD, PANDA, Provisioning Completed Now for PHY+
                        case APP_KEY_STATUS_RECEIVED:
                        // case NETWORK_TRANSMIT_STATUS_RECEIVED:
                        // <<<,
                            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_CONFIGURATION_STATUS) == null) {
                                DialogFragmentConfigurationComplete fragmentConfigComplete = DialogFragmentConfigurationComplete.
                                        newInstance(getString(R.string.title_configuration_compete), getString(R.string.configuration_complete_summary));
                                fragmentConfigComplete.show(getSupportFragmentManager(), DIALOG_FRAGMENT_CONFIGURATION_STATUS);
                            }
                            break;
                        case PROVISIONER_UNASSIGNED:
                            setResultIntent();
                            break;
                        default:
                            break;
                    }

                }
                container.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void onConfigurationCompleted() {
        setResultIntent();
    }

    private void setResultIntent() {
        final Intent returnIntent = new Intent();
        // >>>, PANDA, ADD, for auto jump to publish/subscription activity
        returnIntent.putExtra(Utils.EXTRA_DATA, mViewModel.getProvMeshNode().getValue());
        // <<<,
        if (mViewModel.isProvisioningComplete()) {
            returnIntent.putExtra(Utils.PROVISIONING_COMPLETED, true);
            setResult(Activity.RESULT_OK, returnIntent);
            final ProvisionerProgress progress = mViewModel.getProvisioningStatus().getProvisionerProgress();
            if (progress.getState() == ProvisionerStates.PROVISIONER_UNASSIGNED) {
                returnIntent.putExtra(Utils.PROVISIONER_UNASSIGNED, true);
            } else {
                if (mViewModel.isCompositionDataStatusReceived()) {
                    returnIntent.putExtra(Utils.COMPOSITION_DATA_COMPLETED, true);
                    if (mViewModel.isAppKeyAddCompleted()) {
                        returnIntent.putExtra(Utils.APP_KEY_ADD_COMPLETED, true);
                        if (mViewModel.isNetworkRetransmitSetCompleted()) {
                            returnIntent.putExtra(Utils.NETWORK_TRANSMIT_SET_COMPLETED, true);
                        }
                    }
                }
            }
        }
        finish();
    }

    @Override
    public void onNoOOBSelected() {
        final UnprovisionedMeshNode node = mViewModel.getUnprovisionedMeshNode().getValue();
        if (node != null) {
            try {
                node.setNodeName(mViewModel.getNetworkLiveData().getNodeName());
                setupProvisionerStateObservers(provisioningStatusContainer);
                mProvisioningProgressBar.setVisibility(View.VISIBLE);
                mViewModel.getMeshManagerApi().startProvisioning(node);
            } catch (IllegalArgumentException ex) {
                mViewModel.displaySnackBar(this, mCoordinatorLayout, ex.getMessage(), Snackbar.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onStaticOOBSelected(final StaticOOBType staticOOBType) {
        final UnprovisionedMeshNode node = mViewModel.getUnprovisionedMeshNode().getValue();
        if (node != null) {
            try {
                node.setNodeName(mViewModel.getNetworkLiveData().getNodeName());
                setupProvisionerStateObservers(provisioningStatusContainer);
                mProvisioningProgressBar.setVisibility(View.VISIBLE);
                mViewModel.getMeshManagerApi().startProvisioningWithStaticOOB(node);
            } catch (IllegalArgumentException ex) {
                mViewModel.displaySnackBar(this, mCoordinatorLayout, ex.getMessage(), Snackbar.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onOutputOOBActionSelected(final OutputOOBAction action) {
        final UnprovisionedMeshNode node = mViewModel.getUnprovisionedMeshNode().getValue();
        if (node != null) {
            try {
                node.setNodeName(mViewModel.getNetworkLiveData().getNodeName());
                setupProvisionerStateObservers(provisioningStatusContainer);
                mProvisioningProgressBar.setVisibility(View.VISIBLE);
                mViewModel.getMeshManagerApi().startProvisioningWithOutputOOB(node, action);
            } catch (IllegalArgumentException ex) {
                mViewModel.displaySnackBar(this, mCoordinatorLayout, ex.getMessage(), Snackbar.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onInputOOBActionSelected(final InputOOBAction action) {
        final UnprovisionedMeshNode node = mViewModel.getUnprovisionedMeshNode().getValue();
        if (node != null) {
            try {
                node.setNodeName(mViewModel.getNetworkLiveData().getNodeName());
                setupProvisionerStateObservers(provisioningStatusContainer);
                mProvisioningProgressBar.setVisibility(View.VISIBLE);
                mViewModel.getMeshManagerApi().startProvisioningWithInputOOB(node, action);
            } catch (IllegalArgumentException ex) {
                mViewModel.displaySnackBar(this, mCoordinatorLayout, ex.getMessage(), Snackbar.LENGTH_LONG);
            }
        }
    }
}
