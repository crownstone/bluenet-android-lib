package rocks.crownstone.bluenet

import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.util.Log

class BleScanner(evtBus: EventBus, bleCore: BleCore) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus
	private val core = bleCore
	private val handler = Handler() // TODO: with different thread?

//	private var scanning = false
	private var running = false
	private var scanPause: Long = 100
	private var scanDuration: Long  = 60000

	private var startScanRunnable: Runnable
	private var stopScanRunnable: Runnable

	init {
//		val onScan = { result: Any -> onScan(result as ScanResult) }
//		val subId = eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW.name, onScan)
		val subId = eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW.name, { result: Any -> onScan(result as ScanResult) })

		// Had to init those here, or silly kotlin had some recursion problem
		startScanRunnable = Runnable {
			startInterval()
		}
		stopScanRunnable = Runnable {
			stopInterval()
		}
	}

	fun startScan() : Boolean {
		Log.i(TAG, "startScan")
		if (!running) {
			running = true
			handler.removeCallbacks(null)
			handler.post(startScanRunnable)
		}
		return true
	}

	fun stopScan() : Boolean {
		Log.i(TAG, "stopScan")
		if (running) {
			running = false
			core.stopScan()
			handler.removeCallbacks(null)
		}
		return true
	}

	private fun startInterval() {
		Log.i(TAG, "startScanRunnable")
		if (core.startScan()) {
//			scanning = true

			if (scanPause > 0) {
				handler.postDelayed(stopScanRunnable, scanDuration)
			}
		}
	}

	private fun stopInterval() {
		Log.i(TAG, "stopScanRunnable")
		if (core.stopScan()) {
//			scanning = true

			if (scanPause > 0) {
				handler.postDelayed(startScanRunnable, scanPause)
			}
		}
	}

	fun onScan(result: ScanResult) {
		Log.i(TAG, "onScan")


		if (result.device == null || result.device.name == null) {
			return
		}

		if (result.device.name.equals("bart") == false) {
			return
		}


		if (result.scanRecord == null) {
			return
		}

		val data = result.scanRecord.bytes
		Log.d(TAG, Conversion.bytesToString(data))




		Log.d(TAG, result.toString())

//		val c: Class<*>
//		c = Class.forName("android.bluetooth.le.ScanRecord")
//		val m = c.getMethod("parseFromBytes", *arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType))
//		val o = m.invoke(null, arrayOf(data))
//		Log.d(TAG, o)


		val serviceData = result.scanRecord.serviceData
		for (uuid in serviceData.keys) {
			val data = serviceData.get(uuid)
			val dataStr = Conversion.bytesToString(data)
			Log.d(TAG, "serviceData uuid=$uuid data=$dataStr")
		}

		val uuids = result.scanRecord?.serviceUuids
		if (uuids != null) {
			for (uuid in uuids) {
				val data = result.scanRecord?.getServiceData(uuid)
				val dataStr = Conversion.bytesToString(data)
				Log.d(TAG, "service uuid: $uuid data=$dataStr")
//				when (uuid) {
//				}
			}
		}



		val manufacturerData = result.scanRecord?.manufacturerSpecificData
		if (manufacturerData != null) {
			for (i in 0 until manufacturerData.size()) {
				val manufacturerId = manufacturerData.keyAt(i)
				val data = manufacturerData[manufacturerId]
				val dataStr = Conversion.bytesToString(data)
				Log.d(TAG, "manufacturerData id=$manufacturerId data=$dataStr")
			}
		}


	}

}