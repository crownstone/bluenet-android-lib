/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.SphereStateMap
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.util.*
import kotlin.collections.ArrayList

class CommandBroadcaster(evtBus: EventBus, state: SphereStateMap, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val queue = LinkedList<CommandBroadcastItem>()
	private var broadcasting = false

	/**
	 * Add an item to the queue.
	 */
	@Synchronized
	fun add(item: CommandBroadcastItem) {
		// Remove any items with same sphere, type, and stone id.
		for (it in queue) {
			if (it.type == item.type && it.stoneId == item.stoneId && it.sphereId == it.sphereId) {
				queue.remove(it)
				break
			}
		}
		// Put item in front of the queue.
		queue.addFirst(item)
		broadcastNext()
	}

	@Synchronized
	private fun broadcastNext() {
		Log.d(TAG, "broadcastNext")
		if (broadcasting) {
			return
		}
		val commandBroadcast = getNextCommandBroadcastPacket()
		if (commandBroadcast == null) {
			return
		}
		val sphereId = commandBroadcast.sphereId
		val keySet = encryptionManager.getKeySet(sphereId)
		val keyAccess = keySet?.getHighestKey()
		if (keyAccess == null) {
			Log.w(TAG, "Missing key for sphere $sphereId")
			broadcastNext()
			return
		}

		val sphereState = libState[sphereId]
		if (sphereState == null) {
			Log.w(TAG, "Missing state for sphere $sphereId")
			broadcastNext()
			return
		}
		val locationId = sphereState.locationId
		val profileId = sphereState.profileId
		val rssiOffset = getRssiOffset(sphereState.rssiOffset)
		val tapToToggleEnabled = sphereState.tapToToggleEnabled
		val sphereShortId = sphereState.settings.sphereShortId

		val backgroundBroadcast = BackgroundBroadcastPayloadPacket(0, locationId, profileId, rssiOffset, tapToToggleEnabled)
		val commandBroadcastHeader = CommandBroadcastHeaderPacket(0, sphereShortId, keyAccess.accessLevel.num, backgroundBroadcast)

		val commandBroadcastBytes = commandBroadcast.getArray() ?: return
		val backgroundBroadcastBytes = backgroundBroadcast.getArray() ?: return
		// TODO: put backgroundBroadcast in 4 16bit service UUIDs.
		val commandBroadcastUuid = Conversion.bytesToUuid(commandBroadcastBytes)
		val advertiseData = AdvertiseData.Builder()
				.addServiceUuid(ParcelUuid(commandBroadcastUuid))
				.build()
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
		broadcastNext()
	}

	@Synchronized
	private fun getNextCommandBroadcastPacket(): CommandBroadcastPacket? {
		if (queue.isEmpty()) {
			return null
		}
		val validationTimestamp = Conversion.toUint32(BluenetProtocol.CAFEBABE)
		val firstItem = getNextItemFromQueue() ?: return null

		val payload = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> BroadcastItemListPacket()
			else -> BroadcastSingleItemPacket()
		}
		val type = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> CommandBroadcastType.MULTI_SWITCH
			CommandBroadcastItemType.SET_TIME -> CommandBroadcastType.SET_TIME
		}
		val packet = CommandBroadcastPacket(validationTimestamp, firstItem.sphereId, type, payload)
		val addedItems = ArrayList<CommandBroadcastItem>()
		payload.add(firstItem.payload)
		addedItems.add(firstItem)
		while (!payload.isFull()) {
			val item = getNextItemFromQueue(firstItem)
			if (item == null) {
				break
			}
			payload.add(item.payload)
			addedItems.add(firstItem)
		}
		for (it in addedItems) {
			putItemBackInQueue(it)
		}
		return packet
	}

	/**
	 * Get and remove next item from queue.
	 */
	@Synchronized
	private fun getNextItemFromQueue(): CommandBroadcastItem? {
		if (queue.isEmpty()) {
			return null
		}
		return queue.removeFirst()
	}

	/**
	 * Get and remove next item from queue with similar type and sphere id.
	 */
	@Synchronized
	private fun getNextItemFromQueue(firstItem: CommandBroadcastItem): CommandBroadcastItem? {
		if (queue.isEmpty()) {
			return null
		}
		for (it in queue) {
			if (it.type == firstItem.type && it.sphereId == firstItem.sphereId) {
				queue.remove(it)
				return it
			}
		}
		return null
	}

	/**
	 * Decrease the timeout count, and put item back in queue.
	 */
	@Synchronized
	private fun putItemBackInQueue(item: CommandBroadcastItem) {
		item.timeoutCount -= 1
		if (item.timeoutCount == 0) {
			return
		}
		// Put item at the back of the queue.
		queue.addLast(item)
	}

	/**
	 * Get "compressed" rssi offset, a 4 bit uint.
	 */
	private fun getRssiOffset(offset: Int): Uint8 {
		return Conversion.toUint8(offset / 2 + 8)
	}
}