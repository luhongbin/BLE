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

package com.phyplusinc.android.phymeshprovisioner.provisioners;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentError;
import com.phyplusinc.android.phymeshprovisioner.provisioners.dialogs.DialogFragmentProvisionerAddress;
import com.phyplusinc.android.phymeshprovisioner.provisioners.dialogs.DialogFragmentProvisionerName;
import com.phyplusinc.android.phymeshprovisioner.provisioners.dialogs.DialogFragmentTtl;
import com.phyplusinc.android.phymeshprovisioner.provisioners.dialogs.DialogFragmentUnassign;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.EditProvisionerViewModel;
import com.phyplusinc.android.phymeshprovisioner.widgets.RangeView;

public class EditProvisionerActivity extends AppCompatActivity implements Injectable,
        DialogFragmentProvisionerName.DialogFragmentProvisionerNameListener,
        DialogFragmentTtl.DialogFragmentTtlListener,
        DialogFragmentProvisionerAddress.ProvisionerAddressListener,
        DialogFragmentUnassign.DialogFragmentUnassignListener {

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private TextView provisionerName;
    private TextView provisionerUnicast;
    private TextView provisionerTtl;
    private RangeView unicastRangeView;
    private RangeView groupRangeView;
    private RangeView sceneRangeView;
    private View selectProvisioner;
    private CheckBox checkBox;


    private EditProvisionerViewModel mViewModel;
    private Provisioner mProvisioner;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_provisioner);
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(EditProvisionerViewModel.class);

        //Bind ui
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(R.string.title_edit_provisioner);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View containerProvisionerName = findViewById(R.id.container_name);
        containerProvisionerName.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_label_black_alpha_24dp));
        ((TextView) containerProvisionerName.findViewById(R.id.title)).setText(R.string.name);
        provisionerName = containerProvisionerName.findViewById(R.id.text);
        provisionerName.setVisibility(View.VISIBLE);

        final View containerUnicast = findViewById(R.id.container_unicast);
        containerUnicast.setClickable(false);
        containerUnicast.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_index));
        ((TextView) containerUnicast.findViewById(R.id.title)).setText(R.string.title_unicast_address);
        provisionerUnicast = containerUnicast.findViewById(R.id.text);
        provisionerUnicast.setVisibility(View.VISIBLE);

        final View containerTtl = findViewById(R.id.container_ttl);
        containerTtl.setClickable(false);
        containerTtl.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_timer));
        ((TextView) containerTtl.findViewById(R.id.title)).setText(R.string.title_ttl);
        provisionerTtl = containerTtl.findViewById(R.id.text);
        provisionerTtl.setVisibility(View.VISIBLE);

        selectProvisioner = findViewById(R.id.select_provisioner_container);
        checkBox = findViewById(R.id.check_provisioner);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mProvisioner != null) {
                mProvisioner.setLastSelected(isChecked);
            }
        });

        final View containerUnicastRange = findViewById(R.id.container_unicast_range);
        containerUnicastRange.setClickable(false);
        containerUnicastRange.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_lan_black_alpha_24dp));
        ((TextView) containerUnicastRange.findViewById(R.id.title)).setText(R.string.title_unicast_addresses);
        unicastRangeView = containerUnicastRange.findViewById(R.id.range_view);

        final View containerGroupRange = findViewById(R.id.container_group_range);
        containerGroupRange.setClickable(false);
        containerGroupRange.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_group_black_24dp_alpha));
        ((TextView) containerGroupRange.findViewById(R.id.title)).setText(R.string.title_group_addresses);
        groupRangeView = containerGroupRange.findViewById(R.id.range_view);

        final View containerSceneRange = findViewById(R.id.container_scene_range);
        containerSceneRange.setClickable(false);
        containerSceneRange.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_scene_black_alpha_24dp));
        ((TextView) containerSceneRange.findViewById(R.id.title)).setText(R.string.title_scenes);
        sceneRangeView = containerSceneRange.findViewById(R.id.range_view);

        containerProvisionerName.setOnClickListener(v -> {
            if (mProvisioner != null) {
                final DialogFragmentProvisionerName fragment = DialogFragmentProvisionerName.newInstance(mProvisioner.getProvisionerName());
                fragment.show(getSupportFragmentManager(), null);
            }
        });

        containerUnicast.setOnClickListener(v -> {
            if (mProvisioner != null) {
                final DialogFragmentProvisionerAddress fragment = DialogFragmentProvisionerAddress.newInstance(mProvisioner.getProvisionerAddress());
                fragment.show(getSupportFragmentManager(), null);
            }
        });

        containerTtl.setOnClickListener(v -> {
            if (mProvisioner != null) {
                final DialogFragmentTtl fragment = DialogFragmentTtl.newInstance(mProvisioner.getGlobalTtl());
                fragment.show(getSupportFragmentManager(), null);
            }
        });

        containerUnicastRange.setOnClickListener(v -> {
            if (mProvisioner != null) {
                mViewModel.setSelectedProvisioner(mProvisioner);
                final Intent intent = new Intent(this, RangesActivity.class);
                intent.putExtra(Utils.RANGE_TYPE, Utils.UNICAST_RANGE);
                startActivity(intent);
            }
        });

        containerGroupRange.setOnClickListener(v -> {
            if (mProvisioner != null) {
                mViewModel.setSelectedProvisioner(mProvisioner);
                final Intent intent = new Intent(this, RangesActivity.class);
                intent.putExtra(Utils.RANGE_TYPE, Utils.GROUP_RANGE);
                startActivity(intent);
            }
        });

        containerSceneRange.setOnClickListener(v -> {
            if (mProvisioner != null) {
                mViewModel.setSelectedProvisioner(mProvisioner);
                final Intent intent = new Intent(this, RangesActivity.class);
                intent.putExtra(Utils.RANGE_TYPE, Utils.SCENE_RANGE);
                startActivity(intent);
            }
        });

        mViewModel.getSelectedProvisioner().observe(this, provisioner -> {
            if (provisioner != null) {
                mProvisioner = provisioner;
                updateUi(provisioner);
            }
        });
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
        if (save()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNameChanged(@NonNull final String name) {
        if (mProvisioner != null) {
            mProvisioner.setProvisionerName(name);
            final Provisioner provisioner = mProvisioner;
            if (save(provisioner)) {
                provisionerName.setText(mProvisioner.getProvisionerName());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setAddress(final int sourceAddress) {
        if (mProvisioner != null) {
            if (mProvisioner.assignProvisionerAddress(sourceAddress)) {
                final Provisioner provisioner = mProvisioner;
                if (save(provisioner)) {
                    provisionerUnicast.setText(MeshAddress.formatAddress(sourceAddress, true));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void unassignProvisioner() {
        if (mProvisioner != null) {
            final DialogFragmentUnassign fragmentUnassign = DialogFragmentUnassign
                    .newInstance(getString(R.string.title_unassign_provisioner), getString(R.string.summary_unassign_provisioner));
            fragmentUnassign.show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onProvisionerUnassigned() {
        if (mProvisioner != null) {
            final MeshNetwork network = mViewModel.getMeshManagerApi().getMeshNetwork();
            if (network != null) {
                provisionerUnicast.setText(R.string.unicast_address_unassigned);
                mProvisioner.assignProvisionerAddress(null);
                network.disableConfigurationCapabilities(mProvisioner);
            }
        }
    }

    @Override
    public boolean setDefaultTtl(final int ttl) {
        if (mProvisioner != null) {
            mProvisioner.setGlobalTtl(ttl);
            final Provisioner provisioner = mProvisioner;
            if (save(provisioner)) {
                provisionerTtl.setText(String.valueOf(ttl));
            }
            return true;
        }
        return false;
    }

    private void updateUi(@NonNull final Provisioner provisioner) {
        provisionerName.setText(provisioner.getProvisionerName());
        if (provisioner.getProvisionerAddress() == null) {
            provisionerUnicast.setText(R.string.unicast_address_unassigned);
        } else {
            provisionerUnicast.setText(MeshAddress.formatAddress(provisioner.getProvisionerAddress(), true));
        }
        if (provisioner.isLastSelected()) {
            selectProvisioner.setVisibility(View.GONE);
        } else {
            checkBox.setChecked(provisioner.isLastSelected());
        }
        unicastRangeView.clearRanges();
        groupRangeView.clearRanges();
        sceneRangeView.clearRanges();
        unicastRangeView.addRanges(provisioner.getAllocatedUnicastRanges());
        groupRangeView.addRanges(provisioner.getAllocatedGroupRanges());
        sceneRangeView.addRanges(provisioner.getAllocatedSceneRanges());

        final MeshNetwork network = mViewModel.getMeshManagerApi().getMeshNetwork();
        if (network != null) {
            final String ttl = String.valueOf(provisioner.getGlobalTtl());
            provisionerTtl.setText(ttl);

            unicastRangeView.clearOtherRanges();
            groupRangeView.clearOtherRanges();
            sceneRangeView.clearOtherRanges();
            for (Provisioner other : network.getProvisioners()) {
                if (!other.getProvisionerUuid().equalsIgnoreCase(provisioner.getProvisionerUuid())) {
                    unicastRangeView.addOtherRanges(other.getAllocatedUnicastRanges());
                    groupRangeView.addOtherRanges(other.getAllocatedGroupRanges());
                    sceneRangeView.addOtherRanges(other.getAllocatedSceneRanges());
                }
            }
        }
    }

    private boolean save() {
        final MeshNetwork network = mViewModel.getMeshManagerApi().getMeshNetwork();
        if (network != null) {
            try {
                return network.updateProvisioner(mProvisioner);
            } catch (IllegalArgumentException ex) {
                final DialogFragmentError fragment = DialogFragmentError.
                        newInstance(getString(R.string.title_error), ex.getMessage());
                fragment.show(getSupportFragmentManager(), null);
            }
        }
        return false;
    }

    private boolean save(@NonNull final Provisioner provisioner) {
        final MeshNetwork network = mViewModel.getMeshManagerApi().getMeshNetwork();
        if (network != null) {
            return network.updateProvisioner(provisioner);
        }
        return false;
    }
}
