/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanparsing

import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.scanparsing.servicedata.*
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.getUint8
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
	var version = ServiceDataVersion.UNKNOWN; internal set
	var type = ServiceDataType.UNKNOWN; internal set
	internal var serviceUuid: UUID? = null
	var deviceType = DeviceType.UNKNOWN; internal set
	internal var operationMode = OperationMode.UNKNOWN

	// State
	var crownstoneId: Uint8 = 0U; internal set
	var switchState = SwitchState(0U); internal set
	var temperature : Byte = 0; internal set             // Chip temperature in °C.
	var powerUsageReal = 0.0; internal set               // Real power usage in W.
	var powerUsageApparent = 0.0; internal set           // Apparent power usage in VA.
	var powerFactor = 1.0; internal set                  // Power factor.
	var energyUsed = 0L; internal set                    // Energy used in Joule.
	var externalRssi: Int8 = 0; internal set             // RSSI to external crownstone, only valid when flagExternalData is true.
	var timestamp = 0L; internal set                     // POSIX local time. Only valid when flagTimeSet is true.
	var count: Uint16 = 0U; internal set                  // Sequence number of the service data. Only valid when flagTimeSet is false. Will overflow.
	var changingData = 0; internal set                   // Data that always changes for each new service data. Is a partial timestamp when time is set, counter when time is not set, or random value for old firmware.
	internal var validation = false

	// Flags
	var flagExternalData = false;        internal set    // True when this service data is from another crownstone.
	internal var flagSetup = false
	var flagDimmingAvailable = false;    internal set    // True when dimming is available (hardware).
	var flagDimmable = false;            internal set    // True when dimming is allowed via config.
	var flagError = false;               internal set    // True when there is an error.
	var flagSwitchLocked = false;        internal set    // True when the switch is locked via config.
	var flagTimeSet = false;             internal set    // True when the time is set.
	var flagSwitchCraft = false;         internal set    // True when switchcraft is enabled via config.
	var flagTapToToggleEnabled = false;  internal set    // True when tap to toggle is enabled.
	var flagBehaviourOverridden = false; internal set    // True when behaviour is overridden.
	var flagBehaviourEnabled = false;    internal set    // True when behaviour is enabled.

	// Errors
	var errorOverCurrent = false;       internal set     // Too much current went through the crownstone.
	var errorOverCurrentDimmer = false; internal set     // Too much current went through the dimmer.
	var errorChipTemperature = false;   internal set     // The chip temperature was too high.
	var errorDimmerTemperature = false; internal set     // The dimmer temperature was too high.
	var errorDimmerFailureOn = false;   internal set     // The dimmer is always (partially) on.
	var errorDimmerFailureOff = false;  internal set     // The dimmer is always (partially) off.
	var errorTimestamp: Uint32 = 0U;    internal set     // Timestamp of the first error.

	var unique = false; internal set // Whether this service data was different from the previous.

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
	internal fun parse(keySet: KeySet?): Boolean {
		if (parsedSuccess) {
			return true
		}
		if (parseFailed) {
			return false
		}
		if (parseRemaining(keySet)) {
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

		version = ServiceDataVersion.fromNum(Conversion.toUint8(byteBuffer.get()))
		when (version) {
			ServiceDataVersion.V1 -> return V1.parseHeader(byteBuffer, this)
			ServiceDataVersion.V3 -> return V3.parseHeader(byteBuffer, this)
			ServiceDataVersion.V4 -> return V4.parseHeader(byteBuffer, this)
			ServiceDataVersion.V5 -> return V5.parseHeader(byteBuffer, this)
			ServiceDataVersion.V6 -> return V6.parseHeader(byteBuffer, this)
			ServiceDataVersion.V7 -> return V5.parseHeader(byteBuffer, this)
			else -> return false
		}
	}

	private fun parseRemaining(keySet: KeySet?): Boolean {
		Log.v(TAG, "parseRemaining version=$version remaining=${byteBuffer.remaining()}")
		when (version) {
			ServiceDataVersion.V1-> return V1.parse(byteBuffer, this, keySet?.getKey(AccessLevel.GUEST))
			ServiceDataVersion.V3-> return V3.parse(byteBuffer, this, keySet?.getKey(AccessLevel.GUEST))
			ServiceDataVersion.V4-> return V4.parse(byteBuffer, this, keySet?.getKey(AccessLevel.GUEST))
			ServiceDataVersion.V5-> return V5.parse(byteBuffer, this, keySet?.getKey(AccessLevel.GUEST))
			ServiceDataVersion.V6-> return V6.parse(byteBuffer, this, keySet?.getKey(AccessLevel.GUEST))
			ServiceDataVersion.V7-> return V5.parse(byteBuffer, this, keySet?.getKey(AccessLevel.SERVICE_DATA))
			else -> {
				Log.v(TAG, "invalid version")
				return false
			}
		}
	}

	internal fun setDeviceTypeFromServiceUuid() {
		val uuid = serviceUuid // Make immutable
		deviceType = when (uuid) {
			null -> DeviceType.UNKNOWN
			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG -> DeviceType.CROWNSTONE_PLUG
			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN -> DeviceType.CROWNSTONE_BUILTIN
			BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE -> DeviceType.GUIDESTONE
			else -> DeviceType.UNKNOWN
		}
	}

	internal fun checkUnique(previous: CrownstoneServiceData?) {
		if (previous == null) {
			unique = true
			return
		}
		unique = (previous.changingData != changingData)
	}

	override fun toString(): String {
		return "version=$version type=${type} serviceUuid=$serviceUuid deviceType=${deviceType.name} id=$crownstoneId switchState=$switchState temperature=$temperature powerUsageReal=$powerUsageReal powerUsageApparent=$powerUsageApparent powerFactor=$powerFactor energyUsed=$energyUsed externalRssi=$externalRssi timestamp=$timestamp validation=$validation changingData=$changingData unique=$unique"
	}
}
