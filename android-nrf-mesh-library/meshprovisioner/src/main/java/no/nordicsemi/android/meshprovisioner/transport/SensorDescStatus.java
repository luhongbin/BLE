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

package no.nordicsemi.android.meshprovisioner.transport;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.meshprovisioner.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;

/**
 * To be used as a wrapper class to create generic level status message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SensorDescStatus extends GenericStatusMessage implements Parcelable {

    private static final String TAG = SensorDescStatus.class.getSimpleName();
    private static final int SENSOR_DESC_STATUS_MANDATORY_LENGTH = 8;
    private static final int OP_CODE = ApplicationMessageOpCodes.SENSOR_DESC_STATUS;

    private List<DescInfo> mDescInfo;

    private static final Creator<SensorDescStatus> CREATOR = new Creator<SensorDescStatus>() {
        @Override
        public SensorDescStatus createFromParcel(Parcel in) {
            final AccessMessage message = in.readParcelable(AccessMessage.class.getClassLoader());
            //noinspection ConstantConditions
            return new SensorDescStatus(message);
        }

        @Override
        public SensorDescStatus[] newArray(int size) {
            return new SensorDescStatus[size];
        }
    };

    /**
     * Constructs GenericLevelStatus message
     * @param message access message
     */
    public SensorDescStatus(@NonNull final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        this.mDescInfo = new ArrayList<>();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        Log.v(TAG, "Received sensor descriptor status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        if ( buffer.limit() > SENSOR_DESC_STATUS_MANDATORY_LENGTH && 0 == (buffer.limit() % SENSOR_DESC_STATUS_MANDATORY_LENGTH) ) {
            byte[] temp = new byte[SENSOR_DESC_STATUS_MANDATORY_LENGTH];
            for ( int itr0 = 0; itr0 < buffer.limit() / SENSOR_DESC_STATUS_MANDATORY_LENGTH; itr0 += 1 ) {
                buffer.get(temp);
                DescInfo desc = new DescInfo(temp);
                mDescInfo.add(desc);
            }
        }
    }

    @Override
    int getOpCode() {
        return OP_CODE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        final AccessMessage message = (AccessMessage) mMessage;
        dest.writeParcelable(message, flags);
    }

    /**
     * Returns the descriptor info of the GenericOnOffModel
     *
     * @return List<DescInfo>
     */
    public final List<DescInfo> getDescInfo() {
        return mDescInfo;
    }

    public final class DescInfo {
        int mProperty;
        int mNegaTolr;
        int mPosiTolr;
        int mSmplFunc;
        int mMeasPerd;
        int mUpdaIntr;

        DescInfo(final byte[] para) {
            final ByteBuffer buff = ByteBuffer.wrap(para).order(ByteOrder.LITTLE_ENDIAN);
            int byteTolr;

            mProperty = (int) (buff.getShort());

            byteTolr = buff.get() & 0xFF;
            mNegaTolr = byteTolr;
            byteTolr = buff.get() & 0xFF;
            mNegaTolr = byteTolr & 0x0F << 8 | mNegaTolr;

            mPosiTolr = byteTolr & 0xF0 << 4;
            byteTolr = buff.get() & 0xFF;
            mPosiTolr = byteTolr & 0x0F << 8 | mPosiTolr;

            mSmplFunc = buff.get() & 0xFF;
            mMeasPerd = buff.get() & 0xFF;
            mUpdaIntr = buff.get() & 0xFF;

            Log.v(TAG, "Property: " + mProperty);
            Log.v(TAG, "Negative Tolerance: " + mNegaTolr);
            Log.v(TAG, "Positive Tolerance: " + mPosiTolr);
            Log.v(TAG, "Sampling Method: " + mSmplFunc);
            Log.v(TAG, "Measurement Period: " + mMeasPerd);
            Log.v(TAG, "Update Interval: " + mUpdaIntr);
        }

        public int getProperty() { return mProperty; }
    }
}
