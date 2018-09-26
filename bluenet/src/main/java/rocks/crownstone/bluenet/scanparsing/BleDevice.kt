package rocks.crownstone.bluenet.scanparsing

import android.bluetooth.le.ScanResult
import android.util.Log
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion


// Class that contains scan data of a single ble device.
// This class:
// - Parses advertisement data (ScanRecord)
// - Keeps up address, name, rssi, iBeacon, Crownstone service data
class BleDevice(result: ScanResult) {
	private val TAG = this::class.java.canonicalName

	val scanResult = result
	var serviceData = CrownstoneServiceData()
	var ibeaconData: IbeaconData? = null
	val address: DeviceAddress = result.device?.address!! // Can be null?
	val name = result.device?.name // Can be null
	val rssi = result.rssi
	var operationMode = OperationMode.UNKNOWN



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
				Log.v(TAG, "serviceData uuid=$uuid data=$dataStr")
				if (data != null) {
					if (serviceData.parseHeader(uuid.uuid, data)) {
						operationMode = serviceData.operationMode
					}

				}
			}
		}
	}

	/**
	 * Parses the crownstone service data.
	 */
	fun parseServiceData(key: ByteArray?) {
		if (!serviceData.headerParsedSuccess && !serviceData.headerParseFailed) {
			parseServiceDataHeader()
		}
		serviceData.parse(key)
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
				Log.v(TAG, "manufacturerData id=$manufacturerId data=$dataStr")
				if (manufacturerId == BluenetProtocol.APPLE_COMPANY_ID && data != null) {
					ibeaconData = Ibeacon.parse(data)
				}
			}
		}
	}

	override fun toString(): String {
		return "${scanResult.device.address} ${scanResult.device.name}   ServiceData: $serviceData   Ibeacon: $ibeaconData"
	}
}