package rocks.crownstone.bluenet

import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/*
Class that provides the following:
- When scanning, stopping now and then, to avoid bluetoothLeScanner stop giving results after 30 minutes (see https://stackoverflow.com/questions/43833904/android-bluetooth-le-scanner-stops-after-a-time).
- (to do) Parsing advertisements and decrypting the service data.
- (to do) Keeping up a list of recently seen devices.
- (to do) Set scan filters based on iBeacon, Crownstone type or mode.
- (to do) Making sure startScan is not called too often within a short time (see https://stackoverflow.com/questions/45681711/app-is-scanning-too-frequently-with-scansettings-scan-mode-opportunistic).
- (to do) Emit events: unverified advertisement, verified advertisement,
 */


class BleScanner(evtBus: EventBus, bleCore: BleCore) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus
	private val core = bleCore

//	private val handler = Handler() // Can only be done when constructed on UI thread, do with different thread?
	private val handler: Handler

//	private var scanning = false
	private var running = false
	private var scanPause: Long = 100
	private var scanDuration: Long  = 60000

	private var startScanRunnable: Runnable
	private var stopScanRunnable: Runnable

	init {
		Log.i(TAG, "init")
		val handlerThread = HandlerThread("BleScanner")
		handlerThread.start()
		handler = Handler(handlerThread.looper)

//		val onScan = { result: Any -> onScan(result as ScanResult) }
//		val subId = eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW.name, onScan)
		val subIdScan     = eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW, { result: Any -> onScan(result as ScanResult) })
		val subIdScanFail = eventBus.subscribe(BluenetEvent.SCAN_FAILURE, { result: Any -> onScanFail() })

		// Had to init those here, or silly kotlin had some recursion problem
		startScanRunnable = Runnable {
			startInterval()
		}
		stopScanRunnable = Runnable {
			stopInterval()
		}
	}


	@Synchronized fun setFilter() {

	}

	@Synchronized fun startScan() {
		Log.i(TAG, "startScan")
		if (!running) {
			running = true
			handler.removeCallbacksAndMessages(null)
			handler.post(startScanRunnable)
		}
//		return true
	}

	@Synchronized fun stopScan() {
		Log.i(TAG, "stopScan")
		if (running) {
			running = false
			core.stopScan()
			handler.removeCallbacksAndMessages(null)
		}
//		return true
	}

	@Synchronized private fun startInterval() {
		Log.i(TAG, "startInterval")
//		if (core.startScan()) {
			core.startScan()
//			scanning = true
			if (scanPause > 0) {
				handler.postDelayed(stopScanRunnable, scanDuration)
			}
//		}
	}

	@Synchronized private fun stopInterval() {
		Log.i(TAG, "stopInterval")
//		if (core.stopScan()) {
			core.stopScan()
//			scanning = true

			if (scanPause > 0) {
				handler.postDelayed(startScanRunnable, scanPause)
			}
//		}
	}

	@Synchronized private fun onScan(result: ScanResult) {
		Log.i(TAG, "onScan")

		val device = BleDevice(result)
		eventBus.emit(BluenetEvent.SCAN_RESULT, device)


		return

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

		val scanRecord = result.scanRecord
		if (scanRecord == null) {
			return
		}
		val serviceData = scanRecord.serviceData
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

	@Synchronized private fun onScanFail() {
		// TODO
	}

}