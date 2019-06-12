/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.structs.BluenetProtocol.AES_BLOCK_SIZE
import rocks.crownstone.bluenet.structs.KeyAccessLevelPair
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import java.nio.charset.Charset

/**
 * Class that holds keys with different access levels
 */
class KeySet() {
	private val TAG = this.javaClass.simpleName
	var adminKeyBytes: ByteArray? = null
		private set
	var memberKeyBytes: ByteArray? = null
		private set
	var guestKeyBytes: ByteArray? = null
		private set
	var setupKeyBytes: ByteArray? = null
		internal set
	var serviceDataKeyBytes: ByteArray? = null
		internal set
	var meshAppKeyBytes: ByteArray? = null
		internal set
	var meshNetKeyBytes: ByteArray? = null
		internal set
	var initialized = false
		private set

	constructor(adminKey: String?, memberKey: String?, guestKey: String?, serviceDataKey: String?, meshAppKey: String?, meshNetKey: String?): this() {
		val adminKeyString = getKeyFromString(adminKey)
		val memberKeyString = getKeyFromString(memberKey)
		val guestKeyString = getKeyFromString(guestKey)
		val serviceDataKeyString = getKeyFromString(serviceDataKey)
		val meshAppKeyString = getKeyFromString(meshAppKey)
		val meshNetKeyString = getKeyFromString(meshNetKey)
		adminKeyBytes = null
		memberKeyBytes = null
		guestKeyBytes = null
		setupKeyBytes = null
		serviceDataKeyBytes = null
		meshAppKeyBytes = null
		meshNetKeyBytes = null
		try {
			if (adminKeyString != null) {
				adminKeyBytes = Conversion.hexStringToBytes(adminKeyString)
			}
			if (memberKeyString != null) {
				memberKeyBytes = Conversion.hexStringToBytes(memberKeyString)
			}
			if (guestKeyString != null) {
				guestKeyBytes = Conversion.hexStringToBytes(guestKeyString)
			}
			if (serviceDataKeyString != null) {
				serviceDataKeyBytes = Conversion.hexStringToBytes(serviceDataKeyString)
			}
			if (meshAppKeyString != null) {
				meshAppKeyBytes = Conversion.hexStringToBytes(meshAppKeyString)
			}
			if (meshNetKeyString != null) {
				meshNetKeyBytes = Conversion.hexStringToBytes(meshNetKeyString)
			}
		}
		catch (e: java.lang.NumberFormatException) {
			Log.e(TAG, "Invalid key format")
			e.printStackTrace()
			adminKeyBytes = null
			memberKeyBytes = null
			guestKeyBytes = null
			setupKeyBytes = null
			serviceDataKeyBytes = null
			meshAppKeyBytes = null
			meshNetKeyBytes = null
			return
		}
		initialized = true
	}

	constructor(adminKey: ByteArray?, memberKey: ByteArray?, guestKey: ByteArray?, serviceDataKey: ByteArray?, setupKey: ByteArray?=null): this() {
		if (
				(adminKey != null && adminKey.size != AES_BLOCK_SIZE) ||
				(memberKey != null && memberKey.size != AES_BLOCK_SIZE) ||
				(guestKey != null && guestKey.size != AES_BLOCK_SIZE) ||
				(serviceDataKey != null && serviceDataKey.size != AES_BLOCK_SIZE) ||
				(setupKey != null && setupKey.size != AES_BLOCK_SIZE)
		) {
			Log.e(TAG, "Invalid key size")
			return
		}
		adminKeyBytes = adminKey
		memberKeyBytes = memberKey
		guestKeyBytes = guestKey
		serviceDataKeyBytes = serviceDataKey
		setupKeyBytes = setupKey
		initialized = true
	}

	fun getKey(accessLevel: Uint8): ByteArray? {
		return getKey(AccessLevel.fromNum(accessLevel))
	}

	fun getKey(accessLevel: AccessLevel): ByteArray? {
		when (accessLevel) {
			AccessLevel.ADMIN -> return adminKeyBytes
			AccessLevel.MEMBER -> return memberKeyBytes
			AccessLevel.GUEST -> return guestKeyBytes
			AccessLevel.SETUP -> return setupKeyBytes
			AccessLevel.SERVICE_DATA -> return serviceDataKeyBytes
			else -> return null
		}
	}

	fun getHighestKey(): KeyAccessLevelPair? {
		val adminKey = this.adminKeyBytes
		if (adminKey != null) {
			return KeyAccessLevelPair(adminKey, AccessLevel.ADMIN)
		}
		val memberKey = this.memberKeyBytes
		if (memberKey != null) {
			return KeyAccessLevelPair(memberKey, AccessLevel.MEMBER)
		}
		val guestKey = this.guestKeyBytes
		if (guestKey != null) {
			KeyAccessLevelPair(guestKey, AccessLevel.GUEST)
		}
		return null
	}

	override fun toString(): String {
//		return "Keys: [$adminKeyString, $memberKeyString, $guestKeyString]"
		return "Keys: [" +
				"admin: ${Conversion.bytesToHexString(adminKeyBytes)}, " +
				"member: ${Conversion.bytesToHexString(memberKeyBytes)}, " +
				"guest: ${Conversion.bytesToHexString(guestKeyBytes)}, " +
				"setup: ${Conversion.bytesToHexString(setupKeyBytes)}, " +
				"serviceData: ${Conversion.bytesToHexString(serviceDataKeyBytes)}, " +
				"meshApp: ${Conversion.bytesToHexString(meshAppKeyBytes)}, " +
				"meshNet: ${Conversion.bytesToHexString(meshNetKeyBytes)}" +
				"]"
	}

	private fun getKeyFromString(key: String?): String? {
		if (key == null) {
			return null
		}
		var retKey: String? = null
		if (key.length == AES_BLOCK_SIZE * 2) {
			retKey = key
		}
		if (key.length == AES_BLOCK_SIZE) {
			val keyBytes = key.toByteArray(Charset.forName("UTF-8"))
			retKey = Conversion.bytesToHexString(keyBytes)
		}
		return retKey
	}
}
