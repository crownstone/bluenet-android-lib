/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 21, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.Handler
import android.os.Looper
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.lang.Exception

class BackgroundBroadcaster(eventBus: EventBus, state: BluenetState, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	enum class BroadcastingState {
		STOPPED,
		STOPPING,
		STARTED,
		STARTING
	}

	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val broadcastPacketBuilder = BroadcastPacketBuilder(libState, encryptionManager)
	private var broadcasting = BroadcastingState.STOPPED
	private var started = false

	init {
		eventBus.subscribe(BluenetEvent.BLE_TURNED_OFF,                { data: Any? -> onBleTurnedOff() })
		eventBus.subscribe(BluenetEvent.BLE_TURNED_ON,                 { data: Any? -> onBleTurnedOn() })
		eventBus.subscribe(BluenetEvent.IBEACON_ENTER_REGION,          { data: Any? -> onRegionEnter() })
		eventBus.subscribe(BluenetEvent.IBEACON_EXIT_REGION,           { data: Any? -> onRegionExit() })
		eventBus.subscribe(BluenetEvent.LOCATION_CHANGE,               { data: Any? -> onLocationChange(data as SphereId) })
		eventBus.subscribe(BluenetEvent.TAP_TO_TOGGLE_CHANGED,         { data: Any? -> onTapToToggleChange(data as SphereId?) })
		eventBus.subscribe(BluenetEvent.SUN_TIME_CHANGED,              { data: Any? -> onSunTimeChange(data as SphereId?) })
		eventBus.subscribe(BluenetEvent.IGNORE_FOR_BEHAVIOUR_CHANGED,  { data: Any? -> onIgnoreForBehaviourChange(data as SphereId?) })
		eventBus.subscribe(BluenetEvent.CURRENT_SPHERE_CHANGED,        { data: Any? -> onCurrentSphereChange(data as SphereId?) })
		eventBus.subscribe(BluenetEvent.PROFILE_ID_CHANGED,            { data: Any? -> onProfileIdChange(data as SphereId) })
		eventBus.subscribe(BluenetEvent.DEVICE_TOKEN_CHANGED,          { data: Any? -> onDeviceTokenChange(data as SphereId) })
		eventBus.subscribe(BluenetEvent.SPHERE_SETTINGS_UPDATED,       { data: Any? -> onLibStateChange() })
	}

	@Synchronized
	fun start() {
		started = true
		updateBroadcast()
	}

	@Synchronized
	fun stop() {
		started = false
		cancelRetry()
		stopBroadcasting()
	}

	@Synchronized
	fun update() {
		if (started) {
			updateBroadcast()
		}
	}

	@Synchronized
	private fun updateBroadcast() {
		Log.d(TAG, "updateBroadcast")
		val sphereId = libState.currentSphere
		if (sphereId == null) {
			Log.i(TAG, "No current sphere")
			// TODO: maybe advertise last sphere instead?
			retryUpdateLater()
			return
		}
		val sphereSettings = libState.sphereState[sphereId] ?: return

		val useTimeForBroadcastValidation: Boolean = sphereSettings.useTimeForBroadcastValidation
		val validationTimestamp = when (useTimeForBroadcastValidation) {
			true -> Util.getLocalTimestamp() // TODO: use time from crownstones
			false -> BluenetProtocol.CAFEBABE
		}

		val payloadType =
				if (sphereSettings.sunRiseAfterMidnight >= 0 && sphereSettings.sunSetAfterMidnight >= 0) {
					CommandBroadcastType.SET_TIME
				}
				else {
					CommandBroadcastType.NO_OP
				}
		val commandPayload = BroadcastSingleItemPacket()
		when (payloadType) {
			CommandBroadcastType.SET_TIME -> {
				commandPayload.add(BroadcastSetTimePacket(null, sphereSettings.sunRiseAfterMidnight.toUint32(), sphereSettings.sunSetAfterMidnight.toUint32()))
			}
			else -> { }
		}

		val commandBroadcast = CommandBroadcastPacket(validationTimestamp, sphereId, payloadType, commandPayload)
		val advertiseData = broadcastPacketBuilder.getCommandBroadcastAdvertisement(commandBroadcast.sphereId, AccessLevel.HIGHEST_AVAILABLE, commandBroadcast)
		if (advertiseData == null) {
			Log.d(TAG, "Nothing to broadcast")
			retryUpdateLater()
			return
		}
		when (broadcasting) {
			BroadcastingState.STOPPED -> {
				Log.d(TAG, "startBroadcasting  cmd=$commandBroadcast")
				startBroadcasting(advertiseData)
			}
			BroadcastingState.STARTED -> {
				stopBroadcasting()
						.success { retryUpdateLater(100) }
			}
			BroadcastingState.STARTING -> { retryUpdateLater() }
			BroadcastingState.STOPPING -> { retryUpdateLater() }
		}
	}

	@Synchronized
	private fun startBroadcasting(advertiseData: AdvertiseData) {
		if (broadcasting != BroadcastingState.STOPPED) {
			Log.w(TAG, "Wrong state: $broadcasting")
			retryUpdateLater()
		}
		broadcasting = BroadcastingState.STARTING
		bleCore.backgroundAdvertise(advertiseData)
				.success {
					Log.d(TAG, "Started broadcasting")
					broadcasting = BroadcastingState.STARTED
				}
				.fail {
					Log.w(TAG, "Failed to start broadcasting: $it")
					broadcasting = BroadcastingState.STOPPED
					retryUpdateLater()
				}
	}

	@Synchronized
	private fun stopBroadcasting(): Promise<Unit, Exception> {
		if (broadcasting != BroadcastingState.STARTED) {
			Log.w(TAG, "Wrong state: $broadcasting")
			retryUpdateLater()
		}
		broadcasting = BroadcastingState.STOPPING
		bleCore.stopBackgroundAdvertise()
		Log.d(TAG, "Stopped broadcasting")
		return Util.waitPromise(100, handler)
				.then { broadcasting = BroadcastingState.STOPPED }
	}

	@Synchronized
	private fun retryUpdateLater(delayMs: Long = 500) {
		Log.d(TAG, "retryUpdate")
		cancelRetry()
		if (started) {
			handler.postDelayed(retryRunnable, delayMs)
		}
	}

	@Synchronized
	private fun cancelRetry() {
		Log.d(TAG, "cancelRetry")
		handler.removeCallbacks(retryRunnable)
	}

	private val retryRunnable = Runnable {
		updateBroadcast()
	}

	@Synchronized
	private fun onBleTurnedOff() {
		// Advertising is automatically stopped
		broadcasting = BroadcastingState.STOPPED
	}

	@Synchronized
	private fun onBleTurnedOn() {
		// Start background advertising
		update()
	}

	@Synchronized
	private fun onRegionEnter() {
		// Start background advertising
	}

	@Synchronized
	private fun onRegionExit() {
		// Stop background advertising?
	}

	@Synchronized
	private fun onLocationChange(sphereId: SphereId) {
		update()
	}

	@Synchronized
	private fun onTapToToggleChange(sphereId: SphereId?) {
		update()
	}

	@Synchronized
	private fun onSunTimeChange(sphereId: SphereId?) {
		update()
	}

	@Synchronized
	private fun onIgnoreForBehaviourChange(sphereId: SphereId?) {
		update()
	}

	@Synchronized
	private fun onCurrentSphereChange(sphereId: SphereId?) {
		update()
	}

	@Synchronized
	private fun onProfileIdChange(sphereId: SphereId) {
		update()
	}

	@Synchronized
	private fun onDeviceTokenChange(sphereId: SphereId) {
		update()
	}

	@Synchronized
	private fun onLibStateChange() {
		update()
	}
}