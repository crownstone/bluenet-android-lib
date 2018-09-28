package rocks.crownstone.bluenet.scanparsing

import android.bluetooth.le.ScanResult
import android.util.Log
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion


// Class that contains scan data of a single ble device.
// This class:
// - Parses advertisement data (ScanRecord)
// - Keeps up address, name, rssi, iBeacon, Crownstone service data
class ScannedDevice(result: ScanResult) {
	private val TAG = this::class.java.canonicalName

	val scanResult = result
	var hasServiceData = false // True when there is service data (even if it's invalid data)
//	val serviceData = CrownstoneServiceData()
	var serviceData: CrownstoneServiceData? = null
	var ibeaconData: IbeaconData? = null
	val address: DeviceAddress = result.device?.address!! // Can be null?
	val name = result.device?.name // Can be null
	val rssi = result.rssi
	var operationMode = OperationMode.UNKNOWN
	var validated = false



	/**
	 * Parses the crownstone service data header.
	 */
	fun parseServiceDataHeader() {
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
	fun parseServiceData(key: ByteArray?) {
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
	fun parseIbeacon() {
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
	fun parseDfu() {
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