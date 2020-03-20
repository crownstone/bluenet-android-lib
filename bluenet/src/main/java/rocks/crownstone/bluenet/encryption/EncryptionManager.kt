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
	private var sessionData: SessionData? = null
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
	fun parseSessionData(address: DeviceAddress, data: ByteArray, isEncrypted: Boolean): Promise<Unit, Exception> {
		this.sessionData = null // Make sure it's null when parsing fails.
		val sessionData = if (isEncrypted) {
			val key = getKeySetFromAddress(address)?.guestKeyBytes
			if (key == null) {
				Log.w(TAG, "No key for $address")
				return Promise.ofFail(Errors.EncryptionKeyMissing())
			}
			val decryptedData = Encryption.decryptEcb(data, key)
			if (decryptedData == null) {
				return Promise.ofFail(Errors.Encryption())
			}
			SessionDataParser.getSessionData(decryptedData, isEncrypted)
		}
		else {
			SessionDataParser.getSessionData(data, isEncrypted)
		}

		if (sessionData == null) {
			return Promise.ofFail(Errors.Parse("failed to parse session data"))
		}
		this.sessionData = sessionData
		Log.i(TAG, "session data set: $sessionData")
		return Promise.ofSuccess(Unit)
	}

	@Synchronized
	fun parseSessionKey(address: DeviceAddress, data: ByteArray): Promise<Unit, Exception> {
		val sessionData = this.sessionData
		if (sessionData == null) {
			return Promise.ofFail(Errors.SessionDataMissing())
		}
		sessionData.tempKey = data
		Log.i(TAG, "session key set")
		return Promise.ofSuccess(Unit)
	}

	@Synchronized
	fun encrypt(address: DeviceAddress, data: ByteArray, accessLevel: AccessLevel): ByteArray? {
		when(accessLevel) {
			AccessLevel.ENCRYPTION_DISABLED -> return data
			AccessLevel.UNKNOWN -> return null
			else -> {
				val sessionData = this.sessionData
				if (sessionData == null) {
					Log.w(TAG, "No session data")
					return null
				}
				val setupKey = sessionData.tempKey
				val keyAccessLevel = if ((accessLevel == AccessLevel.SETUP || accessLevel == AccessLevel.HIGHEST_AVAILABLE) && setupKey != null) {
					// Use setup key
					KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
				}
				else {
					// Just use highest available key
					getKeySetFromAddress(address)?.getHighestKey()
				}
				if (keyAccessLevel == null) {
					Log.w(TAG, "No key for $address")
					return null
				}
				return Encryption.encryptCtr(data, sessionData.sessionNonce, sessionData.validationKey, keyAccessLevel.key, keyAccessLevel.accessLevel.num)
			}
		}
	}

	// TODO: use throw instead of promise?
	@Synchronized
	fun decryptPromise(address: DeviceAddress, data: ByteArray, accessLevel: AccessLevel? = null): Promise<ByteArray, Exception> {
		val sessionData = this.sessionData
		if (sessionData == null) {
			return Promise.ofFail(Errors.SessionDataMissing())
		}

		// TODO: handle more cases of provided accessLevel
		if (accessLevel == AccessLevel.ENCRYPTION_DISABLED) {
			Log.d(TAG, "Don't decrypt")
			return Promise.ofSuccess(data)
		}
		val setupKey = sessionData.tempKey
		val keys = if (setupKey != null) {
			Log.i(TAG, "Use setup key")
//			KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
			KeySet(setupKey, setupKey, setupKey, setupKey, setupKey, setupKey)
		}
		else {
			getKeySetFromAddress(address)
		}
		if (keys == null) {
			return Promise.ofFail(Errors.EncryptionKeyMissing())
		}
		val decryptedData = Encryption.decryptCtr(data, sessionData.sessionNonce, sessionData.validationKey, keys)
		if (decryptedData == null) {
			return Promise.ofFail(Errors.Encryption())
		}
		return Promise.ofSuccess(decryptedData)
	}

	// TODO: use throw instead
	@Synchronized
	fun decrypt(address: DeviceAddress, data: ByteArray, accessLevel: AccessLevel? = null): ByteArray? {
		val sessionData = this.sessionData
		if (sessionData == null) {
			Log.e(TAG, Errors.SessionDataMissing().message)
			return null
		}
		// TODO: handle more cases of provided accessLevel
		if (accessLevel == AccessLevel.ENCRYPTION_DISABLED) {
			Log.d(TAG, "Don't decrypt")
			return data
		}
		val setupKey = sessionData.tempKey
		val keys = if (setupKey != null) {
			Log.i(TAG, "Use setup key")
//			KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
			KeySet(setupKey, setupKey, setupKey, setupKey, setupKey, setupKey)
		}
		else {
			getKeySetFromAddress(address)
		}
		if (keys == null) {
			Log.w(TAG, "No key for $address")
			return null
		}
		val decryptedData = Encryption.decryptCtr(data, sessionData.sessionNonce, sessionData.validationKey, keys)
		if (decryptedData == null) {
			Log.e(TAG, Errors.Encryption().message)
			return null
		}
		return decryptedData
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

