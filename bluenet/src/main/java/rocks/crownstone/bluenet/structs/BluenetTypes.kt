/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.structs

import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.encryption.MeshKeySet
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import java.util.*
import kotlin.collections.HashMap


enum class BluenetEvent {
	INITIALIZED,                       // Sent when bluenet has been initialized.
	SPHERE_SETTINGS_UPDATED,           // Sent when sphere settings have been updated. Used internally.
	BLE_TURNED_ON,                     // Bluetooth is turned on.
	BLE_TURNED_OFF,                    // Bluetooth is turned off.
	NO_LOCATION_SERVICE_PERMISSION,    // Sent when location service permission is required, but not granted.
	LOCATION_PERMISSION_GRANTED,       // Sent when location service permission is granted.
	LOCATION_SERVICE_TURNED_ON,        // Sent when location service is turned on.
	LOCATION_SERVICE_TURNED_OFF,       // Sent when location service is turned off.
	CORE_SCANNER_READY,                // Core scanner is ready to be used. Used internally.
	CORE_SCANNER_NOT_READY,            // Core scanner is no longer ready to be used. Used internally.
	SCANNER_READY,                     // Scanner is ready to be used.
	SCANNER_NOT_READY,                 // Scanner is no longer ready to be used.
	SCAN_RESULT_RAW,                   // Device was scanned. ScanResult as data.
	SCAN_FAILURE,                      // Scanning failed.
	SCAN_RESULT,                       // Device was scanned. ScannedDevice as data.
	SCAN_RESULT_VALIDATED,             // Validated device was scanned. ScannedDevice as data.
	SCAN_RESULT_UNIQUE,                // Device was scanned, with unique service data.  ScannedDevice as data.
	SCAN_RESULT_VALIDATED_UNIQUE,      // Validated device was scanned, with unique service data.  ScannedDevice as data.
	NEAREST_STONE,                     // Any device that is a stone was scanned.                The current nearest is calculated. NearestDeviceListEntry as data.
	NEAREST_UNVALIDATED,               // Unvalidated device, that is a stone, was scanned.      The current nearest is calculated. NearestDeviceListEntry as data.
	NEAREST_VALIDATED,                 // Validated device (any operation mode) was scanned.     The current nearest is calculated. NearestDeviceListEntry as data.
	NEAREST_VALIDATED_NORMAL,          // Validated device in normal operation mode was scanned. The current nearest is calculated. NearestDeviceListEntry as data.
	NEAREST_DFU,                       // Validated device in dfu operation mode was scanned.    The current nearest is calculated. NearestDeviceListEntry as data.
	NEAREST_SETUP,                     // Validated device in setup operation mode was scanned.  The current nearest is calculated. NearestDeviceListEntry as data.
	SETUP_PROGRESS,                    // Setup is in progress, Double (progress, where 1.0 is done) as data.
	IBEACON_SCAN,                      // List of iBeacons was scanned. ScannedIbeaconList as data.
	IBEACON_ENTER_REGION,              // Region was entered. IbeaconRegionEventData as data.
	IBEACON_EXIT_REGION,               // Region was exited. IbeaconRegionEventData as data.
	DFU_PROGRESS,                      // DfuProgress as data.
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
typealias SphereShortId = Uint8
typealias Keys = HashMap<SphereId, KeyData>
typealias SphereSettingsMap = HashMap<SphereId, SphereSettings>
typealias SphereStateMap = HashMap<SphereId, SphereState>

/**
 * Struct that holds sphere settings.
 *
 * @param keySet             Keys of the sphere.
 * @param meshKeySet         Mesh keys of the sphere.
 * @param ibeaconUuid        iBeacon UUID of the sphere, should be unique per sphere.
 * @param sphereShortId      Short ID of the sphere, not globally unique, but used as filter.
 * @param deviceToken        Token of this device, should be a unique number for each device (phone) in the sphere.
 */
data class SphereSettings(
		val keySet: KeySet,
		val meshKeySet: MeshKeySet?,
		val ibeaconUuid: UUID,
		var sphereShortId: SphereShortId = 0, // TODO: make this val, for now, this value isn't always known on init.
		var deviceToken: Uint8                // TODO: make this val, for now, this value isn't always known on init.
)

/**
 * Struct that holds state of a sphere.
 *
 * @param settings            Settings of the sphere.
 * @param profileId          Profile this device uses. Influences the behaviour of a Crownstone.
 * @param locationId         ID of the location this device is currently at. Influences the behaviour of a Crownstone.
 * @param tapToToggleEnabled Whether or not this device has tap to toggle enabled.
 * @param rssiOffset         RSSI offset of this device. Influences the distance at which tap to toggle is triggered.
 */
data class SphereState(
		var settings: SphereSettings,
		var commandCount: Uint8 = 0,
		var locationId: Uint8 = 0,
		var profileId: Uint8 = 0,
		var tapToToggleEnabled: Boolean = false,
		var rssiOffset: Int = 0
)

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
typealias IbeaconRegionList = HashMap<UUID, String> // Map with list of entered region UUIDs with reference ID as value.
data class IbeaconRegionEventData(val changedRegion: UUID, val list: IbeaconRegionList) // Changed region is the UUID that was entered or exited.

data class DfuProgress(val percentage: Int, val currentSpeed: Float, val avgSpeed: Float, val currentPart: Int, val totalParts: Int)