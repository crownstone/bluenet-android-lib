package rocks.crownstone.bluenet.scanparsing

import rocks.crownstone.bluenet.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.DeviceType
import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.scanparsing.servicedata.V1
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


//data class CrownstoneServiceData(
//		var version: Int = 0,
//		var type: ServiceDataType = ServiceDataType.UNKNOWN,
//		var serviceUuid: Int = 0,
//		var deviceType: DeviceType = DeviceType.UNKNOWN,
//		)

//enum class ServiceDataType {
//	UNKNOWN,
//	V1,
//	V3,
//	V5,
//	STATE,
//	ERROR,
//	EXT_STATE,
//	EXT_ERROR,
//	SETUP,
//}

// Class that parses the crownstone service data.
// This class:
// - Parses the header, to determine device type, and mode
// - Decrypts and parses the data
class CrownstoneServiceData {
	private val TAG = this::class.java.canonicalName

	private var headerParsedSuccess = false // Whether parsing of the header was successful (correct data format)
	private var headerParseFailed = false
	private var parsedSuccess = false       // Whether parsing of the data was successful (correct data format)
	private var parseFailed = false

	private lateinit var byteBuffer: ByteBuffer // Cache byte buffer so we can continue parsing after header is done.

	// Parsed data
	internal var version = 0
	internal var type = 0
	internal var serviceUuid: UUID? = null
	internal var deviceType = DeviceType.UNKNOWN
	internal var operationMode = OperationMode.UNKNOWN

	internal var crownstoneId = 0
	internal var switchState = 0
//	internal var relay = false
//	internal var dimmer = 0
	internal var temperature : Byte = 0
	internal var powerUsageReal = 0.0
	internal var powerUsageApparent = 0.0
	internal var powerFactor = 1.0
	internal var energyUsed = 0L
	internal var externalRssi = 0
	internal var timestamp = 0L
	internal var validation = false
	internal var changingData = 0

	// Flags
	internal var flagExternalData = false
	internal var flagSetup = false
	internal var flagDimmingAvailable = false
	internal var flagDimmable = false
	internal var flagError = false
	internal var flagSwitchLocked = false
	internal var flagTimeSet = false
	internal var flagSwitchCraft = false

	// Errors
	internal var errorOverCurrent = false
	internal var errorOverCurrentDimmer = false
	internal var errorChipTemperature = false
	internal var errorDimmerTemperature = false
	internal var errorDimmerFailureOn = false
	internal var errorDimmerFailureOff = false
	internal var errorTimestamp = 0L

	/**
	 * Parses first bytes to determine the advertisement type, device type, and operation mode, without decrypting the data.
	 * Result is cached.
	 *
	 * @return true when header data format is correct
	 */
	fun parseHeader(uuid: UUID, data: ByteArray): Boolean {
		if (headerParsedSuccess) {
			return true
		}
		if (headerParseFailed) {
			return false
		}
		serviceUuid = uuid
		if (_parseHeader(data)) {
			headerParsedSuccess = true
			return true
		}
		headerParseFailed = true
		return false
	}

	/**
	 * Parses the service data.
	 * Result is cached.
	 *
	 * @param uuid Service UUID of the service data.
	 * @param data The service data.
	 * @param key  When not null, this key is used to decrypt the service data.
	 *
	 * @return true when data format and size is correct
	 */
	fun parse(uuid: UUID, data: ByteArray, key: ByteArray?): Boolean {
		if (parsedSuccess) {
			return true
		}
		if (parseFailed) {
			return false
		}
		if (!parseHeader(uuid, data)) {
			return false
		}
		if (parseRemaining(key)) {
			parsedSuccess = true
			return true
		}
		parseFailed = true
		return false
	}

	fun isStone(): Boolean {
		return deviceType != DeviceType.UNKNOWN
	}

	private fun _parseHeader(bytes: ByteArray): Boolean {
		if (bytes.size < 3) {
			return false
		}
		byteBuffer = ByteBuffer.wrap(bytes)
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

		version = Conversion.toUint8(byteBuffer.get())
		when (version) {
			1 -> return V1.parseHeader(byteBuffer, this)
			3 -> return parseHeaderDataV3(byteBuffer)
			4 -> return parseHeaderDataV4(byteBuffer)
			5 -> return parseHeaderDataV5(byteBuffer)
			6 -> return parseHeaderDataV6(byteBuffer)
			else -> return false
		}
	}

	private fun parseRemaining(key: ByteArray?): Boolean {
		when (version) {
			1-> return V1.parse(byteBuffer, this, key)
			3-> return parseRemainingV3(byteBuffer)
			4-> return parseRemainingV4(byteBuffer)
			5-> return parseRemainingV5(byteBuffer)
			6-> return parseRemainingV6(byteBuffer)
			else -> return false
		}
	}

	private fun parseHeaderDataV3(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV4(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		setDeviceTypeFromServiceUuid()
		return true
	}

	private fun parseHeaderDataV5(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 17) {
			return false
		}
		deviceType = DeviceType.fromInt(Conversion.toUint8(bb.get()))
		return true
	}

	private fun parseHeaderDataV6(bb: ByteBuffer): Boolean {
		if (bb.remaining() < 17) {
			return false
		}
		deviceType = DeviceType.fromInt(Conversion.toUint8(bb.get()))
		return true
	}

	internal fun setDeviceTypeFromServiceUuid() {
		val uuid = serviceUuid // Make immutable
		when (uuid) {
			null -> deviceType = DeviceType.UNKNOWN
			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG -> deviceType = DeviceType.CROWNSTONE_PLUG
			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN -> deviceType = DeviceType.CROWNSTONE_BUILTIN
			BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE -> deviceType = DeviceType.GUIDESTONE
			else -> deviceType = DeviceType.UNKNOWN
		}
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
		return "version=$version, type=${type}, serviceUuid=$serviceUuid, deviceType=${deviceType.name}"
	}


}