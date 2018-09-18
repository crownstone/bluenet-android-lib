package rocks.crownstone.bluenet


enum class BluenetEvent {
	INITIALIZED, // Sent when bluenet has been initialized.
	NO_LOCATION_SERVICE_PERMISSION, // Sent when permission is required, but not granted.
	BLE_TURNED_ON,
	BLE_TURNED_OFF,
	LOCATION_SERVICE_TURNED_ON,
	LOCATION_SERVICE_TURNED_OFF,
	BLE_READY,         // BLE is ready to be used (connections).
	BLE_NOT_READY,     // BLE is no longer ready to be used (connections).
	SCANNER_READY,     // Scanner is ready to be used.
	SCANNER_NOT_READY, // Scanner is no longer ready to be used.
	READY,             // BleCore is ready to be used.
	NOT_READY,         // BleCore is no longer ready to be used.
	SCAN_RESULT_RAW, // Device was scanned. ScanResult as data.
	SCAN_FAILURE,    // Scanning failed.
	SCAN_RESULT,     // Device was scanned. BleDevice as data.
}


//enum class EventType {
//	FOO,
//	BAR,
//}

