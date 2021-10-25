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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import no.nordicsemi.android.meshprovisioner.opcodes.ConfigMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

/**
 * Creates the ConfigNetKeyStatus Message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ConfigNetKeyStatus extends ConfigStatusMessage implements Parcelable {

    private static final String TAG = ConfigNetKeyStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETKEY_STATUS;
    private int mNetKeyIndex;

    public static final Creator<ConfigNetKeyStatus> CREATOR = new Creator<ConfigNetKeyStatus>() {
        @Override
        public ConfigNetKeyStatus createFromParcel(Parcel in) {
            final AccessMessage message = in.readParcelable(AccessMessage.class.getClassLoader());
            //noinspection ConstantConditions
            return new ConfigNetKeyStatus(message);
        }

        @Override
        public ConfigNetKeyStatus[] newArray(int size) {
            return new ConfigNetKeyStatus[size];
        }
    };

    /**
     * Constructs the ConfigNetKeyStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigNetKeyStatus(@NonNull final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        final ArrayList<Integer> keyIndexes = decode(mParameters.length, 1);
        //NetKey status will only contain one index so we just take the first element
        mNetKeyIndex = keyIndexes.get(0);//ByteBuffer.wrap(netKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();

        Log.v(TAG, "Status code: " + mStatusCode);
        Log.v(TAG, "Status message: " + mStatusCodeName);
        Log.v(TAG, "Net key index: " + Integer.toHexString(mNetKeyIndex));
    }

    @Override
    public final int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the global index of the net key.
     *
     * @return netkey index
     */
    public final int getNetKeyIndex() {
        return mNetKeyIndex;
    }

    /**
     * Returns if the message was successful
     *
     * @return true if the message was successful or false otherwise
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
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
}
