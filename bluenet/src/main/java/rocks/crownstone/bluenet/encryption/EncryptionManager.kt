package rocks.crownstone.bluenet.encryption

import android.util.Log
import rocks.crownstone.bluenet.DeviceAddress
import rocks.crownstone.bluenet.IbeaconData
import rocks.crownstone.bluenet.Keys
import rocks.crownstone.bluenet.SphereId
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import java.util.*
import kotlin.collections.HashMap


class EncryptionManager {
	private val TAG = this::class.java.canonicalName

	private lateinit var keys: Keys
	private val sessionData: SessionData? = null
	private val uuids = HashMap<UUID, SphereId>()
	private val addresses = HashMap<DeviceAddress, SphereId>() // Cached results. TODO: this grows over time!

	@Synchronized fun setKeys(keys: Keys) {
		Log.i(TAG, "setKeys")
		this.keys = keys
		uuids.clear()
		addresses.clear()
		for (entry in keys.entries) {
			val sphereId = entry.key
			val ibeaconUuid = entry.value.ibeaconUuid
			Log.i(TAG, "sphereId=$sphereId ibeaconUuid=$ibeaconUuid keys=${entry.value.keySet}")
			uuids.put(ibeaconUuid, sphereId)
		}
	}

	@Synchronized fun getKeySet(id: SphereId?): KeySet? {
		if (id == null) {
			return null
		}
		return keys.get(id)?.keySet
	}

	@Synchronized fun getKeySet(ibeaconData: IbeaconData?): KeySet? {
		val uuid = ibeaconData?.uuid
		if (uuid != null) {
			val sphereId = uuids.get(uuid)
			if (sphereId != null) {
				return getKeySet(sphereId)
			}
		}
		return null
	}

	@Synchronized fun getKeySetFromAddress(address: DeviceAddress): KeySet? {
		return getKeySet(addresses.get(address))
	}

	@Synchronized fun getKeySet(device: ScannedDevice): KeySet? {
		val uuid = device.ibeaconData?.uuid
		if (uuid != null) {
			val sphereId = uuids.get(uuid)
			if (sphereId != null) {
				// Cache result
				addresses.put(device.address, sphereId)
				return getKeySet(sphereId)
			}
		}
		// Fall back to cached result
		return getKeySetFromAddress(device.address)
	}
}