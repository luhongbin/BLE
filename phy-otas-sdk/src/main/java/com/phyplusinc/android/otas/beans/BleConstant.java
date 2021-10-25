package com.phyplusinc.android.otas.beans;

/**
 * BleConstant
 *
 * @author:zhoululu
 * @date:2018/7/7
 */

public class BleConstant {

    public static final String SERVICE_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_WRITE_UUID = "0000ff02-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_READ_UUID = "0000ff10-0000-1000-8000-00805f9b34fb";
    public static final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String SERVICE_BATTERY_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_BATTERY_READ_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    public static final String SERVICE_OTA_UUID = "5833ff01-9b8b-5191-6142-22a4536ef123";
    public static final String CHARACTERISTIC_OTA_WRITE_UUID = "5833ff02-9b8b-5191-6142-22a4536ef123";
    public static final String CHARACTERISTIC_OTA_INDICATE_UUID = "5833ff03-9b8b-5191-6142-22a4536ef123";
    public static final String CHARACTERISTIC_OTA_DATA_WRITE_UUID = "5833ff04-9b8b-5191-6142-22a4536ef123";


}
