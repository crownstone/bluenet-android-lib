package rocks.crownstone.bluenet.scanparsing

import android.os.Handler
import android.os.SystemClock
import android.util.Log
import rocks.crownstone.bluenet.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


/**
 * Class to simulate iBeacon ranging.
 * Once per second a list of of devices with averaged RSSI is sent.
 * Enter and exit region events are sent.
 */
class IbeaconRanger(val eventBus: EventBus, val handler: Handler) {
	private val TAG = this.javaClass.simpleName
	private val lastSeenRegion = HashMap<UUID, Long>()
	private val inRegion = HashSet<UUID>()
	private val deviceMap = HashMap<DeviceAddress, DeviceData>()
	private var isTimeoutRunning = false
	private val timeoutRunnable = Runnable { onTimeout() }
	private val tickRunnable = Runnable { onTick() }
	init {
		eventBus.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)
		handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
	}

	companion object {
		const val IBEACON_SCAN_INTERVAL_MS: Long = 1000
		const val TICK_INTERVAL_MS: Long = 1000
		const val REGION_TIMEOUT_MS: Long = 30000
	}

	internal data class DeviceData(val ibeaconData: IbeaconData, val averager: IbeaconRssiAverager)

	@Synchronized fun destroy() {
		handler.removeCallbacks(timeoutRunnable)
		handler.removeCallbacks(tickRunnable)
	}

	@Synchronized private fun onScan(data: Any) {
		val device = data as ScannedDevice

		// Keep up last seen per region (uuid), and check for enter region
		val ibeaconData = device.ibeaconData ?: return
		val currentTime = SystemClock.elapsedRealtime()
		lastSeenRegion[ibeaconData.uuid] = currentTime
		if (!inRegion.contains(ibeaconData.uuid)) {
			onEnterRegion(ibeaconData.uuid)
		}

		// Add scan to map and start timeout
		val deviceData = deviceMap.getOrPut(device.address, { DeviceData(ibeaconData, IbeaconRssiAverager()) })
		deviceData.averager.add(device.rssi)
		if (!isTimeoutRunning) {
			isTimeoutRunning = handler.postDelayed(timeoutRunnable, IBEACON_SCAN_INTERVAL_MS)
		}
	}

	@Synchronized private fun onTimeout() {
		Log.v(TAG, "onTimeout")
		val result = ArrayList<ScannedIbeacon>()
		for (entry in deviceMap) {
			Log.v(TAG, "    ${entry.key} uuid=${entry.value.ibeaconData.uuid} rssi=${entry.value.averager.getAverage()}")
			result.add(ScannedIbeacon(entry.key, entry.value.ibeaconData, entry.value.averager.getAverage()))
		}
		deviceMap.clear()
		isTimeoutRunning = false
		eventBus.emit(BluenetEvent.IBEACON_SCAN, result)
	}

	@Synchronized private fun onTick() {
		checkExitRegions()
		handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
	}

	@Synchronized private fun checkExitRegions() {
		val currentTime = SystemClock.elapsedRealtime()
		val pendingExits = ArrayList<UUID>()
		for (uuid in inRegion) {
			if (currentTime - lastSeenRegion.getOrElse(uuid, { 0L }) > REGION_TIMEOUT_MS) {
				pendingExits.add(uuid)
			}
		}
		for (uuid in pendingExits) {
			onExitRegion(uuid)
		}
	}

	@Synchronized private fun onEnterRegion(uuid: UUID) {
		Log.i(TAG, "onEnterRegion $uuid")
		inRegion.add(uuid)
		eventBus.emit(BluenetEvent.IBEACON_ENTER_REGION, uuid)
	}

	@Synchronized private fun onExitRegion(uuid: UUID) {
		Log.i(TAG, "onExitRegion $uuid")
		inRegion.remove(uuid)
		eventBus.emit(BluenetEvent.IBEACON_EXIT_REGION, uuid)
	}


}