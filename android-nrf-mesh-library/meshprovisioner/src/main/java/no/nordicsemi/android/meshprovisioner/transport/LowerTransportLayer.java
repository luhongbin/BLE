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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import no.nordicsemi.android.meshprovisioner.MeshManagerApi;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.control.BlockAcknowledgementMessage;
import no.nordicsemi.android.meshprovisioner.opcodes.TransportLayerOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.ExtendedInvalidCipherTextException;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

/**
 * LowerTransportLayer implementation of the mesh network architecture as per the mesh profile specification.
 * <p>
 * This class generates the messages as per the lower transport layer requirements, segmentation and reassembly of mesh messages sent and received,
 * retransmitting messages.
 * </p>
 */
abstract class LowerTransportLayer extends UpperTransportLayer {

    private static final String TAG = LowerTransportLayer.class.getSimpleName();
    // >>>, PANDA
    private static final int BLOCK_ACK_TIMER = 600;//150; //Increased from minimum value 150;
    private static final int UNSEGMENTED_HEADER = 0;
    private static final int SEGMENTED_HEADER = 1;
    private static final int UNSEGMENTED_MESSAGE_HEADER_LENGTH = 1;
    private static final int SEGMENTED_MESSAGE_HEADER_LENGTH = 4;
    private static final int UNSEGMENTED_ACK_MESSAGE_HEADER_LENGTH = 3;
    private static final long INCOMPLETE_TIMER_DELAY = 10 * 1000; // According to the spec the incomplete timer must be a minimum of 10 seconds.

    private final SparseArray<byte[]> segmentedAccessMessageMap = new SparseArray<>();
    private final SparseArray<byte[]> segmentedControlMessageMap = new SparseArray<>();
    LowerTransportLayerCallbacks mLowerTransportLayerCallbacks;
    private boolean mSegmentedAccessAcknowledgementTimerStarted;
    private Integer mSegmentedAccessBlockAck;
    private boolean mSegmentedControlAcknowledgementTimerStarted;
    private Integer mSegmentedControlBlockAck;
    private boolean mIncompleteTimerStarted;
    private boolean mBlockAckSent;
    private long mDuration;

    /**
     * Runnable for incomplete timer
     */
    private Runnable mIncompleteTimerRunnable = new Runnable() {
        @Override
        public void run() {
            mLowerTransportLayerCallbacks.onIncompleteTimerExpired();
            //Reset the incomplete timer flag once it expires
            mIncompleteTimerStarted = false;
        }
    };

    /**
     * Sets the lower transport layer callbacks
     *
     * @param callbacks {@link LowerTransportLayerCallbacks} callbacks
     */
    abstract void setLowerTransportLayerCallbacks(@NonNull final LowerTransportLayerCallbacks callbacks);

    /**
     * Increments the sequence number and returns the new sequence number.
     *
     * @param src source address, which is the address of the provisioner
     * @return Incremented sequence number.
     */
    protected abstract int incrementSequenceNumber(final int src);

    /**
     * Increments the sequence number and returns the new sequence number.
     *
     * @param provisioner provisioner
     * @return Incremented sequence number.
     */
    protected abstract int incrementSequenceNumber(final Provisioner provisioner);

    /**
     * Increments the given sequence number.
     *
     * @param provisioner    provisioner
     * @param sequenceNumber Sequence number to be incremented.
     * @return Incremented sequence number.
     */
    protected abstract int incrementSequenceNumber(final Provisioner provisioner, @NonNull final byte[] sequenceNumber);

    /**
     * Creates the network layer pdu
     *
     * @param message message with underlying data
     * @return Complete pdu message that is ready to be sent
     */
    protected abstract Message createNetworkLayerPDU(@NonNull final Message message);

    @Override
    void createMeshMessage(@NonNull final Message message) {
        super.createMeshMessage(message);
        if (message instanceof AccessMessage) {
            createLowerTransportAccessPDU((AccessMessage) message);
        } else {
            createLowerTransportControlPDU((ControlMessage) message);
        }
    }

    @Override
    void createVendorMeshMessage(@NonNull final Message message) {
        if (message instanceof AccessMessage) {
            super.createVendorMeshMessage(message);
            createLowerTransportAccessPDU((AccessMessage) message);
        } else {
            createLowerTransportControlPDU((ControlMessage) message);
        }
    }

