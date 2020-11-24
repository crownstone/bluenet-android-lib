/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanhandling

import android.bluetooth.le.ScanResult
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.BluenetEvent
import rocks.crownstone.bluenet.structs.DeviceAddress
import rocks.crownstone.bluenet.structs.OperationMode
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

/**
 * Class that parses scans.
 *
 * Parses iBeacon data.
 * Parses and validates Crownstone service data. Also checks if service data is unique.
 * Parses dfu data.
 *
 * Uses ScannedDevice for parsing.
 * Uses Encryption manager for decryption.
 * Uses Validator for validation.
 *
 * Emits SCAN_RESULT.
 */
class ScanHandler(evtBus: EventBus, encryptionMngr: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val encryptionManager = encryptionMngr
	private val validators = HashMap<DeviceAddress, Validator>() // TODO: grows over time
	private val lastServiceDataMap = HashMap<DeviceAddress, CrownstoneServiceData>() // TODO: grows over time

	init {
		Log.i(TAG, "init")
		eventBus.subscribe(BluenetEvent.SCAN_RESULT_RAW, { data: Any? -> onRawScan(data as ScanResult) })
	}

	@Synchronized
	private fun onRawScan(result: ScanResult) {
		Log.v(TAG, "onRawScan")

		if (result.device.address == null) {
			Log.e(TAG, "Device without address")
			return
		}
		val device = ScannedDevice(result)
		device.parseIbeacon()
		device.parseServiceDataHeader()
		device.parseDfu()

		encryptionManager.cacheSphereId(device)

		// After parsing service data header and dfu, the operation mode should be known.
		// But only if the scan has service data at all..
		when (device.operationMode) {
				OperationMode.NORMAL -> {
					device.sphereId = encryptionManager.getSphereId(device)
					val keySet = encryptionManager.getKeySet(device)
					if (keySet != null) {
						device.parseServiceData(keySet)
					}
				}
				OperationMode.SETUP -> {
					device.parseServiceData(null)
				}
				OperationMode.DFU -> {
					// Nothing?
				}
				else -> {
					// Nothing?
				}
		}

		// Validate or invalidate device
		// TODO: do we really have to check if serviceData != null ?
		val serviceData = device.serviceData
		if (serviceData != null || device.operationMode == OperationMode.DFU) {
			val validator = getValidator(device)
			if (validator.validate(device)) {
				device.validated = true
			}
		}

		if (serviceData != null) {
			serviceData.checkUnique(lastServiceDataMap[device.address])
			lastServiceDataMap[device.address] = serviceData
		}

		eventBus.emit(BluenetEvent.SCAN_RESULT, device)
		if (device.validated) {
			eventBus.emit(BluenetEvent.SCAN_RESULT_VALIDATED, device)
			val unique = serviceData?.unique ?: false
			if (unique) {
				eventBus.emit(BluenetEvent.SCAN_RESULT_VALIDATED_UNIQUE, device)
			}
		}
	}

	@Synchronized
	private fun getValidator(device: ScannedDevice): Validator {
		var validator: Validator?
		validator = validators.get(device.address)
		if (validator == null) {
			validator = Validator()
			validators.put(device.address, validator)
		}
		return validator
	}
}
