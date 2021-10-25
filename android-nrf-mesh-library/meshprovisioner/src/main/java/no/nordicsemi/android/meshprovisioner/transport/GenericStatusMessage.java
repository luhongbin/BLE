package no.nordicsemi.android.meshprovisioner.transport;

import androidx.annotation.NonNull;

public abstract class GenericStatusMessage extends MeshMessage {


    GenericStatusMessage(@NonNull final AccessMessage message) {
        mMessage = message;
    }

    /**
     * Parses the status parameters returned by a status message
     */
    abstract void parseStatusParameters();

    @Override
    public final int getAkf() {
        return 1;
    }

    @Override
    public final int getAid() {
        return mMessage.getAid();
    }

    @Override
    public final byte[] getParameters() {
        return mParameters;
    }

    /**
     * Returns the address where the message was originated from
     */
    public int getSrcAddress() {
        return mMessage.getSrc();
    }
}
