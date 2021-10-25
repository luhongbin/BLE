package com.phyplusinc.android.otas.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;

import com.phyplusinc.android.otas.beans.BleConstant;
import com.phyplusinc.android.otas.beans.OTAType;
import com.phyplusinc.android.otas.utils.BleUtils;
import com.phyplusinc.android.otas.beans.ErrorCode;
import com.phyplusinc.android.otas.beans.FirmWareFile;
import com.phyplusinc.android.otas.beans.Partition;
import com.phyplusinc.android.otas.utils.HexString;

import java.util.List;


/**
 * BleCallBack
 *
 * @date:2018/7/13
 */

public class BleCallBack extends BluetoothGattCallback {

    private static final String TAG = BleCallBack.class.getSimpleName();

    private OTAUtilsCallBack otaUtilsCallBack;
    private FirmWareFile firmWareFile;
    private OTAType otaType;

    private int partitionIndex = 0;
    private int blockIndex = 0;
    private int cmdIndex = 0;
    private long mFlash_addr = 0;

    private List<String> cmdList;

    private boolean isResponse;
    private String response;

    private float totalSize;
    private float finshSize;

    private int retryTimes = 3;
    private int cmdErrorTimes;
    private int blockErrorTimes;

    private boolean isCancle;

    private int errorPartitionId = -1;
    private int errorBlockId = -1;
    private boolean isRetrying;

    public void setOtaUtilsCallBack(OTAUtilsCallBack otaUtilsCallBack) {
        this.otaUtilsCallBack = otaUtilsCallBack;
    }

