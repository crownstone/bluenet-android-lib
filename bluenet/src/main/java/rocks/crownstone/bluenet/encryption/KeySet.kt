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
	var localizationKeyBytes: ByteArray? = null
		internal set
	var initialized = false
		private set

	constructor(adminKey: String?, memberKey: String?, guestKey: String?, serviceDataKey: String?, localizationKey: String?): this() {
		clear()
		try {
			adminKeyBytes = Conversion.getKeyFromString(adminKey)
			memberKeyBytes = Conversion.getKeyFromString(memberKey)
			guestKeyBytes = Conversion.getKeyFromString(guestKey)
			serviceDataKeyBytes = Conversion.getKeyFromString(serviceDataKey)
			localizationKeyBytes = Conversion.getKeyFromString(localizationKey)
		}
		catch (e: java.lang.NumberFormatException) {
			Log.e(TAG, "Invalid key format")
			e.printStackTrace()
			clear()
			return
		}
		initialized = true
	}

	constructor(adminKey: ByteArray?, memberKey: ByteArray?, guestKey: ByteArray?, serviceDataKey: ByteArray?, localizationKey: ByteArray?, setupKey: ByteArray?): this() {
		if (
				(adminKey != null && adminKey.size != AES_BLOCK_SIZE) ||
				(memberKey != null && memberKey.size != AES_BLOCK_SIZE) ||
				(guestKey != null && guestKey.size != AES_BLOCK_SIZE) ||
				(serviceDataKey != null && serviceDataKey.size != AES_BLOCK_SIZE) ||
				(localizationKey != null && localizationKey.size != AES_BLOCK_SIZE) ||
				(setupKey != null && setupKey.size != AES_BLOCK_SIZE)
		) {
			Log.e(TAG, "Invalid key size")
			return
		}
		adminKeyBytes = adminKey
		memberKeyBytes = memberKey
		guestKeyBytes = guestKey
		serviceDataKeyBytes = serviceDataKey
		localizationKeyBytes = localizationKey
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
			AccessLevel.LOCALIZATION -> return localizationKeyBytes
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
			return KeyAccessLevelPair(guestKey, AccessLevel.GUEST)
		}
		return null
	}

	fun clear() {
		adminKeyBytes = null
		memberKeyBytes = null
		guestKeyBytes = null
		setupKeyBytes = null
		serviceDataKeyBytes = null
		localizationKeyBytes = null
		initialized = false
	}

	override fun toString(): String {
		return "Keys: [" +
				"admin: ${Conversion.bytesToHexString(adminKeyBytes)}, " +
				"member: ${Conversion.bytesToHexString(memberKeyBytes)}, " +
				"guest: ${Conversion.bytesToHexString(guestKeyBytes)}, " +
				"setup: ${Conversion.bytesToHexString(setupKeyBytes)}, " +
				"serviceData: ${Conversion.bytesToHexString(serviceDataKeyBytes)}, " +
				"localization: ${Conversion.bytesToHexString(localizationKeyBytes)}, " +
				"]"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as KeySet

		val adminKeyBytes = this.adminKeyBytes
		val otherAdminKeyBytes = other.adminKeyBytes
		if (adminKeyBytes != null) {
			if (otherAdminKeyBytes == null) return false
			if (!adminKeyBytes.contentEquals(otherAdminKeyBytes)) return false
		}
		else if (otherAdminKeyBytes != null) return false

		val memberKeyBytes = this.memberKeyBytes
		val otherMemberKeyBytes = other.memberKeyBytes
		if (memberKeyBytes != null) {
			if (otherMemberKeyBytes == null) return false
			if (!memberKeyBytes.contentEquals(otherMemberKeyBytes)) return false
		}
		else if (otherMemberKeyBytes != null) return false

		val guestKeyBytes = this.guestKeyBytes
		val otherGuestKeyBytes = other.guestKeyBytes
		if (guestKeyBytes != null) {
			if (otherGuestKeyBytes == null) return false
			if (!guestKeyBytes.contentEquals(otherGuestKeyBytes)) return false
		}
		else if (otherGuestKeyBytes != null) return false

		// No need to check setup key, as it's temp.
//		val setupKeyBytes = this.setupKeyBytes
//		val otherSetupKeyBytes = other.setupKeyBytes
//		if (setupKeyBytes != null) {
//			if (otherSetupKeyBytes == null) return false
//			if (!setupKeyBytes.contentEquals(otherSetupKeyBytes)) return false
//		}
//		else if (otherSetupKeyBytes != null) return false

		val serviceDataKeyBytes = this.serviceDataKeyBytes
		val otherServiceDataKeyBytes = other.serviceDataKeyBytes
		if (serviceDataKeyBytes != null) {
			if (otherServiceDataKeyBytes == null) return false
			if (!serviceDataKeyBytes.contentEquals(otherServiceDataKeyBytes)) return false
		}
		else if (otherServiceDataKeyBytes != null) return false

		val localizationKeyBytes = this.localizationKeyBytes
		val otherLocalizationKeyBytes = other.localizationKeyBytes
		if (localizationKeyBytes != null) {
			if (otherLocalizationKeyBytes == null) return false
			if (!localizationKeyBytes.contentEquals(otherLocalizationKeyBytes)) return false
		}
		else if (otherLocalizationKeyBytes != null) return false

		if (initialized != other.initialized) return false

		return true
	}
}
