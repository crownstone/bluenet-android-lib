package rocks.crownstone.bluenet

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


//data class CrownstoneServiceData(
//		var version: Int = 0,
//		var type: ServiceDataType = ServiceDataType.UNKNOWN,
//		var serviceUuid: Int = 0,
//		var deviceType: DeviceType = DeviceType.UNKNOWN,
//		)

enum class ServiceDataType {
	UNKNOWN,
	V1,
	V3,
	V5,
	STATE,
	ERROR,
	EXT_STATE,
	EXT_ERROR,
	SETUP,
}

// Class that parses the crownstone service data.
// This class:
// - Parses the header, to determine device type, and mode
// - Decrypts and parses the data
class CrownstoneServiceData {
	private val TAG = this::class.java.canonicalName

	private var headerParsed = false
	private var headerParseFailed = false

	private lateinit var byteBuffer: ByteBuffer // Cache byte buffer so we can continue parsing after header is done.

	// Parsed data
	internal var version = 0
	internal var type = ServiceDataType.UNKNOWN
	internal var serviceUuid: UUID? = null
	internal var deviceType = DeviceType.UNKNOWN


	// Parses first bytes to determine the advertisement type and device type, without decrypting the data.
	// bytes should start with service UUID
	// Returns null when parsing was unsuccessful
	fun parseHeader(uuid: UUID, bytes: ByteArray): Boolean {
		serviceUuid = uuid
		if (_parseHeader(bytes)) {
			headerParsed = true
			return true
		}
		headerParseFailed = true
		return false
	}

	fun parse(uuid: UUID, bytes: ByteArray): Boolean {
		if (headerParseFailed) {
			return false
		}
		if (!headerParsed) {
			val result = parseHeader(uuid, bytes)
			if (!result) {
				return false
			}
		}
		return parseRemaining()
	}


	private fun _parseHeader(bytes: ByteArray): Boolean {
		if (bytes.size < 3) {
			return false
		}
//		byteBuffer
		byteBuffer = ByteBuffer.wrap(bytes)
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

//		serviceUuid = Conversion.toUint16(bb.getShort().toInt())
		version = Conversion.toUint8(byteBuffer.get())
		when (version) {
			1 -> return parseHeaderDataV1(byteBuffer)
			3 -> return parseHeaderDataV3(byteBuffer)
			4 -> return parseHeaderDataV4(byteBuffer)
			5 -> return parseHeaderDataV5(byteBuffer)
			6 -> return parseHeaderDataV6(byteBuffer)
			else -> return false
		}
	}

	private fun parseRemaining(): Boolean {
		when (version) {
			1-> return parseRemainingV1(byteBuffer)
			3-> return parseRemainingV3(byteBuffer)
			4-> return parseRemainingV4(byteBuffer)
			5-> return parseRemainingV5(byteBuffer)
			6-> return parseRemainingV6(byteBuffer)
			else -> return false
		}
	}


	private fun parseHeaderDataV1(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		type = ServiceDataType.V1
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV3(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		type = ServiceDataType.V3
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV4(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		type = ServiceDataType.SETUP
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV5(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 17) {
			return false
		}
		deviceType = DeviceType.fromInt(Conversion.toUint8(bb.get()))
		type = ServiceDataType.V5
		return true
	}

	private fun parseHeaderDataV6(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 17) {
			return false
		}
		deviceType = DeviceType.fromInt(Conversion.toUint8(bb.get()))
		type = ServiceDataType.SETUP
		return true
	}

	private fun setDeviceTypeFromServiceUuid() {
		val uuid = serviceUuid // Make immutable
		when (uuid) {
			null -> deviceType = DeviceType.UNKNOWN
			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG -> deviceType = DeviceType.CROWNSTONE_PLUG
			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN -> deviceType = DeviceType.CROWNSTONE_BUILTIN
			BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE -> deviceType = DeviceType.GUIDESTONE
			else -> deviceType = DeviceType.UNKNOWN
		}
	}

	private fun parseRemainingV1(bb: ByteBuffer): Boolean {
		return true
	}

	private fun parseRemainingV3(bb: ByteBuffer): Boolean {
		return true
	}

	private fun parseRemainingV4(bb: ByteBuffer): Boolean {
		return true
	}

	private fun parseRemainingV5(bb: ByteBuffer): Boolean {
		return true
	}

	private fun parseRemainingV6(bb: ByteBuffer): Boolean {
		return true
	}

	override fun toString(): String {
		return "version=$version, type=${type.name}, serviceUuid=$serviceUuid, deviceType=${deviceType.name}"
	}


}