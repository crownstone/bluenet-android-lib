package rocks.crownstone.bluenet.util

import android.bluetooth.BluetoothAdapter
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


object Conversion {
	private val TAG = Conversion::class.java.canonicalName

	const val BASE_UUID_START = "0000"
	const val BASE_UUID_END = "-0000-1000-8000-00805f9b34fb"

	// String Bluetooth address, such as "00:43:A8:23:10:F0"
	private const val ADDRESS_LENGTH = 6
	private const val ADDRESS_STRING_LENGTH = 17

	fun uuidToString(uuid: UUID): String {
		val uuidString = uuid.toString()

		if (uuidString.startsWith(BASE_UUID_START) && uuidString.endsWith(BASE_UUID_END)) {
			return uuidString.substring(4, 8)
		}
		return uuidString
	}

	fun stringToUuid(uuidStr: String): UUID {
		var fullUuidStr = uuidStr
		if (uuidStr.length == 4) {
			fullUuidStr = BASE_UUID_START + uuidStr + BASE_UUID_END
		}
		return UUID.fromString(fullUuidStr)
	}

	fun stringToUuid(uuids: Array<String>): Array<UUID> {
		val result = Array<UUID>(uuids.size) { UUID(0,0) }
		for (i in uuids.indices) {
			result[i] = stringToUuid(uuids[i])
		}
		return result
	}

	fun uuidToBytes(uuidStr: String): ByteArray {
		return uuidToBytes(stringToUuid(uuidStr))
	}

	fun uuidToBytes(uuid: UUID): ByteArray {
		val bb = ByteBuffer.allocate(16)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putLong(uuid.leastSignificantBits)
		bb.putLong(uuid.mostSignificantBits)
		return bb.array()
	}

	fun bytesToUuid(bytes: ByteArray): String {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)

		val lsb = bb.long
		val msb = bb.long
		val uuid = UUID(msb, lsb)
		return uuidToString(uuid)
	}

	fun encodedStringToBytes(encoded: String): ByteArray {
		return Base64.decode(encoded, Base64.NO_WRAP)
	}

	fun bytesToEncodedString(bytes: ByteArray): String {
		return Base64.encodeToString(bytes, Base64.NO_WRAP)
	}

//	@JvmOverloads
	fun byteArrayToInt(bytes: ByteArray, offset: Int = 0): Int {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return bb.getInt(offset)
	}

//	@JvmOverloads
	fun byteArrayToShort(bytes: ByteArray, offset: Int = 0): Int {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return bb.getShort(offset).toInt()
	}

//	@JvmOverloads
	fun byteArrayToFloat(bytes: ByteArray, offset: Int = 0): Float {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return bb.getFloat(offset)
	}

	fun intToByteArray(value: Int): ByteArray {
		val bb = ByteBuffer.allocate(4)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putInt(value)
		return bb.array()
	}

	fun shortToByteArray(value: Int): ByteArray {
		val bb = ByteBuffer.allocate(2)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(value.toShort())
		return bb.array()
	}

	fun floatToByteArray(value: Float): ByteArray {
		val bb = ByteBuffer.allocate(4)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putFloat(value)
		return bb.array()
	}

	fun toUint8(b: Byte): Int {
		return b.toInt() and 0xFF
	}

	fun toUint16(num: Short): Int {
		return num.toInt() and 0xFFFF
	}

	fun toUint16(num: Int): Int {
		return num and 0xFFFF
	}

	fun toUint32(num: Int): Long {
		return num.toLong() and 0xFFFFFFFFL
	}

	fun uint32ToByteArray(num: Long): ByteArray {
		val bytes = ByteArray(4)
		bytes[0] = (num shr 0 and 0xFF).toByte()
		bytes[1] = (num shr 8 and 0xFF).toByte()
		bytes[2] = (num shr 16 and 0xFF).toByte()
		bytes[3] = (num shr 24 and 0xFF).toByte()
		return bytes
	}

	fun hexStringToBytes(hex: String): ByteArray {
		val result = ByteArray(hex.length / 2)
		for (i in result.indices) {
			result[i] = Integer.valueOf(hex.substring(2 * i, 2 * i + 2), 16).toByte()
		}
		return result
	}

	fun bytesToHexString(bytes: ByteArray?): String {
		if (bytes == null) {
			return ""
		}
		val sb = StringBuilder()
		for (b in bytes) {
			sb.append(String.format("%02x", b))
		}
		return sb.toString()
	}

	@Throws
	fun addressToBytes(address: String): ByteArray {
		if (address.length != ADDRESS_STRING_LENGTH) {
			throw IllegalArgumentException("invalid address")
		}

		val result = ByteArray(ADDRESS_LENGTH)

//		try {
			for (i in 0 until ADDRESS_LENGTH) {
				result[ADDRESS_LENGTH - 1 - i] = Integer.valueOf(address.substring(3 * i, 3 * i + 2), 16).toByte()
			}
//		} catch (e: java.lang.NumberFormatException) {
//			e.printStackTrace()
//			result = null
//		}

		return result
	}

	fun bytesToAddress(bytes: ByteArray): String {
		val sb = StringBuilder()
		for (b in bytes) {
			sb.append(String.format("%02X:", b))
		}
		sb.deleteCharAt(sb.length - 1) // remove last semicolon
		return sb.toString()
	}

	// Reverse a byte array
	fun reverse(array: ByteArray): ByteArray {
		val result = ByteArray(array.size)
		for (i in array.indices) {
			result[i] = array[array.size - (i + 1)]
		}
		return result
	}

	// Handy for logging, does not turn byte values into the corresponding ascii values!
	fun bytesToString(bytes: ByteArray?): String {
		if (bytes == null) {
			return ""
		}
		if (bytes.isEmpty()) {
			return "[]"
		}
		val sb = StringBuilder("[")
		sb.append(toUint8(bytes[0]))
		for (i in 1 until bytes.size) {
			sb.append(", ")
			sb.append(toUint8(bytes[i]))
		}
		sb.append("]")
		return sb.toString()
	}

	fun isValidAddress(address: String): Boolean {
		return BluetoothAdapter.checkBluetoothAddress(address)
	}

}
