package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.util.*

class CommandBroadcastQueue(state: SphereStateMap, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val encryptionManager = encryptionManager
	private val libState = state
	private val queue = LinkedList<CommandBroadcastItem>()

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
	}

	@Synchronized
	fun hasNext(): Boolean {
		return queue.isNotEmpty()
	}

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
		val encryptedCommandBroadcast = Encryption.encryptCtr(commandBroadcastBytes, 0, 0, commandBroadcastHeader, keyAccessLevel.key) ?: return null

		val advertiseBuilder = AdvertiseData.Builder()
		val commandBroadcastUuid = Conversion.bytesToUuid(encryptedCommandBroadcast)
		advertiseBuilder.addServiceUuid(ParcelUuid(commandBroadcastUuid))
		for (i in 0 until 4) {
			val uuid = Conversion.bytesToUuid2(commandBroadcastHeader, i*2) ?: return null
			advertiseBuilder.addServiceUuid(ParcelUuid(uuid))
		}
		return advertiseBuilder.build()
	}

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

	private fun getBackgroundBroadcast(sphereId: SphereId, sphereState: SphereState): ByteArray? {
		val locationId = sphereState.locationId
		val profileId = sphereState.profileId
		val rssiOffset = getRssiOffset(sphereState.rssiOffset)
		val tapToToggleEnabled = sphereState.tapToToggleEnabled

		val backgroundBroadcast = BackgroundBroadcastPayloadPacket(0, locationId, profileId, rssiOffset, tapToToggleEnabled)
		val backgroundBroadcastArr = backgroundBroadcast.getArray() ?: return null
		return encryptionManager.encryptRC5(sphereId, backgroundBroadcastArr)
	}

	private fun getCommandBroadcastHeader(sphereId: SphereId, sphereState: SphereState, keyAccessLevel: KeyAccessLevelPair): ByteArray? {
		val sphereShortId = sphereState.settings.sphereShortId
		val encryptedBackgroundBroadcastPayload = getBackgroundBroadcast(sphereId, sphereState) ?: return null
		val commandBroadcastHeader = CommandBroadcastHeaderPacket(0, sphereShortId, keyAccessLevel.accessLevel.num, encryptedBackgroundBroadcastPayload)
		return commandBroadcastHeader.getArray()
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