    @Override
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public final void createLowerTransportAccessPDU(@NonNull final AccessMessage message) {
        final byte[] upperTransportPDU = message.getUpperTransportPdu();
        final SparseArray<byte[]> lowerTransportAccessPduMap;
        if (upperTransportPDU.length <= MAX_SEGMENTED_ACCESS_PAYLOAD_LENGTH) {
            message.setSegmented(false);
            final byte[] lowerTransportPDU = createUnsegmentedAccessMessage(message);
            lowerTransportAccessPduMap = new SparseArray<>();
            lowerTransportAccessPduMap.put(0, lowerTransportPDU);
        } else {
            message.setSegmented(true);
            lowerTransportAccessPduMap = createSegmentedAccessMessage(message);
        }

        message.setLowerTransportAccessPdu(lowerTransportAccessPduMap);
    }

    @Override
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public final void createLowerTransportControlPDU(@NonNull final ControlMessage message) {
        switch (message.getPduType()) {
            case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                final SparseArray<byte[]> lowerTransportControlPduArray = new SparseArray<>();
                lowerTransportControlPduArray.put(0, message.getTransportControlPdu());
                message.setLowerTransportControlPdu(lowerTransportControlPduArray);
                break;
            case MeshManagerApi.PDU_TYPE_NETWORK:
                final byte[] transportControlPdu = message.getTransportControlPdu();
                if (transportControlPdu.length <= MAX_UNSEGMENTED_CONTROL_PAYLOAD_LENGTH) {
                    Log.v(TAG, "Creating unsegmented transport control");
                    createUnsegmentedControlMessage(message);
                } else {
                    Log.v(TAG, "Creating segmented transport control");
                    createSegmentedControlMessage(message);
                }
        }
    }

    @Override
    final void reassembleLowerTransportAccessPDU(@NonNull final AccessMessage accessMessage) {
        final SparseArray<byte[]> lowerTransportAccessPdu = removeLowerTransportAccessMessageHeader(accessMessage);
        final byte[] upperTransportPdu = MeshParserUtils.concatenateSegmentedMessages(lowerTransportAccessPdu);
        accessMessage.setUpperTransportPdu(upperTransportPdu);
    }

    @Override
    final void reassembleLowerTransportControlPDU(@NonNull final ControlMessage controlMessage) {
        final SparseArray<byte[]> lowerTransportPdu = removeLowerTransportControlMessageHeader(controlMessage);
        final byte[] lowerTransportControlPdu = MeshParserUtils.concatenateSegmentedMessages(lowerTransportPdu);
        controlMessage.setTransportControlPdu(lowerTransportControlPdu);
    }

    /**
     * Removes the transport header of the access message.
     *
     * @param message access message received.
     * @return map containing the messages.
     */
    private SparseArray<byte[]> removeLowerTransportAccessMessageHeader(@NonNull final AccessMessage message) {
        final SparseArray<byte[]> messages = message.getLowerTransportAccessPdu();
        if (message.isSegmented()) {
            for (int i = 0; i < messages.size(); i++) {
                final byte[] data = messages.get(i);
                final int length = data.length - SEGMENTED_MESSAGE_HEADER_LENGTH; //header size of unsegmented messages is 4;
                messages.put(i, removeHeader(data, 4, length));
            }
        } else {
            final byte[] data = messages.get(0);
            final int length = data.length - UNSEGMENTED_MESSAGE_HEADER_LENGTH; //header size of unsegmented messages is 1;
            messages.put(0, removeHeader(data, 1, length));
        }
        return messages;
    }

    /**
     * Removes the transport header of the control message.
     *
     * @param message control message.
     * @return map containing the messages.
     */
    private SparseArray<byte[]> removeLowerTransportControlMessageHeader(@NonNull final ControlMessage message) {
        final SparseArray<byte[]> messages = message.getLowerTransportControlPdu();
        if (messages.size() > 1) {
            for (int i = 0; i < messages.size(); i++) {
                final byte[] data = messages.get(i);
                final int length = data.length - SEGMENTED_MESSAGE_HEADER_LENGTH; //header size of unsegmented messages is 4;
                messages.put(i, removeHeader(data, 4, length));
            }
        } else {
            final int opCode = message.getOpCode();
            final byte[] data;
            final int length;
            if (opCode == TransportLayerOpCodes.SAR_ACK_OPCODE) {
                data = messages.get(0);
                length = data.length - UNSEGMENTED_ACK_MESSAGE_HEADER_LENGTH; //header size of unsegmented acknowledgement messages is 3;
                messages.put(0, removeHeader(data, UNSEGMENTED_ACK_MESSAGE_HEADER_LENGTH, length));
            } else {
                data = messages.get(0);
                length = data.length - UNSEGMENTED_MESSAGE_HEADER_LENGTH; //header size of unsegmented messages is 1;
                messages.put(0, removeHeader(data, UNSEGMENTED_MESSAGE_HEADER_LENGTH, length));
            }
        }
        return messages;
    }

    /**
     * Removes the header from a given array.
     *
     * @param data   message.
     * @param offset header offset.
     * @param length header length.
     * @return an array without the header.
     */
    private byte[] removeHeader(@NonNull final byte[] data, final int offset, final int length) {
        final ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        buffer.put(data, offset, length);
        return buffer.array();
    }

