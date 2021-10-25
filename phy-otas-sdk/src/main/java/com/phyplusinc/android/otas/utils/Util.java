package com.phyplusinc.android.otas.utils;

import java.util.Arrays;

/**
 * Created by zhoululu on 2017/6/22.
 */

public class Util {

    private static byte csn = 0x00;

    public static byte getCSN(){
        if(csn < 0xff){
            csn += 1;
        }else{
            csn = 0x00;
        }
        return csn;
    }

    public static byte genVerifyByte(byte method,byte csn){
        return (byte)(method ^ csn);
    }

    public static byte genVerifyByte(byte method,byte csn,byte[] command){

        int t = method ^ csn;
        for (int i=0;i<command.length;i++){
            t = t ^ command[i];
        }

        return (byte) t;
    }

    public static boolean containByte(byte[] source,byte[] des){

        for (int i=0;i<source.length/des.length;i++){
            byte[] bytes = new byte[des.length];
            System.arraycopy(source,i*des.length,bytes,0,des.length);

            if(Arrays.equals(bytes,des)){
                return true;
            }
        }
        return false;
    }

    public static byte[] replaceByte(byte[] source,byte[] des,byte[] replace){

        byte[] result = new byte[source.length+replace.length-des.length];

        for (int i=0;i<source.length/des.length;i++){
            byte[] bytes = new byte[des.length];
            System.arraycopy(source,i*des.length,bytes,0,des.length);

            if(Arrays.equals(bytes,des)){
                System.arraycopy(source,0,result,0,i*des.length);
                System.arraycopy(replace,0,result,i*des.length,replace.length);
                System.arraycopy(source,i*des.length+des.length,result,i*des.length+replace.length,source.length-i*des.length-des.length);

                return result;
            }
        }

        return source;
    }

    public static byte[] replaceAllByte(byte[] source,byte[] des,byte[] replace){

        byte[] result =  replaceByte(source,des,replace);

        while (containByte(result,des)){
            result =  replaceByte(source,des,replace);
        }

        return result;
    }

    public static String strAdd0(String str,int lenth){
        int strLength = str.length();
        StringBuffer result = new StringBuffer("");
        for (int i=0;i<lenth-strLength;i++){
            result.append("0");
        }

        return result.append(str).toString();
    }

    public static String translateStr(String str){
        String result = "";
        for (int i =0;i<str.length()/2;i++){
            result = str.substring(i*2,i*2+2) + result;
        }

        return  result;
    }

}
