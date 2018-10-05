package rocks.crownstone.bluenet

import android.util.Log
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import java.util.*

data class NearestDeviceListEntry(val deviceAddress: DeviceAddress, var rssi: Int, var lastSeenTime: Long)

class NearestDeviceList {
	companion object {
		const val TAG = "ScannedDevicesList"
		const val TIMEOUT_MS = 20000
		const val RSSI_LOWEST = -1000
	}
	private val map = HashMap<DeviceAddress, NearestDeviceListEntry>()

	private var nearest = NearestDeviceListEntry("", RSSI_LOWEST, 0)

	fun update(device: ScannedDevice) {
		Log.v(TAG, "update ${device.address} ${device.rssi}")
		val now = Date()
		val entry = NearestDeviceListEntry(device.address, device.rssi, now.time)
		map[device.address] = entry
		if (entry.rssi > nearest.rssi) {
			nearest = entry
		}
		else if (now.time - nearest.lastSeenTime > TIMEOUT_MS) {
			// Need to recalculate when the nearest is timed out
			calcNearest(now)
		}
	}

	fun remove(device: ScannedDevice) {
		Log.v(TAG, "remove ${device.address}")
		val entry = map.remove(device.address)
		if (entry != null && entry.deviceAddress == nearest.deviceAddress) {
			// Need to recalculate when the nearest is removed
			calcNearest(Date())
		}
	}

	fun getNearest(): NearestDeviceListEntry? {
		if (nearest.deviceAddress == "") {
			return null
		}
		return nearest
	}

	private fun calcNearest(now: Date) {
		Log.d(TAG, "calcNearest")
//		val now = Date()
		// Remove old items
//		map.entries.removeIf()
		val it = map.entries.iterator()
		while (it.hasNext()) {
			val entry = it.next().value
			if (now.time - entry.lastSeenTime > TIMEOUT_MS) {
				Log.d(TAG, "remove $entry")
				it.remove()
			}
		}

		// Calc nearest
		nearest = NearestDeviceListEntry("", RSSI_LOWEST, 0)
		for (entry in map.values) {
			if (entry.rssi > nearest.rssi) {
				nearest = entry
			}
		}
		Log.d(TAG, "nearest=$nearest")
	}
}