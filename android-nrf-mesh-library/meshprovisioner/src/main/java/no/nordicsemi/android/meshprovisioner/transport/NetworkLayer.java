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

import android.util.Log;
import android.util.SparseArray;

import org.spongycastle.crypto.InvalidCipherTextException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import no.nordicsemi.android.meshprovisioner.MeshManagerApi;
import no.nordicsemi.android.meshprovisioner.NetworkKey;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.utils.ExtendedInvalidCipherTextException;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils;

/**
 * NetworkLayer implementation of the mesh network architecture as per the mesh profile specification.
 * <p>
 * NetworkLayer encrypts/decrypts mesh messages to be sent or received by the nodes in a mesh network.
 * <p/>
 */
abstract class NetworkLayer extends LowerTransportLayer {

    private static final String TAG = NetworkLayer.class.getSimpleName();
    NetworkLayerCallbacks mNetworkLayerCallbacks;
    private SparseArray<byte[]> segmentedAccessMessagesMessages;
    private SparseArray<byte[]> segmentedControlMessagesMessages;

    /**
     * Set network layer callbacks
     *
     * @param callbacks {@link NetworkLayerCallbacks} callbacks
     */
    abstract void setNetworkLayerCallbacks(@NonNull final NetworkLayerCallbacks callbacks);

    /**
     * Creates a mesh message
     *
     * @param message Message could be of type access or control message.
     */
    protected final void createMeshMessage(@NonNull final Message message) {
        if (message instanceof AccessMessage) {
            super.createMeshMessage(message);
        } else {
            super.createMeshMessage(message);
        }
        createNetworkLayerPDU(message);
    }

    /**
     * Creates a vendor model mesh message
     *
     * @param message Message could be of type access or control message.
     */
    protected final void createVendorMeshMessage(@NonNull final Message message) {
        if (message instanceof AccessMessage) {
            super.createVendorMeshMessage(message);
        } else {
            super.createVendorMeshMessage(message);
        }
        createNetworkLayerPDU(message);
    }

