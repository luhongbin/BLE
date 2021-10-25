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

import dagger.Subcomponent;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.AddAppKeyViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.AddKeysViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.AddNetKeyViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.AddProvisionerViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.AppKeysViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.EditAppKeyViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.EditNetKeyViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.EditProvisionerViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.GroupControlsViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ModelConfigurationViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.NetKeysViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.NodeConfigurationViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.NodeDetailsViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ProvisionersViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ProvisioningViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.PublicationViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.RangesViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ReconnectViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ScannerViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SharedViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SplashViewModel;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ViewModelFactory;

/**
 * A sub component to create ViewModels. It is called by the
 * {@link ViewModelFactory}. Using this component allows
 * ViewModels to define {@link javax.inject.Inject} constructors.
 */
@Subcomponent
public interface ViewModelSubComponent {
    @Subcomponent.Builder
    interface Builder {
        ViewModelSubComponent build();
    }

    SplashViewModel splashViewModel();

    SharedViewModel commonViewModel();

    ScannerViewModel scannerViewModel();

    GroupControlsViewModel groupControlsViewModel();

    ProvisionersViewModel provisionersViewModel();

    AddProvisionerViewModel addProvisionerViewModel();

    EditProvisionerViewModel editProvisionerViewModel();

    RangesViewModel rangesViewModel();

    NetKeysViewModel netKeysViewModel();

    AddNetKeyViewModel addNetKeyViewModel();

    EditNetKeyViewModel editNetKeyViewModel();

    AppKeysViewModel appKeysViewModel();

    AddAppKeyViewModel addAppKeyViewModel();

    EditAppKeyViewModel editAppKeyViewModel();

    ProvisioningViewModel meshProvisionerViewModel();

    NodeConfigurationViewModel nodeConfigurationViewModel();

    AddKeysViewModel addKeysViewModel();

    NodeDetailsViewModel nodeDetailsViewModel();

    ModelConfigurationViewModel modelConfigurationViewModel();

    PublicationViewModel publicationViewModel();

    ReconnectViewModel reconnectViewModule();
}
