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
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_INTERVAL_MS
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_RELIABLE_TIME_MS
import rocks.crownstone.bluenet.BluenetConfig.COMMAND_BROADCAST_TIME_MS
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import kotlin.Exception

/**
 * Class to broadcast command advertisements.
 *
 * Each command will be put in queue and advertised multiple times to increase likeliness it is received.
 * If there are more commands in queue, they will be merged if possible.
 * If there are many commands in queue, they will be advertised interleaved.
 */
class CommandBroadcaster(eventBus: EventBus, state: BluenetState, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val queue = CommandBroadcastQueue(state)
	private val broadcastPacketBuilder = BroadcastPacketBuilder(libState, encryptionManager)
	private var startTime = 0L
	private var broadcasting = false

	init {
		eventBus.subscribe(BluenetEvent.BLE_TURNED_OFF, { data: Any? -> onBleTurnedOff() })
	}

	/**
	 * De-init the library.
	 *
	 * Cleans up everything that isn't automatically cleaned.
	 */
	@Synchronized
	fun destroy() {
		Log.i(TAG, "destroy")
		handler.removeCallbacksAndMessages(null)
		bleCore.stopAdvertise()
	}

	/**
	 * Broadcast a switch command.
	 *
	 * @param sphereId       Sphere ID of the stone.
	 * @param stoneId        The ID of the stone.
	 * @param switchValue    Value to set the switch to.
	 * @param autoExecute    Whether to execute immediately.
	 *                       Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @return Promise
	 */
	@Synchronized
	fun switch(sphereId: SphereId, stoneId: Uint8, switchValue: Uint8, autoExecute: Boolean = true): Promise<Unit, Exception> {
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
		add(item, autoExecute)
		return deferred.promise
	}

	/**
	 * Broadcast a switch command.
	 *
	 * @param sphereId       Sphere ID of the stone.
	 * @param stoneId        The ID of the stone.
	 * @param switchValue    Value to set the switch to.
	 * @param autoExecute    Whether to execute immediately.
	 *                       Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @return Promise
	 */
	fun switch(sphereId: SphereId, stoneId: Uint8, switchValue: SwitchCommandValue, autoExecute: Boolean = true): Promise<Unit, Exception> {
		return switch(sphereId, stoneId, switchValue.num, autoExecute)
	}

	/**
	 * Broadcast a turn switch on command.
	 *
	 * @param sphereId       Sphere ID of the stone.
	 * @param stoneId        The ID of the stone.
	 * @param autoExecute    Whether to execute immediately.
	 *                       Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @return Promise
	 */
	@Synchronized
	fun switchOn(sphereId: SphereId, stoneId: Uint8, autoExecute: Boolean = true): Promise<Unit, Exception> {
		return switch(sphereId, stoneId, SwitchCommandValue.SMART_ON, autoExecute)
	}

	/**
	 * Set behaviour settings.
	 *
	 * @param sphereId       Sphere ID to use.
	 * @param mode           Behaviour mode to set.
	 * @param autoExecute    Whether to execute immediately.
	 *                       Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @return Promise
	 */
	@Synchronized
	fun setBehaviourSettings(sphereId: SphereId, mode: BehaviourSettings, autoExecute: Boolean = true): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastBehaviourSettingsPacket(mode)
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.BEHAVIOUR_SETTINGS,
				null,
				commandItem,
				COMMAND_BROADCAST_RELIABLE_TIME_MS
		)
		add(item, autoExecute)
		return deferred.promise
	}

	/**
	 * Set the time of all Crownstones.
	 *
	 * @param sphereId                 Sphere ID of the stones.
	 * @param currentTime              Current local time.
	 * @param sunRiseAfterMidnight     Seconds after midnight at which the sun rises.
	 * @param sunSetAfterMidnight      Seconds after midnight at which the sun sets.
	 * @param autoExecute              Whether to execute immediately.
	 *                                 Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @param useTimeBasedValidation   Whether to use encryption validation based on the current time.
	 *                                 When the time on the crownstone(s) is offset too much, fall back to fixed value validation.
	 * @return Promise
	 */
	@Synchronized
	fun setTime(sphereId: SphereId, currentTime: Uint32, sunRiseAfterMidnight: Uint32, sunSetAfterMidnight: Uint32, autoExecute: Boolean = true, useTimeBasedValidation: Boolean = true): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSetTimePacket(currentTime, sunRiseAfterMidnight, sunSetAfterMidnight)
		val validationTimestamp = when (useTimeBasedValidation) {
			true -> null
			false -> BluenetProtocol.CAFEBABE
		}
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.SET_TIME,
				null,
				commandItem,
				COMMAND_BROADCAST_TIME_MS,
				validationTimestamp
		)
		add(item, autoExecute)
		return deferred.promise
	}

	/**
	 * Set the sunset and sunrise time of all Crownstones.
	 *
	 * @param sphereId                 Sphere ID of the stones.
	 * @param sunRiseAfterMidnight     Seconds after midnight at which the sun rises.
	 * @param sunSetAfterMidnight      Seconds after midnight at which the sun sets.
	 * @param autoExecute              Whether to execute immediately.
	 *                                 Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @return Promise
	 */
	@Synchronized
	fun setSunTime(sphereId: SphereId, sunRiseAfterMidnight: Uint32, sunSetAfterMidnight: Uint32, autoExecute: Boolean = true): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSetTimePacket(null, sunRiseAfterMidnight, sunSetAfterMidnight)
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.SET_TIME,
				null,
				commandItem,
				COMMAND_BROADCAST_TIME_MS
		)
		add(item, autoExecute)
		return deferred.promise
	}

	/**
	 * Set time of a Crownstone that has lost the time, or is far out of sync.
	 *
	 * This is targeted, as it will need a different validation timestamp for encryption.
	 *
	 * @param sphereId                 Sphere ID of the stones.
	 * @param currentTime              Current local time.
	 * @param sunRiseAfterMidnight     Seconds after midnight at which the sun rises.
	 * @param sunSetAfterMidnight      Seconds after midnight at which the sun sets.
	 * @param stoneId                  The ID of the stone.
	 * @param validationTimestamp      Timestamp to use for validation, the time set at the stone.
	 * @param autoExecute              Whether to execute immediately.
	 *                                 Set to false if you want to broadcast more similar commands, then call execute() after the last one.
	 * @return Promise
	 */
	@Synchronized
	fun setTime(sphereId: SphereId,
				currentTime: Uint32,
				sunRiseAfterMidnight: Uint32,
				sunSetAfterMidnight: Uint32,
				stoneId: Uint8,
				validationTimestamp: Uint32?,
				autoExecute: Boolean = true
	): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = BroadcastSetTimePacket(currentTime, sunRiseAfterMidnight, sunSetAfterMidnight)
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
		add(item, autoExecute)
		return deferred.promise
	}

	@Synchronized
	fun noop(sphereId: SphereId, autoExecute: Boolean = true): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val commandItem = EmptyPacket()
		val item = CommandBroadcastItem(
				deferred,
				sphereId,
				CommandBroadcastItemType.NO_OP,
				null,
				commandItem,
//				COMMAND_BROADCAST_TIME_MS
				COMMAND_BROADCAST_INTERVAL_MS
		)
		add(item, autoExecute)
		return deferred.promise
	}

	/**
	 * Interrupts the current broadcast, and broadcast the next command(s) in queue.
	 * The interrupted broadcast command(s) will be put at the back of the queue.
	 */
	@Synchronized
	fun execute() {
		cancelBroadcast()
		broadcastNext()
	}

	/**
	 * Add an item to the queue.
	 *
	 * If an item with same sphereId, stoneId, and type, was already in queue, it will be overwritten.
	 * With autoExecute true, the current broadcast will be canceled, so that this newly added item will be broadcasted immediately.
	 */
	@Synchronized
	private fun add(item: CommandBroadcastItem, autoExecute: Boolean) {
		if (!bleCore.isBleReady(true)) {
			item.reject(Errors.BleNotReady())
			return
		}
		// This might be increasing the count too often, but that's ok.
		increaseCommandCount(item.sphereId)
		queue.add(item)
		if (autoExecute) {
			execute()
		}
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
					if (it is Errors.BusyAlready) {
						// Fix the broadcasting state.
						broadcasting = true
					}
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
		val timeBroadcasted = when (startTime) {
			0L -> {
				Log.w(TAG, "No start time set")
				0
			}
			else -> {
				val now = SystemClock.elapsedRealtime() // ms since boot
				(now - startTime).toInt()
			}
		}
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
