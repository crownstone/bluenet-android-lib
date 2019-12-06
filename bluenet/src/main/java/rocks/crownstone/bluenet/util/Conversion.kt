/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.util

import android.bluetooth.BluetoothAdapter
import android.util.Base64
import rocks.crownstone.bluenet.structs.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList


object Conversion {
	val TAG = this.javaClass.simpleName

	const val BASE_UUID_START = "0000"
	const val BASE_UUID_END = "-0000-1000-8000-00805f9b34fb"

	// String Bluetooth address, such as "00:43:A8:23:10:F0"
	private const val ADDRESS_LENGTH = 6
	private const val ADDRESS_STRING_LENGTH = 17

	/**
	 * Convert UUID to string.
	 *
	 * If UUID starts with BASE_UUID_START, and ends with BASE_UUID_END,
	 * only the relevant part will be returned.
	 */
	fun uuidToString(uuid: UUID): String {
		val uuidString = uuid.toString()

		if (uuidString.startsWith(BASE_UUID_START) && uuidString.endsWith(BASE_UUID_END)) {
			return uuidString.substring(4, 8)
		}
		return uuidString
	}

	/**
	 * Convert string to UUID.
	 *
	 * Returns null if the string is invalid.
	 */
	fun stringToUuid(uuidStr: String): UUID? {
		var fullUuidStr = uuidStr
		if (uuidStr.length == 4) {
			fullUuidStr = BASE_UUID_START + uuidStr + BASE_UUID_END
		}
		val uuid = try {
			UUID.fromString(fullUuidStr)
		}
		catch (e: IllegalArgumentException) {
			null
		}
		return uuid
	}

//	fun stringToUuid(uuids: Array<String>): Array<UUID?> {
//		val result = Array<UUID?>(uuids.size) { UUID(0,0) }
//		for (i in uuids.indices) {
//			result[i] = stringToUuid(uuids[i])
//		}
//		return result
//	}

	fun uuidToBytes(uuidStr: String): ByteArray? {
		val uuid = stringToUuid(uuidStr) ?: return null
		return uuidToBytes(uuid)
	}

	fun uuidToBytes(uuid: UUID, order: ByteOrder= ByteOrder.LITTLE_ENDIAN): ByteArray {
		val bb = ByteBuffer.allocate(16)
		bb.order(order)
		if (order == ByteOrder.BIG_ENDIAN) {
			bb.putLong(uuid.mostSignificantBits)
			bb.putLong(uuid.leastSignificantBits)
		}
		else {
			bb.putLong(uuid.leastSignificantBits)
			bb.putLong(uuid.mostSignificantBits)
		}
		return bb.array()
	}

	fun bytesToUuid(bytes: ByteArray): UUID? {
		return when (bytes.size) {
			2 -> bytesToUuid2(bytes)
			4 -> bytesToUuid4(bytes)
			16 -> bytesToUuid16(bytes)
			else -> null
		}
	}

	fun bytesToUuid16(bytes: ByteArray): UUID {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		val lsb = bb.long
		val msb = bb.long
		return UUID(msb, lsb)
	}

	fun bytesToUuid4(bytes: ByteArray, offset: Int = 0): UUID? {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		val uint32 = bb.getUint32(offset)
		val str = "%08x".format(uint32.toLong())
		return stringToUuid(str)
	}