    /**
     * Creates an unsegmented access message.
     *
     * @param message access message.
     * @return Unsegmented access message.
     */
    private byte[] createUnsegmentedAccessMessage(@NonNull final AccessMessage message) {
        final byte[] encryptedUpperTransportPDU = message.getUpperTransportPdu();
        final int seg = message.isSegmented() ? 1 : 0;
        final int akfAid = ((message.getAkf() << 6) | message.getAid());
        final byte header = (byte) (((seg << 7) | akfAid));
        final ByteBuffer lowerTransportBuffer = ByteBuffer.allocate(1 + encryptedUpperTransportPDU.length).order(ByteOrder.BIG_ENDIAN);
        lowerTransportBuffer.put(header);
        lowerTransportBuffer.put(encryptedUpperTransportPDU);
        final byte[] lowerTransportPDU = lowerTransportBuffer.array();
        Log.v(TAG, "Unsegmented Lower transport access PDU " + MeshParserUtils.bytesToHex(lowerTransportPDU, false));
        return lowerTransportPDU;
    }

    /**
     * Creates a segmented access message.
     *
     * @param message access message.
     * @return Segmented access message.
     */
    private SparseArray<byte[]> createSegmentedAccessMessage(@NonNull final AccessMessage message) {
        final byte[] encryptedUpperTransportPDU = message.getUpperTransportPdu();
        final int akfAid = ((message.getAkf() << 6) | message.getAid());
        final int aszmic = message.getAszmic();
        final byte[] sequenceNumber = message.getSequenceNumber();
        int seqZero = MeshParserUtils.calculateSeqZero(sequenceNumber);

        final int numberOfSegments = (encryptedUpperTransportPDU.length + (MAX_SEGMENTED_ACCESS_PAYLOAD_LENGTH - 1)) / MAX_SEGMENTED_ACCESS_PAYLOAD_LENGTH;
        final int segN = numberOfSegments - 1; //Zero based segN
        final SparseArray<byte[]> lowerTransportPduMap = new SparseArray<>();
        int offset = 0;
        int length;
        for (int segO = 0; segO < numberOfSegments; segO++) {
            //Here we calculate the size of the segments based on the offset and the maximum payload of a segment access message
            length = Math.min(encryptedUpperTransportPDU.length - offset, MAX_SEGMENTED_ACCESS_PAYLOAD_LENGTH);
            final ByteBuffer lowerTransportBuffer = ByteBuffer.allocate(SEGMENTED_MESSAGE_HEADER_LENGTH + length).order(ByteOrder.BIG_ENDIAN);
            lowerTransportBuffer.put((byte) ((SEGMENTED_HEADER << 7) | akfAid));
            lowerTransportBuffer.put((byte) ((aszmic << 7) | ((seqZero >> 6) & 0x7F)));
            lowerTransportBuffer.put((byte) (((seqZero << 2) & 0xFC) | ((segO >> 3) & 0x03)));
            lowerTransportBuffer.put((byte) (((segO << 5) & 0xE0) | ((segN) & 0x1F)));
            lowerTransportBuffer.put(encryptedUpperTransportPDU, offset, length);
            offset += length;

            final byte[] lowerTransportPDU = lowerTransportBuffer.array();
            Log.v(TAG, "Segmented Lower transport access PDU: " + MeshParserUtils.bytesToHex(lowerTransportPDU, false) + " " + segO + " of " + numberOfSegments);
            lowerTransportPduMap.put(segO, lowerTransportPDU);
        }
        return lowerTransportPduMap;
    }

