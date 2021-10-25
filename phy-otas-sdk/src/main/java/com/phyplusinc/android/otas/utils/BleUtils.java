package com.phyplusinc.android.otas.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;

import com.phyplusinc.android.otas.beans.BleConstant;
import com.phyplusinc.android.otas.beans.FirmWareFile;
import com.phyplusinc.android.otas.beans.Partition;

import java.util.UUID;

/**
 * BleUtils
 *
 * @author:zhoululu
 * @date:2018/7/7
 */

public class BleUtils {
    private static final String TAG = BleUtils.class.getSimpleName();

    public static boolean enableNotifications(BluetoothGatt bluetoothGatt){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_UUID));

        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_WRITE_UUID));

        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);

        BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleConstant.DESCRIPTOR_UUID));
        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);

        return true;
    }

    public static boolean enableIndicateNotifications(BluetoothGatt bluetoothGatt){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));

        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_INDICATE_UUID));

        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);

        BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleConstant.DESCRIPTOR_UUID));
        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);

        return true;
    }

    public static boolean checkIsOTA(BluetoothGatt bluetoothGatt){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));

        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_DATA_WRITE_UUID));
        if(bluetoothGattCharacteristic != null){
            return true;
        }else {
            return false;
        }
    }


    public static boolean sendOTACommand(BluetoothGatt bluetoothGatt, String commd,boolean respons){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));
        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_WRITE_UUID));
        if(!respons){
            bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }else{
            bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
        bluetoothGattCharacteristic.setValue(HexString.parseHexString(commd));
        bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);

        Log.d("send ota commond", commd);

        Log.d(TAG, "sendOTACommand: " + String.format("cmds:%s @ char:%s + resp:%s", commd, bluetoothGattCharacteristic.getUuid().toString(), Boolean.toString(respons)));

        return true;

       // LogUtil.getLogUtilInstance().save("send ota commond: "+commd);
    }


    public static String getOTAMac(String deviceAddress){
        final String firstBytes = deviceAddress.substring(0, 15);
        // assuming that the device address is correct
        final String lastByte = deviceAddress.substring(15);
        final String lastByteIncremented = String.format("%02X", (Integer.valueOf(lastByte, 16) + 1) & 0xFF);

        return firstBytes + lastByteIncremented;
    }

    public static boolean sendOTADate(BluetoothGatt bluetoothGatt,String cmd){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));
        if(bluetoothGattService == null){
            Log.e(" OTA service", "service is null");
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_DATA_WRITE_UUID));

        bluetoothGattCharacteristic.setValue(HexString.parseHexString(cmd.toLowerCase()));
        bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);

        Log.d("send ota data", cmd);

        Log.d(TAG, "sendOTADate: " + String.format("cmds:%s @ char:%s", cmd, bluetoothGattCharacteristic.getUuid().toString()));


        return true;
    }

    public static String make_part_cmd(int index,long flash_addr,String run_addr,int size,int checksum){
        String fa = Util.translateStr(Util.strAdd0(Long.toHexString(flash_addr),8));
        String ra = Util.translateStr(Util.strAdd0(run_addr,8));
        String sz = Util.translateStr(Util.strAdd0(Integer.toHexString(size),8));
        String cs = Util.translateStr(Util.strAdd0(Integer.toHexString(checksum),4));
        String in = Util.strAdd0(Integer.toHexString(index),2);

        Log.d(TAG, "make_part_cmd: " + String.format("cmds:%s", "02"+ in +fa + ra + sz + cs));

        return "02"+ in +fa + ra + sz + cs;
    }

    public static String make_resource_cmd(@NonNull FirmWareFile firmWareFile){

        String startAddress = firmWareFile.getList().get(0).getAddress();
        //&0x12000
        long flashLongAdd = Long.parseLong(startAddress,16) & 0xfffff000;
        long flashLongSize = Long.parseLong(startAddress,16) & 0xfff;
        for (Partition partition : firmWareFile.getList()){
            flashLongSize += partition.getPartitionLength();
        }
        flashLongSize = (flashLongSize + 0xfff) & 0xfffff000;

        String fa = Util.translateStr(Util.strAdd0(Long.toHexString(flashLongAdd),8));
        String sz = Util.translateStr(Util.strAdd0(Long.toHexString(flashLongSize),8));

        Log.d(TAG, "make_part_cmd: " + String.format("cmds:%s", "05" + fa + sz));

        return "05" + fa + sz;
    }

    public static boolean sendPartition(BluetoothGatt gatt, FirmWareFile firmWareFile, int partitionIndex, long flash_addr){
        Partition partition = firmWareFile.getList().get(partitionIndex);
        int checsum = getPartitionCheckSum(partition);
        String cmd = make_part_cmd(partitionIndex, flash_addr, partition.getAddress(), partition.getPartitionLength(), checsum);

        Log.d(TAG, "sendPartition: " + String.format("cmds:%s", cmd));

        return sendOTACommand(gatt,cmd, true);
    }

    public static boolean sendResource(BluetoothGatt gatt, FirmWareFile firmWareFile){
        String cmd = make_resource_cmd(firmWareFile);

        Log.d(TAG, "sendResource: " + String.format("cmds:%s", cmd));

        return sendOTACommand(gatt,cmd, true);
    }

    public static int getPartitionCheckSum(Partition partition){
        return checkSum(0,HexString.parseHexString(partition.getData()));
    }

    private static int checkSum(int crc, byte[] data) {
        // 存储需要产生校验码的数据
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i];
        }
        int len = buf.length;

        for (int pos = 0; pos < len; pos++) {
            if (buf[pos] < 0) {
                // XOR byte into least sig. byte of
                crc ^= (int) buf[pos] + 256;
                // crc
            } else {
                // XOR byte into least sig. byte of crc
                crc ^= (int) buf[pos];
            }
            // Loop over each bit
            for (int i = 8; i != 0; i--) {
                // If the LSB is set
                if ((crc & 0x0001) != 0) {
                    // Shift right and XOR 0xA001
                    crc >>= 1;
                    crc ^= 0xA001;
                } else{
                    // Else LSB is not set
                    // Just shift right
                    crc >>= 1;
                }
            }
        }

        return crc;
    }


}
