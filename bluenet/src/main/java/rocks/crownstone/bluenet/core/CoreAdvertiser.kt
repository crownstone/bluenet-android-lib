/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.core

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.lang.IllegalStateException

open class CoreAdvertiser(appContext: Context, evtBus: EventBus, looper: Looper) : CoreConnection(appContext, evtBus, looper) {
	private var advertiserSettingsBuilder = AdvertiseSettings.Builder()
	private var advertiseCallback: AdvertiseCallback? = null
	private var backgroundAdvertiserSettingsBuilder = AdvertiseSettings.Builder()
	private var backgroundAdvertiseCallback: AdvertiseCallback? = null
	private var backgroundAdvertiseData: AdvertiseData? = null
	private var backgroundAdvertiseStarted = false

	// Some phones can only have one advertiser at a time.
	private var checkedMultipleAdvertisementSupport = false
	private var isMultipleAdvertisementSupported = false

	init {
		// Set maximum advertising frequency, and TX power, so that we can have a low timeout.
		advertiserSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
		advertiserSettingsBuilder.setConnectable(false)
		advertiserSettingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
		backgroundAdvertiserSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
		backgroundAdvertiserSettingsBuilder.setConnectable(false)
		backgroundAdvertiserSettingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
		backgroundAdvertiserSettingsBuilder.setTimeout(0)
	}

	/**
	 * Advertise data.
	 *
	 * @return Promise that resolves when successfully started advertising.
	 *         If promise fails with error busy, you can retry later.
	 */
	@Synchronized
	fun advertise(data: AdvertiseData, timeoutMs: Int): Promise<Unit, Exception> {
		Log.i(TAG, "advertise")
		if (!isAdvertiserReady(true)) {
			return Promise.ofFail(Errors.BleNotReady())
		}
		if (advertiseCallback != null) {
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()

		advertiseCallback = object : AdvertiseCallback() {
			override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
				Log.d(TAG, "advertise started")
				super.onStartSuccess(settingsInEffect)
				deferred.resolve()
			}

			override fun onStartFailure(errorCode: Int) {
				super.onStartFailure(errorCode)
				Log.w(TAG, "advertise failed: $errorCode")
				val err = when (errorCode) {
					ADVERTISE_FAILED_DATA_TOO_LARGE -> Errors.SizeWrong()
					ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Errors.Busy()
					ADVERTISE_FAILED_ALREADY_STARTED -> Errors.Busy()
//					ADVERTISE_FAILED_INTERNAL_ERROR
//					ADVERTISE_FAILED_FEATURE_UNSUPPORTED
					else -> java.lang.Exception("Advertise failure err=$errorCode")
				}
				deferred.reject(err)
			}
		}
		advertiserSettingsBuilder.setTimeout(timeoutMs)
		checkMultipleAdvertisementSupport()
		if (!isMultipleAdvertisementSupported) {
			pauseBackgroundAdvertise()
		}
		try {
			advertiser.startAdvertising(advertiserSettingsBuilder.build(), data, advertiseCallback)
		}
		catch (e: IllegalStateException) {
			Log.w(TAG, "Advertise couldn't start: $e")
			deferred.reject(e)
		}
		return deferred.promise
	}

	@Synchronized
	fun stopAdvertise() {
		if (advertiseCallback != null) {
			Log.d(TAG, "stopAdvertise")
			if (isAdvertiserReady(true)) {
				try {
					advertiser.stopAdvertising(advertiseCallback)
				}
				catch (e: IllegalStateException) {
					Log.e(TAG, "Ble not ready, cache was wrong.")
					checkBleEnabled()
				}
			}
			advertiseCallback = null
		}
		if (!isMultipleAdvertisementSupported) {
			resumeBackgroundAdvertise()
		}
	}

//	private val onAdvertiseDoneRunnable = Runnable {
//		onAdvertiseDone()
//	}
//
//	@Synchronized
//	private fun onAdvertiseDone() {
//		Log.i(TAG, "onAdvertiseDone")
//		stop()
//	}

