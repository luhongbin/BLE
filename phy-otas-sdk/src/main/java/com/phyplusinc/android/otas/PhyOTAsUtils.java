package com.phyplusinc.android.otas;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.phyplusinc.android.otas.beans.ErrorCode;
import com.phyplusinc.android.otas.beans.OTAType;
import com.phyplusinc.android.otas.beans.StatusCode;
import com.phyplusinc.android.otas.ble.OTACallBack;
import com.phyplusinc.android.otas.ble.OTAsBtLeUtils;
import com.phyplusinc.android.otas.utils.BleUtils;

/**
 * PhyOTAsUtils
 *
 */

public class PhyOTAsUtils {
    private static final String TAG = PhyOTAsUtils.class.getSimpleName();

    private static final int CFGS_CONN_RTRY = 5;
    private static final int CFGS_BLCK_RTRY = 5;

    private int mStatus = StatusCode.ConnAppl;
    private int mCfgsConnAtmp = CFGS_CONN_RTRY;
    private int mCurrConnAtmp = CFGS_CONN_RTRY;

    private OTACallBack mOTAsBtLeCallBack;
    private OTAsBtLeUtils otasBtLeUtils;
    private OTAsStatusListener mStatusListener;
    private String address;
    private String filePath;
    private OTAType otaType;
    //是否使用高速ota
    private boolean isQuick;

    public interface OTAsStatusListener {
        void onException(int code);
        void onProgress(float percentage);
        void onStatus(int status);
        void onComplete();
    }

    /**
     */
    public PhyOTAsUtils(Context context, OTAsStatusListener otasStatusListener) {
        /* default configs */
        setConnAtmp(CFGS_CONN_RTRY);
        setBlckAtmp(CFGS_BLCK_RTRY);

        this.mStatusListener = otasStatusListener;

        otasBtLeUtils = new OTAsBtLeUtils(context, mOTAsBtLeCallBack = new OTAsBtLeCallBackImpl());
    }

    public void setConnAtmp(int numb) {
        mCfgsConnAtmp = numb;
    }

    public void setBlckAtmp(int numb) {
        if ( otasBtLeUtils != null ) {
            otasBtLeUtils.setRetryTimes(numb);
        }
    }

    public void updateFirmware(@NonNull String address, @NonNull String filePath) {
        this.address = address;
        this.filePath = filePath;
        this.otaType = OTAType.OTA;
        this.mCurrConnAtmp = mCfgsConnAtmp;

        // Log.d(TAG, "updateFirmware: " + "mStatus = StatusCode.ConnAppl >>>>>>>>>>>");
        mStatus = StatusCode.ConnAppl;
        if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
        otasBtLeUtils.connectDevice(address);
    }

    public void updateResource(@NonNull String address, @NonNull String filePath) {
        this.address = address;
        this.filePath = filePath;
        this.otaType = OTAType.RESOURCE;

        if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
        otasBtLeUtils.connectDevice(address);
    }

    public void cancelUpdate() {
        otasBtLeUtils.cancleOTA();
    }

    private void setError(int code) {
        Log.d(TAG, "setError: " + code);
        if ( null != mStatusListener ) mStatusListener.onException(code);

        otasBtLeUtils.close();
    }

    private void setSuccess() {
        if ( null != mStatusListener ) mStatusListener.onComplete();

        otasBtLeUtils.close();
    }

    private void startOta(){
        if(otaType == OTAType.OTA){
            otasBtLeUtils.updateFirmware(filePath,isQuick);
//            STATUS = OTA_ING;
        }else if(otaType == OTAType.RESOURCE){
            otasBtLeUtils.updateResource(filePath,isQuick);
//            STATUS = RES_ING;
        }
    }

