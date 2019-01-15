/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanhandling

import android.os.SystemClock
import android.util.Log
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.DeviceAddress
import rocks.crownstone.bluenet.structs.OperationMode
import java.util.*

data class NearestDeviceListEntry(
		val deviceAddress: DeviceAddress,
		val rssi: Int,
		val lastSeenTime: Long,
		val isStone: Boolean,
		val validated: Boolean,
		val operationMode: OperationMode,
		val name: String
)

/**
 * Class that keeps up the nearest device.
 *
 * Devices will be added by the update() function.
 * Devices will be removed by timeout or be remove().
 */
class NearestDeviceList {
	val TAG = this.javaClass.simpleName
	companion object {
		const val TIMEOUT_MS = 20000
		const val RSSI_LOWEST = -1000
	}
	private val map = HashMap<DeviceAddress, NearestDeviceListEntry>()

	private var nearest = NearestDeviceListEntry("", RSSI_LOWEST, 0, false, false, OperationMode.UNKNOWN, "")

	@Synchronized internal fun update(device: ScannedDevice) {
		Log.v(TAG, "update ${device.address} ${device.rssi}")
		val now = SystemClock.elapsedRealtime()
		val entry = NearestDeviceListEntry(device.address, device.rssi, now, device.isStone(), device.validated, device.operationMode, device.name)
		map[device.address] = entry
		if (entry.rssi > nearest.rssi) {
			nearest = entry
		}
		else if (now - nearest.lastSeenTime > TIMEOUT_MS) {
			// Need to recalculate when the nearest is timed out
			calcNearest(now)
		}
	}

	@Synchronized internal fun remove(device: ScannedDevice) {
		Log.v(TAG, "remove ${device.address}")
		val entry = map.remove(device.address)
		if (entry != null && entry.deviceAddress == nearest.deviceAddress) {
			// Need to recalculate when the nearest is removed
			calcNearest(SystemClock.elapsedRealtime())
		}
	}

	@Synchronized fun getNearest(): NearestDeviceListEntry? {
		if (nearest.deviceAddress == "") {
			return null
		}
		return nearest
	}

	@Synchronized private fun calcNearest(now: Long) {
		Log.d(TAG, "calcNearest")
		// Remove old items
//		map.entries.removeIf()
		val it = map.entries.iterator()
		while (it.hasNext()) {
			val entry = it.next().value
			if (now - entry.lastSeenTime > TIMEOUT_MS) {
				Log.d(TAG, "remove $entry")
				it.remove()
			}
		}

		// Calc nearest
		nearest = NearestDeviceListEntry("", RSSI_LOWEST, 0, false, false, OperationMode.UNKNOWN, "")
		for (entry in map.values) {
			if (entry.rssi > nearest.rssi) {
				nearest = entry
			}
		}
		Log.d(TAG, "nearest=$nearest")
	}
}