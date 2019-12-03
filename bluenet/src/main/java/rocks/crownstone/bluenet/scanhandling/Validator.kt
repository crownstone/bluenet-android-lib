/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanhandling

import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.Int16
import rocks.crownstone.bluenet.structs.OperationMode
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.toInt16
import rocks.crownstone.bluenet.util.toInt8

/**
 * Class to validate a ble device.
 *
 * It does this by comparing fields in the service data that should remain constant.
 */
class Validator {
	val TAG = this.javaClass.simpleName
	companion object {
		const val THRESHOLD = 1 // Validated once 2 scans with different data have similar constant fields
		const val CROWNSTONE_ID_INIT: Int16 = -1 // Init with an invalid value
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
		Log.v(TAG, "validate ${device.address} operationMode=${device.operationMode.name} this=$this")
		when (device.operationMode) {
			OperationMode.UNKNOWN -> {
				isValidated = false
			}

			OperationMode.SETUP -> {
				isValidated = (device.serviceData?.parsedSuccess == true)
			}
			OperationMode.DFU -> {
				isValidated = true
			}

			OperationMode.NORMAL -> {
				validateNormalMode(device)
			}
		}
		lastOperationMode = device.operationMode
		return isValidated
	}

	private fun validateNormalMode(device: ScannedDevice) {
		if (lastOperationMode != OperationMode.NORMAL) {
			// Reset after mode change.
			Log.v(TAG, "mode change: invalidate")
			reset()
		}

		val serviceData = device.serviceData
		if (serviceData == null || !serviceData.parsedSuccess) {
			// Reset when not parsed, or parse failed.
			Log.v(TAG, "parse not successful: invalidate")
			reset()
			return
		}

		if (!serviceData.validation) {
			Log.v(TAG, "incorrect validation data: invalidate")
			reset()
			return
		}

		Log.v(TAG, "lastId=$lastCrownstoneId curId=${serviceData.crownstoneId}   lastData=$lastChangingData curData=${serviceData.changingData}")
		if (lastCrownstoneId != CROWNSTONE_ID_INIT && lastChangingData != CHANGING_DATA_INIT) {

			// Can't validate with service data of external stone, or service data that was the same as previous.
			if (serviceData.flagExternalData || serviceData.changingData == lastChangingData) {
				// Keep last values as they were.
				Log.v(TAG, "ignored")
				return
			}

			// Compare crownstone id with previous crownstone id
			if (serviceData.crownstoneId.toInt16() != lastCrownstoneId) {
				reset()
				return
			}

			Log.v(TAG, "validCount=${validCount + 1}")
			if (!isValidated && ++validCount >= THRESHOLD) {
				Log.v(TAG, "validated!")
				isValidated = true
			}
		}
		lastCrownstoneId = serviceData.crownstoneId.toInt16()
		lastChangingData = serviceData.changingData
	}

	private fun reset() {
		lastCrownstoneId = CROWNSTONE_ID_INIT
		lastChangingData = CHANGING_DATA_INIT
		validCount = 0
		isValidated = false
	}
}
