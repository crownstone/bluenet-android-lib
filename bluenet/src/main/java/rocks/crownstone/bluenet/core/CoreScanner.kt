/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.core

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import rocks.crownstone.bluenet.structs.BluenetEvent
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.structs.ScanMode

/**
 * Class that adds scanning to the bluetooth LE core class.
 */
open class CoreScanner(appContext: Context, evtBus: EventBus, looper: Looper) : CoreConnection(appContext, evtBus, looper) {
	private var scanFilters: List<ScanFilter> = ArrayList()
	private var scanSettingsBuilder = ScanSettings.Builder()
	private var scanSettings: ScanSettings
	private val lockScanConfig = Any()

//	private var scanning = false


	init {
		// Init scan settings
		val builder = scanSettingsBuilder
		builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
		//builder.setScanResultType(SCAN_RESULT_TYPE_FULL)
		builder.setReportDelay(0)
		if (Build.VERSION.SDK_INT >= 23) {
			builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
			builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
			builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
		}
		if (Build.VERSION.SDK_INT >= 26) {
			builder.setLegacy(true)
		}
		scanSettings = builder.build()
	}

//	@Synchronized
	fun startScan() {
		Log.i(TAG, "startScan")
		synchronized(this) {
			if (!initScanner()) {
				return
			}
			if (!isScannerReady()) {
				return
			}
		}
		synchronized(lockScanConfig) {
			scanner.startScan(scanFilters, scanSettings, scanCallback)
		}
//		synchronized(this) {
//			scanning = true
//		}
	}

//	@Synchronized
	fun stopScan() {
		Log.i(TAG, "stopScan")
		synchronized(this) {
			if (!initScanner()) {
				return
			}
//			scanning = false
			if (!isScannerReady()) {
				return
			}
			scanner.stopScan(scanCallback)
		}
	}

	/**
	 * Change the scan mode used to scan for devices. See [ScanSettings] for the different scan modes.
	 * You need to stop and start scanning again for this to take effect.
	 *
	 * @return True when mode was changed.
	 */
//	@Synchronized
	fun setScanMode(mode: ScanMode): Boolean {
		Log.i(TAG, "setScanMode $mode")
//		synchronized(this) {
		synchronized(lockScanConfig) {
			if (scanSettings.scanMode == mode.num) {
				return false
			}
			scanSettingsBuilder.setScanMode(mode.num)
			scanSettings = scanSettingsBuilder.build()
			return true
		}
//		}
	}

	/**
	 * Set new scan filters.
	 * You need to stop and start scanning again for this to take effect.
	 */
//	@Synchronized
	fun setScanFilters(filters: List<ScanFilter>) {
		Log.i(TAG, "setScanFilters $filters")
//		synchronized(this) {
		synchronized(lockScanConfig) {
			scanFilters = filters
		}
//		}
	}

//	@Synchronized
	private fun onBleScanResult(callbackType: Int, result: ScanResult?) {
		Log.v(TAG, "onBleScanResult $result")
		synchronized(this) {
			// Sometimes a scan result is still received after scanning has been stopped.
			// Sometimes a scan with invalid rssi is received, ignore this result.
//			if (!scanning || result == null || result.rssi >= 0) {
			if (result == null || result.rssi >= 0) {
				return
			}
			eventBus.emit(BluenetEvent.SCAN_RESULT_RAW, result)
		}
	}

//	@Synchronized
	private fun onBleBatchScanResults(results: MutableList<ScanResult>?) {
		synchronized(this) {
			if (results != null) {
				for (result in results) {
					onBleScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
				}
			}
		}
	}

//	@Synchronized
	private fun onBleScanFailed(errorCode: Int) {
		Log.e(TAG, "onScanFailed: $errorCode")
		synchronized(this) {
			if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
				// No problem!
				return
			}
//			scanning = false
			eventBus.emit(BluenetEvent.SCAN_FAILURE)
		}
	}

	private val scanCallback = object: ScanCallback() {
		// These are called from a different thread.
		override fun onScanResult(callbackType: Int, result: ScanResult?) {
			Log.v(TAG, "onScanResult $result")
			handler.post {
				onBleScanResult(callbackType, result)
			}
		}
		override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			handler.post {
				onBleBatchScanResults(results)
			}
		}
		override fun onScanFailed(errorCode: Int) {
			handler.post {
				onBleScanFailed(errorCode)
			}
		}
	}
}