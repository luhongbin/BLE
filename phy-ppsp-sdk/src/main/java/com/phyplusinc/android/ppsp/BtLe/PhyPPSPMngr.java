package com.phyplusinc.android.ppsp.BtLe;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;


import com.phyplusinc.android.ppsp.trns.PhyBaseMsgs;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class PhyPPSPMngr extends PhyBtLeManager<PhyPPSPMngr.PPSPMngrCallbacks> {
    private final String TAG = getClass().getSimpleName();

    public static final int StatScanAppl = 1000;    // Scanning Application Device
    public static final int StatConnAppl = 1001;    // Connecting Application Device
    public static final int StatCondAppl = 1002;    // connected
    public static final int StatLinkFree = 1003;    //
    public static final int StatLinkBusy = 1004;    //


    private static final String SERVICE_PPSP_UUID =         "0000feb3-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_FFD4_UUID =  "0000ffd4-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_FFD5_UUID =  "0000ffd5-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_FFD6_UUID =  "0000ffd6-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_FFD7_UUID =  "0000ffd7-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_FFD8_UUID =  "0000ffd8-0000-1000-8000-00805f9b34fb";


    private String  mDeviAddr;
    private int mMngrStat;

    private PhyBaseMsgs     mMsgsHndl = null;
    private int             mMsgsSegm = 0;
    private final Handler   mMsgsHdlr = new Handler();
    private final Queue<PhyBaseMsgs>  mMsgsQueu = new ConcurrentLinkedQueue<>();
    private final Runnable  mMsgsTout = new Runnable() {
        @Override
        public void run() {
            synchronized (mMsgsQueu) {
                if (null != mMsgsHndl) {
                    mMsgsQueu.remove(mMsgsHndl);
                    mMsgsHndl = null;

                    // sending msgs failure
                    Log.d(TAG, "mMsgsTout: " + "sending msgs wait response tout");
                }
            }
        }
    };

    private final Runnable  mMsgsXfer = new Runnable() {
        @Override
        public void run() {
            synchronized ( mMsgsQueu ) {
                if ( !mMsgsQueu.isEmpty() ) {
                    if (mMsgsHndl == null) {
                        final PhyBaseMsgs msgs = mMsgsQueu.peek();
                        mMsgsHndl = msgs;
                        if (mMsgsHndl != null) {
                            mMsgsSegm = 0;
                            mMsgsHdlr.postDelayed(mMsgsTout, 1000);
                            Log.d(TAG, "MsgsXfer: " + String.format("sending new mesg:%s, segm:%d", Arrays.toString(mMsgsHndl.getData().get(mMsgsSegm)), mMsgsSegm));
                            setData(mIntrGatt, SERVICE_PPSP_UUID, CHARACTERISTIC_FFD5_UUID, mMsgsHndl.getData().get(mMsgsSegm));
                        }
                    } else {
                        if (!mMsgsHdlr.hasCallbacks(mMsgsTout)) {
                            mMsgsSegm += 1;
                            if ( mMsgsSegm < mMsgsHndl.getData().size()) {
                                Log.d(TAG, "MsgsXfer: " + String.format("sending seg mesg:%s, segm:%d", Arrays.toString(mMsgsHndl.getData().get(mMsgsSegm)), mMsgsSegm));
                                setData(mIntrGatt, SERVICE_PPSP_UUID, CHARACTERISTIC_FFD5_UUID, mMsgsHndl.getData().get(mMsgsSegm));
                            } else {
                                mMsgsQueu.remove(mMsgsHndl);
                                mMsgsHndl = null;
                            }
                        }
                    }
                }
            }

            mMsgsHdlr.postDelayed(mMsgsXfer, 200);
        }
    };

    private BluetoothGatt mIntrGatt = null;
    private final PPSPMngrCallbacks mPPSPMngrCallbacks;
    private final PPSPMsgrCallbacks mPPSPMsgrCallbacks;
    private final BtIntrGattCallback mIntrGattCallback = new BtIntrGattCallback() {

        @Override
        public void onScan(BluetoothDevice devi, int rssi, byte[] scan) {
            if ( StatScanAppl == mMngrStat) {
                if (TextUtils.equals(mDeviAddr, devi.getAddress())) {
                    endScan();

                    mMngrStat = StatConnAppl;
                    setConn(devi.getAddress());
                }
//            } else
//            if ( StatusCode.ScanBldr == mMngrStat ) {
//                if (TextUtils.equals(mOtasAddr, devi.getAddress())) {
//                    endScan();
//
//                    mMngrStat = StatusCode.ConnBldr;
//                    setConn(devi.getAddress());
//                }
            } else {
                endScan();
            }
        }

        @Override
        public void onConn(BluetoothDevice devi, boolean user /*action by user*/) {
            Log.d(TAG, "onConn: >>>> " + String.format("devi:%s", devi.getAddress()));
            Log.d(TAG, "onConn: >>>> " + String.format("stat:%d", mMngrStat));

            mMngrStat = StatCondAppl;
        }

        @Override
        public void onDisc(BluetoothDevice devi, boolean user /*action by user*/) {
            Log.d(TAG, "onDisc: >>>> " + String.format("devi:%s", devi.getAddress()));
            Log.d(TAG, "onDisc: >>>> " + String.format("stat:%d", mMngrStat));
        }

        @Override
        public void onEnum(BluetoothGatt gatt) {
            Log.d(TAG, "onEnum: >>>> " + "check support services");

            /* check support services */
            boolean rslt = hasServ(gatt, SERVICE_PPSP_UUID);
            if ( rslt ) {
                mIntrGatt = gatt;

                enaNoti(gatt, SERVICE_PPSP_UUID, CHARACTERISTIC_FFD8_UUID);
            } else {
                // notify upper layer & disconnect
                setDisc();
            }

        }

        @Override
        public void onCharWrit(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            final String uuid = characteristic.getUuid().toString().toLowerCase();
            final String valu = Arrays.toString(characteristic.getValue());

            Log.d(TAG, "onCharWrit: >>>>");

            if (null != mMsgsHndl) {
                if ( Arrays.equals(mMsgsHndl.getData().valueAt(mMsgsSegm), characteristic.getValue()) ) {
                    mMsgsHdlr.removeCallbacks(mMsgsTout);
                    final PPSPMsgrCallbacks hdlr = getPPSPMsgrCallbacks();
                    if ( null != hdlr ) {
                        hdlr.onMsgrRspn();
                    }
                }
            }
        }

        @Override
        public void onCharNtfy(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String uuid = characteristic.getUuid().toString().toLowerCase();
            final String valu = Arrays.toString(characteristic.getValue());

            final PPSPMsgrCallbacks hdlr = getPPSPMsgrCallbacks();

            if ( null != hdlr ) {
                hdlr.onMsgrRspn();
            }
        }

        @Override
        public void OnDescWrit(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if ( "00002902-0000-1000-8000-00805f9b34fb".equals(descriptor.getUuid().toString().toLowerCase()) ) {
                if ( status == BluetoothGatt.GATT_SUCCESS ) {
                    if ( StatCondAppl == mMngrStat) {
                        mMngrStat = StatLinkFree;
                        // notify upper layer link is ready
                        if ( null != getApplGattCallback() ) getApplGattCallback().onLinkOpen();
                    }
                } else {
                    setDisc();
                }
            }
        }
    };

    private PPSPMsgrCallbacks getPPSPMsgrCallbacks() { return mPPSPMsgrCallbacks; }

    public PhyPPSPMngr(@NonNull Context context, @NonNull final PPSPMngrCallbacks PPSPMngrCallback, @NonNull final PPSPMsgrCallbacks PPSPMsgrCallback) {
        super(context);
        Log.d(TAG, "PhyPPSPMngr: >>>>>");

        mPPSPMngrCallbacks = PPSPMngrCallback;
        mPPSPMsgrCallbacks = PPSPMsgrCallback;
        mPPSPMsgrCallbacks.setMsgr(this);

        if ( !mMsgsHdlr.hasCallbacks(mMsgsXfer) ) {
            mMsgsHdlr.postDelayed(mMsgsXfer, 200);
        }
    }

    public void opnLink(@NonNull String deviAddr) {
        mDeviAddr = deviAddr;
        mMngrStat = StatScanAppl;
        bgnScan();
    }


    public void clsLink() {
        setDisc();
    }

    public void sndMsgs(@NonNull PhyBaseMsgs msgs) {
        synchronized (mMsgsQueu) {
            mMsgsQueu.add(msgs);
        }
    }

    @Override
    PPSPMngrCallbacks getApplGattCallback() { return mPPSPMngrCallbacks; }

    @Override
    BtIntrGattCallback getIntrGattCallback() { return mIntrGattCallback; }


    public interface PPSPMngrCallbacks extends PhyBtLeManager.PhyBtLeCallBack {
        void onLinkOpen();
        void onLinkClos();
    }

    public interface PPSPMsgrCallbacks {
        void setMsgr(final PhyPPSPMngr Msgr);
        void onMsgrRspn();
    }
}