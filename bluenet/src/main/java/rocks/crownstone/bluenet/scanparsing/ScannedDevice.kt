/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanparsing

import android.bluetooth.le.ScanResult
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log


// Class that contains scan data of a single ble device.
// This class:
// - Parses advertisement data (ScanRecord)
// - Keeps up address, name, rssi, iBeacon, service data, etc
class ScannedDevice(result: ScanResult) {
	private val TAG = this.javaClass.simpleName

	internal val scanResult = result
	internal var hasServiceData = false            // True when there is service data (even if it's invalid data)
//	val serviceData = CrownstoneServiceData()
	var serviceData: CrownstoneServiceData? = null; internal set  // The service data.
	var ibeaconData: IbeaconData? = null; internal set            // The iBeacon data.
	val address: DeviceAddress = result.device?.address!!         // Address of the device.
	val name = result.device?.name ?: ""                          // Name of the device.
	val rssi = result.rssi                                        // RSSI of the scan.
	var operationMode = OperationMode.UNKNOWN; internal set       // Operation mode of the device.
	var validated = false; internal set                           // Whether or not this is a validated device.
	var sphereId: String? = null; internal set                    // The sphere ID of this device.

	/**
	 * Check if device is a crownstone.
	 *
	 * @return True when the device is a crownstone.
	 */
	fun isStone(): Boolean {
		return operationMode != OperationMode.UNKNOWN
//		// But what about stones in DFU mode?
//		val deviceType = serviceData?.deviceType ?: DeviceType.UNKNOWN
//		return deviceType != DeviceType.UNKNOWN
	}


	/**
	 * Parses the crownstone service data header.
	 */
	internal fun parseServiceDataHeader() {
		val scanRecord = scanResult.scanRecord
		val rawServiceData = scanRecord?.serviceData
		if (rawServiceData != null) {
			for (uuid in rawServiceData.keys) {
				if (uuid.uuid != BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG &&
						uuid.uuid != BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN &&
						uuid.uuid != BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE) {
					break
				}
				val data = rawServiceData[uuid]
				val dataStr = Conversion.bytesToString(data)
				Log.v(TAG, "parseServiceDataHeader: serviceDataUuid=$uuid data=$dataStr")
				if (data != null) {
					if (_getServiceData().parseHeader(uuid.uuid, data)) {
						operationMode = _getServiceData().operationMode
					}

				}
			}
		}
	}

	/**
	 * Parses the crownstone service data.
	 */
	internal fun parseServiceData(key: ByteArray?) {
		Log.v(TAG, "parseServiceData")
		if (!_getServiceData().headerParsedSuccess && !_getServiceData().headerParseFailed) {
			parseServiceDataHeader()
		}
		_getServiceData().parse(key)
	}

	/**
	 * Parses iBeacon data.
	 * Should be called only once.
	 */
	internal fun parseIbeacon() {
		val scanRecord = scanResult.scanRecord
		val manufacturerData = scanRecord?.manufacturerSpecificData
		if (manufacturerData != null) {
			for (i in 0 until manufacturerData.size()) {
				val manufacturerId = manufacturerData.keyAt(i)
				val data = manufacturerData[manufacturerId]
				val dataStr = Conversion.bytesToString(data)
				Log.v(TAG, "parseIbeacon: manufacturerId=$manufacturerId data=$dataStr")
				if (manufacturerId == BluenetProtocol.APPLE_COMPANY_ID && data != null) {
					ibeaconData = Ibeacon.parse(data)
					return
				}
			}
		}
	}

	/**
	 * Parses dfu data.
	 */
	internal fun parseDfu() {
		val scanRecord = scanResult.scanRecord
		val serviceUuids = scanRecord?.serviceUuids
		if (serviceUuids != null) {
			for (uuid in serviceUuids) {
				val data = scanRecord.getServiceData(uuid)
				val dataStr = Conversion.bytesToString(data)
				Log.v(TAG, "parseDfu: serviceUuid=$uuid data=$dataStr")
				if (uuid.uuid == BluenetProtocol.SERVICE_DATA_UUID_DFU) {
					operationMode = OperationMode.DFU
					return
				}
			}
		}
	}

	private fun _getServiceData(): CrownstoneServiceData {
		var tempServiceData = serviceData
		if (tempServiceData == null) {
			tempServiceData = CrownstoneServiceData()
			serviceData = tempServiceData
			hasServiceData = true
		}
		return tempServiceData
	}

	override fun toString(): String {
		return "$address name=$name rssi=$rssi operationMode=${operationMode.name} validated=$validated serviceData=[$serviceData] ibeacon=[$ibeaconData]"
	}
}