    /**
     * Creates an unsegmented control.
     *
     * @param message control message.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    private void createUnsegmentedControlMessage(@NonNull final ControlMessage message) {
        int pduLength;
        final ByteBuffer lowerTransportBuffer;
        message.setSegmented(false);
        final int opCode = message.getOpCode();
        final byte[] parameters = message.getParameters();
        final byte[] upperTransportControlPDU = message.getTransportControlPdu();
        final int header = (byte) ((UNSEGMENTED_HEADER << 7) | opCode);
        if (parameters != null) {
            pduLength = UNSEGMENTED_MESSAGE_HEADER_LENGTH + parameters.length + upperTransportControlPDU.length;
            lowerTransportBuffer = ByteBuffer.allocate(pduLength).order(ByteOrder.BIG_ENDIAN);
            lowerTransportBuffer.put((byte) header);
            lowerTransportBuffer.put(parameters);
        } else {
            pduLength = UNSEGMENTED_MESSAGE_HEADER_LENGTH + upperTransportControlPDU.length;
            lowerTransportBuffer = ByteBuffer.allocate(pduLength).order(ByteOrder.BIG_ENDIAN);
            lowerTransportBuffer.put((byte) header);
        }

        lowerTransportBuffer.put(upperTransportControlPDU);
        final byte[] lowerTransportPDU = lowerTransportBuffer.array();
        Log.v(TAG, "Unsegmented Lower transport control PDU " + MeshParserUtils.bytesToHex(lowerTransportPDU, false));
        final SparseArray<byte[]> lowerTransportControlPduMap = new SparseArray<>();
        lowerTransportControlPduMap.put(0, lowerTransportPDU);
        message.setLowerTransportControlPdu(lowerTransportControlPduMap);
    }

    /**
     * Creates a segmented control message.
     *
     * @param controlMessage control message to be sent.
     */
    private void createSegmentedControlMessage(@NonNull final ControlMessage controlMessage) {
        controlMessage.setSegmented(false);
        final byte[] encryptedUpperTransportControlPDU = controlMessage.getTransportControlPdu();
        final int opCode = controlMessage.getOpCode();
        final int rfu = 0;
        final byte[] sequenceNumber = controlMessage.getSequenceNumber();
        final int seqZero = MeshParserUtils.calculateSeqZero(sequenceNumber);

        final int numberOfSegments = (encryptedUpperTransportControlPDU.length + (MAX_SEGMENTED_CONTROL_PAYLOAD_LENGTH - 1)) / MAX_SEGMENTED_CONTROL_PAYLOAD_LENGTH;
        final int segN = numberOfSegments - 1; //Zero based segN
        final SparseArray<byte[]> lowerTransportControlPduMap = new SparseArray<>();
        int offset = 0;
        int length;
        for (int segO = 0; segO < numberOfSegments; segO++) {
            //Here we calculate the size of the segments based on the offset and the maximum payload of a segment access message
            length = Math.min(encryptedUpperTransportControlPDU.length - offset, MAX_SEGMENTED_CONTROL_PAYLOAD_LENGTH);
            final ByteBuffer lowerTransportBuffer = ByteBuffer.allocate(SEGMENTED_MESSAGE_HEADER_LENGTH + length).order(ByteOrder.BIG_ENDIAN);
            lowerTransportBuffer.put((byte) ((SEGMENTED_HEADER << 7) | opCode));
            lowerTransportBuffer.put((byte) ((rfu << 7) | ((seqZero >> 6) & 0x7F)));
            lowerTransportBuffer.put((byte) (((seqZero << 2) & 0xFC) | ((segO >> 3) & 0x03)));
            lowerTransportBuffer.put((byte) (((segO << 5) & 0xE0) | (segN & 0x1F)));
            lowerTransportBuffer.put(encryptedUpperTransportControlPDU, offset, length);
            offset += length;

            final byte[] lowerTransportPDU = lowerTransportBuffer.array();
            Log.v(TAG, "Segmented Lower transport access PDU: " + MeshParserUtils.bytesToHex(lowerTransportPDU, false) + " " + segO + " of " + numberOfSegments);
            lowerTransportControlPduMap.put(segO, lowerTransportPDU);
        }
        controlMessage.setLowerTransportControlPdu(lowerTransportControlPduMap);
    }

    /**
     * Checks if the received message is a segmented message
     *
     * @param lowerTransportHeader header for the lower transport pdu
     * @return true if segmented and false if not
     */
    /*package*/
    final boolean isSegmentedMessage(final byte lowerTransportHeader) {
        return ((lowerTransportHeader >> 7) & 0x01) == 1;
    }

    /**
     * Parses a unsegmented lower transport access pdu
     *
     * @param pdu The complete pdu was received from the node. This is already de-obfuscated and decrypted at network layer.
     */
    /*package*/
    final void parseUnsegmentedAccessLowerTransportPDU(@NonNull final AccessMessage message, @NonNull final byte[] pdu) {

        final byte header = pdu[10]; //Lower transport pdu starts here
        final int seg = (header >> 7) & 0x01;
        final int akf = (header >> 6) & 0x01;
        final int aid = header & 0x3F;
        if (seg == 0) { //Unsegmented message
            if (akf == 0) {// device key was used to encrypt
                final int lowerTransportPduLength = pdu.length - 10;
                final ByteBuffer lowerTransportBuffer = ByteBuffer.allocate(lowerTransportPduLength).order(ByteOrder.BIG_ENDIAN);
                lowerTransportBuffer.put(pdu, 10, lowerTransportPduLength);
                final byte[] lowerTransportPDU = lowerTransportBuffer.array();
                final SparseArray<byte[]> messages = new SparseArray<>();
                messages.put(0, lowerTransportPDU);
                message.setSegmented(false);
                message.setAszmic(0); //aszmic is always 0 for unsegmented access messages
                message.setAkf(akf);
                message.setAid(aid);
                message.setLowerTransportAccessPdu(messages);
            } else {
                final int lowerTransportPduLength = pdu.length - 10;
                final ByteBuffer lowerTransportBuffer = ByteBuffer.allocate(lowerTransportPduLength).order(ByteOrder.BIG_ENDIAN);
                lowerTransportBuffer.put(pdu, 10, lowerTransportPduLength);
                final byte[] lowerTransportPDU = lowerTransportBuffer.array();
                final SparseArray<byte[]> messages = new SparseArray<>();
                messages.put(0, lowerTransportPDU);
                message.setSegmented(false);
                message.setAszmic(0); //aszmic is always 0 for unsegmented access messages
                message.setAkf(akf);
                message.setAid(aid);
                message.setLowerTransportAccessPdu(messages);
            }
        }
    }

