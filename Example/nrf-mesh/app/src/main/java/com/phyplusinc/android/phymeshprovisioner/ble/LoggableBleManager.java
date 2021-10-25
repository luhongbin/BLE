package com.phyplusinc.android.phymeshprovisioner.ble;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.Logger;

/**
 * The manager that logs to nRF Logger. If nRF Logger is not installed, logs are ignored.
 *
 * @param <T> the callbacks class.
 */
public abstract class LoggableBleManager<T extends BleManagerCallbacks> extends BleManager<T> {
	private ILogSession mLogSession;

	/**
	 * The manager constructor.
	 * <p>
	 * After constructing the manager, the callbacks object must be set with
	 * {@link #setGattCallbacks(BleManagerCallbacks)}.
	 *
	 * @param context the context.
	 */
	LoggableBleManager(@NonNull final Context context) {
		super(context);
	}

	/**
	 * Sets the log session to log into.
	 *
	 * @param session nRF Logger log session to log inti, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final ILogSession session) {
		mLogSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		Logger.log(mLogSession, LogContract.Log.Level.fromPriority(priority), message);
		Log.println(priority, "BleManager", message);
	}
}