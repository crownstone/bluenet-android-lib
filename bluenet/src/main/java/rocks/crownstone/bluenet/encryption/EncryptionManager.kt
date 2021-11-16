/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class EncryptionManager(evtBus: EventBus, state: BluenetState) {
	private val TAG = this.javaClass.simpleName

	private val eventBus = evtBus
	private val libState = state
//	private var keys: Keys? = null
	private val uuids = HashMap<UUID, SphereId>()
	private val addresses = HashMap<DeviceAddress, SphereId>() // Cached results. TODO: this grows over time!

	/**
	 * Map with list of RC5 subkeys for each sphere.
	 */
	private val rc5subKeysMap = HashMap<SphereId, List<Uint16>?>()

	init {
		eventBus.subscribe(BluenetEvent.SPHERE_SETTINGS_UPDATED, { data: Any? -> onSettingsUpdate() })
		setKeys()
	}


	@Synchronized
	private fun onSettingsUpdate() {
		Log.i(TAG, "onSettingsUpdate")
		setKeys()
	}

	private fun setKeys() {
		Log.i(TAG, "setKeys")
		uuids.clear()
		addresses.clear()
		for ((sphereId, state) in libState.sphereState) {
			uuids.put(state.settings.ibeaconUuid, sphereId)
			rc5subKeysMap.put(sphereId, RC5.expandKey(state.settings.keySet.localizationKeyBytes))
			Log.d(TAG, "sphereId=$sphereId ibeaconUuid=${state.settings.ibeaconUuid} keys=${state.settings.keySet}")
		}
	}

//	@Synchronized
//	fun setKeys(keys: Keys) {
//		Log.i(TAG, "setKeys")
//		this.keys = keys
//		uuids.clear()
//		addresses.clear()
//		for (entry in keys.entries) {
//			val sphereId = entry.key
//			val ibeaconUuid = entry.value.ibeaconUuid
//			Log.i(TAG, "sphereId=$sphereId ibeaconUuid=$ibeaconUuid keys=${entry.value.keySet}")
//			uuids.put(ibeaconUuid, sphereId)
//		}
//	}
//
//	// Similar to setting an empty list of keys
//	@Synchronized
//	fun clearKeys() {
//		Log.i(TAG, "clearKeys")
//		keys = null
//		uuids.clear()
//		addresses.clear()
//	}

	@Synchronized
	fun getKeySet(id: SphereId?): KeySet? {
		if (id == null) {
			return null
		}
//		if (libState.get(id)?.settings == null) {
//			Log.w(TAG, "no settings for sphereId $id")
//		}
		return libState.sphereState.get(id)?.settings?.keySet
//		return keys?.get(id)?.keySet
	}

	@Synchronized
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

	@Synchronized
	fun getKeySetFromAddress(address: DeviceAddress): KeySet? {
//		if (!addresses.containsKey(address)) {
//			Log.v(TAG, "no sphere id for $address")
//		}
		return getKeySet(addresses.get(address))
	}

	@Synchronized
	fun getKeySet(device: ScannedDevice): KeySet? {
		// Fall back to cached result
		return getKeySetFromAddress(device.address)
	}

	@Synchronized
	fun getSphereIdFromAddress(address: DeviceAddress): SphereId? {
		return addresses.get(address)
	}

	@Synchronized
	fun getSphereId(device: ScannedDevice): SphereId? {
		// Fall back to cached result
		return getSphereIdFromAddress(device.address)
	}

	private fun cacheSphereId(address: DeviceAddress, sphereId: SphereId) {
		// Only add when sphereId changed, as calculating the original address is a bit expensive.
		if (addresses.get(address) == sphereId) {
			return
		}
		addresses.put(address, sphereId)
		Log.d(TAG, "Cache sphereId for $address to $sphereId")
		// The ibeacon MAC address is the original MAC address with first byte increased by 1.
		// So add the original address as well.
		val addressBytes = Conversion.addressToBytes(address)
		addressBytes[0] = (addressBytes[0] - 1).toByte()
		val originalAddress = Conversion.bytesToAddress(addressBytes)
		addresses.put(originalAddress, sphereId)
		Log.d(TAG, "Cache sphereId for $originalAddress to $sphereId")
	}

	/**
	 * Determine sphere id based on iBeacon uuid and caches the result.
	 */
	@Synchronized
	fun cacheSphereId(device: ScannedDevice) {
		val uuid = device.ibeaconData?.uuid
		if (uuid != null) {
			val sphereId = uuids.get(uuid)
			Log.v(TAG, "no sphere id for ibeacon $uuid")
			if (sphereId != null) {
				cacheSphereId(device.address, sphereId)
			}
		}
	}

	@Synchronized
	fun encryptRC5(sphereId: SphereId, data: ByteArray): ByteArray? {
		if (data.size != 4) {
			Log.w(TAG, "Invalid size: ${data.size}")
			return null
		}
		val subKeys = rc5subKeysMap.get(sphereId)
		if (subKeys == null) {
			Log.w(TAG, "Missing sub keys for sphereId=$sphereId")
			return null
		}
		val dataUint: Uint32 = Conversion.byteArrayTo(data)
		val encryptedUint = RC5.encrypt(dataUint, subKeys) ?: return null
		return Conversion.uint32ToByteArray(encryptedUint)
	}

	@Synchronized
	fun decryptRC5(sphereId: SphereId, data: ByteArray): ByteArray? {
		if (data.size != 4) {
			Log.w(TAG, "Invalid size: ${data.size}")
			return null
		}
		val subKeys = rc5subKeysMap.get(sphereId)
		if (subKeys == null) {
			Log.w(TAG, "Missing sub keys for sphereId=$sphereId")
			return null
		}
		val dataUint: Uint32 = Conversion.byteArrayTo(data)
		val decryptedUint = RC5.decrypt(dataUint, subKeys) ?: return null
		return Conversion.uint32ToByteArray(decryptedUint)
	}
}

