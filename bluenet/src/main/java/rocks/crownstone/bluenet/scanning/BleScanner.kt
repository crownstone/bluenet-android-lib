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
import rocks.crownstone.bluenet.structs.ScanStartFailure
import java.util.*

/**
 * Class that provides the following:
 * - When scanning, stopping now and then, to avoid bluetoothLeScanner stop giving results after 5 or 30 minutes. See:
 *     - https://stackoverflow.com/questions/43833904/android-bluetooth-le-scanner-stops-after-a-time
 *     - https://android.googlesource.com/platform/packages/apps/Bluetooth/+/1fdc7c138db776b02bc751fd7a80c519ea3324d1
 *     - https://android.googlesource.com/platform/packages/apps/Bluetooth/+/623b906dcd0e4c0b85db20c67df76b3bb2884e74
 * - Making sure startScan is not called too often within a short time. See:
 *     - https://stackoverflow.com/questions/45681711/app-is-scanning-too-frequently-with-scansettings-scan-mode-opportunistic
 *     - https://android.googlesource.com/platform/packages/apps/Bluetooth/+/1fdc7c138db776b02bc751fd7a80c519ea3324d1
 * - Set scan filters, via the filterManager.
 * - Set scan interval.
 */
class BleScanner(eventBus: EventBus, bleCore: BleCore, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val core = bleCore

	private val handler = Handler(looper) // Own handler, as we call removeCallbacksAndMessages(null)
	val filterManager = ScanFilterManager(::onScanFilterUpdate)

	// True when currently interval-scanning.
	private var scanning = false

	// True when currently not interval-scanning, but it was (or, should be).
	private var wasScanning = false
	private var scanPause: Long = 100
	private var scanDuration: Long  = 120 * 1000 // Restart every 2 minutes
	private val lastStartTimes = LinkedList<Long>()

	private var startIntervalRunnable: Runnable
	private var stopIntervalRunnable: Runnable
	private var restartRunnable: Runnable

	init {
		Log.i(TAG, "init")

		val subIdScanFail = eventBus.subscribe(BluenetEvent.SCAN_FAILURE, { data: Any? -> onScanFail(data as ScanStartFailure) })
		eventBus.subscribe(BluenetEvent.CORE_SCANNER_READY,               { data: Any? -> onCoreScannerReady() })
		eventBus.subscribe(BluenetEvent.CORE_SCANNER_NOT_READY,           { data: Any? -> onCoreScannerNotReady() })

		// Had to init those here, or silly kotlin had some recursion problem
		startIntervalRunnable = Runnable { startInterval() }
		stopIntervalRunnable = Runnable { stopInterval() }
		restartRunnable = Runnable { restart() }
	}

//	@Synchronized
	fun startScan(delay: Long = 0) {
		Log.i(TAG, "startScan delay=$delay scanning=$scanning")
		synchronized(this) {
			handler.removeCallbacks(restartRunnable)
			Log.d(TAG, "  scanning=$scanning")
			if (!scanning) {
				scanning = true
				handler.removeCallbacksAndMessages(null)
				handler.postDelayed(startIntervalRunnable, delay)
			}
		}
	}

//	@Synchronized
	fun stopScan() {
		Log.i(TAG, "stopScan scanning=$scanning")
		synchronized(this) {
			Log.d(TAG, "  scanning=$scanning")
			handler.removeCallbacksAndMessages(null)
			wasScanning = false
			if (scanning) {
				scanning = false
				core.stopScan()
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
			Log.d(TAG, "restart scanning=$scanning")
			handler.removeCallbacks(restartRunnable)
			if (scanning) {
				val delay = delayMsToPreventStartingTooOften()
				if (delay > 0) {
					// Don't stop until we can start again.
					Log.w(TAG, "delay restart for $delay ms")
					handler.postDelayed(restartRunnable, delay)
					return
				}
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
			val delay = delayMsToPreventStartingTooOften()
			if (delay > 0) {
				// We're starting too often, delay this start.
				Log.w(TAG, "delay start for $delay ms")
				handler.removeCallbacksAndMessages(null)
				handler.postDelayed(startIntervalRunnable, delay)
				return
			}

			// Keep up list of last start times.
			lastStartTimes.addLast(now())
			while (lastStartTimes.size > BluenetConfig.SCAN_CHECK_NUM_PER_PERIOD) {
				lastStartTimes.removeFirst()
			}
			val result = core.startScan()
			if (!result) {
				Log.i(TAG, "Failed to start scan, mark scanning=false and wasScanning=true")
				scanning = false
				wasScanning = true
				return
			}
			if (scanDuration > 0) {
				handler.postDelayed(stopIntervalRunnable, scanDuration)
			}
		}
	}

//	@Synchronized
	private fun stopInterval() {
		Log.i(TAG, "stopInterval")
		synchronized(this) {
			core.stopScan()
			if (scanPause > 0) {
				handler.postDelayed(startIntervalRunnable, scanPause)
			}
		}
	}

//	@Synchronized
	private fun onCoreScannerReady() {
		Log.i(TAG, "onCoreScannerReady wasScanning=$wasScanning")
		synchronized(this) {
			Log.d(TAG, "  wasScanning=$wasScanning")
			if (wasScanning) {
				startScan()
			}
		}
	}

//	@Synchronized
	private fun onCoreScannerNotReady() {
		Log.i(TAG, "onCoreScannerNotReady scanning=$scanning")
		synchronized(this) {
			Log.d(TAG, "  scanning=$scanning")
			if (scanning) {
				stopScan()
				Log.d(TAG, "  mark as wasScanning=true")
				wasScanning = true
			}
		}
	}

//	@Synchronized
	private fun onScanFail(error: ScanStartFailure) {
		Log.e(TAG, "onScanFail: TODO")
		synchronized(this) {
			// 2019-11-19 don't do anything, let the app tell the user to reset bluetooth instead.
		}
	}

	private fun delayMsToPreventStartingTooOften(): Long {
		val now = now()
		if ((lastStartTimes.size >= BluenetConfig.SCAN_CHECK_NUM_PER_PERIOD) && (now - lastStartTimes.first < BluenetConfig.SCAN_CHECK_PERIOD)) {
			return BluenetConfig.SCAN_CHECK_PERIOD + lastStartTimes.first - now
		}
		return 0
	}

	private fun now(): Long {
		return SystemClock.elapsedRealtime() // See https://developer.android.com/reference/android/os/SystemClock
	}
}