	@Synchronized
	fun backgroundAdvertise(data: AdvertiseData): Promise<Unit, Exception> {
		backgroundAdvertiseData = data
		backgroundAdvertiseStarted = true
		return backgroundAdvertise()
	}

	@Synchronized
	fun stopBackgroundAdvertise() {
		Log.d(TAG, "stopBackgroundAdvertise")
		backgroundAdvertiseStarted = false
		stopBackgroundAdvertiseInternal()
	}

	@Synchronized
	private fun backgroundAdvertise(): Promise<Unit, Exception> {
		Log.i(TAG, "backgroundAdvertise")
		if (!isAdvertiserReady(true)) {
			return Promise.ofFail(Errors.BleNotReady())
		}
		val deferred = deferred<Unit, Exception>()

		checkMultipleAdvertisementSupport()
		if (!isMultipleAdvertisementSupported && advertiseCallback != null) {
			// Will be started when advertising stops
			return Promise.ofSuccess(Unit)
		}

		backgroundAdvertiseCallback = object : AdvertiseCallback() {
			override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
				Log.d(TAG, "backgroundAdvertise started")
				super.onStartSuccess(settingsInEffect)
				deferred.resolve()
			}

			override fun onStartFailure(errorCode: Int) {
				super.onStartFailure(errorCode)
				Log.w(TAG, "backgroundAdvertise failed: $errorCode")
				val err = when (errorCode) {
					ADVERTISE_FAILED_DATA_TOO_LARGE -> Errors.SizeWrong()
					ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Errors.Busy()
					ADVERTISE_FAILED_ALREADY_STARTED -> Errors.Busy()
//					ADVERTISE_FAILED_INTERNAL_ERROR
//					ADVERTISE_FAILED_FEATURE_UNSUPPORTED
					else -> java.lang.Exception("Advertise failure err=$errorCode")
				}
				deferred.reject(err)
			}
		}
		try {
			advertiser.startAdvertising(backgroundAdvertiserSettingsBuilder.build(), backgroundAdvertiseData, backgroundAdvertiseCallback)
		}
		catch (e: IllegalStateException) {
			Log.w(TAG, "Advertise couldn't start: $e")
			deferred.reject(e)
		}
		return deferred.promise
	}

	@Synchronized
	private fun stopBackgroundAdvertiseInternal() {
		if (backgroundAdvertiseCallback != null) {
			Log.d(TAG, "stopBackgroundAdvertiseInternal")
			if (isAdvertiserReady(true)) {
				try {
					advertiser.stopAdvertising(backgroundAdvertiseCallback)
				}
				catch (e: IllegalStateException) {
					Log.e(TAG, "Ble not ready, cache was wrong.")
					checkBleEnabled()
				}
			}
			backgroundAdvertiseCallback = null
		}
	}

	@Synchronized
	private fun pauseBackgroundAdvertise() {
		Log.d(TAG, "pauseBackgroundAdvertise")
		stopBackgroundAdvertise()
	}

	@Synchronized
	private fun resumeBackgroundAdvertise() {
		Log.d(TAG, "resumeBackgroundAdvertise")
		if (backgroundAdvertiseStarted) {
			backgroundAdvertise()
		}
	}

	@Synchronized
	private fun checkMultipleAdvertisementSupport() {
		if (!checkedMultipleAdvertisementSupport) {
			// Since BluetoothAdapter.isMultipleAdvertisementSupported() always returns false when bluetooth is off,
			// we should check it when bluetooth is on.
			// For now, we just check it once on start.
			isMultipleAdvertisementSupported = bleAdapter.isMultipleAdvertisementSupported
			Log.i(TAG, "isMultipleAdvertisementSupported=$isMultipleAdvertisementSupported")
		}
	}
}
