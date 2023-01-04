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
import android.os.Looper
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.lang.IllegalStateException

open class CoreAdvertiser(appContext: Context, eventBus: EventBus, looper: Looper) : CoreInit(appContext, eventBus, looper) {
	private val ADVERTISE_START_TIMEOUT_MS = 500L
	private var advertiserSettingsBuilder = AdvertiseSettings.Builder()
//	private var advertiseStarted = false
	private var advertisePromise: Deferred<Unit, Exception>? = null
	private var advertiseCallback = object : AdvertiseCallback() {
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
			super.onStartSuccess(settingsInEffect)
			Log.i(TAG, "Started advertising")
//			advertiseStarted = true
			resolve(advertisePromise)
		}

		override fun onStartFailure(errorCode: Int) {
			super.onStartFailure(errorCode)
			val err = when (errorCode) {
				ADVERTISE_FAILED_DATA_TOO_LARGE -> Errors.SizeWrong()
				ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Errors.Busy()
				ADVERTISE_FAILED_ALREADY_STARTED -> Errors.BusyAlready("started")
//					ADVERTISE_FAILED_INTERNAL_ERROR
//					ADVERTISE_FAILED_FEATURE_UNSUPPORTED
				else -> java.lang.Exception("Advertise failure err=$errorCode")
			}
			Log.i(TAG, "Failed advertising err=$errorCode")
//			advertiseStarted = false
			reject(advertisePromise, err)

			// On failure, it's expected that advertising stopped.
			if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
				stopAdvertise()
			}
		}
	}

	private var backgroundAdvertiserSettingsBuilder = AdvertiseSettings.Builder()
//	private var backgroundAdvertiseStarted = false
	private var backgroundAdvertisePromise: Deferred<Unit, Exception>? = null
	private var backgroundAdvertiseCallback = object : AdvertiseCallback() {
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
			super.onStartSuccess(settingsInEffect)
			Log.i(TAG, "Started background advertising")
//			backgroundAdvertiseStarted = true
			resolve(backgroundAdvertisePromise)
		}

		override fun onStartFailure(errorCode: Int) {
			super.onStartFailure(errorCode)
			val err = when (errorCode) {
				ADVERTISE_FAILED_DATA_TOO_LARGE -> Errors.SizeWrong()
				ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Errors.Busy()
				ADVERTISE_FAILED_ALREADY_STARTED -> Errors.BusyAlready("started")
//					ADVERTISE_FAILED_INTERNAL_ERROR
//					ADVERTISE_FAILED_FEATURE_UNSUPPORTED
				else -> java.lang.Exception("Advertise failure err=$errorCode")
			}
			Log.i(TAG, "Failed background advertising err=$errorCode")
//			backgroundAdvertiseStarted = false
			reject(backgroundAdvertisePromise, err)

			// On failure, it's expected that advertising stopped.
			if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
				stopBackgroundAdvertise()
			}
		}
	}

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
		if (advertisePromise?.promise?.isDone() == false) {
			Log.i(TAG, "busy: promise=${advertisePromise?.promise} done=${advertisePromise?.promise?.isDone()}")
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()
		advertisePromise = deferred

		advertiserSettingsBuilder.setTimeout(timeoutMs)
		try {
			advertiser.startAdvertising(advertiserSettingsBuilder.build(), data, advertiseCallback)
			handler.postDelayed(advertiseTimeoutRunnable, ADVERTISE_START_TIMEOUT_MS)
//			advertiseStarted = true
		}
		catch (e: IllegalStateException) {
			Log.w(TAG, "Advertise couldn't start: $e")
			deferred.reject(e)
		}
		return deferred.promise
	}

	private val advertiseTimeoutRunnable = Runnable {
		advertiseTimeout()
	}

	@Synchronized
	private fun advertiseTimeout() {
		Log.i(TAG, "advertiseTimeout")
		// On failure, it's expected that advertising stopped.
		stopAdvertise()

		reject(advertisePromise, Errors.Timeout())
	}

	@Synchronized
	fun stopAdvertise() {
		Log.i(TAG, "stopAdvertise")
		reject(advertisePromise, Errors.Aborted())
		if (isAdvertiserReady(true)) {
			try {
				advertiser.stopAdvertising(advertiseCallback)
//				advertiseStarted = false
			}
			catch (e: IllegalStateException) {
				Log.e(TAG, "Ble not ready, cache was wrong.")
				checkBleEnabled()
			}
		}
	}



	@Synchronized
	fun backgroundAdvertise(data: AdvertiseData): Promise<Unit, Exception> {
		Log.i(TAG, "backgroundAdvertise")
		if (!isAdvertiserReady(true)) {
			return Promise.ofFail(Errors.BleNotReady())
		}
		if (backgroundAdvertisePromise?.promise?.isDone() == false) {
			Log.i(TAG, "busy: promise=${backgroundAdvertisePromise?.promise} done=${backgroundAdvertisePromise?.promise?.isDone()}")
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()
		backgroundAdvertisePromise = deferred

		try {
			advertiser.startAdvertising(backgroundAdvertiserSettingsBuilder.build(), data, backgroundAdvertiseCallback)
			handler.postDelayed(backgroundAdvertiseTimeoutRunnable, ADVERTISE_START_TIMEOUT_MS)
//			backgroundAdvertiseStarted = true
		}
		catch (e: IllegalStateException) {
			Log.w(TAG, "Background advertise couldn't start: $e")
			deferred.reject(e)
		}
		return deferred.promise
	}

	@Synchronized
	fun stopBackgroundAdvertise() {
		Log.i(TAG, "stopBackgroundAdvertise")
		reject(backgroundAdvertisePromise, Errors.Aborted())
		if (isAdvertiserReady(true)) {
			try {
				advertiser.stopAdvertising(backgroundAdvertiseCallback)
//				backgroundAdvertiseStarted = false
			}
			catch (e: IllegalStateException) {
				Log.e(TAG, "Ble not ready, cache was wrong.")
				checkBleEnabled()
			}
		}
	}

	private val backgroundAdvertiseTimeoutRunnable = Runnable {
		backgroundAdvertiseTimeout()
	}

	@Synchronized
	private fun backgroundAdvertiseTimeout() {
		Log.i(TAG, "backgroundAdvertiseTimeout")
		// On failure, it's expected that advertising stopped.
		stopBackgroundAdvertise()

		reject(backgroundAdvertisePromise, Errors.Timeout())
	}



	@Synchronized
	private fun resolve(promise: Deferred<Unit, Exception>?) {
		if (promise?.promise?.isDone() == false) {
			cancelTimeout(promise)
			promise.resolve()
		}
	}

	@Synchronized
	private fun reject(promise: Deferred<Unit, Exception>?, exception: Exception) {
		if (promise?.promise?.isDone() == false) {
			cancelTimeout(promise)
			promise.reject(exception)
		}
	}

	@Synchronized
	private fun cancelTimeout(promise: Deferred<Unit, Exception>) {
		if (promise == advertisePromise) {
			handler.removeCallbacks(advertiseTimeoutRunnable)
		}
		else {
			handler.removeCallbacks(backgroundAdvertiseTimeoutRunnable)
		}
	}

}
