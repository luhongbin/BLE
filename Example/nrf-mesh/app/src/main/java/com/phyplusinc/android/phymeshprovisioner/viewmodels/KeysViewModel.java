package com.phyplusinc.android.phymeshprovisioner.viewmodels;

import androidx.annotation.NonNull;
import com.phyplusinc.android.phymeshprovisioner.keys.AppKeysActivity;
import com.phyplusinc.android.phymeshprovisioner.keys.NetKeysActivity;

/**
 * ViewModel for {@link NetKeysActivity}, {@link AppKeysActivity}
 */
abstract class KeysViewModel extends BaseViewModel {

    KeysViewModel(@NonNull final NrfMeshRepository nrfMeshRepository) {
        super(nrfMeshRepository);
    }
}
