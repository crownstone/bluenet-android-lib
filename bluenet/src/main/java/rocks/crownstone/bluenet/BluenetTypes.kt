package rocks.crownstone.bluenet


enum class BluenetEvent {
	BLE_TURNED_ON,
	BLE_TURNED_OFF,
	LOCATION_SERVICE_TURNED_ON,
	LOCATION_SERVICE_TURNED_OFF,
	SCAN_RESULT_RAW, // When device was scanned. ScanResult as data
	SCAN_FAILURE,    // When scanning failed.
	SCAN_RESULT,     // When device was scanned. BleDevice as data
}


//enum class EventType {
//	FOO,
//	BAR,
//}

