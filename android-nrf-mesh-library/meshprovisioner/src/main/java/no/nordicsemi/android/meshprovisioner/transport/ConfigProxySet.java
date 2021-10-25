package no.nordicsemi.android.meshprovisioner.transport;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import no.nordicsemi.android.meshprovisioner.opcodes.ConfigMessageOpCodes;

/**
 * To be used as a wrapper class to create a ConfigProxySet message.
 */
@SuppressWarnings({"unused"})
public class ConfigProxySet extends ConfigMessage {

    public static final int PROXY_FEATURE_DISABLED = 0x00;   //The node support Relay feature that is disabled
    public static final int PROXY_FEATURE_ENABLED = 0x01;    //The node supports Relay feature that is enabled
    private static final String TAG = ConfigProxySet.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_GATT_PROXY_SET;
    private final int proxyState;

    /**
     * Constructs ConfigNodeReset message.
     *
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigProxySet(@ProxyState final int proxyState) throws IllegalArgumentException {
        if (proxyState != PROXY_FEATURE_DISABLED && proxyState != PROXY_FEATURE_ENABLED)
            throw new IllegalArgumentException("Invalid proxy state value.");
        this.proxyState = proxyState;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mParameters = new byte[]{(byte) proxyState};
        //Do nothing as ConfigNodeReset message does not have parameters
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PROXY_FEATURE_DISABLED, PROXY_FEATURE_ENABLED})
    public @interface ProxyState {
    }
}
