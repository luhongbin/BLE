package no.nordicsemi.android.meshprovisioner.transport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import androidx.annotation.NonNull;

import static no.nordicsemi.android.meshprovisioner.transport.ConfigStatusMessage.StatusCodeNames.fromStatusCode;

@SuppressWarnings("WeakerAccess")
public abstract class ConfigStatusMessage extends MeshMessage {

    protected int mStatusCode;
    protected String mStatusCodeName;

    public ConfigStatusMessage(@NonNull final AccessMessage message) {
        mMessage = message;
    }

    /**
     * Parses the status parameters returned by a status message
     */
    abstract void parseStatusParameters();

    @Override
    public final int getAkf() {
        return mMessage.getAkf();
    }

    @Override
    public final int getAid() {
        return mMessage.getAid();
    }

    @Override
    public final byte[] getParameters() {
        return mParameters;
    }

    protected ArrayList<Integer> decode(final int dataSize, final int offset) {
        final ArrayList<Integer> arrayList = new ArrayList<>();
        final int size = dataSize - offset;
        if (size == 0) {
            return arrayList;
        }
        if (size == 2) {
            final byte[] netKeyIndex = new byte[]{(byte) (mParameters[offset + 1] & 0x0F), mParameters[offset]};
            final int keyIndex = encode(netKeyIndex);
            arrayList.add(keyIndex);
            return arrayList;
        } else {
            final int firstKeyIndex = encode(new byte[]{(byte) (mParameters[offset + 1] & 0x0F), mParameters[offset]});
            final int secondNetKeyIndex = encode(new byte[]{
                    (byte) ((mParameters[offset + 2] & 0xF0) >> 4),
                    (byte) (mParameters[offset + 2] << 4 | ((mParameters[offset + 1] & 0xF0) >> 4))});
            arrayList.add(firstKeyIndex);
            arrayList.add(secondNetKeyIndex);
            arrayList.addAll(decode(dataSize, offset + 3));
            return arrayList;
        }
    }

    private static int encode(@NonNull final byte[] netKeyIndex) {
        return ByteBuffer.wrap(netKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    /**
     * Returns the status code received by the status message
     *
     * @return Status code
     */
    public final int getStatusCode() {
        return mStatusCode;
    }

    /**
     * Returns the status code name for a status code received by the status message.
     *
     * @return status code name
     */
    public final String getStatusCodeName() {
        return mStatusCodeName;
    }

    /**
     * Returns the status code name for a status code returned by a status message.
     *
     * @param statusCode StatusCode received by the status message
     * @return The specific status code name
     */
    final String getStatusCodeName(final int statusCode) {
        switch (fromStatusCode(statusCode)) {
            case SUCCESS:
                return "成功";
            case INVALID_ADDRESS:
                return "无效地址";
            case INVALID_MODEL:
                return "无效模型";
            case INVALID_APPKEY_INDEX:
                return "索引键无效";
            case INVALID_NETKEY_INDEX:
                return "无效的网络密钥索引";
            case INSUFFICIENT_RESOURCES:
                return "资源不足";
            case KEY_INDEX_ALREADY_STORED:
                return "密钥索引已存储";
            case INVALID_PUBLISH_PARAMETERS:
                return "发布参数无效";
            case NOT_A_SUBSCRIBE_MODEL:
                return "不是订阅模式";
            case STORAGE_FAILURE:
                return "存储故障";
            case FEATURE_NOT_SUPPORTED:
                return "不支持的功能";
            case CANNOT_UPDATE:
                return "无法更新";
            case CANNOT_REMOVE:
                return "无法删除";
            case CANNOT_BIND:
                return "无法绑定";
            case TEMPORARILY_UNABLE_TO_CHANGE_STATE:
                return "暂时无法更改状态";
            case CANNOT_SET:
                return "无法设置";
            case UNSPECIFIED_ERROR:
                return "未指明的错误";
            case INVALID_BINDING:
                return "绑定无效";
            case RFU:
            default:
                return "RFU";
        }
    }

    public enum StatusCodeNames {
        SUCCESS(0x00),
        INVALID_ADDRESS(0x01),
        INVALID_MODEL(0x02),
        INVALID_APPKEY_INDEX(0x03),
        INVALID_NETKEY_INDEX(0x04),
        INSUFFICIENT_RESOURCES(0x05),
        KEY_INDEX_ALREADY_STORED(0x06),
        INVALID_PUBLISH_PARAMETERS(0x07),
        NOT_A_SUBSCRIBE_MODEL(0x08),
        STORAGE_FAILURE(0x09),
        FEATURE_NOT_SUPPORTED(0x0A),
        CANNOT_UPDATE(0x0B),
        CANNOT_REMOVE(0x0C),
        CANNOT_BIND(0x0D),
        TEMPORARILY_UNABLE_TO_CHANGE_STATE(0x0E),
        CANNOT_SET(0x0F),
        UNSPECIFIED_ERROR(0x10),
        INVALID_BINDING(0x11),
        RFU(0x12);

        private final int statusCode;

        StatusCodeNames(final int statusCode) {
            this.statusCode = statusCode;
        }

        public static StatusCodeNames fromStatusCode(final int statusCode) {
            for (StatusCodeNames code : values()) {
                if (code.getStatusCode() == statusCode) {
                    return code;
                }
            }
            throw new IllegalArgumentException("在StatusCodeNames中找不到枚举");
        }

        public final int getStatusCode() {
            return statusCode;
        }
    }
}