    @Override
    public final Message createNetworkLayerPDU(@NonNull final Message message) {
        final SecureUtils.K2Output k2Output = getK2Output(message);
        final int nid = k2Output.getNid();
        final byte[] encryptionKey = k2Output.getEncryptionKey();
        Log.v(TAG, "Encryption key: " + MeshParserUtils.bytesToHex(encryptionKey, false));

        final byte[] privacyKey = k2Output.getPrivacyKey();
        Log.v(TAG, "Privacy key: " + MeshParserUtils.bytesToHex(privacyKey, false));
        final int ctl = message.getCtl();
        final int ttl = message.getTtl();
        final int ivi = message.getIvIndex()[3] & 0x01; // least significant bit of IV Index
        final byte iviNID = (byte) ((ivi << 7) | nid);
        final byte ctlTTL = (byte) ((ctl << 7) | ttl);

        final int src = message.getSrc();
        final SparseArray<byte[]> lowerTransportPduMap;
        final SparseArray<byte[]> encryptedPduPayload = new SparseArray<>();
        final List<byte[]> sequenceNumbers = new ArrayList<>();

        final int pduType = message.getPduType();
        switch (message.getPduType()) {
            case MeshManagerApi.PDU_TYPE_NETWORK:
                if (message instanceof AccessMessage) {
                    lowerTransportPduMap = ((AccessMessage) message).getLowerTransportAccessPdu();
                } else {
                    lowerTransportPduMap = ((ControlMessage) message).getLowerTransportControlPdu();
                }
                for (int i = 0; i < lowerTransportPduMap.size(); i++) {
                    final byte[] lowerTransportPdu = lowerTransportPduMap.get(i);
                    if (i != 0) {
                        final int sequenceNumber = incrementSequenceNumber(mNetworkLayerCallbacks.getProvisioner(), message.getSequenceNumber());
                        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);
                        message.setSequenceNumber(sequenceNum);
                    }
                    sequenceNumbers.add(message.getSequenceNumber());
                    Log.v(TAG, "Sequence Number: " + MeshParserUtils.bytesToHex(sequenceNumbers.get(i), false));
                    final byte[] nonce = createNetworkNonce(ctlTTL, sequenceNumbers.get(i), src, message.getIvIndex());
                    final byte[] encryptedPayload = encryptPdu(lowerTransportPdu, encryptionKey, nonce, message.getDst(), SecureUtils.getNetMicLength(message.getCtl()));
                    encryptedPduPayload.put(i, encryptedPayload);
                    Log.v(TAG, "Encrypted Network payload: " + MeshParserUtils.bytesToHex(encryptedPayload, false));
                }
                break;
            case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                lowerTransportPduMap = ((ControlMessage) message).getLowerTransportControlPdu();
                for (int i = 0; i < lowerTransportPduMap.size(); i++) {
                    final byte[] lowerTransportPdu = lowerTransportPduMap.get(i);
                    mNetworkLayerCallbacks.getProvisioner(message.getSrc());
                    final int sequenceNumber = incrementSequenceNumber(message.getSrc());
                    final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);
                    message.setSequenceNumber(sequenceNum);
                    sequenceNumbers.add(message.getSequenceNumber());
                    final byte[] nonce = createProxyNonce(message.getSequenceNumber(), src, message.getIvIndex());
                    final byte[] encryptedPayload = encryptPdu(lowerTransportPdu, encryptionKey, nonce, message.getDst(), SecureUtils.getNetMicLength(message.getCtl()));
                    encryptedPduPayload.put(i, encryptedPayload);
                    Log.v(TAG, "Encrypted Network payload: " + MeshParserUtils.bytesToHex(encryptedPayload, false));
                }
                break;
        }

        final SparseArray<byte[]> pduArray = new SparseArray<>();
        for (int i = 0; i < encryptedPduPayload.size(); i++) {
            //Create the privacy random
            final byte[] encryptedPayload = encryptedPduPayload.get(i);
            final byte[] privacyRandom = createPrivacyRandom(encryptedPayload);
            //Next we create the PECB
            final byte[] pecb = createPECB(message.getIvIndex(), privacyRandom, privacyKey);

            final byte[] header = obfuscateNetworkHeader(ctlTTL, sequenceNumbers.get(i), src, pecb);
            final byte[] pdu = ByteBuffer.allocate(1 + 1 + header.length + encryptedPayload.length).order(ByteOrder.BIG_ENDIAN)
                    .put((byte) pduType)
                    .put(iviNID)
                    .put(header)
                    .put(encryptedPayload)
                    .array();
            pduArray.put(i, pdu);
            message.setNetworkLayerPdu(pduArray);
        }

        return message;
    }

    @SuppressWarnings("ConstantConditions")
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    final Message createRetransmitNetworkLayerPDU(@NonNull final Message message, final int segment) {
        final SecureUtils.K2Output k2Output = getK2Output(message);
        final int nid = k2Output.getNid();
        final byte[] encryptionKey = k2Output.getEncryptionKey();
        Log.v(TAG, "Encryption key: " + MeshParserUtils.bytesToHex(encryptionKey, false));

        final byte[] privacyKey = k2Output.getPrivacyKey();
        Log.v(TAG, "Privacy key: " + MeshParserUtils.bytesToHex(privacyKey, false));
        final int ctl = message.getCtl();
        final int ttl = message.getTtl();
        final int ivi = message.getIvIndex()[3] & 0x01; // least significant bit of IV Index
        final byte iviNID = (byte) ((ivi << 7) | nid);
        final byte ctlTTL = (byte) ((ctl << 7) | ttl);

        final int src = message.getSrc();
        final SparseArray<byte[]> lowerTransportPduMap;
        if (message instanceof AccessMessage) {
            lowerTransportPduMap = ((AccessMessage) message).getLowerTransportAccessPdu();
        } else {
            lowerTransportPduMap = ((ControlMessage) message).getLowerTransportControlPdu();
        }

        byte[] encryptedNetworkPayload = null;
        final int pduType = message.getPduType();
        if (message.getPduType() == MeshManagerApi.PDU_TYPE_NETWORK) {
            final byte[] lowerTransportPdu = lowerTransportPduMap.get(segment);
            final int sequenceNumber = incrementSequenceNumber(mNetworkLayerCallbacks.getProvisioner(), message.getSequenceNumber());
            final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);
            message.setSequenceNumber(sequenceNum);

            Log.v(TAG, "Sequence Number: " + MeshParserUtils.bytesToHex(sequenceNum, false));

            final byte[] nonce = createNetworkNonce(ctlTTL, sequenceNum, src, message.getIvIndex());
            encryptedNetworkPayload = encryptPdu(lowerTransportPdu, encryptionKey, nonce, message.getDst(), SecureUtils.getNetMicLength(message.getCtl()));
            if (encryptedNetworkPayload == null)
                return null;
            Log.v(TAG, "Encrypted Network payload: " + MeshParserUtils.bytesToHex(encryptedNetworkPayload, false));
        }

        final SparseArray<byte[]> pduArray = new SparseArray<>();
        if (encryptedNetworkPayload == null)
            return null;

        final byte[] privacyRandom = createPrivacyRandom(encryptedNetworkPayload);
        //Next we create the PECB
        final byte[] pecb = createPECB(message.getIvIndex(), privacyRandom, privacyKey);

        final byte[] header = obfuscateNetworkHeader(ctlTTL, message.getSequenceNumber(), src, pecb);
        final byte[] pdu = ByteBuffer.allocate(1 + 1 + header.length + encryptedNetworkPayload.length).order(ByteOrder.BIG_ENDIAN)
                .put((byte) pduType)
                .put(iviNID)
                .put(header)
                .put(encryptedNetworkPayload)
                .array();
        pduArray.put(segment, pdu);
        message.setNetworkLayerPdu(pduArray);
        return message;
    }

    /**
     * Parse received mesh message
     * <p>
     * This method will drop messages with an invalid sequence number as all mesh messages are supposed to have a sequence
     * </p>
     *
     * @param data                    PDU received from the mesh node
     * @param decryptedNetworkPayload Decrypted network payload
     * @return complete {@link Message} that was successfully parsed or null otherwise
     */
    final Message parseMeshMessage(@NonNull final ProvisionedMeshNode node,
                                   @NonNull final byte[] data,
                                   @NonNull final byte[] networkHeader,
                                   final byte[] decryptedNetworkPayload) throws ExtendedInvalidCipherTextException {
        mMeshNode = node;
        final Provisioner provisioner = mNetworkLayerCallbacks.getProvisioner();
        final int ctlTtl = networkHeader[0];
        final int ctl = (ctlTtl >> 7) & 0x01;
        final int ttl = ctlTtl & 0x7F;
        Log.v(TAG, "TTL for received message: " + ttl);
        final byte[] sequenceNumber = ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN).put(networkHeader, 1, 3).array();
        final int src = MeshParserUtils.unsignedBytesToInt(networkHeader[5], networkHeader[4]);
        if (ctl == 1) {
            return parseControlMessage(provisioner.getProvisionerAddress(), data, networkHeader, decryptedNetworkPayload, src, sequenceNumber);
        } else {
            return parseAccessMessage(data, networkHeader, decryptedNetworkPayload, src, sequenceNumber);
        }
    }

    /**
     * Parses access message
     *
     * @param data                    Received from the node
     * @param networkHeader           De-obfuscated network header
     * @param decryptedNetworkPayload Decrypted network payload
     * @param src                     Source address
     * @param sequenceNumber          Sequence number of the received message
     * @return access message
     */
    @VisibleForTesting
    private AccessMessage parseAccessMessage(@NonNull final byte[] data,
                                             @NonNull final byte[] networkHeader,
                                             @NonNull final byte[] decryptedNetworkPayload,
                                             final int src,
                                             @NonNull final byte[] sequenceNumber) throws ExtendedInvalidCipherTextException {
        try {
            final int ttl = networkHeader[0] & 0x7F;
            final int dst = MeshParserUtils.unsignedBytesToInt(decryptedNetworkPayload[1], decryptedNetworkPayload[0]);
            Log.v(TAG, "Dst: " + MeshAddress.formatAddress(dst, true));

            if (isSegmentedMessage(decryptedNetworkPayload[2])) {
                Log.v(TAG, "Received a segmented access message from: " + MeshAddress.formatAddress(src, false));

                //Check if the received segmented message is from the same src as the previous segment
                //Ideal case this check is not needed but let's leave it for now.
                if (!mMeshNode.hasUnicastAddress(src)) {
                    Log.v(TAG, "Segment received is from a different src than the one we are processing, let's drop it");
                    return null;
                }

                if (segmentedAccessMessagesMessages == null) {
                    segmentedAccessMessagesMessages = new SparseArray<>();
                    segmentedAccessMessagesMessages.put(0, data);
                } else {
                    final int k = segmentedAccessMessagesMessages.size();
                    segmentedAccessMessagesMessages.put(k, data);
                }
                //Removing the mDst here
                final byte[] pdu = ByteBuffer.allocate(2 + networkHeader.length + decryptedNetworkPayload.length)
                        .order(ByteOrder.BIG_ENDIAN)
                        .put(data, 0, 2)
                        .put(networkHeader)
                        .put(decryptedNetworkPayload)
                        .array();
                final AccessMessage message = parseSegmentedAccessLowerTransportPDU(pdu);
                if (message != null) {
                    final SparseArray<byte[]> segmentedMessages = segmentedAccessMessagesMessages.clone();
                    segmentedAccessMessagesMessages = null;
                    message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
                    message.setNetworkLayerPdu(segmentedMessages);
                    message.setTtl(ttl);
                    message.setSrc(src);
                    message.setDst(dst);
                    parseUpperTransportPDU(message);
                    parseAccessLayerPDU(message);
                }
                return message;

            } else {
                final AccessMessage message = new AccessMessage();
                message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
                final SparseArray<byte[]> pduArray = new SparseArray<>();
                pduArray.put(0, data);
                message.setNetworkLayerPdu(pduArray);
                message.setTtl(ttl);
                message.setSrc(src);
                message.setDst(dst);
                message.setSequenceNumber(sequenceNumber);

                //Removing the mDst here
                final byte[] pdu = ByteBuffer.allocate(2 + networkHeader.length + decryptedNetworkPayload.length)
                        .order(ByteOrder.BIG_ENDIAN)
                        .put(data, 0, 2)
                        .put(networkHeader)
                        .put(decryptedNetworkPayload)
                        .array();
                parseUnsegmentedAccessLowerTransportPDU(message, pdu);
                parseUpperTransportPDU(message);
                parseAccessLayerPDU(message);
                return message;
            }
        } catch (InvalidCipherTextException ex) {
            throw new ExtendedInvalidCipherTextException(ex.getMessage(), ex.getCause(), TAG);
        }
    }

    /**
     * Parses control message
     *
     * @param provisionerAddress      Provisioner address
     * @param data                    Data received from the node
     * @param networkHeader           De-obfuscated network header
     * @param decryptedNetworkPayload Decrypted network payload
     * @param src                     Source address where the pdu originated from
     * @param sequenceNumber          Sequence number of the received message
     * @return a complete {@link ControlMessage} or null if the message was unable to parsed
     */
    private ControlMessage parseControlMessage(@Nullable final Integer provisionerAddress,
                                               @NonNull final byte[] data,
                                               @NonNull final byte[] networkHeader,
                                               @NonNull final byte[] decryptedNetworkPayload,
                                               final int src,
                                               @NonNull final byte[] sequenceNumber) throws ExtendedInvalidCipherTextException {
        try {
            final int ttl = networkHeader[0] & 0x7F;
            final int dst = MeshParserUtils.unsignedBytesToInt(decryptedNetworkPayload[1], decryptedNetworkPayload[0]);

            //Removing the mDst here
            final byte[] decryptedProxyPdu = ByteBuffer.allocate(2 + networkHeader.length + decryptedNetworkPayload.length)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(data, 0, 2)
                    .put(networkHeader)
                    .put(decryptedNetworkPayload)
                    .array();

            //We check the pdu type
            final int pduType = data[0];
            switch (pduType) {
                case MeshManagerApi.PDU_TYPE_NETWORK:

                    //This is not possible however let's return null
                    if (provisionerAddress == null) {
                        return null;
                    }

                    //Check if the message is directed to us, if its not ignore the message
                    if (provisionerAddress != dst) {
                        Log.v(TAG, "Received a control message that was not directed to us, so we drop it");
                        return null;
                    }

                    if (isSegmentedMessage(decryptedNetworkPayload[2])) {
                        return parseSegmentedControlMessage(data, decryptedProxyPdu, ttl, src, dst);
                    } else {
                        return parseUnsegmentedControlMessage(data, decryptedProxyPdu, ttl, src, dst, sequenceNumber);
                    }
                case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                    //Proxy configuration messages are segmented only at the gatt level
                    return parseUnsegmentedControlMessage(data, decryptedProxyPdu, ttl, src, dst, sequenceNumber);
                default:
                    return null;
            }
        } catch (InvalidCipherTextException ex) {
            throw new ExtendedInvalidCipherTextException(ex.getMessage(), ex.getCause(), TAG);
        }
    }

    /**
     * Parses an unsegmented control message
     *
     * @param data              Received pdu data
     * @param decryptedProxyPdu Decrypted proxy pdu
     * @param ttl               TTL of the pdu
     * @param src               Source address where the pdu originated from
     * @param dst               Destination address to which the pdu was sent
     * @param sequenceNumber    Sequence number of the pdu
     * @return a complete {@link ControlMessage} or null if the message was unable to parsed
     */
    private ControlMessage parseUnsegmentedControlMessage(@NonNull final byte[] data,
                                                          @NonNull final byte[] decryptedProxyPdu,
                                                          final int ttl,
                                                          final int src,
                                                          final int dst,
                                                          @NonNull final byte[] sequenceNumber) throws ExtendedInvalidCipherTextException {
        final ControlMessage message = new ControlMessage();
        message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        final SparseArray<byte[]> proxyPduArray = new SparseArray<>();
        proxyPduArray.put(0, data);
        message.setNetworkLayerPdu(proxyPduArray);
        message.setTtl(ttl);
        message.setSrc(src);
        message.setDst(dst);
        message.setSequenceNumber(sequenceNumber);
        message.setSegmented(false);
        parseUnsegmentedControlLowerTransportPDU(message, decryptedProxyPdu);

        return message;
    }

    /**
     * Parses a unsegmented control message
     *
     * @param data              Received pdu data
     * @param decryptedProxyPdu Decrypted proxy pdu
     * @param ttl               TTL of the pdu
     * @param src               Source address where the pdu originated from
     * @param dst               Destination address to which the pdu was sent
     * @return a complete {@link ControlMessage} or null if the message was unable to parsed
     */
    private ControlMessage parseSegmentedControlMessage(@NonNull final byte[] data, @NonNull final byte[] decryptedProxyPdu, final int ttl, final int src, final int dst) {
        if (segmentedControlMessagesMessages == null) {
            segmentedControlMessagesMessages = new SparseArray<>();
            segmentedControlMessagesMessages.put(0, data);
        } else {
            final int k = segmentedControlMessagesMessages.size();
            segmentedAccessMessagesMessages.put(k, data);
        }

        final ControlMessage message = parseSegmentedControlLowerTransportPDU(decryptedProxyPdu);
        if (message != null) {
            final SparseArray<byte[]> segmentedMessages = segmentedControlMessagesMessages.clone();
            segmentedControlMessagesMessages = null;
            message.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
            message.setNetworkLayerPdu(segmentedMessages);
            message.setTtl(ttl);
            message.setSrc(src);
            message.setDst(dst);
        }
        return message;
    }

    /**
     * Returns the master credentials {@link SecureUtils.K2Output}
     *
     * @param message Message
     */
    private SecureUtils.K2Output getK2Output(final Message message) {
        final NetworkKey networkKey;
        if (message.getAkf() == APPLICATION_KEY_IDENTIFIER) {
            networkKey = mNetworkLayerCallbacks.getPrimaryNetworkKey();
        } else {
            final int netKeyIndex = message.getApplicationKey().getBoundNetKeyIndex();
            networkKey = mNetworkLayerCallbacks.getNetworkKey(netKeyIndex);
        }
        return SecureUtils.calculateK2(networkKey.getKey(), SecureUtils.K2_MASTER_INPUT);
    }

    /**
     * Obfuscates the network header
     *
     * @param ctlTTL         Message type and ttl bit
     * @param sequenceNumber Sequence number of the message
     * @param src            Source address
     * @param pecb           Value derived from the privacy random
     * @return Obfuscated network header
     */
    private byte[] obfuscateNetworkHeader(final byte ctlTTL, @NonNull final byte[] sequenceNumber, final int src, @NonNull final byte[] pecb) {

        final ByteBuffer buffer = ByteBuffer.allocate(1 + sequenceNumber.length + 2).order(ByteOrder.BIG_ENDIAN);
        buffer.put(ctlTTL);
        buffer.put(sequenceNumber);   //sequence number
        buffer.putShort((short) src);       //source address

        final byte[] headerBuffer = buffer.array();

        final ByteBuffer bufferPECB = ByteBuffer.allocate(6);
        bufferPECB.put(pecb, 0, 6);

        final byte[] obfuscated = new byte[6];
        for (int i = 0; i < 6; i++)
            obfuscated[i] = (byte) (headerBuffer[i] ^ pecb[i]);

        return obfuscated;
    }

    /**
     * De-obfuscates the network header
     *
     * @param pdu Received from the node
     * @return Obfuscated network header
     */
    static byte[] deObfuscateNetworkHeader(@NonNull final byte[] pdu, @NonNull final byte[] ivIndex, @NonNull final byte[] privacyKey) {
        final ByteBuffer obfuscatedNetworkBuffer = ByteBuffer.allocate(6);
        obfuscatedNetworkBuffer.order(ByteOrder.BIG_ENDIAN);
        obfuscatedNetworkBuffer.put(pdu, 2, 6);
        final byte[] obfuscatedData = obfuscatedNetworkBuffer.array();

        final ByteBuffer privacyRandomBuffer = ByteBuffer.allocate(7);
        privacyRandomBuffer.order(ByteOrder.BIG_ENDIAN);
        privacyRandomBuffer.put(pdu, 8, 7);
        final byte[] privacyRandom = createPrivacyRandom(privacyRandomBuffer.array());

        final byte[] pecb = createPECB(ivIndex, privacyRandom, privacyKey);
        final byte[] deObfuscatedData = new byte[6];

        for (int i = 0; i < 6; i++)
            deObfuscatedData[i] = (byte) (obfuscatedData[i] ^ pecb[i]);

        return deObfuscatedData;
    }

    /**
     * Creates the privacy random.
     *
     * @param encryptedUpperTransportPDU Encrypted transport pdu
     * @return Privacy random
     */
    private static byte[] createPrivacyRandom(@NonNull final byte[] encryptedUpperTransportPDU) {
        final byte[] privacyRandom = new byte[7];
        System.arraycopy(encryptedUpperTransportPDU, 0, privacyRandom, 0, privacyRandom.length);
        return privacyRandom;
    }

    private static byte[] createPECB(@NonNull final byte[] ivIndex, @NonNull final byte[] privacyRandom, @NonNull final byte[] privacyKey) {
        final ByteBuffer buffer = ByteBuffer.allocate(5 + privacyRandom.length + ivIndex.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        buffer.put(ivIndex);
        buffer.put(privacyRandom);
        final byte[] temp = buffer.array();
        return SecureUtils.encryptWithAES(temp, privacyKey);
    }

    /**
     * Creates the network nonce
     *
     * @param ctlTTL         Combined ctl and ttl value
     * @param sequenceNumber Sequence number of the message
     * @param src            Source address
     * @return Network nonce
     */
    static byte[] createNetworkNonce(final byte ctlTTL, @NonNull final byte[] sequenceNumber, final int src, @NonNull final byte[] ivIndex) {
        final ByteBuffer networkNonce = ByteBuffer.allocate(13);
        networkNonce.put((byte) NONCE_TYPE_NETWORK); //Nonce type
        networkNonce.put(ctlTTL); // CTL and TTL
        networkNonce.put(sequenceNumber);
        networkNonce.putShort((short) src);
        networkNonce.put(new byte[]{PAD_NETWORK_NONCE, PAD_NETWORK_NONCE}); //PAD
        networkNonce.put(ivIndex);
        return networkNonce.array();
    }

    /**
     * Creates the proxy nonce
     *
     * @param sequenceNumber Sequence number of the message
     * @param src            Source address
     * @return Proxy nonce
     */
    static byte[] createProxyNonce(@NonNull final byte[] sequenceNumber, final int src, @NonNull final byte[] ivIndex) {
        final ByteBuffer applicationNonceBuffer = ByteBuffer.allocate(13);
        applicationNonceBuffer.put((byte) NONCE_TYPE_PROXY); //Nonce type
        applicationNonceBuffer.put((byte) PAD_PROXY_NONCE); //PAD
        applicationNonceBuffer.put(sequenceNumber);
        applicationNonceBuffer.putShort((short) src);
        applicationNonceBuffer.put(new byte[]{PAD_PROXY_NONCE, PAD_PROXY_NONCE});
        applicationNonceBuffer.put(ivIndex);
        return applicationNonceBuffer.array();
    }

    /**
     * Encrypts the pdu
     *
     * @param lowerTransportPdu lower transport pdu to be encrypted
     * @param encryptionKey     Encryption key
     * @param nonce             nonce depending on the pdu type
     * @param dst               Destination address
     * @param micLength         Message integrity check length
     */
    private byte[] encryptPdu(@NonNull final byte[] lowerTransportPdu,
                              @NonNull final byte[] encryptionKey,
                              @NonNull final byte[] nonce,
                              final int dst,
                              final int micLength) {
        //Adding the destination address on network layer
        final byte[] unencryptedNetworkPayload = ByteBuffer.allocate(2 + lowerTransportPdu.length).order(ByteOrder.BIG_ENDIAN)
                .putShort((short) dst)
                .put(lowerTransportPdu).array();
        //Network layer encryption
        return SecureUtils.encryptCCM(unencryptedNetworkPayload, encryptionKey, nonce, micLength);
    }
}
