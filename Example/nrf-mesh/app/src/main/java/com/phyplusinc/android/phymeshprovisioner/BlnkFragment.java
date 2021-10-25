/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.phyplusinc.android.phymeshprovisioner;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetVendorDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public class BlnkFragment extends Fragment {
    private static final String AKEY_INDX = "AKEY_INDX";
    private static final String MODL_NUMB = "MODL_NUMB";

    private int mIntModlNumb;
    private int mIntAKeyIndx;

    private static final int CURV_NUMB = 3;
    private static final int SMPL_NUMB = 10;
    private static final int[] CURV_CLRS = {Color.RED, Color.GREEN, Color.BLUE};

    private final Object mObjLock = new Object();
    private int mIntIndx = 0;
    private int mIntStep = 0;
    private int mIntRept = 0;
    private final List<List> mCrvData = new ArrayList<>();
    private final Handler mHdlXfer = new Handler();
    private final Runnable mRunBlnk = new Runnable() {
        @Override
        public void run() {
            final String strOpCo = "D1".trim();
            final StringBuilder strPara = new StringBuilder("2401".trim());

            for (List set : mCrvData) {
                strPara.append(String.format(Locale.US, "%02x", (int) set.get(mIntIndx)));
            }

            final byte[] params;
            if (TextUtils.isEmpty(strPara.toString()) && strPara.length() == 0) {
                params = null;
            } else {
                params = MeshParserUtils.toByteArray(strPara.toString());
            }

            ((BottomSheetVendorDialogFragment.BottomSheetVendorModelControlsListener) requireActivity())
                    .sendVendorModelMessage(mIntModlNumb, mIntAKeyIndx, Integer.parseInt(strOpCo, 16), params, false);

            synchronized (mObjLock) {
                mIntIndx += 1;
                if (SMPL_NUMB <= mIntIndx) mIntRept -= 1;
                mIntIndx %= SMPL_NUMB;
                if (0 < mIntRept && 0 < mIntStep) {
                    mHdlXfer.postDelayed(this, mIntStep * 10);
                } else {
                    mBtSend.setText(R.string.acti_play);
                }
            }
        }
    };

    private LineChart mLcCurv;
    private Button mBtSend;


    public static BlnkFragment getInstance(final int modelId, final int appKeyIndex) {
        final BlnkFragment fragment = new BlnkFragment();
        final Bundle args = new Bundle();
        args.putInt(MODL_NUMB, modelId);
        args.putInt(AKEY_INDX, appKeyIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mIntModlNumb = getArguments().getInt(MODL_NUMB);
            mIntAKeyIndx = getArguments().getInt(AKEY_INDX);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mHdlXfer.removeCallbacks(mRunBlnk);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View vwRoot = inflater.inflate(R.layout.fragment_blnk, null);

        mLcCurv = vwRoot.findViewById(R.id.lc_curv);
        mLcCurv.getDescription().setEnabled(false);
        mLcCurv.getAxisLeft().setDrawLabels(false);
        mLcCurv.getAxisRight().setDrawLabels(false);
        mLcCurv.getLegend().setEnabled(false);

        final TextView tvStep = vwRoot.findViewById(R.id.tv_publ_step_valu);
        final TextView tvRept = vwRoot.findViewById(R.id.tv_publ_rept_valu);
        final SeekBar sbStep = vwRoot.findViewById(R.id.sb_publ_step_seek);
        final SeekBar sbRept = vwRoot.findViewById(R.id.sb_publ_rept_seek);
        mBtSend = vwRoot.findViewById(R.id.action_send);

        sbStep.setMax(50);
        sbStep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int prgs, boolean user) {
                if (0 >= prgs) {
                    tvStep.setText(R.string.disabled);
                    mBtSend.setEnabled(false);
                } else {
                    tvStep.setText(getString(R.string.time_ms, prgs * 10));
                    if (0 < sbRept.getProgress())
                        mBtSend.setEnabled(true);
                }

                synchronized (mObjLock) {
                    mIntStep = prgs;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbStep.incrementProgressBy(1);

        sbRept.setMax(100);
        sbRept.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int prgs, boolean user) {
                if (0 >= prgs) {
                    tvRept.setText(R.string.disabled);
                    mBtSend.setEnabled(false);
                } else {
                    tvRept.setText(String.format(Locale.US, "%d", prgs));
                    if (0 < sbStep.getProgress())
                        mBtSend.setEnabled(true);
                }

                synchronized (mObjLock) {
                    mIntRept = prgs;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbRept.incrementProgressBy(1);

        mBtSend.setOnClickListener(v -> {
            if ( mHdlXfer.hasCallbacks(mRunBlnk) ) {
                mHdlXfer.removeCallbacks(mRunBlnk);

                mBtSend.setText(R.string.acti_play);
            } else {
                mHdlXfer.removeCallbacks(mRunBlnk);

                mBtSend.setText(R.string.acti_stop);

                synchronized (mObjLock) {
                    mIntRept = sbRept.getProgress();
                }
                mHdlXfer.post(mRunBlnk);
            }
        });

        return vwRoot;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        mCrvData.clear();
        for (int z = 0; z < CURV_NUMB; z++) {
            ArrayList<Entry> values = new ArrayList<>();
            List smpl = new ArrayList();

            for (int i = 0; i < SMPL_NUMB; i++) {
                int val = (int) (Math.random() * 255);
                values.add(new Entry(i, (float) val));
                smpl.add(val);
            }

            mCrvData.add(smpl);
            LineDataSet d = new LineDataSet(values, "DataSet " + (z + 1));
            d.setLineWidth(2.5f);
            d.setCircleRadius(4f);


//            int color = colors[z % colors.length];
            d.setColor(CURV_CLRS[z]);
            d.setCircleColor(CURV_CLRS[z]);
            dataSets.add(d);
        }

        LineData Data = new LineData(dataSets);
        mLcCurv.setData(Data);
        mLcCurv.invalidate();
    }
}
