package rocks.crownstone.bluenet.broadcast

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.packets.broadcast.CommandBroadcastHeaderPacket
import rocks.crownstone.bluenet.packets.broadcast.CommandBroadcastPacket
import rocks.crownstone.bluenet.packets.broadcast.CommandBroadcastRC5Packet
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.util.*

class BroadcastPacketBuilder(state: BluenetState, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val encryptionManager = encryptionManager
	private val libState = state

	fun getCommandBroadcastAdvertisement(sphereId: SphereId, accessLevel: AccessLevel, commandBroadcast: CommandBroadcastPacket): AdvertiseData? {
		val advertiseUuids = getCommandBroadcastServiceUuids(sphereId, accessLevel, commandBroadcast) ?: return null
		val advertiseBuilder = AdvertiseData.Builder()
		for (uuid in advertiseUuids) {
			advertiseBuilder.addServiceUuid(ParcelUuid(uuid))
		}
		return advertiseBuilder.build()
	}

	fun getCommandBroadcastServiceUuids(sphereId: SphereId, accessLevel: AccessLevel, commandBroadcast: CommandBroadcastPacket): List<UUID>? {
		val keySet = encryptionManager.getKeySet(sphereId) ?: return null
		val keyAccessLevel = if (accessLevel == AccessLevel.HIGHEST_AVAILABLE) {
			keySet.getHighestKey()
		}
		else {
			val key = keySet.getKey(accessLevel) ?: return null
			KeyAccessLevelPair(key, accessLevel)
		}
		if (keyAccessLevel == null) {
			return null
		}

		val commandBroadcastHeader = getCommandBroadcastHeader(sphereId, keyAccessLevel.accessLevel) ?: return null
		Log.v(TAG, "commandBroadcastHeader: ${Conversion.bytesToString(commandBroadcastHeader)}")
		val encryptedCommandBroadcast = encryptCommandBroadcastPacket(keyAccessLevel.key, commandBroadcast, commandBroadcastHeader) ?: return null

		val advertiseUuids = ArrayList<UUID>()
		val commandBroadcastUuid = Conversion.bytesToUuid(encryptedCommandBroadcast) ?: return null
		advertiseUuids.add(commandBroadcastUuid)
		for (i in 0 until 4) {
			val uuid = Conversion.bytesToUuid2(commandBroadcastHeader, i*2) ?: return null
			advertiseUuids.add(uuid)
		}
		return advertiseUuids
	}

	fun encryptCommandBroadcastPacket(key: ByteArray, commandBroadcast: CommandBroadcastPacket, nonce: ByteArray): ByteArray? {

		val commandBroadcastBytes = commandBroadcast.getArray() ?: return null
		Log.v(TAG, "commandBroadcast: ${Conversion.bytesToString(commandBroadcastBytes)}")
		val encryptedCommandBroadcast = Encryption.encryptCtr(commandBroadcastBytes, 0, 0, nonce, key) ?: return null
		Log.v(TAG, "encryptedCommandBroadcast: ${Conversion.bytesToString(encryptedCommandBroadcast)}")
		return encryptedCommandBroadcast
	}

	/**
	 * Get a command broadcast header.
	 */
	fun getCommandBroadcastHeader(sphereId: SphereId, accessLevel: AccessLevel): ByteArray? {
		val sphereState = libState.sphereState[sphereId]
		if (sphereState == null) {
			Log.w(TAG, "Missing state for sphere $sphereId")
			return null
		}



		val sphereShortId = sphereState.settings.sphereShortId
		val deviceToken = sphereState.settings.deviceToken
		val encryptedBackgroundBroadcastPayload = getBackgroundPacket(sphereId) ?: return null
		Log.v(TAG, "encryptedCommandBroadcastRC5Packet: ${Conversion.bytesToString(encryptedBackgroundBroadcastPayload)}")
		val commandBroadcastHeader = CommandBroadcastHeaderPacket(0, sphereShortId, accessLevel.num, deviceToken, encryptedBackgroundBroadcastPayload)
		Log.v(TAG, "commandBroadcastHeader: $commandBroadcastHeader")
		return commandBroadcastHeader.getArray()
	}

	/**
	 * Get an encrypted background broadcast payload packet.
	 */
	fun getBackgroundPacket(sphereId: SphereId): ByteArray? {
		val sphereState = libState.sphereState[sphereId]
		if (sphereState == null) {
			Log.w(TAG, "Missing state for sphere $sphereId")
			return null
		}

		val locationId = sphereState.locationId
		val profileId = sphereState.profileId
		val rssiOffset = getEncodedRssiOffset(sphereState.rssiOffset)
		val tapToToggleEnabled = sphereState.tapToToggleEnabled
		val counter = sphereState.commandCount

		val rc5Payload = CommandBroadcastRC5Packet(counter, locationId, profileId, rssiOffset, tapToToggleEnabled)
		val rc5PayloadArr = rc5Payload.getArray() ?: return null
		Log.v(TAG, "backgroundBroadcast: $rc5Payload = ${Conversion.bytesToString(rc5PayloadArr)}")
		return encryptionManager.encryptRC5(sphereId, rc5PayloadArr)
	}

	/**
	 * Get encoded rssi offset, a 4 bit uint.
	 */
	private fun getEncodedRssiOffset(offset: Int): Uint8 {
		var encodedOffset = offset / 2 + 8
		encodedOffset = when {
			encodedOffset < 0 -> 0
			encodedOffset > 15 -> 15
			else -> encodedOffset
		}
		return Conversion.toUint8(encodedOffset)
	}
}