	fun bytesToUuid2(bytes: ByteArray, offset: Int = 0): UUID? {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		val uint16 = bb.getUint16(offset)
		val str = "%04x".format(uint16.toInt())
		return stringToUuid(str)
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
	fun byteArrayToShort(bytes: ByteArray, offset: Int = 0): Short {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return bb.getShort(offset)
	}

//	@JvmOverloads
	fun byteArrayToFloat(bytes: ByteArray, offset: Int = 0): Float {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return bb.getFloat(offset)
	}

	fun byteArrayToUint16Array(bytes: ByteArray, offset: Int = 0): List<Uint16> {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.position(offset)
		val result = ArrayList<Uint16>(bb.remaining()/2)
		while (bb.remaining() >= 2) {
			result.add(toUint16(bb.getShort()))
		}
		return result
	}

	fun byteArrayToInt16Array(bytes: ByteArray, offset: Int = 0): List<Int16> {
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.position(offset)
		val result = ArrayList<Int16>(bb.remaining()/2)
		while (bb.remaining() >= 2) {
			result.add(bb.getShort())
		}
		return result
	}


	fun int32ToByteArray(value: Int32): ByteArray {
		val bb = ByteBuffer.allocate(4)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putInt(value)
		return bb.array()
	}

	fun uint32ToByteArray(value: Uint32): ByteArray {
		val bb = ByteBuffer.allocate(4)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putInt(value.toInt())
		return bb.array()
	}
//	fun uint32ToByteArray(num: Long): ByteArray {
//		val bytes = ByteArray(4)
//		bytes[0] = (num shr 0 and 0xFF).toByte()
//		bytes[1] = (num shr 8 and 0xFF).toByte()
//		bytes[2] = (num shr 16 and 0xFF).toByte()
//		bytes[3] = (num shr 24 and 0xFF).toByte()
//		return bytes
//	}

	fun int16ToByteArray(value: Int16): ByteArray {
		val bb = ByteBuffer.allocate(2)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(value)
		return bb.array()
	}

	fun uint16ToByteArray(value: Uint16): ByteArray {
		val bb = ByteBuffer.allocate(2)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(value.toShort())
		return bb.array()
	}

	fun int8ToByteArray(value: Int8): ByteArray {
		return byteArrayOf(value)
	}

	fun uint8ToByteArray(value: Uint8): ByteArray {
		return byteArrayOf(value.toByte())
	}

//	fun uint8ToByteArray(value: Int): ByteArray {
//		return byteArrayOf(value.toByte())
//	}

	fun floatToByteArray(value: Float): ByteArray {
		val bb = ByteBuffer.allocate(4)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putFloat(value)
		return bb.array()
	}



	fun toUint8(num: Byte): Uint8 {
		return num.toUByte()
	}

	fun toUint8(num: Int): Uint8 {
		return num.toUByte()
	}

	fun toUint8(num: UInt): Uint8 {
		return num.toUByte()
	}

	fun toUint16(num: Byte): Uint16 {
		return num.toUShort()
	}

	fun toUint16(num: Short): Uint16 {
		return num.toUShort()
	}

	fun toUint16(num: Int): Uint16 {
		return num.toUShort()
	}

	fun toUint16(num: UInt): Uint16 {
		return num.toUShort()
	}

	fun toUint16(num: Long): Uint16 {
		return num.toUShort()
	}

	fun toUint32(num: Int): Uint32 {
		return num.toUInt()
	}

	fun toUint32(num: UShort): Uint32 {
		return num.toUInt()
	}

	fun uint16ListToUint32Reversed(list: List<Uint16>): Uint32 {
		return (toUint32(list[0]) shl 16) + toUint32(list[1])
	}

	fun uint32ToUint16ListReversed(num: Uint32): List<Uint16> {
		return arrayListOf(toUint16(num shr 16), toUint16(num))
	}

	fun uint32ToUint16List(num: Uint32): List<Uint16> {
		return arrayListOf(toUint16(num), toUint16(num shr 16))
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

	/**
	 * Gets a key from a string.
	 *
	 * String can be a hexadecimal string, or a plain string.
	 */
	fun getKeyFromString(key: String?): ByteArray? {
		if (key == null) {
			return null
		}
		var retKey: ByteArray? = null
		if (key.length == BluenetProtocol.AES_BLOCK_SIZE * 2) {
			retKey = hexStringToBytes(key)
		}
		if (key.length == BluenetProtocol.AES_BLOCK_SIZE) {
			retKey = key.toByteArray(Charset.forName("UTF-8"))
		}
		return retKey
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

	/**
	 * Convert byte array to MAC address string.
	 *
	 * Inverses the byte array.
	 */
	fun bytesToAddress(bytes: ByteArray): String {
		if (bytes.size != ADDRESS_LENGTH) {
			return ""
		}
		val sb = StringBuilder()
		for (b in bytes) {
//			sb.append(String.format("%02X:", b))
			sb.insert(0, String.format("%02X:", b))
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

	/**
	 * Convert byte array to readable string of numbers.
	 *
	 * Does not convert byte values into corresponding ascii values.
	 */
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


	@Throws
	inline fun <reified T> byteArrayTo(array: ByteArray, offset: Int = 0): T {
		Log.i(TAG, "class: ${T::class}")
		// Using T::class doesn't work, maybe because unsigned ints are still experimental.
		when (T::class.simpleName) {
			"Byte", "UByte" -> {
				Log.i(TAG, "expecting size 1, size=${array.size}")
				if (array.size != 1) {
					throw Errors.Parse("Expected size of 1, size=${array.size}")
				}
			}
			"Short", "UShort" -> {
				Log.i(TAG, "expecting size 2, size=${array.size}")
				if (array.size != 2) {
					throw Errors.Parse("Expected size of 2, size=${array.size}")
				}
			}
			"Int", "UInt", "Float" -> {
				Log.i(TAG, "expecting size 4, size=${array.size}")
				if (array.size != 4) {
					throw Errors.Parse("Expected size of 4, size=${array.size}")
				}
			}
			else -> {
				throw Errors.Parse("Unexpected type: ${T::class.simpleName}")
			}
		}
		when (T::class.simpleName) {
			"Byte" ->
				return array[offset] as T
			"UByte" ->
				return toUint8(array[0]) as T
			"Short" ->
				return byteArrayToShort(array, offset) as T
			"UShort" ->
				return toUint16(byteArrayToShort(array, offset)) as T
			"Int" ->
				return byteArrayToInt(array, offset) as T
			"UInt" ->
				return toUint32(byteArrayToInt(array, offset)) as T
			"Float" ->
				return byteArrayToFloat(array, offset) as T
			else ->
				throw Errors.Parse("Unexpected type: ${T::class.simpleName}")
		}
	}

	inline fun <reified T> toByteArray(value: T): ByteArray {
		// Using T::class doesn't work, maybe because unsigned ints are still experimental.
		when (T::class.simpleName) {
			"Boolean" -> {
				val uint8: Uint8 = if (value as Boolean) 1U else 0U
				return uint8ToByteArray(uint8)
			}
			"Byte" ->
				return int8ToByteArray(value as Int8)
			"UByte" ->
				return uint8ToByteArray(value as Uint8)
			"Short" ->
				return int16ToByteArray(value as Int16)
			"UShort" ->
				return uint16ToByteArray(value as Uint16)
			"Int" ->
				return int32ToByteArray(value as Int32)
			"UInt" ->
				return uint32ToByteArray(value as Uint32)
			"Float" ->
				return floatToByteArray(value as Float)
			else ->
				throw Errors.Parse("Unexpected type: ${T::class.simpleName}")
		}
	}
}
