package no.nordicsemi.android.meshprovisioner.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Abstract class for bluetooth mesh addresses
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MeshAddress {

    private static final byte[] VTAD = "vtad".getBytes(Charset.forName("US-ASCII"));

    //Unassigned addresses
    public static final int UNASSIGNED_ADDRESS = 0x0000;

    //Unicast addresses
    public static final int START_UNICAST_ADDRESS = 0x0001;
    public static final int END_UNICAST_ADDRESS = 0x7FFF;

    //Group address start and end defines the address range that can be used to create groups
    public static final int START_GROUP_ADDRESS = 0xC000;
    public static final int END_GROUP_ADDRESS = 0xFEFF;

    //Fixed group addresses
    public static final int ALL_PROXIES_ADDRESS = 0xFFFC;
    public static final int ALL_FRIENDS_ADDRESS = 0xFFFD;
    public static final int ALL_RELAYS_ADDRESS = 0xFFFE;
    public static final int ALL_NODES_ADDRESS = 0xFFFF;

    //Virtual addresses
    private static final byte B1_VIRTUAL_ADDRESS = (byte) 0x80;
    public static final int START_VIRTUAL_ADDRESS = 0x8000;
    public static final int END_VIRTUAL_ADDRESS = 0xBFFF;
    public static final int UUID_HASH_BIT_MASK = 0x3FFF;

    public static String formatAddress(final int address, final boolean add0x) {
        return add0x ?
                "0x" + String.format(Locale.US, "%04X", address) :
                String.format(Locale.US, "%04X", address);
    }

    public static boolean isAddressInRange(@NonNull final byte[] address) {
        return address.length == 2 && isAddressInRange((address[0] & 0xFF) << 8 | address[1] & 0xFF);
    }

    /**
     * Checks if the address is in range
     *
     * @param address address
     * @return true if is in range or false otherwise
     */
    public static boolean isAddressInRange(final int address) {
        return address == (address & 0xFFFF);
    }

    /**
     * Validates an unassigned address
     *
     * @param address 16-bit address
     * @return true if the address is a valid unassigned address or false otherwise
     */
    public static boolean isValidUnassignedAddress(@NonNull final byte[] address) {
        if (!isAddressInRange(address)) {
            return false;
        }
        return isValidUnassignedAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Validates a unassigned address
     *
     * @param address 16-bit address
     * @return true if the address is a valid unassigned address or false otherwise
     */
    public static boolean isValidUnassignedAddress(final int address) {
        return isAddressInRange(address) && (address == UNASSIGNED_ADDRESS);
    }

    /**
     * Validates a unicast address
     *
     * @param address Address in bytes
     * @return true if the address is a valid unicast address or false otherwise
     */
    public static boolean isValidUnicastAddress(@NonNull final byte[] address) {
        if (!isAddressInRange(address)) {
            return false;
        }

        return isValidUnicastAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Validates a unicast address
     *
     * @param address 16-bit address
     * @return true if the address is a valid unicast address or false otherwise
     */
    public static boolean isValidUnicastAddress(final int address) {
        return isAddressInRange(address) && (address >= START_UNICAST_ADDRESS && address <= END_UNICAST_ADDRESS);
    }

    /**
     * Validates a virtual address
     *
     * @param address Address in bytes
     * @return true if the address is a valid virtual address or false otherwise
     */
    public static boolean isValidVirtualAddress(@NonNull final byte[] address) {
        if (!isAddressInRange(address)) {
            return false;
        }
        return isValidVirtualAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Validates a unicast address
     *
     * @param address 16-bit address
     * @return true if the address is a valid virtual address or false otherwise
     */
    public static boolean isValidVirtualAddress(final int address) {
        if (isAddressInRange(address)) {
            return address >= START_VIRTUAL_ADDRESS && address <= END_VIRTUAL_ADDRESS;
        }
        return false;
    }

    public static boolean isValidGroupAddress(final byte[] address) {
        if (!isAddressInRange(address))
            return false;
        return isValidGroupAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Returns true if the its a valid group address
     *
     * @param address 16-bit address
     * @return true if the address is valid and false otherwise
     */
    @SuppressWarnings({"ConstantConditions"})
    public static boolean isValidGroupAddress(final int address) {
        if (!isAddressInRange(address))
            return false;

        final int b0 = address >> 8 & 0xFF;
        final int b1 = address & 0xFF;

        final boolean groupRange = b0 >= 0xC0 && b0 <= 0xFF;
        final boolean rfu = b0 == 0xFF && b1 >= 0x00 && b1 <= 0xFB;
        final boolean allNodes = b0 == 0xFF && b1 == 0xFF;

        return groupRange && !rfu && !allNodes;
    }

    /**
     * Returns true if the its a valid group address
     *
     * @param address 16-bit address
     * @return true if the address is valid and false otherwise
     */
    @SuppressWarnings({"ConstantConditions"})
    public static boolean isValidFixedGroupAddress(final int address) {
        if (!isAddressInRange(address))
            return false;

        final int b0 = address >> 8 & 0xFF;
        final int b1 = address & 0xFF;

        final boolean rfu = b0 == 0xFF && b1 >= 0x00 && b1 <= 0xFB;
        final boolean allNodes = b0 == 0xFF && b1 > 0xFB && b1 <= 0xFF;

        return !rfu && allNodes;
    }

    /**
     * Returns true if the address is a valid subscription address
     *
     * @param address 16-bit address
     * @return true if the address is valid and false otherwise
     */
    public static boolean isValidSubscriptionAddress(final int address) {

        if (isValidUnassignedAddress(address) || isValidUnicastAddress(address) || isValidVirtualAddress(address) || address == 0xFFFF) {
            throw new IllegalArgumentException("The value of the Address field shall not be an unassigned address, unicast address, " +
                    "all-nodes address or virtual address.");
        }

        final int b0 = address >> 8 & 0xFF;
        final int b1 = address & 0xFF;

        final boolean groupRange = b0 >= 0xC0;
        final boolean rfu = b0 == 0xFF && b1 <= 0xFB;
        final boolean allNodes = b0 == 0xFF && b1 == 0xFF;
        return groupRange && !rfu && !allNodes;
    }

    /**
     * Validates a given address for subscriptions
     *
     * @param address group address
     * @return true if is valid and false otherwise
     */
    public static boolean isValidSubscriptionAddress(@NonNull final byte[] address) {
        if (!isAddressInRange(address))
            return false;
        return isValidSubscriptionAddress((address[0] & 0xFF) << 8 | address[1] & 0xFF);
    }

    /**
     * Validates if the given address is a valid address that can be used as a proxy filter
     *
     * @param address Unicast/Virtual or Group address
     * @return true if is valid and false otherwise
     */
    public static boolean isValidProxyFilterAddress(final int address) {
        if (!isAddressInRange(address))
            return false;
        final int b0 = address >> 8 & 0xFF;
        final int b1 = address & 0xFF;

        final boolean groupRange = b0 >= 0xC0;
        final boolean rfu = b0 == 0xFF && b1 <= 0xFB;
        final boolean unicast = isValidUnicastAddress(address);
        final boolean virtual = isValidVirtualAddress(address);
        return unicast || virtual || (groupRange && !rfu);
    }

    /**
     * Validates if the given address is a valid address that can be used as a proxy filter
     *
     * @param address Unicast/Virtual or Group address
     * @return true if is valid and false otherwise
     */
    public static boolean isValidProxyFilterAddress(@NonNull final byte[] address) {
        if (!isAddressInRange(address))
            return false;
        return isValidProxyFilterAddress((address[0] & 0xFF) << 8 | address[1] & 0xFF);
    }

    /**
     * Returns the {@link AddressType}
     *
     * @param address 16-bit mesh address
     */
    @Nullable
    public static AddressType getAddressType(final int address) {
        if (isAddressInRange(address)) {
            if (isValidUnassignedAddress(address)) {
                return AddressType.UNASSIGNED_ADDRESS;
            } else if (isValidUnicastAddress(address)) {
                return AddressType.UNICAST_ADDRESS;
            } else if (isValidGroupAddress(address) || isValidFixedGroupAddress(address)) {
                return AddressType.GROUP_ADDRESS;
            } else {
                return AddressType.VIRTUAL_ADDRESS;
            }
        }
        return null;
    }

    /**
     * Generates a random uuid
     */
    public static UUID generateRandomLabelUUID() {
        return UUID.randomUUID();
    }

    /**
     * Generates a virtual address from a given Lable UUID
     *
     * @param uuid Type 4 UUID
     */
    public static Integer generateVirtualAddress(@NonNull final UUID uuid) {
        final byte[] uuidBytes = MeshParserUtils.uuidToBytes(uuid);
        final byte[] salt = SecureUtils.calculateSalt(VTAD);
        final byte[] encryptedUuid = SecureUtils.calculateCMAC(MeshParserUtils.uuidToBytes(uuid), salt);
        ByteBuffer buffer = ByteBuffer.wrap(encryptedUuid);
        buffer.position(12); //Move the position to 12
        return START_VIRTUAL_ADDRESS | (buffer.getInt() & UUID_HASH_BIT_MASK);
    }

    /**
     * Returns the label UUID for a given virtual address
     *
     * @param address 16-bit virtual address
     */
    @Nullable
    public static UUID getLabelUuid(@NonNull final List<UUID> uuids, final int address) {
        if (MeshAddress.isValidVirtualAddress(address)) {
            for (UUID uuid : uuids) {
                final byte[] salt = SecureUtils.calculateSalt(VTAD);
                //Encrypt the label uuid with the salt as the key
                final byte[] encryptedUuid = SecureUtils.calculateCMAC(MeshParserUtils.uuidToBytes(uuid), salt);
                ByteBuffer buffer = ByteBuffer.wrap(encryptedUuid);
                buffer.position(12); //Move the position to 12
                final int hash = buffer.getInt() & UUID_HASH_BIT_MASK;
                if (hash == getHash(address)) {
                    return uuid;
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of the hash from a virtual address
     * <p>
     * The hash stored in a virtual address is derived from the label UUID.
     * In a virtual address bits 13 to 0 are set to the value of a hash of the corresponding Label UUID.
     * </p>
     *
     * @param address virtual address
     */
    public static int getHash(final int address) {
        if (isValidVirtualAddress(address)) {
            return address & 0x3FFF;
        }
        return 0;
    }

    /**
     * Returns the unicast address as int
     *
     * @param unicastAddress unicast address
     * @return unicast address
     */
    public static byte[] addressIntToBytes(final int unicastAddress) {
        return new byte[]{(byte) ((unicastAddress >> 8) & 0xFF), (byte) (unicastAddress & 0xFF)};
    }

    /**
     * Returns the unicast address as int
     *
     * @param unicastAddress unicast address
     * @return unicast address
     */
    public static short addressBytesToInt(final byte[] unicastAddress) {
        return ByteBuffer.wrap(unicastAddress).order(ByteOrder.BIG_ENDIAN).getShort();
    }
}
