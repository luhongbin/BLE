package com.phyplusinc.android.ppsp.trns;

import android.util.Log;

import com.phyplusinc.android.ppsp.BtLe.PhyPPSPMngr;

public class PhyMsgsHdlr implements PhyPPSPMngr.PPSPMsgrCallbacks {
    private final String TAG = getClass().getSimpleName();



    private PhyPPSPMngr mMsgr;
    private int         mMtus;

    private int         mMesgNumb = 1;
    private int         mSequNumb = 0;

    public void setMsgr(PhyPPSPMngr msgr) {
        this.mMsgr = msgr;
        this.mMtus = msgr.getMtus();
    }

    public void sndMISRMsgs(byte[] para) {
        PhyMISRMsgs msgs = new PhyMISRMsgs();
        msgs.setNumb(mMesgNumb);
        msgs.setPara(para);
        mMsgr.sndMsgs(msgs);
    }

    @Override
    public void onMsgrRspn() {
        Log.d(TAG, "onMsgrRspn: >>>>>");

    }

}
