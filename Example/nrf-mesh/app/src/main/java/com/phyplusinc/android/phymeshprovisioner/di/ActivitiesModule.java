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

package com.phyplusinc.android.phymeshprovisioner.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import no.nordicsemi.android.meshprovisioner.models.SensorServer;

import com.phyplusinc.android.phymeshprovisioner.GroupControlsActivity;
import com.phyplusinc.android.phymeshprovisioner.MainActivity;
import com.phyplusinc.android.phymeshprovisioner.PhyProvisioningActivity;
import com.phyplusinc.android.phymeshprovisioner.ProvisioningActivity;
import com.phyplusinc.android.phymeshprovisioner.SplashScreenActivity;
import com.phyplusinc.android.phymeshprovisioner.ble.OTAsScanActivity;
import com.phyplusinc.android.phymeshprovisioner.ble.ReconnectActivity;
import com.phyplusinc.android.phymeshprovisioner.ble.ScannerActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.AddAppKeyActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.AddAppKeysActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.AddNetKeyActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.AddNetKeysActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.AppKeysActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.EditAppKeyActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.EditNetKeyActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.NetKeysActivity;
import com.phyplusinc.android.phymeshprovisioner.node.ConfigurationClientActivity;
import com.phyplusinc.android.phymeshprovisioner.node.ConfigurationServerActivity;
import com.phyplusinc.android.phymeshprovisioner.node.GenericLevelServerActivity;
import com.phyplusinc.android.phymeshprovisioner.node.GenericOnOffServerActivity;
import com.phyplusinc.android.phymeshprovisioner.node.LightHslServerActivity;
import com.phyplusinc.android.phymeshprovisioner.node.ModelConfigurationActivity;
import com.phyplusinc.android.phymeshprovisioner.node.NodeConfigurationActivity;
import com.phyplusinc.android.phymeshprovisioner.node.NodeDetailsActivity;
import com.phyplusinc.android.phymeshprovisioner.node.PublicationSettingsActivity;
import com.phyplusinc.android.phymeshprovisioner.node.SensorServerActivity;
import com.phyplusinc.android.phymeshprovisioner.node.VendorModelActivity;
import com.phyplusinc.android.phymeshprovisioner.provisioners.AddProvisionerActivity;
import com.phyplusinc.android.phymeshprovisioner.provisioners.EditProvisionerActivity;
import com.phyplusinc.android.phymeshprovisioner.provisioners.ProvisionersActivity;
import com.phyplusinc.android.phymeshprovisioner.provisioners.RangesActivity;

@Module
abstract class ActivitiesModule {

    @ContributesAndroidInjector()
    abstract SplashScreenActivity contributeSplashScreenActivity();

    @ContributesAndroidInjector(modules = FragmentBuildersModule.class)
    abstract MainActivity contributeMainActivity();

    @ContributesAndroidInjector()
    abstract ProvisionersActivity contributeProvisionersActivity();

    @ContributesAndroidInjector()
    abstract AddProvisionerActivity contributeAddProvisionersActivity();

    @ContributesAndroidInjector()
    abstract EditProvisionerActivity contributeEditProvisionersActivity();

    @ContributesAndroidInjector()
    abstract RangesActivity contributeRangesActivity();

    @ContributesAndroidInjector()
    abstract NetKeysActivity contributeNetKeysActivity();

    @ContributesAndroidInjector()
    abstract AddNetKeyActivity contributeAddNetKeyActivity();

    @ContributesAndroidInjector()
    abstract EditNetKeyActivity contributeEditNetKeyActivity();

    @ContributesAndroidInjector()
    abstract AppKeysActivity contributeAppKeysActivity();

    @ContributesAndroidInjector()
    abstract AddAppKeyActivity contributeAddAppKeyActivity();

    @ContributesAndroidInjector()
    abstract EditAppKeyActivity contributeEditAppKeyActivity();

    @ContributesAndroidInjector()
    abstract ProvisioningActivity contributeMeshProvisionerActivity();

	/* PANDA */
	@ContributesAndroidInjector()
	abstract PhyProvisioningActivity contributeMeshPhyProvisionerActivity();

	/* PANDA */
    @ContributesAndroidInjector()
    abstract OTAsScanActivity contributeOTAsScanActivity();

    /* PANDA */
    @ContributesAndroidInjector()
    abstract LightHslServerActivity contributeLightHslServerActivity();

    @ContributesAndroidInjector()
    abstract SensorServerActivity contributeSensorServerActivity();
    // <<<

    @ContributesAndroidInjector()
    abstract NodeConfigurationActivity contributeElementConfigurationActivity();

    @ContributesAndroidInjector()
    abstract AddAppKeysActivity contributeAddAppKeysActivity();

    @ContributesAndroidInjector()
    abstract AddNetKeysActivity contributeAddNetKeysActivity();

    @ContributesAndroidInjector()
    abstract ScannerActivity contributeScannerActivity();



	@ContributesAndroidInjector()
    abstract ReconnectActivity contributeReconnectActivity();

    @ContributesAndroidInjector()
    abstract NodeDetailsActivity contributeNodeDetailsActivity();

    @ContributesAndroidInjector()
    abstract GroupControlsActivity contributeGroupControlsActivity();

    @ContributesAndroidInjector()
    abstract PublicationSettingsActivity contributePublicationSettingsActivity();

    @ContributesAndroidInjector()
    abstract ConfigurationServerActivity contributeConfigurationServerActivity();

    @ContributesAndroidInjector()
    abstract ConfigurationClientActivity contributeConfigurationClientActivity();

    @ContributesAndroidInjector()
    abstract GenericOnOffServerActivity contributeGenericOnOffServerActivity();

    @ContributesAndroidInjector()
    abstract GenericLevelServerActivity contributeGenericLevelServerActivity();

    @ContributesAndroidInjector()
    abstract VendorModelActivity contributeVendorModelActivity();

    @ContributesAndroidInjector()
    abstract ModelConfigurationActivity contributeModelConfigurationActivity();
}
