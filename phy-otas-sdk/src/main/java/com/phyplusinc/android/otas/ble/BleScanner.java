package com.phyplusinc.android.otas.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;

/**
 * Created by zhoululu on 2017/6/21.
 */

public class BleScanner {

    private MyLeScanCallback leScanCallback;
    private MyScanCallBack scanCallBack;
    private OTAUtilsCallBack otaUtilsCallBack;

    private Context context;

    public BleScanner(Context context,OTAUtilsCallBack otaUtilsCallBack) {
        this.context = context;
        this.otaUtilsCallBack = otaUtilsCallBack;
    }

    public void scanDevice(){
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){

            leScanCallback = new MyLeScanCallback();

            adapter.startLeScan(leScanCallback);
        }else {

            scanCallBack = new MyScanCallBack();

            adapter.getBluetoothLeScanner().startScan(scanCallBack);
        }
    }

    public void stopScanDevice(){
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if(adapter != null && adapter.isEnabled()){

            if(leScanCallback != null || scanCallBack != null){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    adapter.stopLeScan(leScanCallback);
                }else {
                    adapter.getBluetoothLeScanner().stopScan(scanCallBack);
                }
            }
        }
    }

    class MyLeScanCallback implements BluetoothAdapter.LeScanCallback{

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(otaUtilsCallBack != null){
                otaUtilsCallBack.onDeviceSearch(device,rssi,scanRecord);
            }else {
                throw  new RuntimeException("PHYBleCallBack is null");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    class MyScanCallBack extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(otaUtilsCallBack != null){
                otaUtilsCallBack.onDeviceSearch(result.getDevice(),result.getRssi(),result.getScanRecord().getBytes());
            }else {
                throw  new RuntimeException("PHYBleCallBack is null");
            }
        }
    }

}
