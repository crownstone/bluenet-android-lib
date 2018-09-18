package rocks.crownstone.bluenet

import android.os.ParcelUuid
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

	// Parsed data
	internal var version = 0
	internal var type = ServiceDataType.UNKNOWN
	internal var serviceUuid: UUID? = null
	internal var deviceType = DeviceType.UNKNOWN


	// Parses first bytes to determine the advertisement type and device type, without decrypting the data.
	// bytes should start with service UUID
	// Returns null when parsing was unsuccessful
	fun parseHeaderBytes(uuid: UUID, bytes: ByteArray) : Boolean {
		serviceUuid = uuid
		if (_parseHeaderBytes(bytes)) {
			headerParsed = true
			return true
		}
		return false
	}


	private fun _parseHeaderBytes(bytes: ByteArray) : Boolean {
		if (bytes.size < 3) {
			return false
		}
		val bb = ByteBuffer.wrap(bytes)
		bb.order(ByteOrder.LITTLE_ENDIAN)

//		serviceUuid = Conversion.toUint16(bb.getShort().toInt())
		version = Conversion.toUint8(bb.get())
		when (version) {
			1 -> return parseHeaderDataV1(bb)
			3 -> return parseHeaderDataV3(bb)
			4 -> return parseHeaderDataV4(bb)
			5 -> return parseHeaderDataV5(bb)
			6 -> return parseHeaderDataV6(bb)
			else -> return false
		}
	}


	private fun parseHeaderDataV1(bb: ByteBuffer) : Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		type = ServiceDataType.V1
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV3(bb: ByteBuffer) : Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		type = ServiceDataType.V3
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV4(bb: ByteBuffer) : Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		type = ServiceDataType.SETUP
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV5(bb: ByteBuffer) : Boolean {
		if (bb.remaining() < 17) {
			return false
		}
		deviceType = DeviceType.fromInt(Conversion.toUint8(bb.get()))
		type = ServiceDataType.V5
		return true
	}

	private fun parseHeaderDataV6(bb: ByteBuffer) : Boolean {
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

	override fun toString(): String {
		return "version=$version, type=${type.name}, serviceUuid=$serviceUuid, deviceType=${deviceType.name}"
	}


}