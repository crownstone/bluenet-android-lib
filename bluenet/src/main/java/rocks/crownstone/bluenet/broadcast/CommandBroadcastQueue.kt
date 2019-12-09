/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.broadcast

import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

/**
 * Class that keeps up a queue of broadcast items.
 *
 * Selects which items to broadcast next.
 * Each item can have a promise that will be resolved when it has been advertised long enough.
 */
class CommandBroadcastQueue {
	private val TAG = this.javaClass.simpleName
	private val queue = LinkedList<CommandBroadcastItem>()
	private val advertisedItems = ArrayList<CommandBroadcastItem>()

	/**
	 * Add an item to the queue.
	 *
	 * @return True when an item in queue was overwritten.
	 */
	@Synchronized
	fun add(item: CommandBroadcastItem): Boolean {
		Log.d(TAG, "add $item")
		var overwritten = false
		// Remove any items with same sphere, type, and stone id.
		for (it in queue) {
			if (it.sphereId == it.sphereId &&
					it.type == item.type &&
					it.stoneId != null &&
					it.stoneId == item.stoneId
			) {
				overwritten = true
				it.reject(Errors.Aborted())
				queue.remove(it)
				break
			}
		}
		for (it in advertisedItems) {
			if (it.sphereId == it.sphereId &&
					it.type == item.type &&
					it.stoneId != null &&
					it.stoneId == item.stoneId
			) {
				overwritten = true
				it.stoppedAdvertising(0, Errors.Aborted())
				advertisedItems.remove(it)
				break
			}
		}
		// Put item in front of the queue.
		queue.addFirst(item)
		return overwritten
	}

	/**
	 * Returns true if there is a next advertisement.
	 */
	@Synchronized
	fun hasNext(): Boolean {
		return queue.isNotEmpty()
	}

	/**
	 * To be called when advertisement has been advertised.
	 */
	@Synchronized
	fun advertisementDone(advertisedTimeMs: Int, error: java.lang.Exception?) {
		for (it in advertisedItems) {
			it.stoppedAdvertising(advertisedTimeMs, error)
			putItemBackInQueue(it)
		}
		advertisedItems.clear()
	}

	/**
	 * Clear queue.
	 *
	 * Rejects any items that are still in queue.
	 */
	@Synchronized
	fun clear(error: Exception) {
		for (it in advertisedItems) {
			it.reject(error)
		}
		advertisedItems.clear()
		for (it in queue) {
			it.reject(error)
		}
		queue.clear()
	}

	/**
	 * Get next command broadcast packet, made from items in queue.
	 */
	@Synchronized
	fun getNextCommandBroadcastPacket(): CommandBroadcastPacket? {
		Log.d(TAG, "getNextCommandBroadcastPacket")
//		Log.v(TAG, "queue:")
//		for (it in queue) {
//			Log.v(TAG, "  $it")
//		}
		if (advertisedItems.isNotEmpty()) {
			Log.w(TAG, "Items are already being advertised.")
		}
		val firstItem = getNextItemFromQueue() ?: return null

		val payload = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> BroadcastItemListPacket()
			else -> BroadcastSingleItemPacket()
		}
		val type = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> CommandBroadcastType.MULTI_SWITCH
			CommandBroadcastItemType.SET_TIME -> CommandBroadcastType.SET_TIME
			CommandBroadcastItemType.SUN_TIME -> CommandBroadcastType.SUN_TIME
		}
		val validationTimestamp = when (firstItem.validationTimestamp) {
			null -> Conversion.toUint32(BluenetProtocol.CAFEBABE) // TODO: use time from crownstones
			else -> firstItem.validationTimestamp
		}
		val packet = CommandBroadcastPacket(validationTimestamp, firstItem.sphereId, type, payload)
		payload.add(firstItem.payload)
		advertisedItems.add(firstItem)
		while (!payload.isFull()) {
			val item = getNextItemFromQueue(firstItem)
			if (item == null) {
				break
			}
			payload.add(item.payload)
			advertisedItems.add(item)
		}
		for (it in advertisedItems) {
			Log.d(TAG, "added item $it")
			it.startedAdvertising()
		}
		return packet
	}

	/**
	 * Get and remove next item from queue.
	 */
	private fun getNextItemFromQueue(): CommandBroadcastItem? {
		if (queue.isEmpty()) {
			return null
		}
		return queue.removeFirst()
	}

	/**
	 * Get and remove next item from queue with similar type and sphere id.
	 */
	private fun getNextItemFromQueue(firstItem: CommandBroadcastItem): CommandBroadcastItem? {
		if (queue.isEmpty()) {
			return null
		}
		var iter = queue.iterator()
		while (iter.hasNext()) {
			val it = iter.next()
			if (it.isDone()) {
				Log.w(TAG, "done item in queue: $it")
				iter.remove()
				continue
			}
			if (it.type == firstItem.type && it.sphereId == firstItem.sphereId) {
				iter.remove()
				return it
			}
		}
		return null
	}

	/**
	 * Put item back in queue.
	 */
	private fun putItemBackInQueue(item: CommandBroadcastItem) {
		if (!item.isDone()) {
			// Put item at the back of the queue.
			queue.addLast(item)
		}
	}

	/**
	 * Removes done items from queue.
	 */
	private fun cleanupQueue() {
//		queue.removeIf { it.isDone() } // Requires min API 24
		var iter = queue.iterator()
		while (iter.hasNext()) {
			if (iter.next().isDone()) {
				iter.remove()
			}
		}
//		advertisedItems.removeIf({ it.isDone() }) // Requires min API 24
		iter = advertisedItems.iterator()
		while (iter.hasNext()) {
			if (iter.next().isDone()) {
				iter.remove()
			}
		}
	}

//	/**
//	 * Get encoded rssi offset, a 4 bit uint.
//	 */
//	private fun getEncodedRssiOffset(offset: Int): Uint8 {
//		var encodedOffset = offset / 2 + 8
//		encodedOffset = when {
//			encodedOffset < 0 -> 0
//			encodedOffset > 15 -> 15
//			else -> encodedOffset
//		}
//		return Conversion.toUint8(encodedOffset)
//	}
}