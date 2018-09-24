package rocks.crownstone.bluenet.encryption

import android.util.Log
import rocks.crownstone.bluenet.BluenetProtocol
import rocks.crownstone.bluenet.BluenetProtocol.SESSION_NONCE_LENGTH
import rocks.crownstone.bluenet.BluenetProtocol.VALIDATION_KEY_LENGTH
import rocks.crownstone.bluenet.util.Conversion

data class SessionData(val sessionNonce: ByteArray, val validationKey: ByteArray)

object SessionDataParser {
	private val TAG = this::class.java.canonicalName

	fun getSessionData(decryptedData: ByteArray, wasEncrypted: Boolean): SessionData? {
		if (decryptedData == null) {
			return null
		}
		// When the data was encrypted, the first 4 bytes should be CAFEBABE, to check if encryption succeeded.
		if (wasEncrypted) {
			if (decryptedData.size < VALIDATION_KEY_LENGTH + SESSION_NONCE_LENGTH) {
				Log.e(TAG, "invalid session data length: " + Conversion.bytesToString(decryptedData))
				return null
			}
			// Bytes 0-3 (validation key) should be CAFEBABE
			if (Conversion.byteArrayToInt(decryptedData) != BluenetProtocol.CAFEBABE) {
				Log.e(TAG, "validation failed: " + Conversion.bytesToString(decryptedData))
				return null
			}
			return getSessionData(decryptedData, VALIDATION_KEY_LENGTH)
		} else {
			if (decryptedData.size < SESSION_NONCE_LENGTH) {
				Log.e(TAG, "invalid session data length: " + Conversion.bytesToString(decryptedData))
				return null
			}
			return getSessionData(decryptedData, 0)
		}
	}

	private fun getSessionData(data: ByteArray, offset: Int): SessionData {
		val sessionNonce = ByteArray(SESSION_NONCE_LENGTH)
		val validationKey = ByteArray(VALIDATION_KEY_LENGTH)

		// Copy bytes 4-8 to sessionNonce, copy bytes 4-7 to validationKey
		System.arraycopy(data, offset, sessionNonce, 0, SESSION_NONCE_LENGTH)
		System.arraycopy(data, offset, validationKey, 0, VALIDATION_KEY_LENGTH)

		return SessionData(sessionNonce, validationKey)
	}
}