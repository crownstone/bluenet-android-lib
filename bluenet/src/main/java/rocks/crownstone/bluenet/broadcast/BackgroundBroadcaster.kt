package rocks.crownstone.bluenet.broadcast

import android.os.Handler
import android.os.Looper
import nl.komponents.kovenant.Promise
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
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val broadcastPacketBuilder = BroadcastPacketBuilder(libState, encryptionManager)
	private var broadcasting = false
	private var started = false

	init {
		evtBus.subscribe(BluenetEvent.BLE_TURNED_OFF, ::onBleTurnedOff)
		evtBus.subscribe(BluenetEvent.BLE_TURNED_ON, ::onBleTurnedOn)
		evtBus.subscribe(BluenetEvent.IBEACON_ENTER_REGION, ::onRegionEnter)
		evtBus.subscribe(BluenetEvent.IBEACON_EXIT_REGION, ::onRegionExit)
		evtBus.subscribe(BluenetEvent.LOCATION_CHANGE, ::onLocationChange)
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
			return
		}
		stopBroadcasting()
				.success {
					broadcasting = true
					bleCore.backgroundAdvertise(advertiseData)
							.success {
								Log.d(TAG, "Started broadcasting")
							}
							.fail {
								Log.w(TAG, "Failed to start broadcasting: $it")
								broadcasting = false
								retryUpdate()
							}
				}
				.fail {
					Log.w(TAG, "Failed to stop broadcasting: $it")
					retryUpdate()
				}
	}

	@Synchronized
	private fun retryUpdate() {
		cancelRetry()
		if (started) {
			handler.postDelayed(retryRunnable, 500)
		}
	}

	@Synchronized
	private fun cancelRetry() {
		handler.removeCallbacks(retryRunnable)
	}

	private val retryRunnable = Runnable {
		updateBroadcast()
	}

	@Synchronized
	private fun stopBroadcasting(): Promise<Unit, Exception> {
		if (broadcasting) {
			bleCore.stopBackgroundAdvertise()
			return Util.waitPromise(100, handler)
		}
		return Promise.ofSuccess(Unit)
	}

	@Synchronized
	private fun onBleTurnedOff(data: Any) {
		// Advertising is automatically stopped
		broadcasting = false
	}

	@Synchronized
	private fun onBleTurnedOn(data: Any) {
		// Start background advertising
	}

	@Synchronized
	private fun onRegionEnter(data: Any) {
		// Start background advertising
	}

	@Synchronized
	private fun onRegionExit(data: Any) {
		// Stop background advertising?
	}

	@Synchronized
	private fun onLocationChange(data: Any) {
		// Change background advertising
	}
}