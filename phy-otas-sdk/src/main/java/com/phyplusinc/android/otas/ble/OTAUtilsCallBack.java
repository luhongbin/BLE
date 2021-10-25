package com.phyplusinc.android.otas.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * OTAUtilsCallBack
 *
 * @author:zhoululu
 * @date:2018/7/13
 */

public interface OTAUtilsCallBack {

    public void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord);
    public void onConnectChange(boolean isConnected);
    public void onProcess(float process);
    public void onError(int code);
    public void onOTAFinish();
    public void onResourceFinish();
    public void onReBootSuccess();

    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status);
    //public void onMtuChanged(BluetoothGatt gatt, int mtu, int status);
}
