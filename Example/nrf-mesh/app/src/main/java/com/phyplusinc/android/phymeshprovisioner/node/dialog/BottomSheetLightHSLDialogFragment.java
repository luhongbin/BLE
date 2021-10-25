package com.phyplusinc.android.phymeshprovisioner.node.dialog;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;

import com.phyplusinc.android.phymeshprovisioner.R;

public class BottomSheetLightHSLDialogFragment extends BottomSheetDialogFragment {
    private static final String PARA_MULT_ADDR = "PARA_MULT_ADDR";
    private static final String PARA_UNIC_ADDR = "PARA_UNIC_ADDR";
    private static final String PARA_MODEL_ID = "MODEL_ID";
    private static final String PARA_KEYS_ID = "PARA_KEYS_ID";

    private int mKeyIndex;


    private Button mRed;
    private Button mGrn;
    private Button mBlue;

    private TextView mHueLevl;
    private TextView mSatLevl;
    private TextView mLigLevl;

    private SeekBar mHueSeekBar;
    private SeekBar mSatSeekBar;
    private SeekBar mLigSeekBar;

    private final View.OnClickListener mRGBOnClickListener = view -> {
        float outHsl[] = new float[3];
        if (R.id.button_red == view.getId()) {
            ColorUtils.colorToHSL(Color.rgb(0xff, 0x00, 0x00), outHsl);
        } else
        if (R.id.button_green == view.getId()) {
            ColorUtils.colorToHSL(Color.rgb(0x00, 0xff, 0x00), outHsl);
        } else
        if (R.id.button_blue == view.getId()) {
            ColorUtils.colorToHSL(Color.rgb(0x00, 0x00, 0xff), outHsl);
        }

        mHueSeekBar.setProgress((int)outHsl[0]);
        mSatSeekBar.setProgress((int)(outHsl[1]*100F));
        mLigSeekBar.setProgress((int)(outHsl[2]*100F));

        final int hueValue = (int) (((float)0xFFFF * (float)mHueSeekBar.getProgress()) / 360F);
        final int satValue = (int) (((float)0xFFFF * (float)mSatSeekBar.getProgress()) / 100F);
        final int ligValue = (int) (((float)0xFFFF * (float)mLigSeekBar.getProgress()) / 100F);
        try {
            ((BottomSheetHSLListener) requireActivity()).toggleLevel(mKeyIndex, hueValue, satValue, ligValue);
        } catch (IllegalArgumentException ex) {
            Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private final SeekBar.OnSeekBarChangeListener mHSLOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (R.id.hue_seekbar == seekBar.getId()) {
                mHueLevl.setText(getString(R.string.light_hue_interval, String.valueOf(progress)));
            } else
            if (R.id.sat_seekbar == seekBar.getId()) {
                mSatLevl.setText(getString(R.string.light_saturation_interval, String.valueOf(progress)));
            } else
            if (R.id.lig_seekbar == seekBar.getId()) {
                mLigLevl.setText(getString(R.string.light_lightness_interval, String.valueOf(progress)));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            final int hueValue = (int) (((float)0xFFFF * (float)mHueSeekBar.getProgress()) / 360F);
            final int satValue = (int) (((float)0xFFFF * (float)mSatSeekBar.getProgress()) / 100F);
            final int ligValue = (int) (((float)0xFFFF * (float)mLigSeekBar.getProgress()) / 100F);

            try {
                ((BottomSheetHSLListener) requireActivity()).toggleLevel(mKeyIndex, hueValue, satValue, ligValue);
            } catch (IllegalArgumentException ex) {
                Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };

//    private final View.OnClickListener mOnOffClickListener = view -> {
//        if (R.id.action_on == view.getId()) {
//            try {
//                ((BottomSheetHSLListener) requireActivity()).toggle(mUnicast.isChecked()?mUnicAddr:mMultAddr, mModelId, mKeyIndex, true, 0, 0, 0);
//            } catch (IllegalArgumentException ex) {
//                Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        } else
//        if (R.id.action_off == view.getId()) {
//            try {
//                ((BottomSheetHSLListener) requireActivity()).toggle(mUnicast.isChecked()?mUnicAddr:mMultAddr, mModelId, mKeyIndex, false, 0, 0, 0);
//            } catch (IllegalArgumentException ex) {
//                Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        }
//    };

    public interface BottomSheetHSLListener {
//        void toggle(final int destAddr, final int modelId, final int keyIndex, boolean state, final int transitionSteps, final int transitionStepResolution, final int delay);
        void toggleLevel(final int keyIndex, final int hue, final int sat, final int lit);
    }

    public static BottomSheetLightHSLDialogFragment getInstance(final int appKeyIndex) {
        final BottomSheetLightHSLDialogFragment fragment = new BottomSheetLightHSLDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(PARA_KEYS_ID, appKeyIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mKeyIndex = getArguments().getInt(PARA_KEYS_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View nodeControlsContainer = inflater.inflate(R.layout.layout_light_hsl_bottom_sheet, container, false);

        mRed = nodeControlsContainer.findViewById(R.id.button_red);
        mGrn = nodeControlsContainer.findViewById(R.id.button_green);
        mBlue = nodeControlsContainer.findViewById(R.id.button_blue);

        mHueLevl = nodeControlsContainer.findViewById(R.id.hue_levl);
        mHueSeekBar = nodeControlsContainer.findViewById(R.id.hue_seekbar);
        mHueSeekBar.setProgress(0);
        mHueSeekBar.setMax(360);

        mSatLevl = nodeControlsContainer.findViewById(R.id.saturation_levl);
        mSatSeekBar = nodeControlsContainer.findViewById(R.id.sat_seekbar);
        mSatSeekBar.setProgress(100);
        mSatSeekBar.setMax(100);

        mLigLevl = nodeControlsContainer.findViewById(R.id.lightness_levl);
        mLigSeekBar = nodeControlsContainer.findViewById(R.id.lig_seekbar);
        mLigSeekBar.setProgress(50);
        mLigSeekBar.setMax(100);

        mRed.setOnClickListener(mRGBOnClickListener);
        mGrn.setOnClickListener(mRGBOnClickListener);
        mBlue.setOnClickListener(mRGBOnClickListener);

        mHueSeekBar.setOnSeekBarChangeListener(mHSLOnSeekBarChangeListener);
        mSatSeekBar.setOnSeekBarChangeListener(mHSLOnSeekBarChangeListener);
        mLigSeekBar.setOnSeekBarChangeListener(mHSLOnSeekBarChangeListener);

//        mOn.setOnClickListener(mOnOffClickListener);
//        mOff.setOnClickListener(mOnOffClickListener);

        return nodeControlsContainer;
    }
}
