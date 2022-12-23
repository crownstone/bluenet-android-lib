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
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.lang.IllegalStateException

open class CoreAdvertiser(appContext: Context, eventBus: EventBus, looper: Looper) : CoreInit(appContext, eventBus, looper) {
	private var advertiserSettingsBuilder = AdvertiseSettings.Builder()
//	private var advertiseStarted = false
	private var advertiseStartPromise: Deferred<Unit, Exception>? = null
	private var advertiseCallback = object : AdvertiseCallback() {
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
			super.onStartSuccess(settingsInEffect)
			Log.i(TAG, "Started advertising")
//			advertiseStarted = true
			advertiseStartPromise?.resolve()
		}

		override fun onStartFailure(errorCode: Int) {
			super.onStartFailure(errorCode)
			val err = when (errorCode) {
				ADVERTISE_FAILED_DATA_TOO_LARGE -> Errors.SizeWrong()
				ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Errors.Busy()
				ADVERTISE_FAILED_ALREADY_STARTED -> Errors.Busy()
//					ADVERTISE_FAILED_INTERNAL_ERROR
//					ADVERTISE_FAILED_FEATURE_UNSUPPORTED
				else -> java.lang.Exception("Advertise failure err=$errorCode")
			}
			Log.i(TAG, "Failed advertising err=$errorCode")
//			advertiseStarted = false
			advertiseStartPromise?.reject(err)
		}
	}

	private var backgroundAdvertiserSettingsBuilder = AdvertiseSettings.Builder()
//	private var backgroundAdvertiseStarted = false
	private var backgroundAdvertiseStartPromise: Deferred<Unit, Exception>? = null
	private var backgroundAdvertiseCallback = object : AdvertiseCallback() {
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
			super.onStartSuccess(settingsInEffect)
			Log.i(TAG, "Started background advertising")
//			backgroundAdvertiseStarted = true
			backgroundAdvertiseStartPromise?.resolve()
		}

		override fun onStartFailure(errorCode: Int) {
			super.onStartFailure(errorCode)
			val err = when (errorCode) {
				ADVERTISE_FAILED_DATA_TOO_LARGE -> Errors.SizeWrong()
				ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Errors.Busy()
				ADVERTISE_FAILED_ALREADY_STARTED -> Errors.Busy()
//					ADVERTISE_FAILED_INTERNAL_ERROR
//					ADVERTISE_FAILED_FEATURE_UNSUPPORTED
				else -> java.lang.Exception("Advertise failure err=$errorCode")
			}
			Log.i(TAG, "Failed background advertising err=$errorCode")
//			backgroundAdvertiseStarted = false
			backgroundAdvertiseStartPromise?.reject(err)
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
		if (advertiseStartPromise?.promise?.isDone() == false) {
			Log.i(TAG, "busy: promise=${advertiseStartPromise?.promise} done=${advertiseStartPromise?.promise?.isDone()}")
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()
		advertiseStartPromise = deferred

		advertiserSettingsBuilder.setTimeout(timeoutMs)
		try {
			advertiser.startAdvertising(advertiserSettingsBuilder.build(), data, advertiseCallback)
//			advertiseStarted = true
		}
		catch (e: IllegalStateException) {
			Log.w(TAG, "Advertise couldn't start: $e")
			deferred.reject(e)
		}
		return deferred.promise
	}

	@Synchronized
	fun stopAdvertise() {
		Log.i(TAG, "stopAdvertise")
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
		if (backgroundAdvertiseStartPromise?.promise?.isDone() == false) {
			Log.i(TAG, "busy: promise=${backgroundAdvertiseStartPromise?.promise} done=${backgroundAdvertiseStartPromise?.promise?.isDone()}")
			return Promise.ofFail(Errors.Busy())
		}
		val deferred = deferred<Unit, Exception>()
		backgroundAdvertiseStartPromise = deferred

		try {
			advertiser.startAdvertising(backgroundAdvertiserSettingsBuilder.build(), data, backgroundAdvertiseCallback)
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
}
