package rocks.crownstone.bluenet.scanparsing

import android.util.Log
import rocks.crownstone.bluenet.OperationMode

/**
 * Class to validate a ble device.
 *
 * It does this by comparing fields in the service data that should remain constant.
 */
class Validator {
	companion object {
		val TAG = this::class.java.canonicalName
		const val THRESHOLD = 3 // Validated once 3 scans with different data have similar constant fields
		const val CROWNSTONE_ID_INIT = -1 // Init with an invalid value
		const val CHANGING_DATA_INIT = -1 // Init with an invalid value
	}

	var lastCrownstoneId = CROWNSTONE_ID_INIT
	var lastChangingData = CHANGING_DATA_INIT
	var lastOperationMode = OperationMode.UNKNOWN
	var validCount = 0
	var isValidated = false

	/**
	 * Validate a device.
	 *
	 * @param device The device to validate.
	 *
	 * @return true when validated
	 */
	fun validate(device: ScannedDevice): Boolean {
		Log.v(TAG, "validate ${device.address} operationMode=${device.operationMode.name}")
		when (device.operationMode) {
			OperationMode.UNKNOWN -> {
				isValidated = false
			}

			OperationMode.SETUP,
			OperationMode.DFU -> {
				isValidated = true
			}

			OperationMode.NORMAL -> {
				if (lastOperationMode != OperationMode.NORMAL) {
					// Invalidate after mode change
					Log.v(TAG, "invalidate")
					lastCrownstoneId = CROWNSTONE_ID_INIT
					lastChangingData = CHANGING_DATA_INIT
					validCount = 0
					isValidated = false
				}

				Log.v(TAG, "lastId=$lastCrownstoneId curId=${device.serviceData.crownstoneId}   lastData=$lastChangingData curData=${device.serviceData.changingData}")
				if (lastCrownstoneId != CROWNSTONE_ID_INIT && lastChangingData != CHANGING_DATA_INIT) {
					// Can't validate with service data of external stone, or service data that was the same as previous.
					if (device.serviceData.flagExternalData || device.serviceData.changingData == lastChangingData) {
						// Keep last values as they were.
						Log.v(TAG, "ignored")
						return isValidated
					}

					// Compare crownstone id with previous crownstone id
					if (device.serviceData.crownstoneId == lastCrownstoneId && device.serviceData.validation) {
						if (!isValidated) {
							Log.v(TAG, "validCount=${validCount+1}")
							if (++validCount >= THRESHOLD) {
								Log.v(TAG, "validated!")
								isValidated = true
							}
						}
					}
					else {
						// Reset count and invalidate
						validCount = 0
						isValidated = false
					}
				}
				lastCrownstoneId = device.serviceData.crownstoneId
				lastChangingData = device.serviceData.changingData
			}
		}
		lastOperationMode = device.operationMode
		return isValidated
	}
}