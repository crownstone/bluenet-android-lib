package rocks.crownstone.bluenet.core

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import rocks.crownstone.bluenet.BluenetEvent
import rocks.crownstone.bluenet.EventBus

/**
 * Class that adds scanning to the bluetooth LE core class.
 */
open class CoreScanner(appContext: Context, evtBus: EventBus) : CoreConnection(appContext, evtBus) {
	private var scanFilters: List<ScanFilter> = ArrayList()
	private var scanSettingsBuilder = ScanSettings.Builder()
	private var scanSettings: ScanSettings
	private var scanning = false

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

	@Synchronized fun startScan() {
		if (!initScanner()) {
			return
		}
		if (!isScannerReady()) {
			return
		}
		scanner.startScan(scanFilters, scanSettings, scanCallback)
		scanning = true
	}

	@Synchronized fun stopScan() {
		if (!initScanner()) {
			return
		}
		scanning = false
		if (!isScannerReady()) {
			return
		}
		scanner.stopScan(scanCallback)

	}

	enum class ScanMode(val num: Int) {
		LOW_POWER(0),
		BALANCED(1),
		LOW_LATENCY(2),
	}

	/**
	 * Change the scan mode used to scan for devices. See [ScanSettings] for the different scan modes.
	 * You need to stop and start scanning again for this to take effect.
	 */
	@Synchronized fun setScanMode(mode: ScanMode) {
		scanSettingsBuilder.setScanMode(mode.num)
		scanSettings = scanSettingsBuilder.build()
	}

	/**
	 * Set new scan filters.
	 * You need to stop and start scanning again for this to take effect.
	 */
	@Synchronized fun setScanFilters(filters: List<ScanFilter>) {
		scanFilters = filters
	}

	@Synchronized private fun onBleScanResult(callbackType: Int, result: ScanResult?) {
		// Sometimes a scan result is still received after scanning has been stopped.
		// Sometimes a scan with invalid rssi is received, ignore this result.
		if (!scanning || result == null || result.rssi >= 0) {
			return
		}

		eventBus.emit(BluenetEvent.SCAN_RESULT_RAW, result)
	}

	@Synchronized private fun onBleBatchScanResults(results: MutableList<ScanResult>?) {
		if (results != null) {
			for (result in results) {
				onBleScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
			}
		}
	}

	@Synchronized private fun onBleScanFailed(errorCode: Int) {
		Log.e(TAG, "onScanFailed: $errorCode")
		if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
			// No problem!
			return
		}
		scanning = false
		eventBus.emit(BluenetEvent.SCAN_FAILURE)
	}

	private val scanCallback = object: ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult?) {
			onBleScanResult(callbackType, result)
		}
		override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			onBleBatchScanResults(results)
		}
		override fun onScanFailed(errorCode: Int) {
			onBleScanFailed(errorCode)
		}
	}

	// Filters:
	// - scan mode - low level
	// - ibeacon uuid - low level
	// - service data header? - not available in every scan -> high level
	// - unique only - requires decryption -> high level
}