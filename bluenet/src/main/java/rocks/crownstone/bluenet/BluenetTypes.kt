package rocks.crownstone.bluenet


enum class BluenetEvent {
	SCAN_RESULT_RAW, // When device was scanned. ScanResult as data
	SCAN_FAILURE,    // When scanning failed.
	SCAN_RESULT,     // When device was scanned. BleDevice as data
}


//enum class EventType {
//	FOO,
//	BAR,
//}

