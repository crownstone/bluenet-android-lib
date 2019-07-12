/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.structs.BluenetProtocol.AES_BLOCK_SIZE
import rocks.crownstone.bluenet.structs.BluenetProtocol.PACKET_NONCE_LENGTH
import rocks.crownstone.bluenet.structs.BluenetProtocol.SESSION_NONCE_LENGTH
import rocks.crownstone.bluenet.structs.BluenetProtocol.VALIDATION_KEY_LENGTH
import rocks.crownstone.bluenet.structs.BluenetProtocol.ACCESS_LEVEL_LENGTH
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encryption {
	private val TAG = this.javaClass.simpleName

	fun encryptCtr(payloadData: ByteArray, sessionNonce: ByteArray, validationKey: ByteArray, key: ByteArray, accessLevel: Uint8): ByteArray? {
		if (payloadData == null || payloadData.isEmpty()) {
			Log.w(TAG, "wrong data length")
			return null
		}
		if (sessionNonce == null || sessionNonce.size != SESSION_NONCE_LENGTH) {
			Log.w(TAG, "wrong session nonce length")
			return null
		}
		if (validationKey == null || validationKey.size != VALIDATION_KEY_LENGTH) {
			Log.w(TAG, "wrong validation key length")
			return null
		}
		if (key == null || key.size != AES_BLOCK_SIZE) {
			Log.w(TAG, "wrong key length")
			return null
		}
		Log.v(TAG, "payloadData: ${Conversion.bytesToString(payloadData)}")

		// Packet nonce is randomly generated.
		val random = SecureRandom()
		val packetNonce = ByteArray(PACKET_NONCE_LENGTH)
		random.nextBytes(packetNonce)
		Log.v(TAG, "packetNonce: ${Conversion.bytesToString(packetNonce)}")

		// Nonce is concatenation of session nonce and packet nonce
		val nonce = ByteArray(PACKET_NONCE_LENGTH + SESSION_NONCE_LENGTH)
		System.arraycopy(packetNonce, 0, nonce, 0, PACKET_NONCE_LENGTH)
		System.arraycopy(sessionNonce, 0, nonce, PACKET_NONCE_LENGTH, SESSION_NONCE_LENGTH)

		// prepend validation key to payload
		val payload = ByteArray(VALIDATION_KEY_LENGTH + payloadData.size)
		System.arraycopy(validationKey, 0, payload, 0, VALIDATION_KEY_LENGTH)
		System.arraycopy(payloadData, 0, payload, VALIDATION_KEY_LENGTH, payloadData.size)

		val encryptedData = encryptCtr(payload, 0, PACKET_NONCE_LENGTH + ACCESS_LEVEL_LENGTH, nonce, key) ?: return null
		System.arraycopy(packetNonce, 0, encryptedData, 0, PACKET_NONCE_LENGTH)
		encryptedData[PACKET_NONCE_LENGTH] = accessLevel.toByte()
		Log.v(TAG, "encryptedData: ${Conversion.bytesToString(encryptedData)}")
		return encryptedData
	}

	fun decryptCtr(encryptedData: ByteArray, sessionNonce: ByteArray, validationKey: ByteArray, keys: KeySet): ByteArray? {
		if (encryptedData == null || encryptedData.size < PACKET_NONCE_LENGTH + ACCESS_LEVEL_LENGTH + AES_BLOCK_SIZE) {
			Log.w(TAG, "wrong data length")
			return null
		}
		if (sessionNonce == null || sessionNonce.size != SESSION_NONCE_LENGTH) {
			Log.w(TAG, "wrong session nonce length")
			return null
		}
		if (validationKey == null || validationKey.size != VALIDATION_KEY_LENGTH) {
			Log.w(TAG, "wrong validation key length")
			return null
		}
		if (keys == null) {
			Log.w(TAG, "no keys supplied")
			return null
		}
		Log.v(TAG, "encryptedData: ${Conversion.bytesToString(encryptedData)}")

		val decryptedData = ByteArray(encryptedData.size - PACKET_NONCE_LENGTH - ACCESS_LEVEL_LENGTH)
		if (decryptedData.size % AES_BLOCK_SIZE != 0) {
			Log.v(TAG, "encrypted data length must be multiple of 16")
			return null
		}

		val accessLevel = Conversion.toUint8(encryptedData[PACKET_NONCE_LENGTH])
		Log.v(TAG, "accessLevel: $accessLevel")
		val key = keys.getKey(accessLevel)
		if (key == null || key.size != AES_BLOCK_SIZE) {
			Log.w(TAG, "wrong key length: $key")
			return null
		}

		// Create iv by concatting session nonce and packet nonce
		val iv = ByteArray(AES_BLOCK_SIZE)
		System.arraycopy(encryptedData, 0, iv, 0, PACKET_NONCE_LENGTH)
		System.arraycopy(sessionNonce, 0, iv, PACKET_NONCE_LENGTH, SESSION_NONCE_LENGTH)

		// Decrypt encrypted payload
		try {
			val cipher = Cipher.getInstance("AES/CTR/NoPadding")
			cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
			Log.v(TAG, "IV: ${Conversion.bytesToString(cipher.iv)}")
			// doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
			cipher.doFinal(encryptedData, PACKET_NONCE_LENGTH + ACCESS_LEVEL_LENGTH, decryptedData.size, decryptedData, 0)
		}
		catch (e: GeneralSecurityException) {
			e.printStackTrace()
			return null
		}

		// Check validation key
		for (i in 0 until VALIDATION_KEY_LENGTH) {
			if (decryptedData[i] != validationKey[i]) {
				Log.w(TAG, "validationkey: ${Conversion.bytesToString(validationKey)} decrypted: ${Conversion.bytesToString(decryptedData)}")
				Log.w(TAG, "incorrect validation key")
				return null
			}
		}

		val payloadData = ByteArray(decryptedData.size - VALIDATION_KEY_LENGTH)
		System.arraycopy(decryptedData, VALIDATION_KEY_LENGTH, payloadData, 0, payloadData.size)
		return payloadData
	}

	fun decryptEcb(payloadData: ByteBuffer, key: ByteArray): ByteArray? {
		if (payloadData.remaining() < AES_BLOCK_SIZE) {
			Log.w(TAG, "payload data too short")
			return null
		}
		val byteArray = ByteArray(AES_BLOCK_SIZE)
		payloadData.get(byteArray)
		return decryptEcb(byteArray, key)
	}

	fun decryptEcb(payloadData: ByteArray, key: ByteArray, inputOffset: Int = 0): ByteArray? {
		if (inputOffset < 0) {
			return null
		}
		if (payloadData == null || payloadData.size - inputOffset < AES_BLOCK_SIZE) {
			Log.w(TAG, "payload data too short")
			return null
		}
		val length = payloadData.size - inputOffset
		if (length % AES_BLOCK_SIZE != 0) {
			Log.w(TAG, "wrong payload data length")
			return null
		}
		val decryptedData = ByteArray(length)
		if (key == null) {
			// If there is no key set, then simply do not decrypt.
			System.arraycopy(payloadData, inputOffset, decryptedData, 0, length)
			return decryptedData
		}
		if (key.size != AES_BLOCK_SIZE) {
			Log.w(TAG, "wrong key length: ${key.size}")
			return null
		}
		try {
			val cipher = Cipher.getInstance("AES/ECB/NoPadding")
			cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
			cipher.doFinal(payloadData, inputOffset, length, decryptedData, 0)
		}
		catch (e: GeneralSecurityException) {
			e.printStackTrace()
			return null
		}

		return decryptedData
	}

	/**
	 * Encrypt data using AES CTR.
	 *
	 * Assumes correct size of input parameters.
	 *
	 * @param data                Data to be encrypted.
	 * @param dataOffset          Offset of the data to encrypt.
	 * @param encryptedDataOffset Output array offset, encrypted data will start at this offset.
	 * @param nonce               Nonce.
	 * @param key                 Key.
	 * @return                    Encrypted data.
	 */
	fun encryptCtr(data: ByteArray, dataOffset: Int, encryptedDataOffset: Int, nonce: ByteArray, key: ByteArray): ByteArray? {
		// Create iv with nonce nonce and packet nonce.
		val iv = ByteArray(AES_BLOCK_SIZE) // Already initialized with zeroes
		System.arraycopy(nonce, 0, iv, 0, nonce.size)

		// Pad with zeroes.
		val dataSize = data.size - dataOffset
		val paddingSize = (AES_BLOCK_SIZE - dataSize % AES_BLOCK_SIZE) % AES_BLOCK_SIZE
		val paddedData = ByteArray(dataSize + paddingSize) // Already initialized with zeroes
		//Arrays.fill(payload, (byte)0); // Already zeroes by default
		System.arraycopy(data, dataOffset, paddedData, 0, dataSize)
		Log.v(TAG, "paddedData: ${Conversion.bytesToString(paddedData)}")

		val encryptedData = ByteArray(paddedData.size + encryptedDataOffset)

		// Encrypt payload
		try {
			val cipher = Cipher.getInstance("AES/CTR/NoPadding")
			cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
			Log.v(TAG, "IV before: ${Conversion.bytesToString(cipher.iv)}")
			// doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
			cipher.doFinal(paddedData, 0, paddedData.size, encryptedData, encryptedDataOffset)
			Log.v(TAG, "IV after: ${Conversion.bytesToString(cipher.iv)}")
		}
		catch (e: GeneralSecurityException) {
			e.printStackTrace()
			return null
		}
		Log.v(TAG, "encryptedData: ${Conversion.bytesToString(encryptedData)}")
		return encryptedData
	}
}