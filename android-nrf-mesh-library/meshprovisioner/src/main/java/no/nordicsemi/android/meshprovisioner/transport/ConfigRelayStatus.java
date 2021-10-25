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
import androidx.annotation.NonNull;
import android.util.Log;

import no.nordicsemi.android.meshprovisioner.opcodes.ConfigMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.RelaySettings;

/**
 * To be used as a wrapper class for when creating the ConfigRelayStatus message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ConfigRelayStatus extends ConfigStatusMessage implements Parcelable {

    private static final String TAG = ConfigRelayStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETWORK_TRANSMIT_STATUS;
    private int mRelay;
    private int mRelayRetransmitCount;
    private int mRelayRetransmitIntervalSteps;

    private static final Creator<ConfigRelayStatus> CREATOR = new Creator<ConfigRelayStatus>() {
        @Override
        public ConfigRelayStatus createFromParcel(Parcel in) {
            final AccessMessage message = in.readParcelable(AccessMessage.class.getClassLoader());
            //noinspection ConstantConditions
            return new ConfigRelayStatus(message);
        }

        @Override
        public ConfigRelayStatus[] newArray(int size) {
            return new ConfigRelayStatus[size];
        }
    };

    /**
     * Constructs a ConfigRelayStatus message.
     *
     * @param message Access message received
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigRelayStatus(@NonNull final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    final void parseStatusParameters() {
        final byte[] payload = ((AccessMessage) mMessage).getAccessPdu();
        mRelay = payload[2];
        mRelayRetransmitCount = payload[3] & 0b111;
        mRelayRetransmitIntervalSteps = (payload[3] >> 3) & 0b11111;
        Log.d(TAG, "Relay: " + mRelay);
        Log.d(TAG, "Retransmit count: " + mRelayRetransmitCount);
        Log.d(TAG, "Retransmit Interval steps: " + mRelayRetransmitIntervalSteps);
    }


    /**
     * Returns the {@link RelaySettings.RelayState} of the node
     */
    @RelaySettings.RelayState
    public int getRelay() {
        return mRelay;
    }

    /**
     * Returns the Relay retransmit count set in this message
     *
     * @return Relay retransmit count
     */
    public int getRelayRetransmitCount() {
        return mRelayRetransmitCount;
    }

    /**
     * Returns the Relay retransmit interval steps set in this message
     *
     * @return Relay retransmit interval steps
     */
    public int getRelayRetransmitIntervalSteps() {
        return mRelayRetransmitIntervalSteps;
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
