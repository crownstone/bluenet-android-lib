package rocks.crownstone.bluenet

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.util.Log

class BleCore(appContext: Context, evtBus: EventBus) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus
	private val context = appContext
	private lateinit var bleManager: BluetoothManager
	private lateinit var bleAdapter: BluetoothAdapter
	private lateinit var scanner: BluetoothLeScanner
	private lateinit var advertiser: BluetoothLeAdvertiser

	private var scanFilters = ArrayList<ScanFilter>()
	private var scanSettings: ScanSettings
	private var scanning = false

	init {
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


	fun init() {
		bleManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bleAdapter = bleManager.adapter
		scanner = bleAdapter.bluetoothLeScanner
		advertiser = bleAdapter.bluetoothLeAdvertiser
	}

	fun startScan() {
//		if (scanner == null) {
//			return false
//		}
		scanner.startScan(scanFilters, scanSettings, scanCallback)
		scanning = true
//		return true
	}

	fun stopScan() {
//		if (scanner == null) {
//			return false
//		}
		scanner.stopScan(scanCallback)
		scanning = false
//		return true
	}


	private val scanCallback = object: ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult?) {

			// Sometimes a scan result is still received after scanning has been stopped.
			// Sometimes a scan with invalid rssi is received, ignore this result.
			if (!scanning || result == null || result.rssi >= 0) {
				return
			}

			eventBus.emit(BluenetEvent.SCAN_RESULT_RAW, result)
		}

		override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			if (results != null) {
				for (result in results) {
					onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
				}
			}
		}

		override fun onScanFailed(errorCode: Int) {
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