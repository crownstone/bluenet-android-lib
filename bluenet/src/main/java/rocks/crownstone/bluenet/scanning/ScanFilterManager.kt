/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanning

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import android.util.Log
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteOrder
import java.util.*


/**
 * Manages a list of scan filters.
 *
 * The list of scan filters act as OR.
 * Keeps up a map of filters with string as id, so you can avoid double entries, and remove filters again.
 *
 * @updateCallback Function that's called when filter list changed.
 */
class ScanFilterManager(val updateCallback: (List<ScanFilter>) -> Unit) {
	private val TAG = this.javaClass.simpleName
	private val filters = HashMap<String, Filter>()

	private data class Filter(val filter: ScanFilter)

	@Synchronized fun getFilters(): List<ScanFilter> {
		val scanFilters = ArrayList<ScanFilter>()
		for (entry in filters) {
			scanFilters.add(entry.value.filter)
		}
		return scanFilters
	}

	/**
	 * Adds filter for any iBeacon.
	 */
	@Synchronized fun addIbeaconFilter() {
		val id = "ibeacons"
		Log.i(TAG, "addIbeaconFilter id=$id")
		if (filters.containsKey(id)) {
			return
		}
		val data = ByteArray(BluenetProtocol.APPLE_HEADER_SIZE + BluenetProtocol.IBEACON_SIZE, { 0 })
		val dataMask = ByteArray(BluenetProtocol.APPLE_HEADER_SIZE + BluenetProtocol.IBEACON_SIZE, { 0 })
		data[0] = BluenetProtocol.IBEACON_TYPE.toByte()
		data[1] = BluenetProtocol.IBEACON_SIZE.toByte()
		dataMask[0] = 1
		dataMask[1] = 1
		val scanFilter = ScanFilter.Builder()
				.setManufacturerData(BluenetProtocol.APPLE_COMPANY_ID, data, dataMask)
				.build()
		filters[id] = Filter(scanFilter)
		updateCallback(getFilters())
	}

	/**
	 * Remove the any iBeacon filter.
	 */
	@Synchronized fun remIbeaconFilter() {
		val id = "ibeacon"
		Log.i(TAG, "remIbeaconFilter id=$id")
		if (!filters.containsKey(id)) {
			return
		}
		filters.remove(id)
		updateCallback(getFilters())
	}

	/**
	 * Adds filter for iBeacons with certain UUID.
	 */
	@Synchronized fun addIbeaconFilter(ibeaconUuid: UUID) {
		val id = "ibeacon $ibeaconUuid"
		Log.i(TAG, "addIbeaconFilter id=$id")
		if (filters.containsKey(id)) {
			return
		}
		// iBeacon data is in big endian format
		val uuidArray = Conversion.uuidToBytes(ibeaconUuid, ByteOrder.BIG_ENDIAN)
		val data = ByteArray(BluenetProtocol.APPLE_HEADER_SIZE + BluenetProtocol.IBEACON_SIZE, { 0 })
		val dataMask = ByteArray(BluenetProtocol.APPLE_HEADER_SIZE + BluenetProtocol.IBEACON_SIZE, { 0 })
		data[0] = BluenetProtocol.IBEACON_TYPE.toByte()
		data[1] = BluenetProtocol.IBEACON_SIZE.toByte()
		dataMask[0] = 1
		dataMask[1] = 1
//		System.arraycopy(uuidArray, 0, data, 2, uuidArray.size)
		for (i in 0 until uuidArray.size) {
			data[i+2] = uuidArray[i]
			dataMask[i+2] = 1
		}
//			updateCallback(getFilters())
//		}
		val scanFilter = ScanFilter.Builder()
				.setManufacturerData(BluenetProtocol.APPLE_COMPANY_ID, data, dataMask)
				.build()
		filters[id] = Filter(scanFilter)
		updateCallback(getFilters())
	}

	/**
	 * Remove filter for iBeacons with certain UUID.
	 */
	@Synchronized fun remIbeaconFilter(ibeaconUuid: UUID) {
		val id = "ibeacons $ibeaconUuid"
		Log.i(TAG, "remIbeaconFilter id=$id")
		if (!filters.containsKey(id)) {
			return
		}
		filters.remove(id)
		updateCallback(getFilters())
	}

	@Synchronized fun addCrownstoneFilter() {
		Log.i(TAG, "addCrownstoneFilter")
		var update = false
		if (addServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG, false)) {
			update = true
		}
		if (addServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN, false)) {
			update = true
		}
		if (addServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE, false)) {
			update = true
		}
		if (addServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_DFU, false)) {
			update = true
		}
		if (update) {
			updateCallback(getFilters())
		}
	}

	@Synchronized fun remCrownstoneFilter() {
		Log.i(TAG, "remCrownstoneFilter")
		var update = false
		if (remServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG, false)) {
			update = true
		}
		if (remServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN, false)) {
			update = true
		}
		if (remServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE, false)) {
			update = true
		}
		if (remServiceDataFilter(BluenetProtocol.SERVICE_DATA_UUID_DFU, false)) {
			update = true
		}
		if (update) {
			updateCallback(getFilters())
		}
	}

	@Synchronized fun addServiceDataFilter(serviceUuid: UUID) {
		addServiceDataFilter(serviceUuid, true)
	}

	@Synchronized private fun addServiceDataFilter(serviceUuid: UUID, update: Boolean): Boolean {
		val id = "serviceData $serviceUuid"
		Log.i(TAG, "addServiceDataFilter id=$id update=$update")
		if (filters.containsKey(id)) {
			return false
		}
		val parcelUuid = ParcelUuid(serviceUuid)
		val scanFilterBuilder = ScanFilter.Builder()
		scanFilterBuilder.setServiceData(parcelUuid, null)
		val scanFilter = scanFilterBuilder.build()
		filters[id] = Filter(scanFilter)
		if (update) {
			updateCallback(getFilters())
		}
		return true
	}

	@Synchronized fun remServiceDataFilter(serviceUuid: UUID) {
		remServiceDataFilter(serviceUuid, true)
	}

	@Synchronized fun remServiceDataFilter(serviceUuid: UUID, update: Boolean): Boolean {
		val id = "serviceData $serviceUuid"
		Log.i(TAG, "remServiceDataFilter id=$id update=$update")
		if (!filters.containsKey(id)) {
			return false
		}
		filters.remove(id)
		if (update) {
			updateCallback(getFilters())
		}
		return true
	}
}