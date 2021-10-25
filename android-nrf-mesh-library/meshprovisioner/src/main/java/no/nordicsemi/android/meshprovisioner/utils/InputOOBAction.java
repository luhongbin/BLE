package no.nordicsemi.android.meshprovisioner.utils;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Input OOB Actions
 */
@SuppressWarnings("unused")
public enum InputOOBAction {

    /**
     * Output OOB Actions
     */
    NO_INPUT((short) 0x0000),
    PUSH((short) 0x0001),
    TWIST((short) 0x0002),
    INPUT_NUMERIC((short) 0x0004),
    INPUT_ALPHA_NUMERIC((short) 0x0008);

    private static final String TAG = InputOOBAction.class.getSimpleName();
    private short inputOOBAction;

    InputOOBAction(final short outputOOBAction) {
        this.inputOOBAction = outputOOBAction;
    }

    public short getInputOOBAction() {
        return inputOOBAction;
    }

    /**
     * Returns the oob method used for authentication
     *
     * @param method auth method used
     */
    public static InputOOBAction fromValue(final short method) {
        switch (method) {
            default:
            case 0x0000:
                return NO_INPUT;
            case 0x0001:
                return PUSH;
            case 0x0002:
                return TWIST;
            case 0x0008:
                return INPUT_NUMERIC;
            case 0x0010:
                return INPUT_ALPHA_NUMERIC;
        }
    }

    /**
     * Returns the Input OOB Action description
     *
     * @param type Input OOB type
     * @return Input OOB type descrption
     */
    public static String getInputOOBActionDescription(final InputOOBAction type) {
        switch (type) {
            case NO_INPUT:
                return "不支持";
            case PUSH:
                return "推送";
            case TWIST:
                return "扭转";
            case INPUT_NUMERIC:
                return "输入数字";
            case INPUT_ALPHA_NUMERIC:
                return "字母数字输入";
            default:
                return "不清楚";
        }
    }

    public static ArrayList<InputOOBAction> parseInputActionsFromBitMask(final int inputAction) {
        final InputOOBAction[] inputActions = {PUSH, TWIST, INPUT_NUMERIC, INPUT_ALPHA_NUMERIC};
        final ArrayList<InputOOBAction> supportedActionValues = new ArrayList<>();
        for (InputOOBAction action : inputActions) {
            if ((inputAction & action.inputOOBAction) == action.inputOOBAction) {
                supportedActionValues.add(action);
                Log.v(TAG, "Input oob action type value: " + getInputOOBActionDescription(action));
            }
        }
        return supportedActionValues;
    }

    /**
     * Returns the Input OOB Action value
     *
     * @param type input OOB type
     */
    public static int getInputOOBActionValue(final InputOOBAction type) {
        switch (type) {
            case PUSH:
                return 0;
            case TWIST:
                return 1;
            case INPUT_NUMERIC:
                return 2;
            case INPUT_ALPHA_NUMERIC:
                return 3;
            case NO_INPUT:
            default:
                return -1;
        }
    }

    /**
     * Returns the Input OOB Action value
     *
     * @param type input OOB type
     */
    public static int getInputOOBActionValue(final short type) {
        switch (fromValue(type)) {
            case PUSH:
                return 0;
            case TWIST:
                return 1;
            case INPUT_NUMERIC:
                return 2;
            case INPUT_ALPHA_NUMERIC:
                return 3;
            case NO_INPUT:
            default:
                return -1;
        }
    }

    /**
     * Generates the Input OOB Authentication value
     *
     * @param inputOOBAction selected {@link InputOOBAction}
     * @param input          Input authentication
     * @return 128-bit authentication value
     */
    public static byte[] generateInputOOBAuthenticationValue(final InputOOBAction inputOOBAction, final byte[] input) {
        final int authLength = 16;
        final ByteBuffer buffer = ByteBuffer.allocate(authLength).order(ByteOrder.BIG_ENDIAN);
        switch (inputOOBAction) {
            case PUSH:
            case TWIST:
            case INPUT_NUMERIC:
                buffer.position(8);
                final long longValue = Long.valueOf(MeshParserUtils.bytesToHex(input, false), 16);
                buffer.putLong(longValue);
                return buffer.array();
            case INPUT_ALPHA_NUMERIC:
                buffer.put(input);
                return buffer.array();
            default:
                return null;
        }
    }

    /**
     * Returns a randomly generated Input OOB Authentication value to be input by the user
     *
     * @param inputOOBAction selected {@link InputOOBAction}
     * @param size           oob size
     */
    public static byte[] getInputOOOBAuthenticationValue(final short inputOOBAction, final byte size) {
        switch (fromValue(inputOOBAction)) {
            case PUSH:
            case TWIST:
                //We override the value here to 1 so we generate a 1 digit value for presses.
                return MeshParserUtils.generateOOBCount(1);
            case INPUT_NUMERIC:
                return MeshParserUtils.generateOOBNumeric(size);
            case INPUT_ALPHA_NUMERIC:
                return MeshParserUtils.generateOOBAlphaNumeric(size);
            default:
                return null;
        }
    }
}
