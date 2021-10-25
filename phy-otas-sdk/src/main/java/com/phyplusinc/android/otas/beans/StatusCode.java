package com.phyplusinc.android.otas.beans;

/**
 * ErrorCode
 *
 * @author:zhoululu
 * @date:2018/7/9
 */

public class StatusCode {

    public static final int ErrsStat = -1;

    public static final int ScanAppl = 1000;    // Scanning Application Device

    public static final int ConnAppl = 1001;    // Connecting Application Device

    public static final int CondAppl = 1002;    // connected

    public static final int EntrBldr = 1003;    // Entering Bootloader

    public static final int ScanBldr = 1004;    // Scanning Bootloader

    public static final int ConnBldr = 1005;    // Connecting Bootloader

    public static final int CondBldr = 1006;    // Connected

    public static final int OtasProg = 1007;    // OTAing

    public static final int OtasComp = 1008;    // OTA Completed

    public static final int OtasRset = 1009;    // Reboot

}
