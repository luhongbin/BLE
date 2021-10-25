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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nordicsemi.android.meshprovisioner.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

/**
 * To be used as a wrapper class to create generic level status message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SensorStatus extends GenericStatusMessage implements Parcelable {

    private static final String TAG = SensorStatus.class.getSimpleName();

    private static final int SENSOR_DESC_STATUS_MANDATORY_LENGTH = 2;
    private static final int OP_CODE = ApplicationMessageOpCodes.SENSOR_STATUS;

    private Map<Integer,SnsrData> mSnsrData;

    private static final Creator<SensorStatus> CREATOR = new Creator<SensorStatus>() {
        @Override
        public SensorStatus createFromParcel(Parcel in) {
            final AccessMessage message = in.readParcelable(AccessMessage.class.getClassLoader());
            //noinspection ConstantConditions
            return new SensorStatus(message);
        }

        @Override
        public SensorStatus[] newArray(int size) {
            return new SensorStatus[size];
        }
    };

    /**
     * Constructs GenericLevelStatus message
     * @param message access message
     */
    public SensorStatus(@NonNull final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        this.mSnsrData = new HashMap<>();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        Log.v(TAG, "Received sensor status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        Log.d(TAG, "parseStatusParameters: " + MeshParserUtils.bytesToHex(buffer.array(), true));

        final int limi = buffer.limit();
        int posi = 0;
        int leng;
        int prop;
        byte temp;

        // get sensor desc + data size
        while ( true ) {
            temp = buffer.get();
            posi = posi + 1;
            if (0x00 == (temp & 0x01)) {
                if (1 > limi - posi) break;

                leng = (temp & 0x1E) >> 1;
                prop = buffer.get();
                prop = ((prop & 0xFF) << 3) | ((temp & 0xE0) >> 5);
                posi = posi + 1;
            } else {
                if (2 > limi - posi) break;

                leng = (temp & 0xFE) >> 1;
                prop = buffer.getShort();
                posi = posi + 2;
            }
            if (leng > limi - posi) break;

            byte[] byts = new byte[leng];
            buffer.get(byts);
            posi = posi + leng;

            SnsrData data = new SnsrData(prop, byts);
            mSnsrData.put(prop, data);

            if (1 > limi - posi) break;
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
    public final Map<Integer,SnsrData> getSnsrData() {
        return mSnsrData;
    }

    public final class SnsrData {
        int     mProperty;
        byte[]  mSnsrData;

        SnsrData(final int prop, final byte[] para) {
            mProperty = prop;
            mSnsrData = para.clone();

            Log.v(TAG, "Property: " + mProperty);
            Log.v(TAG, "Sensor Data: " + MeshParserUtils.bytesToHex(mSnsrData, true));
        }

        public int getProperty() { return mProperty; }

        public byte[] getSnsrData() { return mSnsrData; }
    }
}