    /**
     * Parses a segmented lower transport access pdu.
     *
     * @param pdu The complete pdu was received from the node. This is already de-obfuscated and decrypted at network layer.
     */
    /*package*/
    final AccessMessage parseSegmentedAccessLowerTransportPDU(@NonNull final byte[] pdu) {

        final byte header = pdu[10]; //Lower transport pdu starts here
        final int akf = (header >> 6) & 0x01;
        final int aid = header & 0x3F;

        final int szmic = (pdu[11] >> 7) & 0x01;
        final int seqZero = ((pdu[11] & 0x7F) << 6) | ((pdu[12] & 0xFC) >> 2);
        final int segO = ((pdu[12] & 0x03) << 3) | ((pdu[13] & 0xE0) >> 5);
        final int segN = ((pdu[13]) & 0x1F);

        final int ttl = mLowerTransportLayerCallbacks.getTtl();// pdu[2] & 0x7F;
        final byte[] src = MeshParserUtils.getSrcAddress(pdu);
        final byte[] dst = MeshParserUtils.getDstAddress(pdu);

        final int blockAckSrc = MeshParserUtils.unsignedBytesToInt(dst[1], dst[0]); //Destination of the received packet would be the source for the ack
        final int blockAckDst = MeshParserUtils.unsignedBytesToInt(src[1], src[0]); //Source of the received packet would be the destination for the ack

        Log.v(TAG, "SEG O: " + segO);
        Log.v(TAG, "SEG N: " + segN);

        final int ivIndex = ByteBuffer.wrap(mUpperTransportLayerCallbacks.getIvIndex()).order(ByteOrder.BIG_ENDIAN).getInt();
        final int seqAuth = ivIndex | getTransportLayerSequenceNumber(MeshParserUtils.getSequenceNumberFromPDU(pdu), seqZero);
        final Integer lastSeqAuth = mMeshNode.getSeqAuth(blockAckDst);
        if (lastSeqAuth != null)
            Log.v(TAG, "Last SeqAuth value " + lastSeqAuth);

        Log.v(TAG, "Current SeqAuth value " + seqAuth);

        final int payloadLength = pdu.length - 10;
        final ByteBuffer payloadBuffer = ByteBuffer.allocate(payloadLength);
        payloadBuffer.put(pdu, 10, payloadLength);

        //Check if the current SeqAuth value is greater than the last and if the incomplete timer has not started, start it!
        if ((lastSeqAuth == null || lastSeqAuth < seqAuth)) {
            segmentedAccessMessageMap.clear();
            segmentedAccessMessageMap.put(segO, payloadBuffer.array());
            mMeshNode.setSeqAuth(blockAckDst, seqAuth);
            //Reset the block acknowledgement value
            mSegmentedAccessBlockAck = BlockAcknowledgementMessage.calculateBlockAcknowledgement(null, segO);

            Log.v(TAG, "Starting incomplete timer for src: " + MeshAddress.formatAddress(blockAckDst, false));
            initIncompleteTimer();

            //Start acknowledgement timer only for messages directed to a unicast address.
            if (MeshAddress.isValidUnicastAddress(dst)) {
                //Start the block acknowledgement timer irrespective of which segment was received first
                initSegmentedAccessAcknowledgementTimer(seqZero, ttl, blockAckSrc, blockAckDst, segN);
            }
        } else {
            //if the seqauth values are the same and the init complete timer has already started for a received segmented message, we need to restart the incomplete timer
            if (lastSeqAuth == seqAuth) {
                if (mIncompleteTimerStarted) {
                    if (segmentedAccessMessageMap.get(segO) == null) {
                        segmentedAccessMessageMap.put(segO, payloadBuffer.array());
                    }
                    final int receivedSegmentedMessageCount = segmentedAccessMessageMap.size();
                    Log.v(TAG, "Received segment message count: " + receivedSegmentedMessageCount);
                    //Add +1 to segN since its zero based
                    if (receivedSegmentedMessageCount != (segN + 1)) {
                        restartIncompleteTimer();
                        mSegmentedAccessBlockAck = BlockAcknowledgementMessage.calculateBlockAcknowledgement(mSegmentedAccessBlockAck, segO);
                        Log.v(TAG, "Restarting incomplete timer for src: " + MeshAddress.formatAddress(blockAckDst, false));

                        //Start acknowledgement timer only for messages directed to a unicast address.
                        //We also have to make sure we restart the acknowledgement timer only if the acknowledgement timer is not active and the incomplete timer is active
                        if (MeshAddress.isValidUnicastAddress(dst) && !mSegmentedAccessAcknowledgementTimerStarted) {
                            Log.v(TAG, "Restarting block acknowledgement timer for src: " + MeshAddress.formatAddress(blockAckDst, false));
                            //Start the block acknowledgement timer irrespective of which segment was received first
                            initSegmentedAccessAcknowledgementTimer(seqZero, ttl, blockAckSrc, blockAckDst, segN);
                        }
                    } else {
                        mSegmentedAccessBlockAck = BlockAcknowledgementMessage.calculateBlockAcknowledgement(mSegmentedAccessBlockAck, segO);
                        Log.v(TAG, "SEG O BLOCK ACK VAL: " + mSegmentedAccessBlockAck);
                        handleImmediateBlockAcks(seqZero, ttl, blockAckSrc, blockAckDst, segN);

                        final int upperTransportSequenceNumber = getTransportLayerSequenceNumber(MeshParserUtils.getSequenceNumberFromPDU(pdu), seqZero);
                        final byte[] sequenceNumber = MeshParserUtils.getSequenceNumberBytes(upperTransportSequenceNumber);
                        final AccessMessage accessMessage = new AccessMessage();
                        accessMessage.setAszmic(szmic);
                        accessMessage.setSequenceNumber(sequenceNumber);
                        accessMessage.setAkf(akf);
                        accessMessage.setAid(aid);
                        accessMessage.setSegmented(true);
                        final SparseArray<byte[]> segmentedMessages = segmentedAccessMessageMap.clone();
                        accessMessage.setLowerTransportAccessPdu(segmentedMessages);
                        return accessMessage;
                    }
                } else {
                    Log.v(TAG, "Ignoring message since the incomplete timer has expired and all messages have been received");
                }
            }
        }
        return null;
    }

