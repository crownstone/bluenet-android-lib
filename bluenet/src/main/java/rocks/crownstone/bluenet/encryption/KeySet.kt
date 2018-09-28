package rocks.crownstone.bluenet.encryption

import android.util.Log
import rocks.crownstone.bluenet.BluenetProtocol.AES_BLOCK_SIZE
import rocks.crownstone.bluenet.KeyAccessLevelPair
import rocks.crownstone.bluenet.util.Conversion
import java.nio.charset.Charset

/**
 * Class that holds keys with different access levels
 */
class KeySet() {
	private val TAG = this::class.java.canonicalName
//	private var adminKeyString: String? = null
//	private var memberKeyString: String? = null
//	private var guestKeyString: String? = null
	private var adminKeyBytes: ByteArray? = null
	private var memberKeyBytes: ByteArray? = null
	private var guestKeyBytes: ByteArray? = null
	private var initialized = false

	constructor(adminKey: String?, memberKey: String?, guestKey: String?): this() {
		val adminKeyString = getKeyFromString(adminKey)
		val memberKeyString = getKeyFromString(memberKey)
		val guestKeyString = getKeyFromString(guestKey)
		adminKeyBytes = null
		memberKeyBytes = null
		guestKeyBytes = null
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
		}
		catch (e: java.lang.NumberFormatException) {
			Log.e(TAG, "Invalid key format")
			e.printStackTrace()
//			adminKeyString = null
//			memberKeyString = null
//			guestKeyString = null
			adminKeyBytes = null
			memberKeyBytes = null
			guestKeyBytes = null
			return
		}
		initialized = true
	}

	constructor(adminKey: ByteArray?, memberKey: ByteArray?, guestKey: ByteArray?): this() {
		if ((adminKey != null && adminKey.size != AES_BLOCK_SIZE) ||
			(memberKey != null && memberKey.size != AES_BLOCK_SIZE) ||
			(guestKey != null && guestKey.size != AES_BLOCK_SIZE)) {
			Log.e(TAG, "Invalid key size")
			return
		}
		adminKeyBytes = adminKey
		memberKeyBytes = memberKey
		guestKeyBytes = guestKey
		initialized = true
	}

	fun getAdminKey(): ByteArray? {
		return adminKeyBytes
	}

	fun getMemberKey(): ByteArray? {
		return memberKeyBytes
	}

	fun getGuestKey(): ByteArray? {
		return guestKeyBytes
	}

	fun getKey(accessLevel: Int): ByteArray? {
		when (AccessLevel.fromInt(accessLevel)) {
			AccessLevel.ADMIN -> return getAdminKey()
			AccessLevel.MEMBER -> return getMemberKey()
			AccessLevel.GUEST -> return getGuestKey()
			else -> return null
		}
	}

	fun getHighestKey(): KeyAccessLevelPair? {
		if (getAdminKey() != null) {
			return KeyAccessLevelPair(getAdminKey(), AccessLevel.ADMIN)
		}
		if (getMemberKey() != null) {
			return KeyAccessLevelPair(getMemberKey(), AccessLevel.MEMBER)
		}
		if (getGuestKey() != null) {
			KeyAccessLevelPair(getGuestKey(), AccessLevel.GUEST)
		}
		return null
	}

	override fun toString(): String {
//		return "Keys: [$adminKeyString, $memberKeyString, $guestKeyString]"
		return "Keys: [${Conversion.bytesToHexString(adminKeyBytes)}, ${Conversion.bytesToHexString(memberKeyBytes)}, ${Conversion.bytesToHexString(guestKeyBytes)}]"
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