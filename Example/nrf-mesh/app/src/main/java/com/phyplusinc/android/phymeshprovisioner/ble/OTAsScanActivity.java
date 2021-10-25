package com.phyplusinc.android.phymeshprovisioner.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.adapter.ExtendedBluetoothDevice;
import com.phyplusinc.android.phymeshprovisioner.ble.adapter.FltrDeviAdpr;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ScannerLiveData;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ScannerViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OTAsScanActivity extends AppCompatActivity implements Injectable,
        FltrDeviAdpr.OnItemClickListener {
    private static final int REQUEST_ENABLE_BLUETOOTH = 1021; // random number
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 1022; // random number

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    @BindView(R.id.nsv_phy_devi)
    View mDevicesView;

    @BindView(R.id.state_scanning)
    View mScanningView;
    @BindView(R.id.no_devices)
    View mEmptyView;
    @BindView(R.id.no_location_permission)
    View mNoLocationPermissionView;
    @BindView(R.id.action_grant_location_permission)
    Button mGrantPermissionButton;
    @BindView(R.id.action_permission_settings)
    Button mPermissionSettingsButton;
    @BindView(R.id.no_location)
    View mNoLocationView;
    @BindView(R.id.bluetooth_off)
    View mNoBluetoothView;

    private ScannerViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otas_scan);
        ButterKnife.bind(this);

        // Create view model containing utility methods for scanning
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(ScannerViewModel.class);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_scanner);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setSubtitle(R.string.sub_title_otas_scan);

        // Configure the recycler view
        final RecyclerView provDevices = findViewById(R.id.rv_prov_node);
        provDevices.setLayoutManager(new LinearLayoutManager(this));
        final DividerItemDecoration decoProv = new DividerItemDecoration(provDevices.getContext(), DividerItemDecoration.VERTICAL);
        provDevices.addItemDecoration(decoProv);

        final SimpleItemAnimator animProv = (SimpleItemAnimator) provDevices.getItemAnimator();
        if (animProv != null) animProv.setSupportsChangeAnimations(false);

        final FltrDeviAdpr adptProv = new FltrDeviAdpr(this, mViewModel.getScannerRepository().getScannerState(), BleMeshManager.MESH_PROXY_UUID);
        adptProv.setOnItemClickListener(this);
        provDevices.setAdapter(adptProv);

        final RecyclerView uprvDevi = findViewById(R.id.rv_uprv_node);
        uprvDevi.setLayoutManager(new LinearLayoutManager(this));
        final DividerItemDecoration decoUprv = new DividerItemDecoration(uprvDevi.getContext(), DividerItemDecoration.VERTICAL);
        uprvDevi.addItemDecoration(decoUprv);

        final SimpleItemAnimator animUprv = (SimpleItemAnimator) uprvDevi.getItemAnimator();
        if (animUprv != null) animUprv.setSupportsChangeAnimations(false);

        final FltrDeviAdpr adptUprov = new FltrDeviAdpr(this, mViewModel.getScannerRepository().getScannerState(), BleMeshManager.MESH_PROVISIONING_UUID);
        adptUprov.setOnItemClickListener(this);
        uprvDevi.setAdapter(adptUprov);

        mViewModel.getScannerRepository().getScannerState().observe(this, this::startScan);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.getScannerRepository().getScannerState().startScanning();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
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
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ReconnectActivity.REQUEST_DEVICE_READY) {
            if (resultCode == RESULT_OK) {
                final boolean isDeviceReady = data.getBooleanExtra(Utils.ACTIVITY_RESULT, false);
                if (isDeviceReady) {
                    finish();
                }
            }
        } else if (requestCode == Utils.PROVISIONING_SUCCESS) {
            if (resultCode == RESULT_OK) {
                setResultIntent(data);
            }
        } else if (requestCode == Utils.CONNECT_TO_NETWORK) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        } else if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                startScan(mViewModel.getScannerRepository().getScannerState());
            }
        }
    }

    @Override
    public void onItemClick(final ExtendedBluetoothDevice device) {
        //We must disconnect from any nodes that we are connected to before we start scanning.
        stopScan();
        mViewModel.disconnect();

        final Intent intent;
        intent = new Intent();
        intent.putExtra(Utils.EXTRA_DEVICE, device);
        setResultIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION) {
            mViewModel.getScannerRepository().getScannerState().refresh();
        }
    }

    @OnClick(R.id.action_enable_location)
    public void onEnableLocationClicked() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @OnClick(R.id.action_enable_bluetooth)
    public void onEnableBluetoothClicked() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
    }

    @OnClick(R.id.action_grant_location_permission)
    public void onGrantLocationPermissionClicked() {
        Utils.markLocationPermissionRequested(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
    }

    @OnClick(R.id.action_permission_settings)
    public void onPermissionSettingsClicked() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private void startScan(final ScannerLiveData state) {
        // First, check the Location permission. This is required on Marshmallow onwards in order to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(this)) {
            mNoLocationPermissionView.setVisibility(View.GONE);

            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                mNoBluetoothView.setVisibility(View.GONE);

                // We are now OK to start scanning
                final List<UUID> fltr = new ArrayList<>();
                fltr.add(BleMeshManager.MESH_PROVISIONING_UUID); fltr.add(BleMeshManager.MESH_PROXY_UUID);
                mViewModel.getScannerRepository().startScan(fltr);
                mScanningView.setVisibility(View.VISIBLE);

                if (state.isEmpty()) {
                    mEmptyView.setVisibility(View.VISIBLE);

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        mNoLocationView.setVisibility(View.INVISIBLE);
                    } else {
                        mNoLocationView.setVisibility(View.VISIBLE);
                    }
                } else {
                    mDevicesView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            } else {
                mNoBluetoothView.setVisibility(View.VISIBLE);
                mScanningView.setVisibility(View.INVISIBLE);
                mDevicesView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            mNoLocationPermissionView.setVisibility(View.VISIBLE);
            mScanningView.setVisibility(View.INVISIBLE);
            mNoBluetoothView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            mGrantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            mPermissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private void stopScan() {
        mViewModel.getScannerRepository().stopScan();
    }

    private void setResultIntent(final Intent data) {
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
