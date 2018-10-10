package rocks.crownstone.bluenet.encryption

import android.util.Log
import rocks.crownstone.bluenet.BluenetProtocol.AES_BLOCK_SIZE
import rocks.crownstone.bluenet.KeyAccessLevelPair
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.util.Conversion
import java.nio.charset.Charset

/**
 * Class that holds keys with different access levels
 */
class KeySet() {
	private val TAG = this.javaClass.simpleName
//	private var adminKeyString: String? = null
//	private var memberKeyString: String? = null
//	private var guestKeyString: String? = null
	var adminKeyBytes: ByteArray? = null
		private set
	var memberKeyBytes: ByteArray? = null
		private set
	var guestKeyBytes: ByteArray? = null
		private set
	var setupKeyBytes: ByteArray? = null
		internal set
	var initialized = false
		private set

	constructor(adminKey: String?, memberKey: String?, guestKey: String?): this() {
		val adminKeyString = getKeyFromString(adminKey)
		val memberKeyString = getKeyFromString(memberKey)
		val guestKeyString = getKeyFromString(guestKey)
		adminKeyBytes = null
		memberKeyBytes = null
		guestKeyBytes = null
		setupKeyBytes = null
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

	constructor(adminKey: ByteArray?, memberKey: ByteArray?, guestKey: ByteArray?, setupKey: ByteArray?=null): this() {
		if ((adminKey != null && adminKey.size != AES_BLOCK_SIZE) ||
			(memberKey != null && memberKey.size != AES_BLOCK_SIZE) ||
			(guestKey != null && guestKey.size != AES_BLOCK_SIZE)) {
			Log.e(TAG, "Invalid key size")
			return
		}
		setupKeyBytes = setupKey
		adminKeyBytes = adminKey
		memberKeyBytes = memberKey
		guestKeyBytes = guestKey
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
		return "Keys: [${Conversion.bytesToHexString(adminKeyBytes)}, ${Conversion.bytesToHexString(memberKeyBytes)}, ${Conversion.bytesToHexString(guestKeyBytes)}, ${Conversion.bytesToHexString(setupKeyBytes)}]"
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