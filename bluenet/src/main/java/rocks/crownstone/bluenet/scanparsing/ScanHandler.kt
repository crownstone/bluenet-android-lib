package rocks.crownstone.bluenet.scanparsing

import android.bluetooth.le.ScanResult
import android.util.Log
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.EncryptionManager

class ScanHandler(evtBus: EventBus, encryptionMngr: EncryptionManager) {
	private val TAG = this::class.java.canonicalName
	private val eventBus = evtBus
	private val encryptionManager = encryptionMngr

	init {
		eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW, { result: Any -> onRawScan(result as ScanResult) })
	}

	private fun onRawScan(result: ScanResult) {
		Log.d(TAG, "onScan")

		if (result.device.address == null) {
			Log.w(TAG, "Device without address")
			return
		}
		val device = BleDevice(result)
		device.parseIbeacon()
		device.parseServiceDataHeader()
		if (device.serviceData.isStone()) {
			when (device.serviceData.operationMode) {
				OperationMode.NORMAL -> {
					val key = encryptionManager.getKeySet(device)?.getGuestKey()
					if (key != null) {
						device.parseServiceData(key)
					}
				}
				OperationMode.SETUP -> {
					device.parseServiceData(null)
				}
				OperationMode.DFU -> {
					// TODO
				}
				else -> {
					// TODO
				}
			}
		}
		
		// TODO: validation

		eventBus.emit(BluenetEvent.SCAN_RESULT, device)
	}
}