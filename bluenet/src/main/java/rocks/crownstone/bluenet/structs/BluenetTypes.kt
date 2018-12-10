package rocks.crownstone.bluenet.structs

import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import java.util.*


enum class BluenetEvent {
	INITIALIZED, // Sent when bluenet has been initialized.
	SCANNER_READY,
	SCANNER_NOT_READY,
	NO_LOCATION_SERVICE_PERMISSION, // Sent when permission is required, but not granted.
	BLE_TURNED_ON,
	BLE_TURNED_OFF,
	LOCATION_SERVICE_TURNED_ON,
	LOCATION_SERVICE_TURNED_OFF,
	LOCATION_PERMISSION_GRANTED,
	BLE_READY,         // BLE is ready to be used (connections).
	BLE_NOT_READY,     // BLE is no longer ready to be used (connections).
	CORE_SCANNER_READY,     // Scanner is ready to be used.
	CORE_SCANNER_NOT_READY, // Scanner is no longer ready to be used.
	READY,             // BleCore is ready to be used.
	NOT_READY,         // BleCore is no longer ready to be used.
	SCAN_RESULT_RAW, // Device was scanned. ScanResult as data.
	SCAN_FAILURE,    // Scanning failed.
	SCAN_RESULT,                  // Device was scanned. ScannedDevice as data.
	SCAN_RESULT_VALIDATED,        // Validated device was scanned. ScannedDevice as data.
	SCAN_RESULT_UNIQUE,           // Device was scanned, with unique service data.  ScannedDevice as data.
	SCAN_RESULT_VALIDATED_UNIQUE, // Validated device was scanned, with unique service data.  ScannedDevice as data.
	NEAREST_VALIDATED,        // Validated device (regardless of operation mode) was scanned. NearestDeviceListEntry as data.
	NEAREST_VALIDATED_NORMAL, // Validated device in normal operation mode was scanned. NearestDeviceListEntry as data.
	NEAREST_DFU,              // Validated device in dfu operation mode was scanned. NearestDeviceListEntry as data.
	NEAREST_SETUP,            // Validated device in setup operation mode was scanned. NearestDeviceListEntry as data.
	SETUP_PROGRESS,  // Setup is in progress, Double (progress, where 1.0 is done) as data.
	IBEACON_SCAN,    // List of iBeacons was scanned. ScannedIbeaconList as data.
	IBEACON_ENTER_REGION, // Region was entered. IbeaconRegionEventData as data.
	IBEACON_EXIT_REGION,  // Region was exited. IbeaconRegionEventData as data.
	DFU_PROGRESS,
}

typealias Int8 = Byte
typealias Uint8 = Short
typealias Int16 = Short
typealias Uint16 = Int
typealias Int32 = Int
typealias Uint32 = Long


//class Int8: Byte
//class Uint8: Short
//class Int16: Short
//class Uint16: Int
//class Int32: Int
//class Uint32: Long


typealias DeviceAddress = String
typealias SphereId = String
typealias Keys = HashMap<SphereId, KeyData>
data class KeyData(val keySet: KeySet, val ibeaconUuid: UUID)

enum class CrownstoneMode {
	NORMAL,
	SETUP,
	DFU,
	UNKNOWN
}

enum class ProcessResult {
	NOT_DONE,
	DONE,
	ERROR,
}

enum class ScanMode(val num: Int) {
	LOW_POWER(0),   // 5120ms interval, of which 512ms scan time.
	BALANCED(1),    // 4096ms interval, of which 1024ms scan time.
	LOW_LATENCY(2), // 4096ms interval, of which 4096ms scan time.
}

typealias ProcessCallback = (ByteArray) -> ProcessResult

//data class BleNotification(val characteristicUuid: UUID, val data: ByteArray)

/**
 * Class that holds a key with its access level
 */
data class KeyAccessLevelPair(val key: ByteArray, val accessLevel: AccessLevel) {
	override fun toString(): String {
		return "key: " + Conversion.bytesToString(key) + " access level: " + accessLevel
	}
	// TODO: override equals() and hashCode()
}

data class ScannedIbeacon(val address: DeviceAddress, val ibeaconData: IbeaconData, val rssi: Int, val referenceId: String)
typealias ScannedIbeaconList = ArrayList<ScannedIbeacon>
typealias IbeaconRegionList = HashMap<UUID, String> // Map with list of entered region UUIDs with reference id as value.
data class IbeaconRegionEventData(val changedRegion: UUID, val list: IbeaconRegionList) // Changed region is the UUID that was entered or exited.
