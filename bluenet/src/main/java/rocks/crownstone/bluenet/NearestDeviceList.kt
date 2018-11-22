package rocks.crownstone.bluenet

import android.os.SystemClock
import android.util.Log
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import java.util.*

data class NearestDeviceListEntry(val deviceAddress: DeviceAddress, var rssi: Int, var lastSeenTime: Long)

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

	private var nearest = NearestDeviceListEntry("", RSSI_LOWEST, 0)

	@Synchronized internal fun update(device: ScannedDevice) {
		Log.v(TAG, "update ${device.address} ${device.rssi}")
		val now = SystemClock.elapsedRealtime()
		val entry = NearestDeviceListEntry(device.address, device.rssi, now)
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
		nearest = NearestDeviceListEntry("", RSSI_LOWEST, 0)
		for (entry in map.values) {
			if (entry.rssi > nearest.rssi) {
				nearest = entry
			}
		}
		Log.d(TAG, "nearest=$nearest")
	}
}