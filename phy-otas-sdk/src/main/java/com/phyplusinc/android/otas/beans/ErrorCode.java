package com.phyplusinc.android.otas.beans;

/**
 * ErrorCode
 *
 * @author:zhoululu
 * @date:2018/7/9
 */

public class ErrorCode {

    //文件解析错误
    public static final int FILE_ERROR = 1000;

    //进入OTA状态后连接错误
    public static final int OTA_CONNTEC_ERROR = 1001;

    //OTA数据发送service未找到
    public static final int OTA_DATA_SERVICE_NOT_FOUND = 1002;

    //OTA命令发送service未找到
    public static final int OTA_SERVICE_NOT_FOUND = 1003;

    //OTA数据写入错误
    public static final int OTA_DATA_WRITE_ERROR = 1004;

    //OTA响应错误
    public static final int OTA_RESPONSE_ERROR = 1005;

    //断开连接
    public static final int CONNECT_ERROR = 1006;

    //设备未连接
    public static final int DEVICE_NOT_CONNECT = 1007;

    //设备不在OTA状态
    public static final int DEVICE_NOT_IN_OTA = 1008;

    //设置MTU大小错误
    public static final int SET_MTU_ERROR = 1009;
}