    private class OTAsBtLeCallBackImpl implements OTACallBack {
        @Override
        public void onConnected(boolean isConnected) {
            if ( isConnected ) {
                isQuick = OTAsBtLeUtils.MTU_SIZE > 23;

                if ( StatusCode.ConnAppl == mStatus ) {
                    if ( otaType == OTAType.OTA ) {
                        mStatus = StatusCode.CondAppl;
                        if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                        // Log.d(TAG, "onConnected: " + "mStatus = StatusCode.CondAppl >>>>>>>>>>>");

                        /* send entering bootloader cmd */
                        mStatus = StatusCode.EntrBldr;
                        if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                        // Log.d(TAG, "onConnected: " + "mStatus = StatusCode.EntrBldr >>>>>>>>>>>");

                        otasBtLeUtils.startOTA();   // calling this, will connection will auto terminated by remote device, please capture onDisconnected Event
                    } else
                    if ( otaType == OTAType.RESOURCE ) {
                        otasBtLeUtils.startResource();
                    }
                } else
                if ( StatusCode.ConnBldr == mStatus ) {
                    mStatus = StatusCode.CondBldr;
                    if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                    // Log.d(TAG, "onConnected: " + "mStatus = StatusCode.CondBldr >>>>>>>>>>>");


                    /* initialize ota cmd */
                    mCurrConnAtmp = mCfgsConnAtmp;
                    mStatus = StatusCode.OtasProg;
                    if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                    // Log.d(TAG, "onConnected: " + "mStatus = StatusCode.OtasProg >>>>>>>>>>>");

                    if ( isQuick && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                        otasBtLeUtils.setPHY();
                    }else {
                        startOta();
                    }
                } else {
                    Log.d(TAG,String.format("Exception: mStatus=%d", mStatus));
                }
            } else {  // disconnected
//                if(STATUS == START_OTA || STATUS == START_RES){
                if ( StatusCode.EntrBldr == mStatus ) {
                    mStatus = StatusCode.ScanBldr;
                    if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                    // Log.d(TAG, "onConnected: " + "mStatus = StatusCode.ScanBldr >>>>>>>>>>>");

                    otasBtLeUtils.starScan();
                } else
                if ( StatusCode.ConnBldr == mStatus ) {
                    if ( 0 < mCurrConnAtmp) {
                        mCurrConnAtmp -= 1;
                        mStatus = StatusCode.ConnBldr;
                        otasBtLeUtils.connectDevice(address);
                        //Log.d(TAG, "onConnected: " + "mStatus = StatusCode.ConnBldr >>>>>>>>>>>");
                    } else {
                        mStatus = StatusCode.ErrsStat;
                        if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                    }
                } else
                if ( StatusCode.OtasComp == mStatus ) {
                    mStatus = StatusCode.OtasRset;
                    if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                    setSuccess();
                } else
//                if(STATUS == OTA_ING || STATUS == RES_ING){
                if ( StatusCode.OtasProg == mStatus ){
                    if ( 0 < mCurrConnAtmp) {
                        mCurrConnAtmp -= 1;
                        mStatus = StatusCode.ConnBldr;
                        otasBtLeUtils.connectDevice(address);
                        // Log.d(TAG, "onConnected: " + "mStatus = StatusCode.ConnBldr >>>>>>>>>>>");
                    } else
                    setError(ErrorCode.OTA_CONNTEC_ERROR);
                } else {
                    setError(ErrorCode.CONNECT_ERROR);
                }
            }
        }

        @Override
        public void onOTA(boolean isConnected) {
            if(isConnected){
                if(isQuick && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    otasBtLeUtils.setPHY();
                }else{
                    startOta();
                }
            }
        }

        @Override
        public void onDeviceSearch(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//            if((STATUS == START_OTA || STATUS == START_RES) && device.getAddress().equals(BleUtils.getOTAMac(address))) {
            if ( StatusCode.ScanBldr == mStatus /*&& device.getAddress().equals(BleUtils.getOTAMac(address))*/ ) {
                otasBtLeUtils.stopScan();

                address = device.getAddress();
                mCurrConnAtmp = mCfgsConnAtmp;
                mStatus = StatusCode.ConnBldr;
                if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);
                // Log.d(TAG, "onDeviceSearch: " + "stop scan, mStatus = StatusCode.ConnBldr >>>>>>>>>>");

                otasBtLeUtils.connectDevice(device.getAddress());
            }
        }

        @Override
        public void onProcess(float process) {
            mStatusListener.onProgress(process);
        }

        @Override
        public void onError(int code) {
            Log.d("onException","setError:"+code);
            mStatus = StatusCode.ErrsStat;
            if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);

            setError(code);
        }

        @Override
        public void onOTAFinish() {
            mStatus = StatusCode.OtasComp;
            if ( null != mStatusListener ) mStatusListener.onStatus(mStatus);

            otasBtLeUtils.reBoot();
        }

        @Override
        public void onResource(boolean isConnected) {
            if(isConnected){
                if(isQuick && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    otasBtLeUtils.setPHY();
                }else{
                    startOta();
                }
            }
        }

        @Override
        public void onResourceFinish() {
            otasBtLeUtils.reBoot();
        }

        @Override
        public void onReboot() {
        }

        @Override
        public void onRebootSuccess() {
            otasBtLeUtils.disConnectDevice();
        }

        @Override
        public void onPhyUpdate() {
            startOta();
        }
    }

}