    /**
     * Send immediate block acknowledgement
     *
     * @param seqZero seqzero of the message
     * @param ttl     ttl of the message
     * @param src     source address of the message
     * @param dst     destination address of the message
     * @param segN    total segment count
     */
    private void handleImmediateBlockAcks(final int seqZero, final int ttl, final int src, final int dst, final int segN) {
        cancelIncompleteTimer();
        sendBlockAck(seqZero, ttl, src, dst, segN);
    }

    /**
     * Parses a unsegmented lower transport control pdu.
     *
     * @param decryptedProxyPdu The complete pdu was received from the node. This is already de-obfuscated and decrypted at network layer.
     */
    /*package*/
    final void parseUnsegmentedControlLowerTransportPDU(@NonNull final ControlMessage controlMessage,
                                                        @NonNull final byte[] decryptedProxyPdu) throws ExtendedInvalidCipherTextException {

        final SparseArray<byte[]> unsegmentedMessages = new SparseArray<>();
        final int lowerTransportPduLength = decryptedProxyPdu.length - 10;
        final ByteBuffer lowerTransportBuffer = ByteBuffer.allocate(lowerTransportPduLength).order(ByteOrder.BIG_ENDIAN);
        lowerTransportBuffer.put(decryptedProxyPdu, 10, lowerTransportPduLength);
        final byte[] lowerTransportPDU = lowerTransportBuffer.array();
        unsegmentedMessages.put(0, lowerTransportPDU);
        final int opCode;
        final int pduType = decryptedProxyPdu[0];
        switch (pduType) {
            case MeshManagerApi.PDU_TYPE_NETWORK:
                final byte header = decryptedProxyPdu[10]; //Lower transport pdu starts here
                opCode = header & 0x7F;
                controlMessage.setPduType(MeshManagerApi.PDU_TYPE_NETWORK);//Set the pdu type here
                controlMessage.setAszmic(0);
                controlMessage.setOpCode(opCode);
                controlMessage.setLowerTransportControlPdu(unsegmentedMessages);
                parseLowerTransportLayerPDU(controlMessage);
                break;
            case MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION:
                controlMessage.setPduType(MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION);
                controlMessage.setLowerTransportControlPdu(unsegmentedMessages);
                parseUpperTransportPDU(controlMessage);
                break;
        }
    }

