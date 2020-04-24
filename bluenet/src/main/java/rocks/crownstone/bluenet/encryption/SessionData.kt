/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.packets.other.SessionDataPacket
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.BluenetProtocol.SESSION_NONCE_LENGTH
import rocks.crownstone.bluenet.structs.BluenetProtocol.VALIDATION_KEY_LENGTH
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log

data class SessionData(
		val sessionNonce: ByteArray,
		val validationKey: ByteArray,
		val protocolVersion: Uint8? = null
)

object SessionDataParser {
	private val TAG = this.javaClass.simpleName

	fun getSessionData(decryptedData: ByteArray, wasEncrypted: Boolean, v5: Boolean): SessionData? {
		if (v5) {
			val packet = SessionDataPacket()
			if (!packet.fromArray(decryptedData)) {
				return null
			}
			if (Conversion.byteArrayToInt(packet.validation) != BluenetProtocol.CAFEBABE) {
				Log.e(TAG, "validation failed: " + Conversion.bytesToString(decryptedData))
				return null
			}
			return SessionData(packet.sessionNonce, packet.validationKey, packet.protocol)
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
		}
		else {
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

		// Copy bytes 0-4 to sessionNonce, copy bytes 0-3 to validationKey
		System.arraycopy(data, offset, sessionNonce, 0, SESSION_NONCE_LENGTH)
		System.arraycopy(data, offset, validationKey, 0, VALIDATION_KEY_LENGTH)

		return SessionData(sessionNonce, validationKey)
	}
}