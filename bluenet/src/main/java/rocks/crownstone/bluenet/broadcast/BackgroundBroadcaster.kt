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
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.Util
import java.lang.Exception

class BackgroundBroadcaster(evtBus: EventBus, state: BluenetState, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	enum class BroadcastingState {
		STOPPED,
		STOPPING,
		STARTED,
		STARTING
	}

	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val broadcastPacketBuilder = BroadcastPacketBuilder(libState, encryptionManager)
	private var broadcasting = BroadcastingState.STOPPED
	private var started = false

	init {
		evtBus.subscribe(BluenetEvent.BLE_TURNED_OFF,          { data: Any? -> onBleTurnedOff() })
		evtBus.subscribe(BluenetEvent.BLE_TURNED_ON,           { data: Any? -> onBleTurnedOn() })
		evtBus.subscribe(BluenetEvent.IBEACON_ENTER_REGION,    { data: Any? -> onRegionEnter() })
		evtBus.subscribe(BluenetEvent.IBEACON_EXIT_REGION,     { data: Any? -> onRegionExit() })
		evtBus.subscribe(BluenetEvent.LOCATION_CHANGE,         { data: Any? -> onLocationChange(data as SphereId) })
		evtBus.subscribe(BluenetEvent.TAP_TO_TOGGLE_CHANGED,   { data: Any? -> onTapToToggleChange(data as SphereId?) })
		evtBus.subscribe(BluenetEvent.SPHERE_SETTINGS_UPDATED, { data: Any? -> onLibStateChange() })
		evtBus.subscribe(BluenetEvent.IGNORE_FOR_BEHAVIOUR_CHANGED,  { data: Any? -> onIgnoreForBehaviourChange(data as SphereId?) })
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
		val validationTimestamp = Conversion.toUint32(BluenetProtocol.CAFEBABE) // TODO: use time from crownstones
		val commandBroadcast = CommandBroadcastPacket(validationTimestamp, sphereId, CommandBroadcastType.NO_OP, BroadcastSingleItemPacket())
		val advertiseData = broadcastPacketBuilder.getCommandBroadcastAdvertisement(commandBroadcast.sphereId, AccessLevel.HIGHEST_AVAILABLE, commandBroadcast)
		if (advertiseData == null) {
			Log.d(TAG, "Nothing to broadcast")
			retryUpdateLater()
			return
		}
		when (broadcasting) {
			BroadcastingState.STOPPED -> {
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
	private fun onIgnoreForBehaviourChange(sphereId: SphereId?) {
		update()
	}

	@Synchronized
	private fun onLibStateChange() {
		update()
	}
}