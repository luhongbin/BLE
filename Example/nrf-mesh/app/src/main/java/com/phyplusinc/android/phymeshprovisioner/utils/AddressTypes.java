package com.phyplusinc.android.phymeshprovisioner.utils;

/**
 * Address types
 */
@SuppressWarnings("unused")
public enum AddressTypes {

    UNICAST_ADDRESS(0),
    GROUP_ADDRESS(1),
    ALL_PROXIES(2),
    ALL_FRIENDS(3),
    ALL_RELAYS(4),
    ALL_NODES(5),
    VIRTUAL_ADDRESS(6);

    private int type;

    /**
     * Constructs address type
     *
     * @param type Address type
     */
    AddressTypes(final int type) {
        this.type = type;
    }

    /**
     * Returns the address type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the oob method used for authentication
     *
     * @param method auth method used
     */
    public static AddressTypes fromValue(final int method) {
        switch (method) {
            default:
                return null;
            case 0:
                return UNICAST_ADDRESS;
            case 1:
                return GROUP_ADDRESS;
            case 2:
                return ALL_PROXIES;
            case 3:
                return ALL_FRIENDS;
            case 4:
                return ALL_RELAYS;
            case 5:
                return ALL_NODES;
            case 6:
                return VIRTUAL_ADDRESS;
        }
    }

    /**
     * Returns the address type name
     *
     * @param type Address type
     */
    public static String getTypeName(final AddressTypes type) {
        switch (type) {
            default:
                return "单播地址";
            case GROUP_ADDRESS:
                return "组地址";
            case ALL_PROXIES:
                return "所有代理";
            case ALL_FRIENDS:
                return "所有好友";
            case ALL_RELAYS:
                return "所有继电器";
            case ALL_NODES:
                return "所有节点";
            case VIRTUAL_ADDRESS:
                return "虚拟地址";
        }
    }
}
