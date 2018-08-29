package rocks.crownstone.bluenet

import android.bluetooth.le.ScanResult
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


// Class that contains scan data of a single ble device.
// This class:
// - Parses advertisement data (ScanRecord)
// - ..
class BleDevice(result: ScanResult) {
	private val TAG = this::class.java.canonicalName

	var serviceData = CrownstoneServiceData()
	var ibeaconData: IbeaconData? = null

	init {
		parse(result)
	}

	fun parse(result: ScanResult) {

		val scanRecord = result.scanRecord
		val rawServiceData = scanRecord?.serviceData
		if (rawServiceData != null) {
			for (uuid in rawServiceData.keys) {
				val data = rawServiceData[uuid]
				val dataStr = Conversion.bytesToString(data)
				Log.d(TAG, "serviceData uuid=$uuid data=$dataStr")
				if (data != null) {
					serviceData.parseHeaderBytes(data)
				}
			}
		}

		val serviceUuids = scanRecord?.serviceUuids
		if (serviceUuids != null) {
			for (uuid in serviceUuids) {
				val data = result.scanRecord?.getServiceData(uuid)
				val dataStr = Conversion.bytesToString(data)
				Log.d(TAG, "service uuid: $uuid data=$dataStr")
			}
		}

		val manufacturerData = scanRecord?.manufacturerSpecificData
		if (manufacturerData != null) {
			for (i in 0 until manufacturerData.size()) {
				val manufacturerId = manufacturerData.keyAt(i)
				val data = manufacturerData[manufacturerId]
				val dataStr = Conversion.bytesToString(data)
				Log.d(TAG, "manufacturerData id=$manufacturerId data=$dataStr")
				if (data != null) {
					ibeaconData = Ibeacon.parse(data)
				}
			}
		}
	}


}