    public void setFirmWareFile(FirmWareFile firmWareFile,OTAType otaType) {
        this.firmWareFile = firmWareFile;
        this.otaType = otaType;

        totalSize = firmWareFile.getLength();

        initData();
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    private void initData(){
        partitionIndex = 0;
        blockIndex = 0;
        cmdIndex = 0;
        mFlash_addr = 0;

        cmdList = null;

        finshSize = 0;

        isCancle = false;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if(newState == BluetoothProfile.STATE_CONNECTED){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                gatt.requestMtu(512);
            }else {
                gatt.discoverServices();
            }

        }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
            if(gatt != null){
                gatt.close();
            }

            otaUtilsCallBack.onConnectChange(false);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered: success");

            //开启notification,返回false,断开连接
            boolean b = true;

            if(BleUtils.checkIsOTA(gatt)){
                b = BleUtils.enableIndicateNotifications(gatt);
            }else{
                //b = BleUtils.enableNotifications(gatt);
                //如果不是OTA状态，直接返回

                otaUtilsCallBack.onConnectChange(true);
            }

            if(!b){
                gatt.disconnect();
            }

        }else{
            gatt.disconnect();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicWrite: " + String.format("cmds:%s @ char:%s + resp:",
                HexString.hexifyByteArray(characteristic.getValue()), characteristic.getUuid().toString()/*, Boolean.toString(response)*/));

        if(characteristic.getUuid().toString().equals(BleConstant.CHARACTERISTIC_OTA_WRITE_UUID)){
            //0081，开始ota，发送partition命令
            Log.d(TAG, "onCharacteristicWrite: " + response);


            if(("0081").equals(response) && isResponse) {

                if(otaType == OTAType.OTA){
                    sendPartition(gatt,firmWareFile,partitionIndex,mFlash_addr);
                    blockIndex = 0;
                }else if(otaType == OTAType.RESOURCE){
                    sendResource(gatt,firmWareFile);
                }


                //0084  发送partition命令respons，开始发送ota数据
            }else if(("0084").equals(response) && isResponse) {
                cmdIndex = 0;

                cmdList = firmWareFile.getList().get(partitionIndex).getBlocks().get(blockIndex);

                sendOTAData(gatt,cmdList.get(cmdIndex));
            }else if("0089".equals(response)){
                sendPartition(gatt,firmWareFile,partitionIndex,mFlash_addr);
                blockIndex = 0;
            }

            if("0102".equals(HexString.parseStringHex(characteristic.getValue())) || "0103".equals(HexString.parseStringHex(characteristic.getValue()))){
                gatt.disconnect();
                Log.d(TAG, "start ota or resource");
            }

            isResponse = false;

        }else
        if(characteristic.getUuid().toString().equals(BleConstant.CHARACTERISTIC_OTA_DATA_WRITE_UUID)){
            //一条ota数据发送成功
            if(status == 0){

                if(cmdErrorTimes > 0){
                    cmdErrorTimes = 0;
                }

                if(errorPartitionId == partitionIndex && errorBlockId == blockIndex){

                }else{
                    if(isRetrying){
                        isRetrying = false;
                        errorPartitionId = -1;
                        errorBlockId = -1;
                    }
                    finshSize += characteristic.getValue().length;
                    otaUtilsCallBack.onProcess(finshSize*100/totalSize);
                }

                cmdIndex ++;
                if(cmdIndex < cmdList.size()){
                    sendOTAData(gatt,cmdList.get(cmdIndex));
                }

            }else{
                retryCmd(gatt,ErrorCode.OTA_DATA_WRITE_ERROR);
                //otaUtilsCallBack.onException(ErrorCode.OTA_DATA_WRITE_ERROR);
            }

            //LogUtil.getLogUtilInstance().save("send ota data status: "+status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();

        if(uuid.equals(BleConstant.CHARACTERISTIC_OTA_INDICATE_UUID)){
            Log.d(TAG, "onCharacteristicChanged: " + HexString.parseStringHex(characteristic.getValue()));

            response = HexString.parseStringHex(characteristic.getValue());

            isResponse = true;

            //0087  一组16*20 ota数据发送成功，开始下一组
            if(("0087").equals(response)) {

                if(blockErrorTimes > 0){
                    blockErrorTimes = 0;
                }

                blockIndex++;
                cmdIndex = 0;

                if (blockIndex < firmWareFile.getList().get(partitionIndex).getBlocks().size()) {

                    cmdList = firmWareFile.getList().get(partitionIndex).getBlocks().get(blockIndex);
                    sendOTAData(gatt,cmdList.get(cmdIndex));
                }
                //0085  一个partition 数据发送成功，发送下一个partition命令
            }else if(("0085").equals(response)){
                partitionIndex++;

                blockIndex = 0;

                if(partitionIndex < firmWareFile.getList().size()){
                    if(otaType == OTAType.OTA){
                        //后面地址由前一个长度决定
                        Partition prePartition = firmWareFile.getList().get(partitionIndex-1);
                        //run addr 在11000000 ~ 1107ffff， flash addr=run addr，其余的，flash addr从0开始递增
                        Log.d(TAG, "onCharNtfy: " + String.format("mFlshAddr:%x->%x", mFlash_addr, mFlash_addr));

                        if( (0x11000000 > Long.parseLong(prePartition.getAddress(),16)) || (Long.parseLong(prePartition.getAddress(),16) > 0x1107ffff)){
                            mFlash_addr = mFlash_addr + prePartition.getPartitionLength() + 16 - (prePartition.getPartitionLength()+4)%4;
                        }

                        Log.d(TAG, "onCharNtfy: " + String.format("mFlshAddr:%x->%x", mFlash_addr, mFlash_addr));
                    }

                    sendPartition(gatt,firmWareFile,partitionIndex,mFlash_addr);
                }
                //0083 所有ota数据发送成功
            }else if(("0083").equals(response)){
                if(otaType == OTAType.OTA){
                    otaUtilsCallBack.onOTAFinish();
                }else if(otaType == OTAType.RESOURCE){
                    otaUtilsCallBack.onResourceFinish();
                }
            //高速模式下 发送reboot
            }else if("008a".equals(response.toLowerCase())){
                otaUtilsCallBack.onReBootSuccess();

            //高速模式下 进入ota
            }else if(("00").equals(response)){
                gatt.disconnect();
            }else if(("6887".equals(response))){
                if(!isCancle){
                    retry(gatt,ErrorCode.OTA_RESPONSE_ERROR);
                }
            }else if(("0081").equals(response) || ("0084").equals(response) || "0089".equals(response)){

            }else{
                otaUtilsCallBack.onError(ErrorCode.OTA_RESPONSE_ERROR);
                Log.d(TAG,"error:"+response);
            }



            /*if(("0065".equals(response))){
                if(!isCancle){
                    retry(gatt,ErrorCode.OTA_RESPONSE_ERROR);
                }
            }else if(("0081").equals(response) || ("0083").equals(response) || ("0084").equals(response) ||
                    ("0085").equals(response) || ("0087").equals(response) || "0089".equals(response) || "008a".equals(response)){

            }else{
                otaUtilsCallBack.onException(ErrorCode.OTA_RESPONSE_ERROR);
                Log.d(TAG,"error:"+response);
            }*/
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if(BleConstant.DESCRIPTOR_UUID.equals(descriptor.getUuid().toString().toLowerCase())){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //连接设备，在discoverService和设置notification之后返回
                otaUtilsCallBack.onConnectChange(true);
            }else{
                gatt.disconnect();
            }
        }
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        otaUtilsCallBack.onPhyUpdate(gatt, txPhy, rxPhy, status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            OTAsBtLeUtils.MTU_SIZE = mtu;
        }else{
            OTAsBtLeUtils.MTU_SIZE = 23;
        }
        gatt.discoverServices();
    }

    private void sendOTAData(BluetoothGatt gatt, String data){

        if(isCancle){
            return;
        }

        boolean success = BleUtils.sendOTADate(gatt,data);
        if(!success){
            otaUtilsCallBack.onError(ErrorCode.OTA_DATA_SERVICE_NOT_FOUND);
        }
    }

    private void sendPartition(BluetoothGatt gatt,FirmWareFile firmWareFile,int partitionIndex,long flash_addr){
        if(isCancle){
            return;
        }

        //run addr 在11000000 ~ 1107ffff， flash addr=run addr，其余的，flash addr从0开始递增
        Partition partition = firmWareFile.getList().get(partitionIndex);
        if( (0x11000000 <= Long.parseLong(partition.getAddress(),16)) && (Long.parseLong(partition.getAddress(),16) <= 0x1107ffff)){
            flash_addr = Long.parseLong(partition.getAddress(),16);
        }

        if(otaType == OTAType.RESOURCE){
            mFlash_addr = 0;
            flash_addr = 0;
        }

        boolean success = BleUtils.sendPartition(gatt,firmWareFile,partitionIndex,flash_addr);
        if(!success){
            otaUtilsCallBack.onError(ErrorCode.OTA_SERVICE_NOT_FOUND);
        }
    }

    private void sendResource(BluetoothGatt gatt,FirmWareFile firmWareFile){
        if(isCancle){
            return;
        }

        boolean success = BleUtils.sendResource(gatt,firmWareFile);
        if(!success){
            otaUtilsCallBack.onError(ErrorCode.OTA_SERVICE_NOT_FOUND);
        }
    }

    public void setCancel(){
        isCancle = true;
    }

    private void retry(BluetoothGatt gatt,int errorCode){
        Log.d(TAG, "retry block:");

        if(blockErrorTimes < retryTimes){
            errorBlockId = blockIndex;
            errorPartitionId = partitionIndex;
            isRetrying = true;

            cmdIndex = 0;
            cmdList = firmWareFile.getList().get(partitionIndex).getBlocks().get(blockIndex);
            sendOTAData(gatt,cmdList.get(cmdIndex));

            blockErrorTimes++;
        }else{
            otaUtilsCallBack.onError(errorCode);
        }
    }

    private void retryCmd(BluetoothGatt gatt,int errorCode){
        Log.d(TAG, "retryCmd: ");

        if(cmdErrorTimes < retryTimes){
            sendOTAData(gatt,cmdList.get(cmdIndex));

            cmdErrorTimes++;
        }else{
            otaUtilsCallBack.onError(errorCode);
        }
    }

}
