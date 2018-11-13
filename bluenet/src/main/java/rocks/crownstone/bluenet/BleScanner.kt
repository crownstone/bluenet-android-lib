package rocks.crownstone.bluenet

import android.bluetooth.le.ScanFilter
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
	val filterManager = ScanFilterManager(::onScanFilterUpdate)

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

	@Synchronized fun startScan(delay: Long = 0) {
		Log.i(TAG, "startScan")
		if (!running) {
			running = true
			handler.removeCallbacksAndMessages(null)
			handler.postDelayed(startScanRunnable, delay)
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

	@Synchronized private fun onScanFilterUpdate(filters: List<ScanFilter>) {
		Log.i(TAG, "onScanFilterUpdate: $filters")
		// TODO: delay?
		// TODO: return a promise
		val wasRunning = running
		stopScan()
		core.setScanFilters(filters)
		if (wasRunning) {
			startScan(500)
		}
	}

	@Synchronized private fun startInterval() {
		Log.i(TAG, "startInterval")
		// TODO: check if we don't start too often in a certain amount of time.
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
		Log.e(TAG, "onScanFail: TODO")
		// TODO
	}

}