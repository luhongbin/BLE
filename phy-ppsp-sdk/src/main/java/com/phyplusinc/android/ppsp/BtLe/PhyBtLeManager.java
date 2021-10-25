package com.phyplusinc.android.ppsp.BtLe;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.UUID;


public abstract class PhyBtLeManager<E extends PhyBtLeManager.PhyBtLeCallBack> {
    private final String TAG = getClass().getSimpleName();

    public static final int     MTU_SIZE_MIN = 23;
    private static final int    MTU_SIZE_MAX = 517;

    private int     mConnAtmp;  //


    public interface PhyBtLeCallBack {
        void onScan();
        void onConn();
        void onDisc();
        void onEnum();
    }

    private boolean mUserConn = false;
    private boolean mUserDisc = false;
    private int     mMTUs = MTU_SIZE_MIN;

    private Context             mCtx;
    private PhyScanCallback mNewLeScanCallback;
    private LeScanCallback  mOldLeScanCallback;
    private BluetoothGatt   mBluetoothGatt;
    private E               mBtApplGattCallback;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected /*abstract*/ class PhyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult: >>>>>>>>>>>>");
            getIntrGattCallback().onScan(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
        }
    }

    protected /*abstract*/ class LeScanCallback implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes /*scan record*/) {
            Log.d(TAG, "onLeScan: >>>>>>>>>>>>>>>>");
            getIntrGattCallback().onScan(bluetoothDevice, rssi, bytes);
        }
    }

    protected abstract class BtIntrGattCallback extends BluetoothGattCallback {
        public abstract void onScan(BluetoothDevice devi, int rssi, byte[] scan);
        public abstract void onConn(BluetoothDevice devi, boolean user);
        public abstract void onDisc(BluetoothDevice devi, boolean user);
        public abstract void onEnum(BluetoothGatt gatt);
        public abstract void onCharWrit(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);
        public abstract void onCharNtfy(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
        public abstract void OnDescWrit(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);

        @Override
        public final void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.d(TAG, "onConnectionStateChange: " + String.format("devi:%s,stat:%d>>%d",gatt.getDevice().getAddress(),status,newState));

            if ( BluetoothProfile.STATE_CONNECTED ==    newState ) {
                mUserDisc = false;

                getIntrGattCallback().onConn(gatt.getDevice(), mUserConn);
                // getApplGattCallback().onConn();

                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                    gatt.requestMtu(MTU_SIZE_MAX);
                } else {
                    gatt.discoverServices();
                }
            } else
            if ( BluetoothProfile.STATE_DISCONNECTED == newState ) {
                mUserConn = false;

                if ( 0 < mConnAtmp && !mUserDisc ) {
                    mConnAtmp -= 1;
                    gatt.getDevice().connectGatt(mCtx, false, getIntrGattCallback());
                } else {
                    getIntrGattCallback().onDisc(gatt.getDevice(), mUserDisc);
                    gatt.close();
                    mBluetoothGatt = null;
                }
            } else {

            }
        }

        @Override
        public final void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            for ( BluetoothGattService gattServ : gatt.getServices() ) {
                Log.i(TAG, "onServicesDiscovered: " + String.format("gattServ:@%s", gattServ.getUuid().toString()));
                for ( BluetoothGattCharacteristic gattChar : gattServ.getCharacteristics() ) {
                    Log.i(TAG, "onServicesDiscovered: " + String.format("gattChar:@%s", gattChar.getUuid().toString()));
                }
            }

            getIntrGattCallback().onEnum(gatt);
        }

        @Override
        public final void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged: " + String.format("devi:%s,mtu:%d,status:%d",gatt.getDevice().getAddress(),mtu,status));

            super.onMtuChanged(gatt, mtu, status);

            if ( BluetoothGatt.GATT_SUCCESS == status ) {
                mMTUs = mtu;
            } else {
                mMTUs = MTU_SIZE_MIN;
            }
            gatt.discoverServices();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
             super.onCharacteristicWrite(gatt, characteristic, status);

             Log.d(TAG, "onCharacteristicWrite: " + String.format("valu:%s, stat:%d", Arrays.toString(characteristic.getValue()), status));

            onCharWrit(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
             super.onCharacteristicChanged(gatt, characteristic);

            // Log.d(TAG, "onCharacteristicChanged: " + String.format("valu:%s", HexString.hexifyByteArray(characteristic.getValue())));

            onCharNtfy(gatt, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            // BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE
            Log.d(TAG, "onDescriptorWrite: " + String.format("stat:%d", status));

            OnDescWrit(gatt, descriptor, status);
        }
    }

    public PhyBtLeManager(@NonNull final Context context) {
        mCtx = context;
        mNewLeScanCallback = new PhyScanCallback();
        mOldLeScanCallback = new LeScanCallback();

    }

    abstract E getApplGattCallback();
    abstract BtIntrGattCallback getIntrGattCallback();

    /* connect device */
    public void setConn(@NonNull final String addr) {
        Log.d(TAG, "setConn: >>>>");

        BluetoothDevice devi = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);

        mUserConn = true;
        mConnAtmp = 3;
        mBluetoothGatt = devi.connectGatt(mCtx, false, getIntrGattCallback());
    }

    public void setDisc() {
        Log.d(TAG, "setDisc: >>>>");

        mUserDisc = true;
        if ( null != mBluetoothGatt ) {
            mBluetoothGatt.disconnect();
        }
    }

    public void EnmServ() {
        if ( null != mBluetoothGatt ) {
            mBluetoothGatt.discoverServices();
        }
    }

    public int getMtus() {
        return mMTUs;
    }

    public boolean hasServ(@NonNull BluetoothGatt gatt, @NonNull String servUUID) {
        BluetoothGattService gatt_serv = gatt.getService(UUID.fromString(servUUID));

        if ( gatt_serv != null ) {
            return true;
        }

        return false;
    }

    public boolean hasChar(@NonNull BluetoothGatt gatt, @NonNull String servUUID, @NonNull String charUUID) {
        BluetoothGattService gatt_serv = gatt.getService(UUID.fromString(servUUID));

        if ( gatt_serv != null ) {
            BluetoothGattCharacteristic gatt_char = gatt_serv.getCharacteristic(UUID.fromString(charUUID));
            if ( gatt_char != null ) {
                return true;
            }
        }

        return false;
    }

    public boolean enaNoti(@NonNull BluetoothGatt gatt, @NonNull String servUUID, @NonNull String charUUID) {
        BluetoothGattService gatt_serv = gatt.getService(UUID.fromString(servUUID));

        if ( gatt_serv != null ) {
            BluetoothGattCharacteristic gatt_char = gatt_serv.getCharacteristic(UUID.fromString(charUUID));
            gatt.setCharacteristicNotification(gatt_char, true);

            BluetoothGattDescriptor bluetoothGattDescriptor = gatt_char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(bluetoothGattDescriptor);

            return true;
        }

        return false;
    }

    public boolean enaIndi(@NonNull BluetoothGatt gatt, @NonNull String servUUID, @NonNull String charUUID) {
        return false;
    }


    protected boolean setData(@NonNull BluetoothGatt gatt, @NonNull String servUUID, @NonNull String charUUID, @NonNull byte[] data) {
        BluetoothGattService gatt_serv = gatt.getService(UUID.fromString(servUUID));

        if ( gatt_serv != null ) {
            BluetoothGattCharacteristic gatt_char = gatt_serv.getCharacteristic(UUID.fromString(charUUID));

            gatt_char.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            gatt_char.setValue(data);

            gatt.writeCharacteristic(gatt_char);
            return true;
        }

        return false;
    }

    public void bgnScan() throws NullPointerException {
        Log.d(TAG, "bgnScan: >>>>");

        BluetoothAdapter adpt = BluetoothAdapter.getDefaultAdapter();
        if ( adpt.isEnabled() ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                BluetoothAdapter.getDefaultAdapter().startLeScan(mOldLeScanCallback);
            } else {
                BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().startScan(mNewLeScanCallback);
            }
        }
    }

    public void endScan() throws NullPointerException {
        Log.d(TAG, "endScan: >>>>");

        BluetoothAdapter adpt = BluetoothAdapter.getDefaultAdapter();
        if ( adpt.isEnabled() ) {
            if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
                BluetoothAdapter.getDefaultAdapter().stopLeScan(mOldLeScanCallback);
            }else {
                BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(mNewLeScanCallback);
            }
        }
    }
}
