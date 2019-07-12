/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import android.os.Handler
import android.os.Looper
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_TIME_MS
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.lang.Exception

class CommandBroadcaster(evtBus: EventBus, state: SphereStateMap, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val queue = CommandBroadcastQueue(libState, encryptionManager)
	private var broadcasting = false

	init {
		evtBus.subscribe(BluenetEvent.BLE_TURNED_OFF, ::onBleTurnedOff)
	}

	/**
	 * Broadcast a switch command.
	 */
	@Synchronized
	fun switch(sphereId: SphereId, stoneId: Uint8, switchValue: Uint8): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSwitchItemPacket(stoneId, switchValue)
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.SWITCH,
				stoneId,
				commandItem,
				COMMAND_BROADCAST_TIME_MS / COMMAND_BROADCAST_INTERVAL_MS
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
				COMMAND_BROADCAST_TIME_MS / COMMAND_BROADCAST_INTERVAL_MS
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
				COMMAND_BROADCAST_TIME_MS / COMMAND_BROADCAST_INTERVAL_MS,
				validationTimestamp
		)
		add(item)
		return deferred.promise
	}

	/**
	 * Add an item to the queue.
	 */
	@Synchronized
	private fun add(item: CommandBroadcastItem) {
		if (!bleCore.isBleReady()) {
			item.reject(Errors.BleNotReady())
			return
		}
		queue.add(item)
		broadcastNext()
	}

	@Synchronized
	private fun broadcastNext() {
		Log.d(TAG, "broadcastNext")
		if (!queue.hasNext()) {
			return
		}
		val advertiseData = queue.getNextAdvertisement()
		if (advertiseData == null) {
			broadcastNext()
			return
		}
		broadcasting = true
		bleCore.advertise(advertiseData, BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS)
		handler.postDelayed(onBroadcastDoneRunnable, BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS.toLong())
	}

	private val onBroadcastDoneRunnable = Runnable {
		onBroadcastDone()
	}

	@Synchronized
	private fun onBroadcastDone() {
		Log.i(TAG, "onBroadcastDone")
		broadcasting = false
		queue.advertisementDone(null)
		broadcastNext()
	}

	@Synchronized
	private fun onBleTurnedOff(data: Any) {
		// Fail the current broadcast and anything in queue
		handler.removeCallbacks(onBroadcastDoneRunnable)
		queue.clear(Errors.BleNotReady())
		broadcasting = false
	}
}