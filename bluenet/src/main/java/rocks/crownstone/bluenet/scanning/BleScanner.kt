/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanning

import android.bluetooth.le.ScanFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.structs.BluenetEvent
import rocks.crownstone.bluenet.structs.ScanMode
import java.util.*

/**
 * Class that provides the following:
 * - When scanning, stopping now and then, to avoid bluetoothLeScanner stop giving results after 5 or 30 minutes. See:
 *     - https://stackoverflow.com/questions/43833904/android-bluetooth-le-scanner-stops-after-a-time
 *     - https://android.googlesource.com/platform/packages/apps/Bluetooth/+/1fdc7c138db776b02bc751fd7a80c519ea3324d1
 *     - https://android.googlesource.com/platform/packages/apps/Bluetooth/+/623b906dcd0e4c0b85db20c67df76b3bb2884e74
 * - (to do) Making sure startScan is not called too often within a short time. See:
 *     - https://stackoverflow.com/questions/45681711/app-is-scanning-too-frequently-with-scansettings-scan-mode-opportunistic
 *     - https://android.googlesource.com/platform/packages/apps/Bluetooth/+/1fdc7c138db776b02bc751fd7a80c519ea3324d1
 * - Set scan filters, via the filterManager.
 * - Set scan interval.
 */
class BleScanner(evtBus: EventBus, bleCore: BleCore, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val core = bleCore

	private val handler = Handler(looper) // Own handler, as we call removeCallbacksAndMessages(null)
	val filterManager = ScanFilterManager(::onScanFilterUpdate)

	// True when currently interval-scanning.
	private var running = false

	// True when currently not interval-scanning, but it was (or, should be).
	private var wasRunning = false
	private var scanPause: Long = 100
	private var scanDuration: Long  = 120 * 1000 // Restart every 2 minutes
	private val lastStartTimes = LinkedList<Long>()

	private var startScanRunnable: Runnable
	private var stopScanRunnable: Runnable

	init {
		Log.i(TAG, "init")

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

//	@Synchronized
	fun startScan(delay: Long = 0) {
		Log.i(TAG, "startScan delay=$delay")
		synchronized(this) {
			if (!running) {
				running = true
				handler.removeCallbacksAndMessages(null)
				handler.postDelayed(startScanRunnable, delay)
			}
		}
	}

//	@Synchronized
	fun stopScan() {
		Log.i(TAG, "stopScan")
		synchronized(this) {
			wasRunning = false
			if (running) {
				running = false
				core.stopScan()
				handler.removeCallbacksAndMessages(null)
			}
		}
	}

//	@Synchronized
	fun setScanInterval(mode: ScanMode) {
		Log.i(TAG, "setScanInterval $mode")
		synchronized(this) {
			val changed = core.setScanMode(mode)
			if (changed) {
				restart()
			}
		}
	}

//	@Synchronized
	private fun restart() {
		Log.i(TAG, "restart")
		synchronized(this) {
			val wasRunning = running
			Log.d(TAG, "restart wasRunning=$wasRunning")
			if (wasRunning) {
				stopScan()
				startScan(500)
			}
		}
	}

//	@Synchronized
	private fun onScanFilterUpdate(filters: List<ScanFilter>) {
		Log.i(TAG, "onScanFilterUpdate: $filters")
		synchronized(this) {
			core.setScanFilters(filters)
			restart()
		}
	}

//	@Synchronized
	private fun startInterval() {
		Log.i(TAG, "startInterval")
		synchronized(this) {
			val now = SystemClock.elapsedRealtime() // See https://developer.android.com/reference/android/os/SystemClock
			// Check if we start too often.
			if ((lastStartTimes.size >= BluenetConfig.SCAN_CHECK_NUM_PER_PERIOD) && (now - lastStartTimes.first < BluenetConfig.SCAN_CHECK_PERIOD)) {
				// We're starting too often, delay this start.
				handler.removeCallbacksAndMessages(null)
				Log.i(TAG, "delay for ${BluenetConfig.SCAN_CHECK_PERIOD + lastStartTimes.first - now} ms")
				handler.postDelayed(startScanRunnable, BluenetConfig.SCAN_CHECK_PERIOD + lastStartTimes.first - now)
//				startScan(BluenetConfig.SCAN_CHECK_PERIOD + lastStartTimes.first - now) // Won't work, as it checks for running
				return
			}
			// Keep up list of last start times.
			lastStartTimes.addLast(now)
			while (lastStartTimes.size > BluenetConfig.SCAN_CHECK_NUM_PER_PERIOD) {
				lastStartTimes.removeFirst()
			}
			core.startScan()
			if (scanDuration > 0) {
				handler.postDelayed(stopScanRunnable, scanDuration)
			}
		}
	}

//	@Synchronized
	private fun stopInterval() {
		Log.i(TAG, "stopInterval")
		synchronized(this) {
			core.stopScan()
			if (scanPause > 0) {
				handler.postDelayed(startScanRunnable, scanPause)
			}
		}
	}

//	@Synchronized
	private fun onCoreScannerReady(data: Any) {
		Log.i(TAG, "onCoreScannerReady")
		synchronized(this) {
			if (wasRunning) {
				startScan()
			}
		}
	}

//	@Synchronized
	private fun onCoreScannerNotReady(data: Any) {
		Log.i(TAG, "onCoreScannerNotReady")
		synchronized(this) {
			if (running) {
				stopScan()
				wasRunning = true
			}
		}
	}

//	@Synchronized
	private fun onScanFail() {
		Log.e(TAG, "onScanFail: TODO")
		synchronized(this) {
			// TODO
		}
	}

}