package rocks.crownstone.bluenet.scanparsing

import android.os.Handler
import rocks.crownstone.bluenet.BluenetEvent
import rocks.crownstone.bluenet.DeviceAddress
import rocks.crownstone.bluenet.EventBus
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet


/**
 * Class to simulate ibeacon ranging.
 * Once per second a list of of devices with averaged RSSI is sent.
 */
class IbeaconRanger(val eventBus: EventBus, val handler: Handler) {
	private val TAG = this.javaClass.simpleName
	private val lastSeenRegion = HashMap<UUID, Long>()
	private val inRegion = HashSet<UUID>()
	private val averageRssiList = HashMap<DeviceAddress, IbeaconRssiAverager>()
	init {
		eventBus.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)
	}

	companion object {

	}

	fun destroy() {
//		handler.remove
	}

	@Synchronized private fun onScan(data: Any) {
		val device = data as ScannedDevice

	}


}