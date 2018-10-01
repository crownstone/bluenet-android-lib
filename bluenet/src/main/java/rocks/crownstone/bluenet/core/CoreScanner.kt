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
	private var scanFilters = ArrayList<ScanFilter>()
	private var scanSettings: ScanSettings
	private var scanning = false

	init {
		// Init scan settings
		val builder = ScanSettings.Builder()
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


	private val scanCallback = object: ScanCallback() {
		@Synchronized override fun onScanResult(callbackType: Int, result: ScanResult?) {

			// Sometimes a scan result is still received after scanning has been stopped.
			// Sometimes a scan with invalid rssi is received, ignore this result.
			if (!scanning || result == null || result.rssi >= 0) {
				return
			}

			eventBus.emit(BluenetEvent.SCAN_RESULT_RAW, result)
		}

		@Synchronized override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			if (results != null) {
				for (result in results) {
					onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
				}
			}
		}

		@Synchronized override fun onScanFailed(errorCode: Int) {
			Log.e(TAG, "onScanFailed: $errorCode")
			if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
				// No problem!
				return
			}
			scanning = false
			eventBus.emit(BluenetEvent.SCAN_FAILURE)
		}
	}
}