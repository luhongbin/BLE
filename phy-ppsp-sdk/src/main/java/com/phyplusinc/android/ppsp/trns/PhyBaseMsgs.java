package com.phyplusinc.android.ppsp.trns;

import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class PhyBaseMsgs {
    public final int  MSGS_TYPE_ISSUE_RESP = 0;
    public final int  MSGS_TYPE_ISSUE_NRSP = 1;


    private final int   MSGS_HDER_SIZE = 4;
    private final int   MSGR_MSGS_SIZE = 20;

    private SparseArray<byte[]> mData;
    private int                 mNumb;
    private int                 mOpco;
    private int                 mType;
    private int                 mStat;

    public SparseArray<byte[]>
    getData() { return mData; }

    public void
    setNumb(int numb) { mNumb = numb; }

    public void
    setOpco(int opco) { mOpco = opco; }

    public int
    getOpco() { return mOpco; }

    public void
    setType(int type) { mType = type; }

    public int
    getType() { return mType; }

    public void
    setStat(int stat) { mStat = stat; }

    public int
    getStat() { return mStat; }

    public void
    setPara(byte[] para) {
        mData = new SparseArray<>();

        int load = MSGR_MSGS_SIZE - MSGS_HDER_SIZE;
        int rslt = para.length / load;
        int rema = para.length % load;
        int segm = rslt + ((0 < rema) ? 1 : 0);
        if ( 0 < segm ) {
            for ( int itr0 = 0; itr0 < segm; itr0 += 1) {
                int offs = itr0 * load;
                int leng = (para.length >= (offs + load)) ? load : (para.length - offs);

                ByteBuffer buff = ByteBuffer.allocate(leng + MSGS_HDER_SIZE);

                buff.put((byte) (mNumb<<4 | 0<<3 | 0)); // mesg sequ, encr flag, reserved
                buff.put((byte) mOpco);            // mesg opco
                buff.put((byte) ((itr0&0x0F)<<4 | (segm&0x0F)<<0)); // segm sequ, segm numb
                buff.put((byte) leng);  // payload length
                buff.put(para, offs, leng);

                mData.append(itr0, buff.array());
            }
        } else {
            ByteBuffer buff = ByteBuffer.allocate(MSGS_HDER_SIZE);

            buff.put((byte) (mNumb<<4 | 0<<3 | 0)); // mesg sequ, encr flag, reserved
            buff.put((byte) mOpco);
            buff.put((byte) ((0xFF&0x0F)<<4 | (segm&0x0F)<<0)); //  no payload
            buff.put((byte) 0x00); //  payload length

            mData.append(0, buff.array());
        }
    }

//    public byte[]
//    getPara() { mData }

}
