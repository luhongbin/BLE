package no.nordicsemi.android.meshprovisioner.utils;

/**
 * Static OOB Type
 */
@SuppressWarnings("unused")
public enum StaticOOBType {

    /**
     * Static OOB Type
     */
    STATIC_OOB_AVAILABLE((short) 0x0001);

    private static final String TAG = StaticOOBType.class.getSimpleName();
    private short staticOobType;

    StaticOOBType(final short staticOobType) {
        this.staticOobType = staticOobType;
    }

    public static String parseStaticOOBActionInformation(final StaticOOBType type) {
        switch (type) {
            case STATIC_OOB_AVAILABLE:
                return "静态 OOB 操作可用";
            default:
                return "静态 OOB 不可用操作";
        }
    }

    /**
     * Returns the static oob type value
     */
    public short getStaticOobType() {
        return staticOobType;
    }
}
