package rocks.crownstone.bluenet

import android.bluetooth.le.ScanResult
import android.util.Log


// Class that contains scan data of a single ble device.
// This class:
// - Parses advertisement data (ScanRecord)
// - Keeps up address, name, rssi, iBeacon, Crownstone service data
class BleDevice(result: ScanResult) {
	private val TAG = this::class.java.canonicalName

	val scanResult = result
	var serviceData = CrownstoneServiceData()
	var ibeaconData: IbeaconData? = null
	val address = result.device?.address // Can be null?
	val name = result.device?.name // Can be null
	val rssi = result.rssi

	// Should be called only once
	fun parseHeader() {
		parseIbeacon()
		parseServiceData()
	}

	// Should be called only once
	// Can be called after parseHeader()
	fun parse() {
		if (ibeaconData == null) {
			parseIbeacon()
		}
		parseServiceData()
	}

	private fun parseServiceDataHeader() {

		val scanRecord = scanResult.scanRecord
		val rawServiceData = scanRecord?.serviceData
		if (rawServiceData != null) {
			for (uuid in rawServiceData.keys) {
				val data = rawServiceData[uuid]
				val dataStr = Conversion.bytesToString(data)
				Log.v(TAG, "serviceData uuid=$uuid data=$dataStr")
				if (data != null) {
					serviceData.parseHeader(uuid.uuid, data)
				}
			}
		}

//		val serviceUuids = scanRecord?.serviceUuids
//		if (serviceUuids != null) {
//			for (uuid in serviceUuids) {
//				val data = result.scanRecord?.getServiceData(uuid)
//				val dataStr = Conversion.bytesToString(data)
//				Log.v(TAG, "service uuid: $uuid data=$dataStr")
//			}
//		}
	}


	private fun parseServiceData() {
		val scanRecord = scanResult.scanRecord
		val rawServiceData = scanRecord?.serviceData
		if (rawServiceData != null) {
			for (uuid in rawServiceData.keys) {
				val data = rawServiceData[uuid]
				val dataStr = Conversion.bytesToString(data)
				Log.v(TAG, "serviceData uuid=$uuid data=$dataStr")
				if (data != null) {
					serviceData.parse(uuid.uuid, data)
				}
			}
		}
	}

	private fun parseIbeacon() {
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