package rocks.crownstone.bluenet

import android.bluetooth.le.ScanFilter
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
//	private val scanFilters = ArrayList<ScanFilter>()

	private val filters = HashMap<String, Filter>()


//	enum class FilterType {
//		IBEACON,
//		IBEACON_UUID,
//	}

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
//		scanFilters.add(scanFilter)
		updateCallback(getFilters())
	}

	/**
	 * Remove filter for any iBeacon.
	 */
	@Synchronized fun remIbeaconFilter() {
		val id = "ibeacons"
		if (filters.containsKey(id)) {
			return
		}
		filters.remove(id)
		updateCallback(getFilters())
	}

	/**
	 * Adds filter for iBeacons with certain UUID.
	 */
	@Synchronized fun addIbeaconFilter(ibeaconUUID: UUID) {
		val id = "ibeacons $ibeaconUUID"
		if (filters.containsKey(id)) {
			return
		}
		// iBeacon data is in big endian format
		val uuidArray = Conversion.uuidToBytes(ibeaconUUID, ByteOrder.BIG_ENDIAN)
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
		val scanFilter = ScanFilter.Builder()
				.setManufacturerData(BluenetProtocol.APPLE_COMPANY_ID, data, dataMask)
				.build()
		filters[id] = Filter(scanFilter)
		updateCallback(getFilters())
	}

	/**
	 * Remove filter for iBeacons with certain UUID.
	 */
	@Synchronized fun remIbeaconFilter(ibeaconUUID: UUID) {
		val id = "ibeacons $ibeaconUUID"
		if (filters.containsKey(id)) {
			return
		}
		filters.remove(id)
		updateCallback(getFilters())
	}
}