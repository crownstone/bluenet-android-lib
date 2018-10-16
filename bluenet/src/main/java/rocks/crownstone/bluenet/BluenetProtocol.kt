package rocks.crownstone.bluenet

import java.util.UUID

object BluenetProtocol {
	val SERVICE_DATA_UUID_CROWNSTONE_PLUG    = UUID.fromString("0000C001-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_CROWNSTONE_BUILTIN = UUID.fromString("0000C002-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_GUIDESTONE         = UUID.fromString("0000C003-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_DFU                = UUID.fromString("00001530-1212-efde-1523-785feabcd123")

	const val APPLE_COMPANY_ID = 0x004c
	const val IBEACON_ADVERTISEMENT_ID = 0x0215 // Actually 2 separate fields: type and length

	// Crownstone service
	val CROWNSTONE_SERVICE_UUID =  UUID.fromString("24f00000-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_CONTROL_UUID =        UUID.fromString("24f00001-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_MESH_CONTROL_UUID =   UUID.fromString("24f00002-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_CONFIG_CONTROL_UUID = UUID.fromString("24f00004-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_CONFIG_READ_UUID =    UUID.fromString("24f00005-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_STATE_CONTROL_UUID =  UUID.fromString("24f00006-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_STATE_READ_UUID =     UUID.fromString("24f00007-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SESSION_NONCE_UUID =  UUID.fromString("24f00008-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_RECOVERY_UUID =       UUID.fromString("24f00009-7d10-4805-bfc1-7663a01c3bff")

	// Setup service
	val SETUP_SERVICE_UUID =             UUID.fromString("24f10000-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_CONTROL_UUID =        UUID.fromString("24f10001-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_MAC_ADDRESS_UUID =          UUID.fromString("24f10002-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SESSION_KEY_UUID =          UUID.fromString("24f10003-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_CONFIG_CONTROL_UUID = UUID.fromString("24f10004-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_CONFIG_READ_UUID =    UUID.fromString("24f10005-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_GOTO_DFU_UUID =       UUID.fromString("24f10006-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_SESSION_NONCE_UUID =  UUID.fromString("24f10008-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_CONTROL2_UUID =       UUID.fromString("24f10007-7d10-4805-bfc1-7663a01c3bff")

	// Device Information Service
	val DEVICE_INFO_SERVICE_UUID =    UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
	val CHAR_HARDWARE_REVISION_UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
	val CHAR_FIRMWARE_REVISION_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

	val DESCRIPTOR_CHAR_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

	// Verification code for ECB encryption
//	const val CAFEBABE = 0xCAFEBABE // Kotlin made this -0x35014542
//	const val CAFEBABE = -0x35014542
	const val CAFEBABE = 0xCAFEBABE.toInt()

	const val AES_BLOCK_SIZE = 16
	const val VALIDATION_KEY_LENGTH = 4
	const val SESSION_NONCE_LENGTH = 5
	const val PACKET_NONCE_LENGTH = 3
	const val ACCESS_LEVEL_LENGTH = 1

	const val MULTIPART_NOTIFICATION_MAX_SIZE = 512
	const val MULTIPART_NOTIFICATION_LAST_NR = 255

	const val OPCODE_READ: Uint8 = 0
	const val OPCODE_WRITE: Uint8 = 1
	const val OPCODE_NOTIFY: Uint8 = 2
}

enum class ControlType(val num: Uint8) {
	SWITCH(0),
	PWM(1),
	SET_TIME(2),
	GOTO_DFU(3),
	RESET(4),
	FACTORY_RESET(5),
	KEEP_ALIVE_STATE(6),
	KEEP_ALIVE(7),
	ENABLE_MESH(8),
	ENABLE_ENCRYPTION(9),
	ENABLE_IBEACON(10),
	ENABLE_CONT_POWER_MEASURE(11),
	ENABLE_SCANNER(12),
	SCAN_DEVICES(13),
	USER_FEEDBACK(14),
	SCHEDULE_ENTRY_SET(15),
	RELAY(16),
	VALIDATE_SETUP(17),
	REQUEST_SERVICE_DATA(18),
	DISCONNECT(19),
	NOOP(21),
	INCREASE_TX(22),
	RESET_STATE_ERRORS(23),
	KEEP_ALIVE_REPEAT_LAST(24),
	MULTI_SWITCH(25),
	SCHEDULE_ENTRY_REMOVE(26),
	KEEP_ALIVE_MESH(27),
	MESH_COMMAND(28),
	ALLOW_DIMMING(29),
	LOCK_SWITCH(30),
	SETUP(31),
	ENABLE_SWITCHCRAFT(32),
	UART_MSG(33),
	UNKNOWN(255);
	companion object {
		private val map = ControlType.values().associateBy(ControlType::num)
		fun fromNum(type: Uint8): ControlType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class StateType(val num: Uint8) {
	RESET_COUNTER(128),
	SWITCH_STATE(129),
	ACCUMULATED_ENERGY(130),
	POWER_USAGE(131),
	TRACKED_DEVICES(132),
	SCHEDULE(133),
	OPERATION_MODE(134),
	TEMPERATURE(135),
	TIME(136),
	ERRORS(139),
	UNKNOWN(255);
	companion object {
		private val map = StateType.values().associateBy(StateType::num)
		fun fromNum(type: Uint8): StateType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class ConfigType(val num: Uint8) {
	NAME(0),
	DEVICE_TYPE(1),
	ROOM(2),
	FLOOR(3),
	NEARBY_TIMEOUT(4),
	PWM_PERIOD(5),
	IBEACON_MAJOR(6),
	IBEACON_MINOR(7),
	IBEACON_PROXIMITY_UUID(8),
	IBEACON_TXPOWER(9),
	WIFI_SETTINGS(10),
	TX_POWER(11),
	ADV_INTERVAL(12),
	PASSKEY(13),
	MIN_ENV_TEMP(14),
	MAX_ENV_TEMP(15),
	SCAN_DURATION(16),
	SCAN_SEND_DELAY(17),
	SCAN_BREAK_DURATION(18),
	BOOT_DELAY(19),
	MAX_CHIP_TEMP(20),
	SCAN_FILTER(21),
	SCAN_FILTER_SEND_FRACTION(22),
	MESH_ENABLED(24),
	ENCRYPTION_ENABLED(25),
	IBEACON_ENABLED(26),
	SCANNER_ENABLED(27),
	CONT_POWER_SAMPLER_ENABLED(28),
	TRACKER_ENABLED(29),
	ADC_SAMPLE_RATE(30),
	POWER_SAMPLE_BURST_INTERVAL(31),
	POWER_SAMPLE_CONT_INTERVAL(32),
	POWER_SAMPLE_CONT_NUM_SAMPLES(33),
	CROWNSTONE_ID(34),
	KEY_ADMIN(35),
	KEY_MEMBER(36),
	KEY_GUEST(37),
	DEFAULT_ON(38),
	SCAN_INTERVAL(39),
	SCAN_WINDOW(40),
	RELAY_HIGH_DURATION(41),
	LOW_TX_POWER(42),
	VOLTAGE_MULTIPLIER(43),
	CURRENT_MULTIPLIER(44),
	VOLTAGE_ZERO(45),
	CURRENT_ZERO(46),
	POWER_ZERO(47),
	POWER_AVG_WINDOW(48),
	MESH_ACCESS_ADDRESS(49),
	CURRENT_THRESHOLD(50),
	CURRENT_THRESHOLD_DIMMER(51),
	DIMMER_TEMP_UP(52),
	DIMMER_TEMP_DOWN(53),
	DIMMING_ALLOWED(54),
	SWITCH_LOCKED(55),
	SWITCHCRAFT_ENABLED(56),
	SWITCHCRAFT_THRESHOLD(57),
	MESH_CHANNEL(58),
	UART_ENABLED(59),
	UNKNOWN(255);
	companion object {
		private val map = ConfigType.values().associateBy(ConfigType::num)
		fun fromNum(type: Uint8): ConfigType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class DeviceType(val num: Int) {
	UNKNOWN(0),
	CROWNSTONE_PLUG(1),
	GUIDESTONE(2),
	CROWNSTONE_BUILTIN(3),
	CROWNSTONE_DONGLE(4);

	companion object {
		private val map = DeviceType.values().associateBy(DeviceType::num)
		//		fun fromInt(type: Int) = map.getOrDefault(type, UNKNOWN)
		//@JvmStatic
		fun fromInt(type: Int): DeviceType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class OperationMode() {
	UNKNOWN,
	NORMAL,
	SETUP,
	DFU,
}



object BluenetConfigOld {

	val BLE_DEVICE_ADDRESS_LENGTH = 6
	val BLE_MAX_MULTIPART_NOTIFICATION_LENGTH = 512

	//
	// UUID string should be written with lower case!
	//

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Dfu service
	val DFU_SERVICE_UUID = "00001530-1212-efde-1523-785feabcd123"
	val DFU_CONTROL_UUID = "00001531-1212-efde-1523-785feabcd123"

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Crownstone Service
	val CROWNSTONE_SERVICE_UUID = "24f00000-7d10-4805-bfc1-7663a01c3bff"
	// Crownstone Service - Characteristics
	val CHAR_CONTROL_UUID = "24f00001-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_MESH_CONTROL_UUID = "24f00002-7d10-4805-bfc1-7663a01c3bff"
	// public static final String CHAR_MESH_READ_UUID =                    "24f00003-7d10-4805-bfc1-7663a01c3bff";
	val CHAR_CONFIG_CONTROL_UUID = "24f00004-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_CONFIG_READ_UUID = "24f00005-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_STATE_CONTROL_UUID = "24f00006-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_STATE_READ_UUID = "24f00007-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SESSION_NONCE_UUID = "24f00008-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_RECOVERY_UUID = "24f00009-7d10-4805-bfc1-7663a01c3bff"

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Setup Service
	val SETUP_SERVICE_UUID = "24f10000-7d10-4805-bfc1-7663a01c3bff"
	// Setup Service - Characteristics
	val CHAR_SETUP_CONTROL_UUID = "24f10001-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_MAC_ADDRESS_UUID = "24f10002-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SESSION_KEY_UUID = "24f10003-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SETUP_CONFIG_CONTROL_UUID = "24f10004-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SETUP_CONFIG_READ_UUID = "24f10005-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SETUP_GOTO_DFU_UUID = "24f10006-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SETUP_SESSION_NONCE_UUID = "24f10008-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SETUP_CONTROL2_UUID = "24f10007-7d10-4805-bfc1-7663a01c3bff"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// General Service
	val GENERAL_SERVICE_UUID = "24f20000-7d10-4805-bfc1-7663a01c3bff"
	// General Service - Characteristics
	val CHAR_TEMPERATURE_UUID = "24f20001-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_RESET_UUID = "24f20002-7d10-4805-bfc1-7663a01c3bff"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Power Service
	val POWER_SERVICE_UUID = "24f30000-7d10-4805-bfc1-7663a01c3bff"
	// Power Service - Characteristics
	val CHAR_PWM_UUID = "24f30001-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_RELAY_UUID = "24f30002-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_POWER_SAMPLES_UUID = "24f30003-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_POWER_CONSUMPTION_UUID = "24f30004-7d10-4805-bfc1-7663a01c3bff"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Indoor Localization Service
	val INDOOR_LOCALIZATION_SERVICE_UUID = "24f40000-7d10-4805-bfc1-7663a01c3bff"
	// Indoor Localization Service - Characteristics
	val CHAR_TRACK_CONTROL_UUID = "24f40001-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_TRACKED_DEVICES_UUID = "24f40002-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SCAN_CONTROL_UUID = "24f40003-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_SCANNED_DEVICES_UUID = "24f40004-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_RSSI_UUID = "24f40005-7d10-4805-bfc1-7663a01c3bff"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Schedule Service
	val SCHEDULE_SERVICE_UUID = "24f50000-7d10-4805-bfc1-7663a01c3bff"
	// Alert Service - Characteristics
	val CHAR_CURRENT_TIME_UUID = "24f50001-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_WRITE_SCHEDULE_ENTRY = "24f50002-7d10-4805-bfc1-7663a01c3bff"
	val CHAR_LIST_SCHEDULE_ENTRIES = "24f50003-7d10-4805-bfc1-7663a01c3bff"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Mesh Service
	val MESH_SERVICE_UUID = "0000fee4-0000-1000-8000-00805f9b34fb"
	// Mesh Service - Characteristics
	val MESH_META_CHARACTERISTIC_UUID = "2a1e0004-fd51-d882-8ba8-b98c0000cd1e"
	val MESH_DATA_CHARACTERISTIC_UUID = "2a1e0005-fd51-d882-8ba8-b98c0000cd1e"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Device Information Service
	//	public static final String DEVICE_INFO_SERVICE_UUID =               "0x180a";
	val DEVICE_INFO_SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb"
	// Device Information Service - Characteristics
	//	public static final String CHAR_HARDWARE_REVISION_UUID =            "0x2a27";
	//	public static final String CHAR_SOFTWARE_REVISION_UUID =            "0x2a26";
	val CHAR_HARDWARE_REVISION_UUID = "00002a27-0000-1000-8000-00805f9b34fb"
	val CHAR_SOFTWARE_REVISION_UUID = "00002a26-0000-1000-8000-00805f9b34fb"


	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ControlMsg types
	val CMD_SWITCH: Char = 0.toChar()    // 0x00
	val CMD_PWM: Char = 1.toChar()    // 0x01
	val CMD_SET_TIME: Char = 2.toChar()    // 0x02
	val CMD_GOTO_DFU: Char = 3.toChar()    // 0x03
	val CMD_RESET: Char = 4.toChar()    // 0x04
	val CMD_FACTORY_RESET: Char = 5.toChar()    // 0x05
	val CMD_KEEP_ALIVE_STATE: Char = 6.toChar()    // 0x06
	val CMD_KEEP_ALIVE: Char = 7.toChar()    // 0x07
	val CMD_ENABLE_MESH: Char = 8.toChar()    // 0x08
	val CMD_ENABLE_ENCRYPTION: Char = 9.toChar()    // 0x09
	val CMD_ENABLE_IBEACON: Char = 10.toChar()   // 0x0A
	val CMD_ENABLE_CONT_POWER_MEASURE: Char = 11.toChar()   // 0x0B
	val CMD_ENABLE_SCANNER: Char = 12.toChar()   // 0x0C
	val CMD_SCAN_DEVICES: Char = 13.toChar()   // 0x0D
	val CMD_USER_FEEDBACK: Char = 14.toChar()   // 0x0E
	val CMD_SCHEDULE_ENTRY_SET: Char = 15.toChar()   // 0x0F
	val CMD_RELAY: Char = 16.toChar()   // 0x10
	val CMD_VALIDATE_SETUP: Char = 17.toChar()   // 0x11
	val CMD_REQUEST_SERVICE_DATA: Char = 18.toChar()   // 0x12
	val CMD_DISCONNECT: Char = 19.toChar()   // 0x13
	val CMD_SET_LED: Char = 20.toChar()   // 0x14
	val CMD_NOP: Char = 21.toChar()   // 0x15
	val CMD_INCREASE_TX: Char = 22.toChar()   // 0x16
	val CMD_RESET_STATE_ERRORS: Char = 23.toChar()   // 0x17
	val CMD_KEEP_ALIVE_REPEAT_LAST: Char = 24.toChar()   // 0x18
	val CMD_MULTI_SWITCH: Char = 25.toChar()   // 0x19
	val CMD_SCHEDULE_ENTRY_CLEAR: Char = 26.toChar()   // 0x1A
	val CMD_KEEP_ALIVE_MESH: Char = 27.toChar()   // 0x1B
	val CMD_MESH_COMMAND: Char = 28.toChar()   // 0x1C
	val CMD_ALLOW_DIMMING: Char = 29.toChar()
	val CMD_LOCK_SWITCH: Char = 30.toChar()
	val CMD_SETUP: Char = 31.toChar()
	val CMD_ENABLE_SWITCHCRAFT: Char = 32.toChar()
	val CMD_UART_MSG: Char = 33.toChar()
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Configuration types
	val CONFIG_NAME: Char = 0.toChar()    // 0x00
	val CONFIG_DEVICE_TYPE: Char = 1.toChar()    // 0x01
	val CONFIG_ROOM: Char = 2.toChar()    // 0x02
	val CONFIG_FLOOR: Char = 3.toChar()    // 0x03
	val CONFIG_NEARBY_TIMEOUT: Char = 4.toChar()    // 0x04
	val CONFIG_PWM_PERIOD: Char = 5.toChar()    // 0x05
	val CONFIG_IBEACON_MAJOR: Char = 6.toChar()    // 0x06
	val CONFIG_IBEACON_MINOR: Char = 7.toChar()    // 0x07
	val CONFIG_IBEACON_PROXIMITY_UUID: Char = 8.toChar()    // 0x08
	val CONFIG_IBEACON_TXPOWER: Char = 9.toChar()    // 0x09
	val CONFIG_WIFI_SETTINGS: Char = 10.toChar()   // 0x0A
	val CONFIG_TX_POWER: Char = 11.toChar()   // 0x0B
	val CONFIG_ADV_INTERVAL: Char = 12.toChar()   // 0x0C
	val CONFIG_PASSKEY: Char = 13.toChar()   // 0x0D
	val CONFIG_MIN_ENV_TEMP: Char = 14.toChar()   // 0x0E
	val CONFIG_MAX_ENV_TEMP: Char = 15.toChar()   // 0x0F
	val CONFIG_SCAN_DURATION: Char = 16.toChar()   // 0x10
	val CONFIG_SCAN_SEND_DELAY: Char = 17.toChar()   // 0x11
	val CONFIG_SCAN_BREAK_DURATION: Char = 18.toChar()   // 0x12
	val CONFIG_BOOT_DELAY: Char = 19.toChar()   // 0x13
	val CONFIG_MAX_CHIP_TEMP: Char = 20.toChar()   // 0x14
	val CONFIG_SCAN_FILTER: Char = 21.toChar()   // 0x15
	val CONFIG_SCAN_FILTER_SEND_FRACTION: Char = 22.toChar()   // 0x16
	val CONFIG_CURRENT_LIMIT: Char = 23.toChar()   // 0x17
	val CONFIG_MESH_ENABLED: Char = 24.toChar()   // 0x18
	val CONFIG_ENCRYPTION_ENABLED: Char = 25.toChar()   // 0x19
	val CONFIG_IBEACON_ENABLED: Char = 26.toChar()   // 0x1A
	val CONFIG_SCANNER_ENABLED: Char = 27.toChar()   // 0x1B
	val CONFIG_CONT_POWER_SAMPLER_ENABLED: Char = 28.toChar()   // 0x1C
	val CONFIG_TRACKER_ENABLED: Char = 29.toChar()   // 0x1D
	val CONFIG_ADC_SAMPLE_RATE: Char = 30.toChar()   // 0x1E
	val CONFIG_POWER_SAMPLE_BURST_INTERVAL: Char = 31.toChar()   // 0x1F
	val CONFIG_POWER_SAMPLE_CONT_INTERVAL: Char = 32.toChar()   // 0x20
	val CONFIG_POWER_SAMPLE_CONT_NUM_SAMPLES: Char = 33.toChar()   // 0x21
	val CONFIG_CROWNSTONE_ID: Char = 34.toChar()   // 0x22
	val CONFIG_KEY_ADMIN: Char = 35.toChar()   //! 0x23
	val CONFIG_KEY_MEMBER: Char = 36.toChar()   //! 0x24
	val CONFIG_KEY_GUEST: Char = 37.toChar()   //! 0x25
	val CONFIG_DEFAULT_ON: Char = 38.toChar()   //! 0x26
	val CONFIG_SCAN_INTERVAL: Char = 39.toChar()   //! 0x27
	val CONFIG_SCAN_WINDOW: Char = 40.toChar()   //! 0x28
	val CONFIG_RELAY_HIGH_DURATION: Char = 41.toChar()   //! 0x29
	val CONFIG_LOW_TX_POWER: Char = 42.toChar()   //! 0x2A
	val CONFIG_VOLTAGE_MULTIPLIER: Char = 43.toChar()   //! 0x2B
	val CONFIG_CURRENT_MULTIPLIER: Char = 44.toChar()   //! 0x2C
	val CONFIG_VOLTAGE_ZERO: Char = 45.toChar()   //! 0x2D
	val CONFIG_CURRENT_ZERO: Char = 46.toChar()   //! 0x2E
	val CONFIG_POWER_ZERO: Char = 47.toChar()   //! 0x2F
	val CONFIG_POWER_AVG_WINDOW: Char = 48.toChar()   //! 0x30
	val CONFIG_MESH_ACCESS_ADDRESS: Char = 49.toChar()   //! 0x31
	val CONFIG_CURRENT_THRESHOLD: Char = 50.toChar()   //! 0x32
	val CONFIG_CURRENT_THRESHOLD_DIMMER: Char = 51.toChar()   //! 0x33
	val CONFIG_DIMMER_TEMP_UP: Char = 52.toChar()   //! 0x34
	val CONFIG_DIMMER_TEMP_DOWN: Char = 53.toChar()   //! 0x35
	val CONFIG_PWM_ALLOWED: Char = 54.toChar()   //! 0x36
	val CONFIG_SWITCH_LOCKED: Char = 55.toChar()   //! 0x37
	val CONFIG_SWITCHCRAFT_ENABLED: Char = 56.toChar()   //! 0x38
	val CONFIG_SWITCHCRAFT_THRESHOLD: Char = 57.toChar()   //! 0x39
	val CONFIG_MESH_CHANNEL: Char = 58.toChar()   //! 0x3A
	val CONFIG_UART_ENABLED: Char = 59.toChar()   //! 0x3B


	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// State types
	val STATE_RESET_COUNTER: Char = 128.toChar()  // 0x80
	val STATE_SWITCH_STATE: Char = 129.toChar()  // 0x81
	val STATE_ACCUMULATED_ENERGY: Char = 130.toChar()  // 0x82
	val STATE_POWER_USAGE: Char = 131.toChar()  // 0x83
	val STATE_TRACKED_DEVICES: Char = 132.toChar()  // 0x84
	val STATE_SCHEDULE: Char = 133.toChar()  // 0x85
	val STATE_OPERATION_MODE: Char = 134.toChar()  // 0x86
	val STATE_TEMPERATURE: Char = 135.toChar()  // 0x87
	val STATE_TIME: Char = 136.toChar()  // 0x88
	val STATE_ERRORS: Char = 139.toChar()  // 0x8B
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Value Op Code
	val READ_VALUE: Char = 0.toChar()    // 0x00
	val WRITE_VALUE: Char = 1.toChar()    // 0x01
	val NOTIFY_VALUE: Char = 2.toChar()    // 0x02

	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Value set at reserved bytes for alignment
	val RESERVED: Char = 0x00.toChar()
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Mesh handles (channel)
	val MESH_HANDLE_KEEP_ALIVE: Char = 1.toChar()    // 0x01
	val MESH_HANDLE_STATE_BROADCAST: Char = 2.toChar()    // 0x02
	val MESH_HANDLE_STATE_CHANGE: Char = 3.toChar()    // 0x03
	val MESH_HANDLE_COMMAND: Char = 4.toChar()    // 0x04
	val MESH_HANDLE_COMMAND_REPLY: Char = 5.toChar()    // 0x05
	val MESH_HANDLE_SCAN_RESULT: Char = 6.toChar()    // 0x06
	val MESH_HANDLE_BIG_DATA: Char = 7.toChar()    // 0x07
	val MESH_HANDLE_MULTI_SWITCH: Char = 8.toChar()    // 0x08
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Mesh command types
	val MESH_MAX_PAYLOAD_SIZE = 92 // bytes
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Mesh command types
	val MESH_CMD_CONTROL: Char = 0.toChar()    // 0x00;
	val MESH_CMD_BEACON: Char = 1.toChar()    // 0x01;
	val MESH_CMD_CONFIG: Char = 2.toChar()    // 0x02;
	val MESH_CMD_STATE: Char = 3.toChar()    // 0x03;
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Mesh reply types
	val MESH_REPLY_STATUS: Char = 0.toChar()    // 0x00;
	val MESH_REPLY_CONFIG: Char = 1.toChar()    // 0x01;
	val MESH_REPLY_STATE: Char = 2.toChar()    // 0x02;
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Switch intent types
	val SWITCH_INTENT_SPHERE_ENTER: Char = 0.toChar()    // 0x00;
	val SWITCH_INTENT_SPHERE_EXIT: Char = 1.toChar()    // 0x01;
	val SWITCH_INTENT_ENTER: Char = 2.toChar()    // 0x02;
	val SWITCH_INTENT_EXIT: Char = 3.toChar()    // 0x03;
	val SWITCH_INTENT_MANUAL: Char = 4.toChar()    // 0x04;
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// State error bits
	val STATE_ERROR_POS_OVERCURRENT: Char = 0.toChar()
	val STATE_ERROR_POS_OVERCURRENT_DIMMER: Char = 1.toChar()
	val STATE_ERROR_POS_TEMP_CHIP: Char = 2.toChar()
	val STATE_ERROR_POS_TEMP_DIMMER: Char = 3.toChar()
	val STATE_ERROR_POS_DIMMER_ON_FAILURE: Char = 4.toChar()
	val STATE_ERROR_POS_DIMMER_OFF_FAILURE: Char = 5.toChar()
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// iBeacon Identifiers
	val APPLE_COMPANY_ID = 0x004c
	val IBEACON_ADVERTISEMENT_ID = 0x0215 // Actually 2 separate fields: type and length
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Crownstone
	val CROWNSTONE_PLUG_SERVICE_DATA_UUID = 0xC001
	val CROWNSTONE_BUILTIN_SERVICE_DATA_UUID = 0xC002
	val GUIDESTONE_SERVICE_DATA_UUID = 0xC003
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Reset OP codes
	val RESET_DEFAULT = 1     // 0x01
	val RESET_DFU = 66    // 0x42
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Recovery code
	val RECOVERY_CODE = 0xDEADBEEF // Kotlin made this -0x21524111
	val FACTORY_RESET_CODE = 0xDEADBEEF // Kotlin made this -0x21524111
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Verification code for ECB encryption
	val CAFEBABE = 0xCAFEBABE // Kotlin made this -0x35014542
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Device Type Identifiers
	val DEVICE_UNDEF = 0
	val DEVICE_CROWNSTONE_PLUG = 1
	val DEVICE_GUIDESTONE = 2
	val DEVICE_CROWNSTONE_BUILTIN = 3
	val DEVICE_CROWNSTONE_DONGLE = 4
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// constant used to convert the advertisement interval from ms to the unit expected by the
	// characteristic (increments of 0.625 ms)
	val ADVERTISEMENT_INCREMENT = 0.625
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	val PWM_ON = 100
	val PWM_OFF = 0
	val RELAY_ON = 128 // Can be 100 too actually
	val RELAY_OFF = 0
	val SWITCH_ON = 100 // Fully on
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	val KEEP_ALIVE_NO_ACTION = 255
	///////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Error codes
	val ERR_SUCCESS = 0x00
	val ERR_WAIT_FOR_SUCCESS = 0x01

	val ERR_BUFFER_UNASSIGNED = 0x10
	val ERR_BUFFER_LOCKED = 0x11

	val ERR_WRONG_PAYLOAD_LENGTH = 0x20
	val ERR_WRONG_PARAMETER = 0x21
	val ERR_INVALID_MESSAGE = 0x22
	val ERR_UNKNOWN_OP_CODE = 0x23
	val ERR_UNKNOWN_TYPE = 0x24
	val ERR_NOT_FOUND = 0x25

	val ERR_NO_ACCESS = 0x30

	val ERR_NOT_AVAILABLE = 0x40
	val ERR_NOT_IMPLEMENTED = 0x41
	val ERR_WRONG_SETTING = 0x42

	val ERR_WRITE_DISABLED = 0x50
	val ERR_WRITE_NOT_ALLOWED = 0x51
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
}
