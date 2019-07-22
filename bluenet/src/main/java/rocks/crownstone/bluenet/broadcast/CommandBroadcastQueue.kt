package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.encryption.EncryptionManager
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
 * Selects which items to broadcast next and build an AdvertiseData packet to be advertised.
 * Each item can have a promise that will be resolved when it has been advertised long enough.
 */
class CommandBroadcastQueue(state: SphereStateMap, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val encryptionManager = encryptionManager
	private val libState = state
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
	 * Get next advertisement data, made from items in queue.
	 *
	 * Should not be called when items are already being broadcasted.
	 */
	@Synchronized
	fun getNextAdvertisement(): AdvertiseData? {
		Log.d(TAG, "getNextAdvertisement")
		if (advertisedItems.isNotEmpty()) {
			Log.w(TAG, "Items are already being advertised.")
		}
		val advertiseUuids = getNextAdvertisementUuids() ?: return null
		val advertiseBuilder = AdvertiseData.Builder()
		for (uuid in advertiseUuids) {
			advertiseBuilder.addServiceUuid(ParcelUuid(uuid))
		}
		return advertiseBuilder.build()
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
	 * Get next service data uuids to advertise.
	 */
	internal fun getNextAdvertisementUuids(): List<UUID>? {
		val commandBroadcast = getNextCommandBroadcastPacket()
		if (commandBroadcast == null) {
			return null
		}
		val sphereId = commandBroadcast.sphereId
		val keySet = encryptionManager.getKeySet(sphereId)
		val keyAccessLevel = keySet?.getHighestKey()
		if (keyAccessLevel == null) {
			Log.w(TAG, "Missing key for sphere $sphereId")
			return null
		}

		val sphereState = libState[sphereId]
		if (sphereState == null) {
			Log.w(TAG, "Missing state for sphere $sphereId")
			return null
		}

		val commandBroadcastHeader = getCommandBroadcastHeader(sphereId, sphereState, keyAccessLevel) ?: return null
		val commandBroadcastBytes = commandBroadcast.getArray() ?: return null
		Log.v(TAG, "commandBroadcastHeader: ${Conversion.bytesToString(commandBroadcastHeader)}")
		Log.v(TAG, "commandBroadcast: commandBroadcast = ${Conversion.bytesToString(commandBroadcastBytes)}")
		val encryptedCommandBroadcast = Encryption.encryptCtr(commandBroadcastBytes, 0, 0, commandBroadcastHeader, keyAccessLevel.key) ?: return null
		Log.v(TAG, "encryptedCommandBroadcast: ${Conversion.bytesToString(encryptedCommandBroadcast)}")

		val advertiseUuids = ArrayList<UUID>()
		val commandBroadcastUuid = Conversion.bytesToUuid(encryptedCommandBroadcast) ?: return null
		advertiseUuids.add(commandBroadcastUuid)

		for (i in 0 until 4) {
			val uuid = Conversion.bytesToUuid2(commandBroadcastHeader, i*2) ?: return null
			advertiseUuids.add(uuid)
		}
		return advertiseUuids
	}

	/**
	 * Get next command broadcast packet, made from items in queue.
	 */
	internal fun getNextCommandBroadcastPacket(): CommandBroadcastPacket? {
		Log.d(TAG, "getNextCommandBroadcastPacket")
//		Log.v(TAG, "queue:")
//		for (it in queue) {
//			Log.v(TAG, "  $it")
//		}
		val firstItem = getNextItemFromQueue() ?: return null

		val payload = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> BroadcastItemListPacket()
			else -> BroadcastSingleItemPacket()
		}
		val type = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> CommandBroadcastType.MULTI_SWITCH
			CommandBroadcastItemType.SET_TIME -> CommandBroadcastType.SET_TIME
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
	 * Get a command broadcast header.
	 */
	internal fun getCommandBroadcastHeader(sphereId: SphereId, sphereState: SphereState, keyAccessLevel: KeyAccessLevelPair): ByteArray? {
		val sphereShortId = sphereState.settings.sphereShortId
		val deviceToken = sphereState.settings.deviceToken
		val encryptedBackgroundBroadcastPayload = getBackgroundPayload(sphereId, sphereState) ?: return null
		Log.v(TAG, "encryptedBackgroundBroadcastPayload: ${Conversion.bytesToString(encryptedBackgroundBroadcastPayload)}")
		val commandBroadcastHeader = CommandBroadcastHeaderPacket(0, sphereShortId, keyAccessLevel.accessLevel.num, deviceToken, encryptedBackgroundBroadcastPayload)
		Log.v(TAG, "commandBroadcastHeader: $commandBroadcastHeader")
		return commandBroadcastHeader.getArray()
	}

	/**
	 * Get an encrypted background broadcast payload packet.
	 */
	internal fun getBackgroundPayload(sphereId: SphereId, sphereState: SphereState): ByteArray? {
		val locationId = sphereState.locationId
		val profileId = sphereState.profileId
		val rssiOffset = getRssiOffset(sphereState.rssiOffset)
		val tapToToggleEnabled = sphereState.tapToToggleEnabled
		val counter = sphereState.commandCount

		val backgroundPayload = CommandBroadcastRC5Packet(counter, locationId, profileId, rssiOffset, tapToToggleEnabled)
		val backgroundPayloadArr = backgroundPayload.getArray() ?: return null
		Log.v(TAG, "backgroundBroadcast: $backgroundPayload = ${Conversion.bytesToString(backgroundPayloadArr)}")
		return encryptionManager.encryptRC5(sphereId, backgroundPayloadArr)
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

	/**
	 * Get "compressed" rssi offset, a 4 bit uint.
	 */
	private fun getRssiOffset(offset: Int): Uint8 {
		return Conversion.toUint8(offset / 2 + 8)
	}
}