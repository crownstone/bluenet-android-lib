/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.advertising

import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.advertising.AdvertiseItemListPacket
import rocks.crownstone.bluenet.packets.advertising.AdvertiseSingleItemPacket
import rocks.crownstone.bluenet.packets.advertising.CommandAdvertisementPacket
import rocks.crownstone.bluenet.packets.advertising.CommandAdvertisementType
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import java.util.*
import kotlin.collections.ArrayList

class CommandAdvertiser(evtBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val queue = LinkedList<CommandAdvertiserItem>()

	/**
	 * Add an item to the queue.
	 */
	fun add(item: CommandAdvertiserItem) {
		// Remove any items with same type and id.
		for (it in queue) {
			if (it.type == item.type && it.id == item.id) {
				queue.remove(it)
				break
			}
		}
		// Put item in front of the queue.
		queue.addFirst(item)
	}



	private fun getNextCommandAdvertisementPacket(): CommandAdvertisementPacket? {
		if (queue.isEmpty()) {
			return null
		}
		val validationTimestamp = Conversion.toUint32(BluenetProtocol.CAFEBABE)
		val firstItem = getNextItemFromQueue(null) ?: return null

		val payload = when (firstItem.type) {
			CommandAdvertiserItemType.SWITCH -> AdvertiseItemListPacket()
			else -> AdvertiseSingleItemPacket()
		}
		val type = when (firstItem.type) {
			CommandAdvertiserItemType.SWITCH -> CommandAdvertisementType.MULTI_SWITCH
			CommandAdvertiserItemType.SET_TIME -> CommandAdvertisementType.SET_TIME
		}
		val packet = CommandAdvertisementPacket(validationTimestamp, type, payload)
		val addedItems = ArrayList<CommandAdvertiserItem>()
		payload.add(firstItem.payload)
		addedItems.add(firstItem)
		while (!payload.isFull()) {
			val item = getNextItemFromQueue(firstItem.type)
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
	 * Get and remove next item from queue with similar type.
	 */
	private fun getNextItemFromQueue(type: CommandAdvertiserItemType?): CommandAdvertiserItem? {
		if (queue.isEmpty()) {
			return null
		}
		if (type == null) {
			return queue.removeFirst()
		}
		for (it in queue) {
			if (it.type == type) {
				queue.remove(it)
				return it
			}
		}
		return null
	}

	/**
	 * Decrease the timeout count, and put item back in queue.
	 */
	private fun putItemBackInQueue(item: CommandAdvertiserItem) {
		item.timeoutCount -= 1
		if (item.timeoutCount == 0) {
			return
		}
		// Put item at the back of the queue.
		queue.addLast(item)
	}
}