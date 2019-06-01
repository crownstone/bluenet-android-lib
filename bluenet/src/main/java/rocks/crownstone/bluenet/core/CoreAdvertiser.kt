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
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

open class CoreAdvertiser(appContext: Context, evtBus: EventBus, looper: Looper) : CoreConnection(appContext, evtBus, looper) {
	private var advertiserSettingsBuilder = AdvertiseSettings.Builder()
	init {
		// Set maximum advertising frequency, and TX power, so that we can have a low timeout.
		advertiserSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
		advertiserSettingsBuilder.setConnectable(false)
		advertiserSettingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
	}

	/**
	 * Advertise data.
	 *
	 * @return Promise that resolves when successfully started advertising.
	 *         If promise fails with error busy, you can retry later.
	 */
	fun advertise(data: AdvertiseData, timeoutMs: Int): Promise<Unit, Exception> {
		Log.i(TAG, "advertise")
		val deferred = deferred<Unit, Exception>()
		val advertiseCallback = object : AdvertiseCallback() {
			override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
				super.onStartSuccess(settingsInEffect)
				deferred.resolve()
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
				deferred.reject(err)
			}
		}
		advertiserSettingsBuilder.setTimeout(timeoutMs)
		advertiser.startAdvertising(advertiserSettingsBuilder.build(), data, advertiseCallback)
		return deferred.promise
	}
}
