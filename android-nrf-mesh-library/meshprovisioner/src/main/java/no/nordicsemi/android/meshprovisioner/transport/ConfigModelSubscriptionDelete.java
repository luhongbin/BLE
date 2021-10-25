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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import no.nordicsemi.android.meshprovisioner.opcodes.ConfigMessageOpCodes;

import static no.nordicsemi.android.meshprovisioner.utils.MeshAddress.addressIntToBytes;
import static no.nordicsemi.android.meshprovisioner.utils.MeshAddress.isValidUnassignedAddress;
import static no.nordicsemi.android.meshprovisioner.utils.MeshAddress.isValidUnicastAddress;
import static no.nordicsemi.android.meshprovisioner.utils.MeshAddress.isValidVirtualAddress;

/**
 * Creates the ConfigModelSubscriptionDelete message
 */
@SuppressWarnings({"unused"})
public final class ConfigModelSubscriptionDelete extends ConfigMessage {

    private static final String TAG = ConfigModelSubscriptionDelete.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_MODEL_SUBSCRIPTION_DELETE;

    private static final int SIG_MODEL_APP_KEY_BIND_PARAMS_LENGTH = 6;
    private static final int VENDOR_MODEL_APP_KEY_BIND_PARAMS_LENGTH = 8;

    private final int mElementAddress;
    private final int mSubscriptionAddress;
    private final int mModelIdentifier;

    /**
     * Constructs ConfigModelSubscriptionDelete message.
     *
     * @param elementAddress      Address of the element to which the model belongs to.
     * @param subscriptionAddress Address to unsubscribe from or deleted.
     * @param modelIdentifier     identifier of the model, 16-bit for Sig model and 32-bit model id for vendor models.
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigModelSubscriptionDelete(final int elementAddress,
                                         final int subscriptionAddress,
                                         final int modelIdentifier) throws IllegalArgumentException {

        if (!isValidUnicastAddress(elementAddress))
            throw new IllegalArgumentException("Invalid unicast address, unicast address must be a 16-bit value, and must range from 0x0001 to 0x7FFF");
        this.mElementAddress = elementAddress;
        if (isValidUnassignedAddress(subscriptionAddress) || isValidUnicastAddress(subscriptionAddress) || isValidVirtualAddress(subscriptionAddress) || subscriptionAddress == 0xFFFF)
            throw new IllegalArgumentException("The value of the Address field shall not be an unassigned address, unicast address, " +
                    "all-nodes address or virtual address.");
        this.mSubscriptionAddress = subscriptionAddress;
        this.mModelIdentifier = modelIdentifier;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {

        final ByteBuffer paramsBuffer;
        //We check if the model identifier value is within the range of a 16-bit value here. If it is then it is a sigmodel
        final byte[] elementaddress = addressIntToBytes(mElementAddress);
        final byte[] subscriptionAddress = addressIntToBytes(mSubscriptionAddress);
        if (mModelIdentifier >= Short.MIN_VALUE && mModelIdentifier <= Short.MAX_VALUE) {
            paramsBuffer = ByteBuffer.allocate(SIG_MODEL_APP_KEY_BIND_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.put(elementaddress[1]);
            paramsBuffer.put(elementaddress[0]);
            paramsBuffer.put(subscriptionAddress[1]);
            paramsBuffer.put(subscriptionAddress[0]);
            paramsBuffer.putShort((short) mModelIdentifier);
            mParameters = paramsBuffer.array();
        } else {
            paramsBuffer = ByteBuffer.allocate(VENDOR_MODEL_APP_KEY_BIND_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.put(elementaddress[1]);
            paramsBuffer.put(elementaddress[0]);
            paramsBuffer.put(subscriptionAddress[1]);
            paramsBuffer.put(subscriptionAddress[0]);
            final byte[] modelIdentifier = new byte[]{(byte) ((mModelIdentifier >> 24) & 0xFF),
                    (byte) ((mModelIdentifier >> 16) & 0xFF), (byte) ((mModelIdentifier >> 8) & 0xFF), (byte) (mModelIdentifier & 0xFF)};
            paramsBuffer.put(modelIdentifier[1]);
            paramsBuffer.put(modelIdentifier[0]);
            paramsBuffer.put(modelIdentifier[3]);
            paramsBuffer.put(modelIdentifier[2]);
            mParameters = paramsBuffer.array();
        }
    }
}
