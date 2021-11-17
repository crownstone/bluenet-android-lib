/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanhandling

import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.BluenetEvent
import rocks.crownstone.bluenet.structs.OperationMode

/**
 * Class that emits the nearest devices.
 *
 * Emits nearest:
 * - validated device (any mode).
 * - validated device in normal mode.
 * - validated device in setup mode.
 * - validated device in dfu mode.
 *
 * Uses NearestDeviceList to keep up the nearest.
 */
class NearestDevices(eventBus: EventBus) {
	val TAG = this.javaClass.simpleName
	private val eventBus = eventBus

	internal val nearestStone = NearestDeviceList()
	internal val nearestUnvalidated = NearestDeviceList()
	internal val nearestValidated = NearestDeviceList()
	internal val nearestValidatedNormal = NearestDeviceList()
	internal val nearestValidatedDfu = NearestDeviceList()
	internal val nearestValidatedSetup = NearestDeviceList()

	init {
		eventBus.subscribe(BluenetEvent.SCAN_RESULT, { data: Any? -> onScan(data as ScannedDevice) })
	}

	private fun onScan(device: ScannedDevice) {
		if (!device.isStone()) {
			return
		}
		nearestStone.update(device)
		updateAndEmit(device, nearestStone, BluenetEvent.NEAREST_STONE)
		if (!device.validated) {
			nearestValidated.remove(device)
			nearestValidatedNormal.remove(device)
			nearestValidatedDfu.remove(device)
			nearestValidatedSetup.remove(device)
			updateAndEmit(device, nearestUnvalidated, BluenetEvent.NEAREST_UNVALIDATED)
			return
		}
		nearestUnvalidated.remove(device)
		updateAndEmit(device, nearestValidated, BluenetEvent.NEAREST_VALIDATED)
		when (device.operationMode) {
			OperationMode.NORMAL -> {
				nearestValidatedSetup.remove(device)
				nearestValidatedDfu.remove(device)
				updateAndEmit(device, nearestValidatedNormal, BluenetEvent.NEAREST_VALIDATED_NORMAL)
			}
			OperationMode.SETUP -> {
				nearestValidatedNormal.remove(device)
				nearestValidatedDfu.remove(device)
				updateAndEmit(device, nearestValidatedSetup, BluenetEvent.NEAREST_SETUP)
			}
			OperationMode.DFU -> {
				nearestValidatedNormal.remove(device)
				nearestValidatedSetup.remove(device)
				updateAndEmit(device, nearestValidatedDfu, BluenetEvent.NEAREST_DFU)
			}
			OperationMode.UNKNOWN -> {}
		}
	}

	private fun updateAndEmit(device: ScannedDevice, list: NearestDeviceList, event: BluenetEvent) {
		list.update(device)
		val nearest = list.getNearest()
		if (nearest != null) {
			eventBus.emit(event, nearest)
		}
	}
}