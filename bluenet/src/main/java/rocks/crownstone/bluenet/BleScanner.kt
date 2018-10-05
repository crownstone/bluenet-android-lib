package rocks.crownstone.bluenet

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
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val core = bleCore

//	private val handler = Handler() // Can only be done when constructed on UI thread, do with different thread?
	private val handler: Handler

//	private var scanning = false
	private var running = false
	private var wasRunning = false
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
		val subIdScanFail = eventBus.subscribe(BluenetEvent.SCAN_FAILURE, { result: Any -> onScanFail() })
		eventBus.subscribe(BluenetEvent.CORE_SCANNER_READY, ::onCoreScannerReady)
		eventBus.subscribe(BluenetEvent.CORE_SCANNER_NOT_READY, ::onCoreScannerNotReady)

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
		wasRunning = false
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

	@Synchronized private fun onCoreScannerReady(data: Any) {
		Log.i(TAG, "onCoreScannerReady")
		if (wasRunning) {
			startScan()
		}
	}

	@Synchronized private fun onCoreScannerNotReady(data: Any) {
		Log.i(TAG, "onCoreScannerNotReady")
		if (running) {
			stopScan()
			wasRunning = true
		}
	}

	@Synchronized private fun onScanFail() {
		// TODO
	}

}