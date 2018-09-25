package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.DeviceAddress
import rocks.crownstone.bluenet.IbeaconData
import rocks.crownstone.bluenet.SphereId
import rocks.crownstone.bluenet.scanparsing.BleDevice
import java.util.*
import kotlin.collections.HashMap


class EncryptionManager() {
	private val TAG = this::class.java.canonicalName

	private val keys = HashMap<SphereId, KeySet>()
	private val sessionData: SessionData? = null
	private val uuids = HashMap<UUID, SphereId>()
	private val addresses = HashMap<DeviceAddress, SphereId>()

	fun getKeySet(id: SphereId?): KeySet? {
		if (id == null) {
			return null
		}
		return keys.get(id)
	}

	fun getKeySet(ibeaconData: IbeaconData?): KeySet? {
		val uuid = ibeaconData?.uuid
		if (uuid != null) {
			val sphereId = uuids.get(uuid)
			if (sphereId != null) {
				return getKeySet(sphereId)
			}
		}
		return null
	}

	fun getKeySetFromAddress(address: DeviceAddress): KeySet? {
//		if (address == null) {
//			return null
//		}
		return getKeySet(addresses.get(address))
	}

	fun getKeySet(device: BleDevice): KeySet? {
		val uuid = device.ibeaconData?.uuid
		if (uuid != null) {
			val sphereId = uuids.get(uuid)
			if (sphereId != null) {
				addresses.put(device.address, sphereId)
				return getKeySet(sphereId)
			}
		}
		return getKeySetFromAddress(device.address)
	}
}