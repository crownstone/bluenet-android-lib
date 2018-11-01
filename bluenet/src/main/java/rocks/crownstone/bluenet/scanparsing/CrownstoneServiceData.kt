package rocks.crownstone.bluenet.scanparsing

import android.util.Log
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.scanparsing.servicedata.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


// Class that parses the crownstone service data.
// This class:
// - Parses the header, to determine device type, and mode
// - Decrypts and parses the data
class CrownstoneServiceData {
	private val TAG = this.javaClass.simpleName

	internal var headerParsedSuccess = false // Whether parsing of the header was successful (correct data format)
	internal var headerParseFailed = false
	internal var parsedSuccess = false       // Whether parsing of the data was successful (correct data format)
	internal var parseFailed = false

	private lateinit var byteBuffer: ByteBuffer // Cache byte buffer so we can continue parsing after header is done.

	// Header
	internal var version = 0
	var type = 0; internal set
	internal var serviceUuid: UUID? = null
	var deviceType = DeviceType.UNKNOWN; internal set
	internal var operationMode = OperationMode.UNKNOWN

	// State
	var crownstoneId: Uint8 = 0; internal set
	var switchState: Uint8 = 0; internal set
	var temperature : Byte = 0; internal set
	var powerUsageReal = 0.0; internal set
	var powerUsageApparent = 0.0; internal set
	var powerFactor = 1.0; internal set
	var energyUsed = 0L; internal set
	var externalRssi: Int8 = 0; internal set
	var timestamp = 0L; internal set
	internal var validation = false
	internal var changingData = 0

	// Flags
	var flagExternalData = false;     internal set
	var flagSetup = false;            internal set
	var flagDimmingAvailable = false; internal set
	var flagDimmable = false;         internal set
	var flagError = false;            internal set
	var flagSwitchLocked = false;     internal set
	var flagTimeSet = false;          internal set
	var flagSwitchCraft = false;      internal set

	// Errors
	var errorOverCurrent = false;       internal set
	var errorOverCurrentDimmer = false; internal set
	var errorChipTemperature = false;   internal set
	var errorDimmerTemperature = false; internal set
	var errorDimmerFailureOn = false;   internal set
	var errorDimmerFailureOff = false;  internal set
	var errorTimestamp = 0L;            internal set

	/**
	 * Parses first bytes to determine the advertisement type, device type, and operation mode, without decrypting the data.
	 * Result is cached.
	 *
	 * @return true when header data format is correct
	 */
	internal fun parseHeader(uuid: UUID, data: ByteArray): Boolean {
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
	 * Parses the remaining service data, after header has been parsed.
	 * Result is cached.
	 *
	 * @param key  When not null, this key is used to decrypt the service data.
	 *
	 * @return true when data format and size is correct
	 */
	internal fun parse(key: ByteArray?): Boolean {
		if (parsedSuccess) {
			return true
		}
		if (parseFailed) {
			return false
		}
		if (parseRemaining(key)) {
			parsedSuccess = true
			return true
		}
		parseFailed = true
		return false
	}

	internal fun isStone(): Boolean {
		return deviceType != DeviceType.UNKNOWN
	}

	private fun _parseHeader(bytes: ByteArray): Boolean {
		if (bytes.size < 3) {
			return false
		}
		byteBuffer = ByteBuffer.wrap(bytes)
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

		version = Conversion.toUint8(byteBuffer.get()).toInt()
		when (version) {
			1 -> return V1.parseHeader(byteBuffer, this)
			3 -> return V3.parseHeader(byteBuffer, this)
			4 -> return V4.parseHeader(byteBuffer, this)
			5 -> return V5.parseHeader(byteBuffer, this)
			6 -> return V6.parseHeader(byteBuffer, this)
			else -> return false
		}
	}

	private fun parseRemaining(key: ByteArray?): Boolean {
		Log.v(TAG, "parseRemaining version=$version remaining=${byteBuffer.remaining()}")
		when (version) {
			1-> return V1.parse(byteBuffer, this, key)
			3-> return V3.parse(byteBuffer, this, key)
			4-> return V4.parse(byteBuffer, this, key)
			5-> return V5.parse(byteBuffer, this, key)
			6-> return V6.parse(byteBuffer, this, key)
			else -> {
				Log.v(TAG, "invalid version")
				return false
			}
		}
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

	override fun toString(): String {
		return "version=$version type=${type} serviceUuid=$serviceUuid deviceType=${deviceType.name} id=$crownstoneId switchState=$switchState temperature=$temperature powerUsageReal=$powerUsageReal powerUsageApparent=$powerUsageApparent powerFactor=$powerFactor energyUsed=$energyUsed externalRssi=$externalRssi timestamp=$timestamp validation=$validation changingData=$changingData"
	}
}