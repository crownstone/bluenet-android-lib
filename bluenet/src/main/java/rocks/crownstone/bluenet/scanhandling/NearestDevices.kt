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
class NearestDevices(evtBus: EventBus) {
	val TAG = this.javaClass.simpleName
	private val eventBus = evtBus

	internal val nearestValidated = NearestDeviceList()
	internal val nearestNormal = NearestDeviceList()
	internal val nearestDfu = NearestDeviceList()
	internal val nearestSetup = NearestDeviceList()

	init {
		eventBus.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)
	}

	private fun onScan(data: Any) {
		val device = data as ScannedDevice
		if (!device.validated) {
			return
		}
		updateAndEmit(device, nearestValidated, BluenetEvent.NEAREST_VALIDATED)
		when (device.operationMode) {
			OperationMode.NORMAL -> {
				nearestSetup.remove(device)
				nearestDfu.remove(device)
				updateAndEmit(device, nearestNormal, BluenetEvent.NEAREST_VALIDATED_NORMAL)
			}
			OperationMode.SETUP -> {
				nearestNormal.remove(device)
				nearestDfu.remove(device)
				updateAndEmit(device, nearestSetup, BluenetEvent.NEAREST_SETUP)
			}
			OperationMode.DFU -> {
				nearestNormal.remove(device)
				nearestSetup.remove(device)
				updateAndEmit(device, nearestDfu, BluenetEvent.NEAREST_DFU)
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