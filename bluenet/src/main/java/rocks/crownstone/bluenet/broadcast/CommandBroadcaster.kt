/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_TIME_MS
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.lang.Exception

/**
 * Class to broadcast command advertisements.
 *
 * Each command will be put in queue and advertised multiple times to increase likeliness it is received.
 * If there are more commands in queue, they will be merged if possible.
 * If there are many commands in queue, they will be advertised interleaved.
 */
class CommandBroadcaster(evtBus: EventBus, state: BluenetState, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val queue = CommandBroadcastQueue()
	private val broadcastPacketBuilder = BroadcastPacketBuilder(libState, encryptionManager)
	private var startTime = 0L
	private var broadcasting = false

	init {
		evtBus.subscribe(BluenetEvent.BLE_TURNED_OFF, { data: Any? -> onBleTurnedOff() })
	}

	/**
	 * Broadcast a switch command.
	 */
	@Synchronized
	fun switch(sphereId: SphereId, stoneId: Uint8, switchValue: Uint8): Promise<Unit, Exception> {
//		return Promise.ofSuccess(Unit)
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSwitchItemPacket(stoneId, switchValue)
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.SWITCH,
				stoneId,
				commandItem,
				COMMAND_BROADCAST_TIME_MS
		)
		add(item)
		return deferred.promise
	}

	/**
	 * Set the time of all Crownstones.
	 */
	@Synchronized
	fun setTime(sphereId: SphereId, timestamp: Uint32): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSetTimePacket(timestamp)
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.SET_TIME,
				null,
				commandItem,
				COMMAND_BROADCAST_TIME_MS
		)
		add(item)
		return deferred.promise
	}

	/**
	 * Set time of a Crownstone that has lost the time, or is far out of sync.
	 *
	 * This is targeted, as it will need a different validation timestamp for encryption.
	 */
	@Synchronized
	fun setTime(sphereId: SphereId, timestamp: Uint32, stoneId: Uint8, validationTimestamp: Uint32?): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSetTimePacket(timestamp)
		// TODO: if validationTimestamp == null, get the time of that crownstone.
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.SET_TIME,
				stoneId,
				commandItem,
				COMMAND_BROADCAST_TIME_MS,
				validationTimestamp
		)
		add(item)
		return deferred.promise
	}

	/**
	 * Add an item to the queue.
	 *
	 * If an item with same sphereId, stoneId, and type, was already in queue, it will be overwritten.
	 * The current broadcast will be canceled, so that this newly added item will be broadcasted immediately.
	 */
	@Synchronized
	private fun add(item: CommandBroadcastItem) {
		if (!bleCore.isBleReady(true)) {
			item.reject(Errors.BleNotReady())
			return
		}
		increaseCommandCount(item.sphereId)
		queue.add(item)
		cancelBroadcast()
		broadcastNext()
	}

	private fun increaseCommandCount(sphereId: SphereId) {
		val sphereState = libState.sphereState[sphereId]
		if (sphereState == null) {
			Log.w(TAG, "Missing state for sphere $sphereId")
			return
		}
		sphereState.commandCount = Conversion.toUint8(sphereState.commandCount + 1U) // Make sure it overflows
	}

	@Synchronized
	private fun broadcastNext() {
		if (broadcasting) {
			return
		}
		Log.i(TAG, "broadcastNext")
		if (!queue.hasNext()) {
			return
		}

		val commandBroadcast = queue.getNextCommandBroadcastPacket()
		if (commandBroadcast == null) {
			broadcastNext()
			return
		}
		val advertiseData = broadcastPacketBuilder.getCommandBroadcastAdvertisement(commandBroadcast.sphereId, AccessLevel.HIGHEST_AVAILABLE, commandBroadcast)
		if (advertiseData == null) {
			broadcastNext()
			return
		}
		broadcasting = true
		bleCore.advertise(advertiseData, COMMAND_BROADCAST_INTERVAL_MS)
				.success {
					startTime = SystemClock.elapsedRealtime() // ms since boot
					handler.postDelayed(onBroadcastDoneRunnable, COMMAND_BROADCAST_INTERVAL_MS.toLong())
				}
				.fail {
					broadcasting = false
					queue.advertisementDone(0, it)
				}
	}

	private val onBroadcastDoneRunnable = Runnable {
		onBroadcastDone()
	}

	@Synchronized
	private fun onBroadcastDone() {
		Log.i(TAG, "onBroadcastDone")
		bleCore.stopAdvertise()
		val now = SystemClock.elapsedRealtime() // ms since boot
		val timeBroadcasted = (now - startTime).toInt()
		broadcasting = false
		queue.advertisementDone(timeBroadcasted, null)
		broadcastNext()
	}

	@Synchronized
	private fun cancelBroadcast() {
		Log.i(TAG, "cancelBroadcast")
		handler.removeCallbacks(onBroadcastDoneRunnable)
		onBroadcastDone()
	}

	@Synchronized
	private fun onBleTurnedOff() {
		// Fail the current broadcast and anything in queue
		handler.removeCallbacks(onBroadcastDoneRunnable)
		queue.clear(Errors.BleNotReady())
		broadcasting = false
	}
}