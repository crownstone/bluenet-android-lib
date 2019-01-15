/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanhandling

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.SubscriptionId
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


/**
 * Class to simulate iBeacon ranging.
 *
 * Only iBeacons with a tracked iBeaconUUID will be handled.
 * Once per second a list of of devices with averaged RSSI is sent.
 * Enter and exit region events are sent.
 */
class IbeaconRanger(val eventBus: EventBus, looper: Looper) {
	private val TAG = this.javaClass.simpleName
	private val handler = Handler(looper)
	private val lastSeenRegion = HashMap<UUID, Long>()
	private val inRegion = HashSet<UUID>()
	private val deviceMap = HashMap<DeviceAddress, DeviceData>()
//	private val trackedUuids = HashSet<UUID>()
	private val trackedUuids = HashMap<UUID, String>()
	private var isRunning = false
	private var isTimeoutRunning = false
	private val timeoutRunnable = Runnable { onTimeout() }
	private val tickRunnable = Runnable { onTick() }
	private val subId: SubscriptionId

	init {
		subId = eventBus.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)
//		handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
	}

	companion object {
		const val IBEACON_SCAN_INTERVAL_MS: Long = 1000 // Every interval, the list of iBeacons is sent.
		const val TICK_INTERVAL_MS: Long = 1000         // Every tick, the region timeout is checked.
		const val REGION_TIMEOUT_MS: Long = 30000       // After not getting a scan for <timeout> time, the region has been left.
	}

	internal data class DeviceData(val ibeaconData: IbeaconData, val averager: IbeaconRssiAverager)

	@Synchronized
	fun destroy() {
		eventBus.unsubscribe(subId)
		handler.removeCallbacks(timeoutRunnable)
		handler.removeCallbacks(tickRunnable)
	}

	/**
	 * Start tracking a certain iBeacon UUID.
	 *
	 * @param ibeaconUuid The UUID to track.
	 * @param referenceId A reference ID for this UUID, it will be included in events.
	 */
	@Synchronized
	fun track(ibeaconUuid: UUID, referenceId: String) {
		trackedUuids.put(ibeaconUuid, referenceId)
		resume()
	}

	/**
	 * Stop tracking a certain iBeacon UUID.
	 *
	 * @param ibeaconUuid The UUID to stop tracking.
	 */
	@Synchronized
	fun stopTracking(ibeaconUuid: UUID) {
		trackedUuids.remove(ibeaconUuid)
		lastSeenRegion.remove(ibeaconUuid)
		inRegion.remove(ibeaconUuid)
		val keys = deviceMap.keys
		for (key in keys) {
			if (deviceMap[key]?.ibeaconData?.uuid == ibeaconUuid) {
				deviceMap.remove(key)
			}
		}
		if (trackedUuids.isEmpty()) {
			pause()
		}
	}

	/**
	 * Stop tracking.
	 *
	 * Clears the list of tracked iBeacon UUIDs.
	 */
	@Synchronized
	fun stopTracking() {
		trackedUuids.clear()
		lastSeenRegion.clear()
		inRegion.clear()
		deviceMap.clear()
		pause()
	}

	/**
	 * Stop tracking, but keep the list of tracked iBeacon UUIDs.
	 *
	 * Assume we are out of all regions.
	 * Stops sending any tracking events: iBeacon, enter/exit region.
	 */
	@Synchronized
	fun pause() {
		if (isRunning) {
			handler.removeCallbacks(timeoutRunnable)
			handler.removeCallbacks(tickRunnable)
			isTimeoutRunning = false
		}
		isRunning = false
	}

	/**
	 * Start tracking again, with the list that is already there.
	 */
	@Synchronized
	fun resume() {
		if (!isRunning) {
			handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
		}
		isRunning = true
	}

	@Synchronized
	private fun onScan(data: Any) {
		if (!isRunning) {
			return
		}
		val device = data as ScannedDevice

		// Keep up last seen per region (uuid), and check for enter region
		val ibeaconData = device.ibeaconData ?: return
		if (ibeaconData.uuid !in trackedUuids) {
			return
		}
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

	@Synchronized
	private fun onTimeout() {
		Log.v(TAG, "onTimeout")
//		val result = ArrayList<ScannedIbeacon>()
		val result = ScannedIbeaconList()
		for (entry in deviceMap) {
			val referenceId = trackedUuids[entry.value.ibeaconData.uuid] ?: ""
			Log.v(TAG, "    ${entry.key} uuid=${entry.value.ibeaconData.uuid} rssi=${entry.value.averager.getAverage()}")
			result.add(ScannedIbeacon(entry.key, entry.value.ibeaconData, entry.value.averager.getAverage(), referenceId))
		}
		deviceMap.clear()
		isTimeoutRunning = false
		eventBus.emit(BluenetEvent.IBEACON_SCAN, result)
	}

	@Synchronized
	private fun onTick() {
		checkExitRegions()
		handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
	}

	@Synchronized
	private fun checkExitRegions() {
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

	@Synchronized
	private fun onEnterRegion(uuid: UUID) {
		Log.i(TAG, "onEnterRegion $uuid")
		inRegion.add(uuid)
		eventBus.emit(BluenetEvent.IBEACON_ENTER_REGION, getEventData(uuid))
	}

	@Synchronized
	private fun onExitRegion(uuid: UUID) {
		Log.i(TAG, "onExitRegion $uuid")
		inRegion.remove(uuid)
		eventBus.emit(BluenetEvent.IBEACON_EXIT_REGION, getEventData(uuid))
	}

	@Synchronized
	private fun getEventData(changedUuid: UUID): IbeaconRegionEventData {
		val list = IbeaconRegionList()
		for (uuid in inRegion) {
			val referenceId = trackedUuids[uuid] ?: ""
			list[uuid] = referenceId
		}
		return IbeaconRegionEventData(changedUuid, list)
	}


}