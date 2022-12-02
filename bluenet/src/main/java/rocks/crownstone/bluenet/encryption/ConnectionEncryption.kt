package rocks.crownstone.bluenet.encryption

import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.DeviceAddress
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.structs.KeyAccessLevelPair
import rocks.crownstone.bluenet.util.Log
import java.lang.Exception

/**
 * Class that handles encryption for a single connection.
 * - Parses and keeps up session data.
 * - Encrypt / decrypt data, using the session data.
 */
class ConnectionEncryption(encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val encryptionManager = encryptionManager
	private var sessionData: SessionData? = null
	private var sessionKey: ByteArray? = null

	@Synchronized
	fun clearSessionData() {
		this.sessionData = null
		this.sessionKey = null
	}

	@Synchronized
	fun parseSessionData(address: DeviceAddress, data: ByteArray, isEncrypted: Boolean, setupMode: Boolean, v5: Boolean): Promise<SessionData, Exception> {
		val sessionData =
				if (isEncrypted) {
					val key = if (setupMode) {
						sessionKey
					}
					else {
						encryptionManager.getKeySetFromAddress(address)?.guestKeyBytes
					}
					if (key == null) {
						Log.w(TAG, "No key for $address")
						return Promise.ofFail(Errors.EncryptionKeyMissing())
					}
					val decryptedData = Encryption.decryptEcb(data, key)
					if (decryptedData == null) {
						return Promise.ofFail(Errors.Encryption())
					}
					SessionDataParser.getSessionData(decryptedData, isEncrypted, v5)
				}
				else {
					SessionDataParser.getSessionData(data, isEncrypted, v5)
				}

		if (sessionData == null) {
			return Promise.ofFail(Errors.Parse("failed to parse session data"))
		}
		this.sessionData = sessionData
		Log.i(TAG, "session data set: $sessionData")
		return Promise.ofSuccess(sessionData)
	}

	@Synchronized
	fun parseSessionKey(address: DeviceAddress, data: ByteArray): Promise<Unit, Exception> {
		if (data.size < BluenetProtocol.AES_BLOCK_SIZE) {
			return Promise.ofFail(Errors.SizeWrong())
		}
		this.sessionKey = data
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
				val setupKey = sessionKey
				val keyAccessLevel = if ((accessLevel == AccessLevel.SETUP || accessLevel == AccessLevel.HIGHEST_AVAILABLE) && setupKey != null) {
					// Use setup key
					KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
				}
				else {
					// Just use highest available key
					encryptionManager.getKeySetFromAddress(address)?.getHighestKey()
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
		val setupKey = sessionKey
		val keys = if (setupKey != null) {
			Log.i(TAG, "Use setup key")
//			KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
			KeySet(setupKey, setupKey, setupKey, setupKey, setupKey, setupKey)
		}
		else {
			encryptionManager.getKeySetFromAddress(address)
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
		val setupKey = sessionKey
		val keys = if (setupKey != null) {
			Log.i(TAG, "Use setup key")
//			KeyAccessLevelPair(setupKey, AccessLevel.SETUP)
			KeySet(setupKey, setupKey, setupKey, setupKey, setupKey, setupKey)
		}
		else {
			encryptionManager.getKeySetFromAddress(address)
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
}