    /**
     * Parses a segmented lower transport control pdu.
     *
     * @param pdu The complete pdu was received from the node. This is already de-obfuscated and decrypted at network layer.
     */
    /*package*/
    final ControlMessage parseSegmentedControlLowerTransportPDU(@NonNull final byte[] pdu) {

        final byte header = pdu[10]; //Lower transport pdu starts here
        final int akf = (header >> 6) & 0x01;
        final int aid = header & 0x3F;

        final int szmic = (pdu[11] >> 7) & 0x01;
        final int seqZero = ((pdu[11] & 0x7F) << 6) | ((pdu[12] & 0xFC) >> 2);
        final int segO = ((pdu[12] & 0x3) << 3) | ((pdu[13] & 0xe0) >> 5);
        final int segN = ((pdu[13]) & 0x1F);

        final int ttl = pdu[2] & 0x7F;
        final byte[] src = MeshParserUtils.getSrcAddress(pdu);
        final byte[] dst = MeshParserUtils.getDstAddress(pdu);

        final int blockAckSrc = MeshParserUtils.unsignedBytesToInt(dst[1], dst[0]); //Destination of the received packet would be the source for the ack
        final int blockAckDst = MeshParserUtils.unsignedBytesToInt(src[1], src[0]); //Source of the received packet would be the destination for the ack

        Log.v(TAG, "SEG O: " + segO);
        Log.v(TAG, "SEG N: " + segN);

        //Start the timer irrespective of which segment was received first
        initSegmentedControlAcknowledgementTimer(seqZero, ttl, blockAckDst, blockAckSrc, segN);
        mSegmentedControlBlockAck = BlockAcknowledgementMessage.calculateBlockAcknowledgement(mSegmentedControlBlockAck, segO);
        Log.v(TAG, "Block acknowledgement value for " + mSegmentedControlBlockAck + " Seg O " + segO);

        final int payloadLength = pdu.length - 10;

        final ByteBuffer payloadBuffer = ByteBuffer.allocate(payloadLength);
        payloadBuffer.put(pdu, 10, payloadLength);
        segmentedControlMessageMap.put(segO, payloadBuffer.array());

        //Check the message count against the zero-based segN;
        final int receivedSegmentedMessageCount = segmentedControlMessageMap.size() - 1;
        if (segN == receivedSegmentedMessageCount) {
            Log.v(TAG, "All segments received");
            //Remove the incomplete timer if all segments were received
            mHandler.removeCallbacks(mIncompleteTimerRunnable);
            Log.v(TAG, "Block ack sent? " + mBlockAckSent);
            if (mDuration > System.currentTimeMillis() && !mBlockAckSent) {
                if (MeshAddress.isValidUnicastAddress(dst)) {
                    mHandler.removeCallbacksAndMessages(null);
                    Log.v(TAG, "Cancelling Scheduled block ack and incomplete timer, sending an immediate block ack");
                    sendBlockAck(seqZero, ttl, blockAckSrc, blockAckDst, segN);
                    //mBlockAckSent = false;
                }
            }
            final int upperTransportSequenceNumber = getTransportLayerSequenceNumber(MeshParserUtils.getSequenceNumberFromPDU(pdu), seqZero);
            final byte[] sequenceNumber = MeshParserUtils.getSequenceNumberBytes(upperTransportSequenceNumber);
            final ControlMessage message = new ControlMessage();
            message.setAszmic(szmic);
            message.setSequenceNumber(sequenceNumber);
            message.setAkf(akf);
            message.setAid(aid);
            message.setSegmented(true);
            final SparseArray<byte[]> segmentedMessages = segmentedControlMessageMap.clone();
            segmentedControlMessageMap.clear();
            message.setLowerTransportControlPdu(segmentedMessages);
            return message;
        }

        return null;
    }

    /**
     * Start incomplete timer for segmented messages.
     */
    private void initIncompleteTimer() {
        mHandler.postDelayed(mIncompleteTimerRunnable, INCOMPLETE_TIMER_DELAY);
        mIncompleteTimerStarted = true;
    }

    /**
     * Restarts the incomplete timer
     */
    private void restartIncompleteTimer() {
        //Remove the existing incomplete timer
        if (mIncompleteTimerStarted) {
            mHandler.removeCallbacks(mIncompleteTimerRunnable);
        }
        //Call init to start the timer again
        initIncompleteTimer();
    }

    /**
     * Cancels an already started the incomplete timer
     */
    private void cancelIncompleteTimer() {
        //Remove the existing incomplete timer
        mIncompleteTimerStarted = false;
        mHandler.removeCallbacks(mIncompleteTimerRunnable);
    }

