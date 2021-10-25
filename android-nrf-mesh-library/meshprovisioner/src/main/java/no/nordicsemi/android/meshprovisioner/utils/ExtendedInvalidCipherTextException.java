package no.nordicsemi.android.meshprovisioner.utils;

import org.spongycastle.crypto.InvalidCipherTextException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ExtendedInvalidCipherTextException extends InvalidCipherTextException {

    private final String tag;
    private final String message;
    private final Throwable cause;

    /**
     * Constructs ExtendedInvalidCipherTextException
     *
     * @param message Exception message
     * @param cause   Throwable cause
     * @param tag     class tag name
     */
    public ExtendedInvalidCipherTextException(@NonNull final String message, @Nullable final Throwable cause, final String tag) {
        this.message = message;
        this.cause = cause;
        this.tag = tag;

    }

    /**
     * Returns the tag name of the class.
     * <p>
     * Using the tag we can find out on which class the exception occurred.
     * </p>
     */
    public String getTag() {
        return tag;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
