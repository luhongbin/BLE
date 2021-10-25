package com.phyplusinc.android.phymeshprovisioner.node;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.adapter.SensorDescAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import no.nordicsemi.android.meshprovisioner.ApplicationKey;
import no.nordicsemi.android.meshprovisioner.models.SensorServer;
import no.nordicsemi.android.meshprovisioner.transport.Element;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.MeshModel;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.transport.SensorDescGet;
import no.nordicsemi.android.meshprovisioner.transport.SensorDescStatus;
import no.nordicsemi.android.meshprovisioner.transport.SensorGet;
import no.nordicsemi.android.meshprovisioner.transport.SensorStatus;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;

public class SensorServerActivity extends BaseModelConfigurationActivity {

    private static final String TAG = SensorServerActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private TextView mTvNumb;
    private TextView remainingTime;
    private Button mActionOnOff;
    protected int mTransitionStepResolution;
    protected int mTransitionSteps;

    private List<SensorDescStatus.DescInfo> mLsDesc = new ArrayList<>();
    private Map<Integer, SensorStatus.SnsrData> mMpData = new HashMap<>();
    private RecyclerView mRvDesc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final MeshModel model = mViewModel.getSelectedModel().getValue();
        if (model instanceof SensorServer) {
            final ConstraintLayout container = findViewById(R.id.node_controls_container);
            final View nodeControlsContainer = LayoutInflater.from(this).inflate(R.layout.layout_sensor_controls, container);

            mRvDesc = findViewById(R.id.rv_desc);
            mRvDesc.setLayoutManager(new LinearLayoutManager(this));
            final SensorDescAdapter adpt = new SensorDescAdapter(this, mViewModel.getNetworkLiveData().getMeshNetwork(), mLsDesc, mMpData);
            mRvDesc.setAdapter(adpt);


//            final TextView time = nodeControlsContainer.findViewById(R.id.transition_time);
            mTvNumb = nodeControlsContainer.findViewById(R.id.tv_desc_numb);
//            remainingTime = nodeControlsContainer.findViewById(R.id.transition_state);
//            final SeekBar transitionTimeSeekBar = nodeControlsContainer.findViewById(R.id.transition_seekbar);
//            transitionTimeSeekBar.setProgress(0);
//            transitionTimeSeekBar.incrementProgressBy(1);
//            transitionTimeSeekBar.setMax(230);
//
//            final SeekBar delaySeekBar = nodeControlsContainer.findViewById(R.id.delay_seekbar);
//            delaySeekBar.setProgress(0);
//            delaySeekBar.setMax(255);
//            final TextView delayTime = nodeControlsContainer.findViewById(R.id.delay_time);
//
//            mActionOnOff = nodeControlsContainer.findViewById(R.id.action_on);
//            mActionOnOff.setOnClickListener(v -> {
//                try {
//                    if (mActionOnOff.getText().toString().equals(getString(R.string.action_generic_on))) {
//                        sendGenericOnOff(true, delaySeekBar.getProgress());
//                    } else {
//                        sendGenericOnOff(false, delaySeekBar.getProgress());
//                    }
//                } catch (IllegalArgumentException ex) {
//                    mViewModel.displaySnackBar(this, mContainer, ex.getMessage(), Snackbar.LENGTH_LONG);
//                }
//            });
//
            mActionRead = nodeControlsContainer.findViewById(R.id.action_read);
            mActionRead.setOnClickListener(v -> sendSnsrDescGet());
//
//            transitionTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//                int lastValue = 0;
//                double res = 0.0;
//
//                @Override
//                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
//
//                    if (progress >= 0 && progress <= 62) {
//                        lastValue = progress;
//                        mTransitionStepResolution = 0;
//                        mTransitionSteps = progress;
//                        res = progress / 10.0;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(res), "s"));
//                    } else if (progress >= 63 && progress <= 118) {
//                        if (progress > lastValue) {
//                            mTransitionSteps = progress - 56;
//                            lastValue = progress;
//                        } else if (progress < lastValue) {
//                            mTransitionSteps = -(56 - progress);
//                        }
//                        mTransitionStepResolution = 1;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps), "s"));
//
//                    } else if (progress >= 119 && progress <= 174) {
//                        if (progress > lastValue) {
//                            mTransitionSteps = progress - 112;
//                            lastValue = progress;
//                        } else if (progress < lastValue) {
//                            mTransitionSteps = -(112 - progress);
//                        }
//                        mTransitionStepResolution = 2;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "s"));
//                    } else if (progress >= 175 && progress <= 230) {
//                        if (progress >= lastValue) {
//                            mTransitionSteps = progress - 168;
//                            lastValue = progress;
//                        } else {
//                            mTransitionSteps = -(168 - progress);
//                        }
//                        mTransitionStepResolution = 3;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "min"));
//                    }
//                }
//
//                @Override
//                public void onStartTrackingTouch(final SeekBar seekBar) {
//
//                }
//
//                @Override
//                public void onStopTrackingTouch(final SeekBar seekBar) {
//
//                }
//            });
//
//            delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//                @Override
//                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
//                    delayTime.setText(getString(R.string.transition_time_interval, String.valueOf(progress * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms"));
//                }
//
//                @Override
//                public void onStartTrackingTouch(final SeekBar seekBar) {
//
//                }
//
//                @Override
//                public void onStopTrackingTouch(final SeekBar seekBar) {
//
//                }
//            });
        }
    }

    @Override
    protected void enableClickableViews() {
        super.enableClickableViews();
        if (mActionOnOff != null && !mActionOnOff.isEnabled())
            mActionOnOff.setEnabled(true);
    }

    @Override
    protected void disableClickableViews() {
        super.disableClickableViews();
        if (mActionOnOff != null)
            mActionOnOff.setEnabled(false);
    }

    @Override
    protected void updateMeshMessage(final MeshMessage meshMessage) {
        super.updateMeshMessage(meshMessage);

        if (meshMessage instanceof SensorDescStatus) {
            mLsDesc.clear();
            mLsDesc.addAll(((SensorDescStatus) meshMessage).getDescInfo());
            if ( !mLsDesc.isEmpty() ) {
                sendSnsrGet();

                mTvNumb.setText(getString(R.string.snsr_numb, mLsDesc.size()));
                mRvDesc.getAdapter().notifyDataSetChanged();
            }
        } else
        if ( meshMessage instanceof SensorStatus ) {
            mMpData.clear();
            mMpData.putAll(((SensorStatus) meshMessage).getSnsrData());
            if ( !mMpData.isEmpty() ) {
                mRvDesc.getAdapter().notifyDataSetChanged();
            }
        }
    }

    /**
     * Send generic on off get to mesh node
     */
    public void sendSnsrDescGet() {
        if (!checkConnectivity()) return;
        final Element element = mViewModel.getSelectedElement().getValue();
        if (element != null) {
            final MeshModel model = mViewModel.getSelectedModel().getValue();
            if (model != null) {
                if (!model.getBoundAppKeyIndexes().isEmpty()) {
                    final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
                    final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);

                    final int address = element.getElementAddress();
                    Log.v(TAG, "Sending message to element's unicast address: " + MeshAddress.formatAddress(address, true));

                    final SensorDescGet snsrDescGet = new SensorDescGet(appKey);
                    sendMessage(address, snsrDescGet);
                } else {
                    mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    /**
     * Send generic on off set to mesh node
     */
    public void sendSnsrGet() {
        if (!checkConnectivity()) return;
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        if (node != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    if (!model.getBoundAppKeyIndexes().isEmpty()) {
                        final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
                        final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
                        final int address = element.getElementAddress();
                        final SensorGet genericOnOffSet = new SensorGet(appKey);
                        sendMessage(address, genericOnOffSet);
                    } else {
                        mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
                    }
                }
            }
        }
    }
}
