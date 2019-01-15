/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import android.util.Log
import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap


class EncryptionManager {
	private val TAG = this.javaClass.simpleName

	private lateinit var keys: Keys
	private var sessionData: SessionData? = null
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

	@Synchronized fun parseSessionData(address: DeviceAddress, data: ByteArray, isEncrypted: Boolean): Promise<Unit, Exception> {
		val sessionData = if (isEncrypted) {
			val key = getKeySetFromAddress(address)?.guestKeyBytes
			if (key == null) {
				Log.w(TAG, "No key")
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

	@Synchronized fun parseSessionKey(address: DeviceAddress, data: ByteArray): Promise<Unit, Exception> {
		val sessionData = this.sessionData
		if (sessionData == null) {
			return Promise.ofFail(Errors.SessionDataMissing())
		}
		sessionData.tempKey = data
		Log.i(TAG, "session key set")
		return Promise.ofSuccess(Unit)
	}

	@Synchronized fun encrypt(address: DeviceAddress, data: ByteArray, accessLevel: AccessLevel): ByteArray? {
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
					Log.w(TAG, "No key")
					return null
				}
				return Encryption.encryptCtr(data, sessionData.sessionNonce, sessionData.validationKey, keyAccessLevel.key, keyAccessLevel.accessLevel.num)
			}
		}
	}

	// TODO: use throw instead of promise?
	@Synchronized fun decryptPromise(address: DeviceAddress, data: ByteArray): Promise<ByteArray, Exception> {
		val sessionData = this.sessionData
		if (sessionData == null) {
			return Promise.ofFail(Errors.SessionDataMissing())
		}
		val setupKey = sessionData.tempKey

		val keys = if (setupKey != null) {
			Log.i(TAG, "Use setup key")
//			KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
			KeySet(setupKey, setupKey, setupKey, setupKey)
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
	@Synchronized fun decrypt(address: DeviceAddress, data: ByteArray): ByteArray? {
		val sessionData = this.sessionData
		if (sessionData == null) {
			Log.e(TAG, Errors.SessionDataMissing().message)
			return null
		}
		val setupKey = sessionData.tempKey

		val keys = if (setupKey != null) {
			Log.i(TAG, "Use setup key")
//			KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
			KeySet(setupKey, setupKey, setupKey, setupKey)
		}
		else {
			getKeySetFromAddress(address)
		}
		if (keys == null) {
			Log.e(TAG, Errors.EncryptionKeyMissing().message)
			return null
		}
		val decryptedData = Encryption.decryptCtr(data, sessionData.sessionNonce, sessionData.validationKey, keys)
		if (decryptedData == null) {
			Log.e(TAG, Errors.Encryption().message)
			return null
		}
		return decryptedData
	}
}