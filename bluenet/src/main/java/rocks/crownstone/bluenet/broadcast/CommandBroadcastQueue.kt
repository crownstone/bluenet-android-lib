package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.support.annotation.VisibleForTesting
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.util.*
import kotlin.collections.ArrayList

class CommandBroadcastQueue(state: SphereStateMap, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val encryptionManager = encryptionManager
	private val libState = state
	private val queue = LinkedList<CommandBroadcastItem>()
	private val broadcastedItems = ArrayList<CommandBroadcastItem>()

	/**
	 * Add an item to the queue.
	 */
	@Synchronized
	fun add(item: CommandBroadcastItem) {
		Log.d(TAG, "add $item")
		// Remove any items with same sphere, type, and stone id.
		for (it in queue) {
			if (it.sphereId == it.sphereId &&
					it.type == item.type &&
					it.stoneId != null &&
					it.stoneId == item.stoneId
			) {
				it.reject(Errors.Aborted())
				queue.remove(it)
				break
			}
		}
		for (it in broadcastedItems) {
			if (it.sphereId == it.sphereId &&
					it.type == item.type &&
					it.stoneId != null &&
					it.stoneId == item.stoneId
			) {
				it.stoppedBroadcasting(Errors.Aborted())
				broadcastedItems.remove(it)
				break
			}
		}
		// Put item in front of the queue.
		queue.addFirst(item)
	}

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
		Log.d(TAG, "broadcastNext")
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

		Log.v(TAG, "UUIDs:")
		Log.v(TAG, "  ${Conversion.bytesToUuid(encryptedCommandBroadcast)}")
		for (i in 0 until 4) {
			val uuid = Conversion.bytesToUuid2(commandBroadcastHeader, i * 2) ?: return null
			Log.v(TAG, "  $uuid")
		}
		return null

		val advertiseBuilder = AdvertiseData.Builder()
		val commandBroadcastUuid = Conversion.bytesToUuid(encryptedCommandBroadcast)
		advertiseBuilder.addServiceUuid(ParcelUuid(commandBroadcastUuid))
		for (i in 0 until 4) {
			val uuid = Conversion.bytesToUuid2(commandBroadcastHeader, i*2) ?: return null
			advertiseBuilder.addServiceUuid(ParcelUuid(uuid))
		}
		return advertiseBuilder.build()
	}

	/**
	 * To be called when advertisement has been advertised.
	 */
	@Synchronized
	fun advertisementDone(error: java.lang.Exception?) {
		for (it in broadcastedItems) {
			it.stoppedBroadcasting(error)
			putItemBackInQueue(it)
		}
		broadcastedItems.clear()
	}

	/**
	 * Get next command broadcast packet, made from items in queue.
	 */
	private fun getNextCommandBroadcastPacket(): CommandBroadcastPacket? {
		Log.v(TAG, "getNextCommandBroadcastPacket")
//		Log.v(TAG, "queue:")
//		for (it in queue) {
//			Log.v(TAG, "  $it")
//		}
		val firstItem = getNextItemFromQueue() ?: return null
		Log.v(TAG, "firstItem: $firstItem")

		val payload = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> BroadcastItemListPacket()
			else -> BroadcastSingleItemPacket()
		}
		val type = when (firstItem.type) {
			CommandBroadcastItemType.SWITCH -> CommandBroadcastType.MULTI_SWITCH
			CommandBroadcastItemType.SET_TIME -> CommandBroadcastType.SET_TIME
		}
		val validationTimestamp = Conversion.toUint32(BluenetProtocol.CAFEBABE) // TODO: use time from crownstones
		val packet = CommandBroadcastPacket(validationTimestamp, firstItem.sphereId, type, payload)
//		val addedItems = ArrayList<CommandBroadcastItem>()
		payload.add(firstItem.payload)
		broadcastedItems.add(firstItem)
		while (!payload.isFull()) {
			val item = getNextItemFromQueue(firstItem)
			if (item == null) {
				break
			}
			Log.v(TAG, "add item: $item")
			payload.add(item.payload)
			broadcastedItems.add(item)
		}
		for (it in broadcastedItems) {
			it.startedBroadcasting()
//			putItemBackInQueue(it)
		}
		return packet
	}

	/**
	 * Get a command broadcast header
	 */
	private fun getCommandBroadcastHeader(sphereId: SphereId, sphereState: SphereState, keyAccessLevel: KeyAccessLevelPair): ByteArray? {
		val sphereShortId = sphereState.settings.sphereShortId
		val encryptedBackgroundBroadcastPayload = getBackgroundBroadcast(sphereId, sphereState) ?: return null
		Log.v(TAG, "encryptedBackgroundBroadcastPayload: ${Conversion.bytesToString(encryptedBackgroundBroadcastPayload)}")
		val commandBroadcastHeader = CommandBroadcastHeaderPacket(0, sphereShortId, keyAccessLevel.accessLevel.num, encryptedBackgroundBroadcastPayload)
		Log.v(TAG, "commandBroadcastHeader: $commandBroadcastHeader")
		return commandBroadcastHeader.getArray()
	}

	/**
	 * Get an encrypted background broadcast packet
	 */
	private fun getBackgroundBroadcast(sphereId: SphereId, sphereState: SphereState): ByteArray? {
		val locationId = sphereState.locationId
		val profileId = sphereState.profileId
		val rssiOffset = getRssiOffset(sphereState.rssiOffset)
		val tapToToggleEnabled = sphereState.tapToToggleEnabled

		val backgroundBroadcast = BackgroundBroadcastPayloadPacket(0, locationId, profileId, rssiOffset, tapToToggleEnabled)
		val backgroundBroadcastArr = backgroundBroadcast.getArray() ?: return null
		Log.v(TAG, "backgroundBroadcast: $backgroundBroadcast = ${Conversion.bytesToString(backgroundBroadcastArr)}")
		return encryptionManager.encryptRC5(sphereId, backgroundBroadcastArr)
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
//		queue.removeIf { it.isDone() }
		var iter = queue.iterator()
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