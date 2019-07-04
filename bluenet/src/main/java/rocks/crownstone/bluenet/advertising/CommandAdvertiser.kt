/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.advertising

import android.bluetooth.le.AdvertiseData
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.advertising.*
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.SphereState
import rocks.crownstone.bluenet.structs.SphereStateMap
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.util.*
import kotlin.collections.ArrayList

class CommandAdvertiser(evtBus: EventBus, state: SphereStateMap, bleCore: BleCore, encryptionManager: EncryptionManager, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val libState = state
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	private val handler = Handler(looper)
	private val queue = LinkedList<CommandAdvertiserItem>()
	private var advertising = false

	/**
	 * Add an item to the queue.
	 */
	@Synchronized
	fun add(item: CommandAdvertiserItem) {
		// Remove any items with same sphere, type, and stone id.
		for (it in queue) {
			if (it.type == item.type && it.stoneId == item.stoneId && it.sphereId == it.sphereId) {
				queue.remove(it)
				break
			}
		}
		// Put item in front of the queue.
		queue.addFirst(item)
		advertiseNext()
	}

	@Synchronized
	private fun advertiseNext() {
		Log.d(TAG, "advertiseNext")
		if (advertising) {
			return
		}
		val commandAdvertisement = getNextCommandAdvertisementPacket()
		if (commandAdvertisement == null) {
			return
		}
		val sphereId = commandAdvertisement.sphereId
		val keySet = encryptionManager.getKeySet(sphereId)
		val keyAccess = keySet?.getHighestKey()
		if (keyAccess == null) {
			Log.w(TAG, "Missing key for sphere $sphereId")
			advertiseNext()
			return
		}

		val sphereState = libState[sphereId]
		if (sphereState == null) {
			Log.w(TAG, "Missing state for sphere $sphereId")
			advertiseNext()
			return
		}
		val locationId = sphereState.locationId
		val profileId = sphereState.profileId
		val rssiOffset = getRssiOffset(sphereState.rssiOffset)
		val tapToToggleEnabled = sphereState.tapToToggleEnabled
		val sphereShortId = sphereState.settings.sphereShortId

		val backgroundAdvertisement = BackgroundAdvertisementPayloadPacket(0, locationId, profileId, rssiOffset, tapToToggleEnabled)
		val commandAdvertisementHeader = CommandAdvertisementHeaderPacket(0, sphereShortId, keyAccess.accessLevel.num, backgroundAdvertisement)

		val commandAdvertisementBytes = commandAdvertisement.getArray() ?: return
		val backgroundAdvertisementBytes = backgroundAdvertisement.getArray() ?: return
		// TODO: put backgroundAdvertisement in 4 16bit service UUIDs.
		val commandAdvertiesmentUuid = Conversion.bytesToUuid(commandAdvertisementBytes)
		val advertiseData = AdvertiseData.Builder()
				.addServiceUuid(ParcelUuid(commandAdvertiesmentUuid))
				.build()
		advertising = true
		bleCore.advertise(advertiseData, BluenetConfig.COMMAND_ADVERTISER_INTERVAL_MS)
		handler.postDelayed(onAdvertisementDoneRunnable, BluenetConfig.COMMAND_ADVERTISER_INTERVAL_MS.toLong())
	}

	private val onAdvertisementDoneRunnable = Runnable {
		onAdvertisementDone()
	}

	@Synchronized
	private fun onAdvertisementDone() {
		Log.i(TAG, "onAdvertisementDone")
		advertising = false
		advertiseNext()
	}

	@Synchronized
	private fun getNextCommandAdvertisementPacket(): CommandAdvertisementPacket? {
		if (queue.isEmpty()) {
			return null
		}
		val validationTimestamp = Conversion.toUint32(BluenetProtocol.CAFEBABE)
		val firstItem = getNextItemFromQueue() ?: return null

		val payload = when (firstItem.type) {
			CommandAdvertiserItemType.SWITCH -> AdvertiseItemListPacket()
			else -> AdvertiseSingleItemPacket()
		}
		val type = when (firstItem.type) {
			CommandAdvertiserItemType.SWITCH -> CommandAdvertisementType.MULTI_SWITCH
			CommandAdvertiserItemType.SET_TIME -> CommandAdvertisementType.SET_TIME
		}
		val packet = CommandAdvertisementPacket(validationTimestamp, firstItem.sphereId, type, payload)
		val addedItems = ArrayList<CommandAdvertiserItem>()
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
	private fun getNextItemFromQueue(): CommandAdvertiserItem? {
		if (queue.isEmpty()) {
			return null
		}
		return queue.removeFirst()
	}

	/**
	 * Get and remove next item from queue with similar type and sphere id.
	 */
	@Synchronized
	private fun getNextItemFromQueue(firstItem: CommandAdvertiserItem): CommandAdvertiserItem? {
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
	private fun putItemBackInQueue(item: CommandAdvertiserItem) {
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