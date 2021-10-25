package com.phyplusinc.android.ppsp.trns;

public class PhyMISRMsgs extends PhyBaseMsgs {
    private final int    MSGS_MISR_OPCO = 0x02;

    PhyMISRMsgs() {
        setOpco(MSGS_MISR_OPCO);
        setType(MSGS_TYPE_ISSUE_RESP);
    }

//    @Override
//    public void setPara(byte[] para) {
//
//    }

//    @Override
//    public byte[] getPara() {
//        return new byte[0];
//    }
}