    /**
     * Start acknowledgement timer for segmented messages.
     *
     * @param seqZero seqzero of the segmented messages.
     * @param ttl     ttl of the segmented messages.
     * @param dst     destination address.
     */
    private void initSegmentedAccessAcknowledgementTimer(final int seqZero, final int ttl, final int src, final int dst, final int segN) {
        if (!mSegmentedAccessAcknowledgementTimerStarted) {
            mSegmentedAccessAcknowledgementTimerStarted = true;
            Log.v(TAG, "TTL: " + ttl);
            final int duration = (BLOCK_ACK_TIMER + (50 * ttl));
            Log.v(TAG, "Duration: " + duration);
            mDuration = System.currentTimeMillis() + duration;
            mHandler.postDelayed(() -> {
                Log.v(TAG, "Acknowledgement timer expiring");
                sendBlockAck(seqZero, ttl, src, dst, segN);
            }, duration);
        }
    }

    /**
     * Start acknowledgement timer for segmented messages.
     *
     * @param seqZero seqzero of the segmented messages.
     * @param ttl     ttl of the segmented messages.
     * @param src     source address which is the element address
     * @param dst     destination address.
     */
    private void initSegmentedControlAcknowledgementTimer(final int seqZero, final int ttl, final int src, final int dst, final int segN) {
        if (!mSegmentedControlAcknowledgementTimerStarted) {
            mSegmentedControlAcknowledgementTimerStarted = true;
            final int duration = BLOCK_ACK_TIMER + (50 * ttl);
            mDuration = System.currentTimeMillis() + duration;
            mHandler.postDelayed(() -> sendBlockAck(seqZero, ttl, src, dst, segN), duration);
        }
    }

    /**
     * Send block acknowledgement
     *
     * @param seqZero seqzero of the segmented messages.
     * @param ttl     ttl of the segmented messages.
     * @param src     source address which is the element address
     * @param dst     destination address.
     */
    private void sendBlockAck(final int seqZero, final int ttl, final int src, final int dst, final int segN) {
        final int blockAck = mSegmentedAccessBlockAck;
        //mSegmentedAccessBlockAck = null;
        if (BlockAcknowledgementMessage.hasAllSegmentsBeenReceived(blockAck, segN)) {
            Log.v(TAG, "All segments received cancelling incomplete timer");
            cancelIncompleteTimer();
        }

        final byte[] upperTransportControlPdu = createAcknowledgementPayload(seqZero, blockAck);
        Log.v(TAG, "Block acknowledgement payload: " + MeshParserUtils.bytesToHex(upperTransportControlPdu, false));
        final ControlMessage controlMessage = new ControlMessage();
        controlMessage.setOpCode(TransportLayerOpCodes.SAR_ACK_OPCODE);
        controlMessage.setTransportControlPdu(upperTransportControlPdu);
        controlMessage.setTtl(ttl);
        controlMessage.setPduType(MeshManagerApi.PDU_TYPE_NETWORK);
        controlMessage.setSrc(src);
        controlMessage.setDst(dst);
        controlMessage.setIvIndex(mUpperTransportLayerCallbacks.getIvIndex());
        final int sequenceNumber = incrementSequenceNumber(controlMessage.getSrc());
        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);
        controlMessage.setSequenceNumber(sequenceNum);
        mBlockAckSent = true;
        mLowerTransportLayerCallbacks.sendSegmentAcknowledgementMessage(controlMessage);
        mSegmentedAccessAcknowledgementTimerStarted = false;
    }

    /**
     * Creates the acknowledgement parameters.
     *
     * @param seqZero seqzero of the message.
     * @return acknowledgement parameters.
     */
    private byte[] createAcknowledgementPayload(final int seqZero, final int blockAcknowledgement) {
        final int obo = 0;
        final int rfu = 0;

        final ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) ((obo << 7) | (seqZero >> 6) & 0x7F));
        buffer.put((byte) (((seqZero << 2) & 0xFC) | rfu));
        buffer.putInt(blockAcknowledgement);
        return buffer.array();
    }

    /**
     * Parse transport layer control pdu.
     *
     * @param controlMessage underlying message containing the access pdu.
     */
    private void parseLowerTransportLayerPDU(@NonNull final ControlMessage controlMessage) {
        //First we reassemble the transport layer message if its a segmented message
        reassembleLowerTransportControlPDU(controlMessage);
        final byte[] transportControlPdu = controlMessage.getTransportControlPdu();
        final int opCode = controlMessage.getOpCode();

        if (opCode == TransportLayerOpCodes.SAR_ACK_OPCODE) {
            final BlockAcknowledgementMessage acknowledgement = new BlockAcknowledgementMessage(transportControlPdu);
            controlMessage.setTransportControlMessage(acknowledgement);
        }

    }
}
