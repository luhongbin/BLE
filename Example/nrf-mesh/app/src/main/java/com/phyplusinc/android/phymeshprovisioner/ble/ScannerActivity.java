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

package com.phyplusinc.android.phymeshprovisioner.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.phyplusinc.android.phymeshprovisioner.PhyProvisioningActivity;
import com.phyplusinc.android.phymeshprovisioner.ProvisioningActivity;
import com.phyplusinc.android.phymeshprovisioner.adapter.ExtendedBluetoothDevice;
import com.phyplusinc.android.phymeshprovisioner.ble.adapter.DevicesAdapter;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ScannerLiveData;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ScannerViewModel;

public class ScannerActivity extends AppCompatActivity implements Injectable,
        DevicesAdapter.OnItemClickListener {
    private static final int REQUEST_ENABLE_BLUETOOTH = 1021; // random number
    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022; // random number

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

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
    View mNoLocationView;//无节点视图
    @BindView(R.id.bluetooth_off)
    View mNoBluetoothView;

    private ScannerViewModel mViewModel;
    private boolean mScanWithProxyService;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);//写了这个就不用再 findViewById这些东西啦

        // Create view model containing utility methods for scanning
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(ScannerViewModel.class);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_scanner);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//显示返回箭头

        if (getIntent() != null) {
            mScanWithProxyService = getIntent().getBooleanExtra(Utils.EXTRA_DATA_PROVISIONING_SERVICE, true);
            if (mScanWithProxyService) {
                getSupportActionBar().setSubtitle(R.string.sub_title_scanning_nodes);
            } else {
                getSupportActionBar().setSubtitle(R.string.sub_title_scanning_proxy_node);
            }
        }

        // Configure the recycler view
        final RecyclerView recyclerViewDevices = findViewById(R.id.recycler_view_ble_devices);
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerViewDevices.getContext(), DividerItemDecoration.VERTICAL);
        recyclerViewDevices.addItemDecoration(dividerItemDecoration);

        final SimpleItemAnimator itemAnimator = (SimpleItemAnimator) recyclerViewDevices.getItemAnimator();
        if (itemAnimator != null) itemAnimator.setSupportsChangeAnimations(false);

        final DevicesAdapter adapter = new DevicesAdapter(this, mViewModel.getScannerRepository().getScannerState());
        adapter.setOnItemClickListener(this);//设置监听
        recyclerViewDevices.setAdapter(adapter);

        mViewModel.getScannerRepository().getScannerState().observe(this, this::startScan);//observe异步监视搜索数组发生的变化
    }

    @Override
    protected void onStart() {//onStart()是activity界面被显示出来的时候执行的
        super.onStart();
        mViewModel.getScannerRepository().getScannerState().startScanning();//引入扫描存储区/扫描实时数据/开始
    }

    @Override
    protected void onStop() {//用户按下了home键
        super.onStop();
        stopScan();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
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

    //点击搜索到的设备
    @Override
    public void onItemClick(final ExtendedBluetoothDevice device) {
        //We must disconnect from any nodes that we are connected to before we start scanning.
        mViewModel.disconnect();
        final Intent intent;
        stopScan();
        if (mScanWithProxyService) {//新设备

            // >>, ADD, PANDA, Phy Easy Binding
            boolean flagPhyBinding = false;
            final Map<ParcelUuid,byte[]> servData = device.getScanResult().getScanRecord().getServiceData();
            if (null != servData ) {//判断是否有Mesh服务
                final byte[] data = servData.get(new ParcelUuid(BleMeshManager.MESH_PROVISIONING_UUID));
                if (null != data && data[0] == 0x05 && data[1] == 0x04 && data[2] == 0x62 && data[3] == 0x12) {
                    flagPhyBinding = true;
                }
            }
            // <<<,
            intent = new Intent(this, flagPhyBinding? PhyProvisioningActivity.class: ProvisioningActivity.class);
            intent.putExtra(Utils.EXTRA_DEVICE, device);
            startActivityForResult(intent, Utils.PROVISIONING_SUCCESS);
        } else {//已经记忆的设备重新连接
            intent = new Intent(this, ReconnectActivity.class);
            intent.putExtra(Utils.EXTRA_DEVICE, device);//将设备传给新窗口
            startActivityForResult(intent, Utils.CONNECT_TO_NETWORK);//去另一个Activity返回时所带的数据
        }
    }

    @Override//定位权限
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            mViewModel.getScannerRepository().getScannerState().refresh();
        }
    }

    @OnClick(R.id.action_enable_location)//使能按钮
    public void onEnableLocationClicked() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @OnClick(R.id.action_enable_bluetooth)//蓝牙权限
    public void onEnableBluetoothClicked() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
    }

    @OnClick(R.id.action_grant_location_permission)//使能按钮
    public void onGrantLocationPermissionClicked() {
        Utils.markLocationPermissionRequested(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
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
            mNoLocationPermissionView.setVisibility(View.GONE);//8不可见

            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                mNoBluetoothView.setVisibility(View.GONE);//8不可见

                    // We are now OK to start scanning
                    List<UUID> fltr = new ArrayList<>();
                    if (mScanWithProxyService) { //搜索新的节点
                        fltr.add(BleMeshManager.MESH_PROVISIONING_UUID);//
                        mViewModel.getScannerRepository().startScan(fltr);
                    } else {
                        fltr.add(BleMeshManager.MESH_PROXY_UUID);
                        mViewModel.getScannerRepository().startScan(fltr);
                    }
                    mScanningView.setVisibility(View.VISIBLE);//搜索进度条显示

                    if (state.isEmpty()) {
                        mEmptyView.setVisibility(View.VISIBLE);//显示设备例表视图

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        mNoLocationView.setVisibility(View.INVISIBLE);
                    } else {
                        mNoLocationView.setVisibility(View.VISIBLE);//显示无节点视图
                    }
                } else {
                    mEmptyView.setVisibility(View.GONE);
                }
            } else {
                mNoBluetoothView.setVisibility(View.VISIBLE);
                mScanningView.setVisibility(View.INVISIBLE);
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            mNoLocationPermissionView.setVisibility(View.VISIBLE);
            mNoBluetoothView.setVisibility(View.GONE);
            mScanningView.setVisibility(View.INVISIBLE);
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
