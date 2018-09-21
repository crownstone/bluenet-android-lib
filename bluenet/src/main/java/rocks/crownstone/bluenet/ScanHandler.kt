package rocks.crownstone.bluenet

import android.bluetooth.le.ScanResult
import android.util.Log

class ScanHandler(evtBus: EventBus) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus

	init {
		eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW, { result: Any -> onRawScan(result as ScanResult) })
	}

	private fun onRawScan(result: ScanResult) {
		Log.d(TAG, "onScan")

		val device = BleDevice(result)
		eventBus.emit(BluenetEvent.SCAN_RESULT, device)


//		val scanRecord = result.scanRecord
//		if (scanRecord == null) {
//			return
//		}
//		val serviceData = scanRecord.serviceData
//		for (uuid in serviceData.keys) {
//			val data = serviceData.get(uuid)
//			val dataStr = Conversion.bytesToString(data)
//			Log.d(TAG, "serviceData uuid=$uuid data=$dataStr")
//		}
//
//		val uuids = result.scanRecord?.serviceUuids
//		if (uuids != null) {
//			for (uuid in uuids) {
//				val data = result.scanRecord?.getServiceData(uuid)
//				val dataStr = Conversion.bytesToString(data)
//				Log.d(TAG, "service uuid: $uuid data=$dataStr")
//			}
//		}
//
//		val manufacturerData = result.scanRecord?.manufacturerSpecificData
//		if (manufacturerData != null) {
//			for (i in 0 until manufacturerData.size()) {
//				val manufacturerId = manufacturerData.keyAt(i)
//				val data = manufacturerData[manufacturerId]
//				val dataStr = Conversion.bytesToString(data)
//				Log.d(TAG, "manufacturerData id=$manufacturerId data=$dataStr")
//			}
//		}
	}
}