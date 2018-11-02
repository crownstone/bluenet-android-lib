package rocks.crownstone.bluenet

import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.util.Conversion
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
	SCAN_RESULT,     // Device was scanned. ScannedDevice as data.
//	NOTIFICATION_RAW_,  // Notification is received. Characteristic UUID will be appended to the event type. ByteArray as data. Only used internally
	NEAREST_VALIDATED,        // Validated device (regardless of operation mode) was scanned. NearestDeviceListEntry as data.
	NEAREST_VALIDATED_NORMAL, // Validated device in normal operation mode was scanned. NearestDeviceListEntry as data.
	NEAREST_DFU,              // Validated device in dfu operation mode was scanned. NearestDeviceListEntry as data.
	NEAREST_SETUP,            // Validated device in setup operation mode was scanned. NearestDeviceListEntry as data.
	SETUP_PROGRESS,  // Setup is in progress, Double (progress, where 1.0 is done) as data.
	IBEACON_SCAN,    // List of iBeacons was scanned. ScannedIbeaconList as data.
	IBEACON_ENTER_REGION, // Region was entered. iBeacon UUID as data.
	IBEACON_EXIT_REGION,  // Region was exited. iBeacon UUID as data.
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

enum class ProcessResult {
	NOT_DONE,
	DONE,
	ERROR,
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

data class ScannedIbeacon(val address: DeviceAddress, val ibeaconData: IbeaconData, val rssi: Int)
typealias ScannedIbeaconList = List<ScannedIbeacon>
