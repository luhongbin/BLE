
package no.nordicsemi.android.meshprovisioner;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.IntDef;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;

/**
 * Defines the features supported by a {@link ProvisionedMeshNode}
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Features implements Parcelable {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISABLED, ENABLED, UNSUPPORTED})
    @interface FeatureState {
    }

    // Key refresh phases
    public static final int DISABLED = 0; //Feature is disabled
    public static final int ENABLED = 1; //Feature is enabled
    public static final int UNSUPPORTED = 2; //Feature is not supported

    @SerializedName("friend")
    @Expose
    private int friend; //friend feature
    @SerializedName("lowPower")
    @Expose
    private int lowPower; //low power feature
    @SerializedName("proxy")
    @Expose
    private int proxy; //proxy feature
    @SerializedName("relay")
    @Expose
    private int relay; //relay feature

    /**
     * Constructs the features of a provisioned node
     *
     * @param friend   Specifies if the friend feature is supported based on {@link FeatureState}
     * @param lowPower Specifies if the low power feature is supported based on {@link FeatureState}
     * @param proxy    Specifies if the proxy feature is supported based on {@link FeatureState}
     * @param relay    Specifies if the relay feature is supported based on {@link FeatureState}
     */
    public Features(@FeatureState final int friend, @FeatureState final int lowPower, @FeatureState final int proxy, @FeatureState final int relay) {
        this.friend = friend;
        this.lowPower = lowPower;
        this.proxy = proxy;
        this.relay = relay;
    }

    private Features(Parcel in) {
        friend = in.readInt();
        lowPower = in.readInt();
        proxy = in.readInt();
        relay = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(friend);
        dest.writeInt(lowPower);
        dest.writeInt(proxy);
        dest.writeInt(relay);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Features> CREATOR = new Creator<Features>() {
        @Override
        public Features createFromParcel(Parcel in) {
            return new Features(in);
        }

        @Override
        public Features[] newArray(int size) {
            return new Features[size];
        }
    };

    /**
     * Returns the friend feature state
     */
    @FeatureState
    public int getFriend() {
        return friend;
    }

    /**
     * Sets the friend feature of the node
     *
     * @param friend {@link FeatureState}
     */
    public void setFriend(@FeatureState final int friend) {
        this.friend = friend;
    }

    /**
     * Returns the low power feature state
     */
    @FeatureState
    public int getLowPower() {
        return lowPower;
    }

    /**
     * Sets the low power feature of the node
     *
     * @param lowPower {@link FeatureState}
     */
    public void setLowPower(@FeatureState final int lowPower) {
        this.lowPower = lowPower;
    }

    /**
     * Returns the proxy feature state
     */
    @FeatureState
    public int getProxy() {
        return proxy;
    }

    /**
     * Sets the proxy feature of the node
     *
     * @param proxy {@link FeatureState}
     */
    public void setProxy(@FeatureState final int proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns the relay feature state
     */
    @FeatureState
    public int getRelay() {
        return relay;
    }

    /**
     * Sets the relay feature of the node
     *
     * @param relay {@link FeatureState}
     */
    public void setRelay(@FeatureState final int relay) {
        this.relay = relay;
    }

    /**
     * Returns true if friend feature is supported and false otherwise
     */
    public boolean isFriendFeatureSupported() {
        switch (friend) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    /**
     * Returns true if relay feature is supported and false otherwise
     */
    public boolean isRelayFeatureSupported() {
        switch (relay) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    /**
     * Returns true if proxy feature is supported and false otherwise
     */
    public boolean isProxyFeatureSupported() {
        switch (proxy) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

    /**
     * Returns true if low power feature is supported and false otherwise
     */
    public boolean isLowPowerFeatureSupported() {
        switch (lowPower) {
            case UNSUPPORTED:
                return false;
            case ENABLED:
            case DISABLED:
            default:
                return true;
        }
    }

}
