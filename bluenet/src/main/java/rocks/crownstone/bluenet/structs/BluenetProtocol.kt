/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.structs

import rocks.crownstone.bluenet.util.Util
import java.util.UUID

object BluenetProtocol {
	val SERVICE_DATA_UUID_CROWNSTONE_PLUG    = UUID.fromString("0000C001-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_CROWNSTONE_BUILTIN = UUID.fromString("0000C002-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_GUIDESTONE         = UUID.fromString("0000C003-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_DFU                = UUID.fromString("00001530-1212-efde-1523-785feabcd123")

	val SETUP_IBEACON_UUID = UUID.fromString("A643423E-E175-4AF0-A2E4-31E32f729A8A")

	const val APPLE_COMPANY_ID = 0x004c
	const val APPLE_HEADER_SIZE = 2
	const val IBEACON_TYPE = 0x02
	const val IBEACON_SIZE = 0x15
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

	// DFU service
	val DFU_SERVICE_UUID = UUID.fromString("00001530-1212-efde-1523-785feabcd123")
	val CHAR_DFU_CONTROL_UUID = UUID.fromString("00001531-1212-efde-1523-785feabcd123")

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

	const val RECOVERY_CODE: Uint32 = 0xDEADBEEF
	const val FACTORY_RESET_CODE: Uint32 = 0xDEADBEEF

	// State error bitmask
	const val STATE_ERROR_POS_OVERCURRENT =        0
	const val STATE_ERROR_POS_OVERCURRENT_DIMMER = 1
	const val STATE_ERROR_POS_TEMP_CHIP =          2
	const val STATE_ERROR_POS_TEMP_DIMMER =        3
	const val STATE_ERROR_POS_DIMMER_ON_FAILURE =  4
	const val STATE_ERROR_POS_DIMMER_OFF_FAILURE = 5

	// Schedule override bitmask
	const val SCHEDULE_OVERRIDE_BIT_POS_ALL =      0
	const val SCHEDULE_OVERRIDE_BIT_POS_LOCATION = 1

	const val SCHEDULE_WEEKDAY_BIT_POS_SUNDAY    = 0
	const val SCHEDULE_WEEKDAY_BIT_POS_MONDAY    = 1
	const val SCHEDULE_WEEKDAY_BIT_POS_TUESDAY   = 2
	const val SCHEDULE_WEEKDAY_BIT_POS_WEDNESDAY = 3
	const val SCHEDULE_WEEKDAY_BIT_POS_THURSDAY  = 4
	const val SCHEDULE_WEEKDAY_BIT_POS_FRIDAY    = 5
	const val SCHEDULE_WEEKDAY_BIT_POS_SATURDAY  = 6
	const val SCHEDULE_WEEKDAY_BIT_POS_ALL_DAYS  = 7
	const val SCHEDULE_WEEKDAY_MASK_ALL_DAYS = 0x7F // 01111111
}

enum class OpcodeType(val num: Uint8) {
	READ(0),
	WRITE(1),
	NOTIFY(2),
	RESULT(3),
	UNKNOWN(255);
	companion object {
		private val map = OpcodeType.values().associateBy(OpcodeType::num)
		fun fromNum(type: Uint8): OpcodeType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class ResultType(val num: Uint16) {
	SUCCESS(0),
	WAIT_FOR_SUCCESS(1),
	BUFFER_UNASSIGNED(16),
	BUFFER_LOCKED(17),
	WRONG_PAYLOAD_LENGTH(32),
	WRONG_PARAMETER(33),
	INVALID_MESSAGE(34),
	UNKNOWN_OP_CODE(35),
	UNKNOWN_TYPE(36),
	NOT_FOUND(37),
	NO_ACCESS(48),
	NOT_AVAILABLE(64),
	NOT_IMPLEMENTED(65),
	WRITE_DISABLED(80),
	ERR_WRITE_NOT_ALLOWED(81),
	ADC_INVALID_CHANNEL(96),
	UNKNOWN(65535);
	companion object {
		private val map = ResultType.values().associateBy(ResultType::num)
		fun fromNum(type: Uint16): ResultType {
			return map[type] ?: return UNKNOWN
		}
	}
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
	UART_ENABLE(34),
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

enum class DeviceType(val num: Uint8) {
	UNKNOWN(0),
	CROWNSTONE_PLUG(1),
	GUIDESTONE(2),
	CROWNSTONE_BUILTIN(3),
	CROWNSTONE_DONGLE(4);
	companion object {
		private val map = DeviceType.values().associateBy(DeviceType::num)
		//		fun fromInt(type: Int) = map.getOrDefault(type, UNKNOWN)
		//@JvmStatic
		fun fromNum(type: Uint8): DeviceType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class OperationMode {
	UNKNOWN,
	NORMAL,
	SETUP,
	DFU,
}

enum class ServiceDataVersion(val num: Uint8) {
	UNKNOWN(0),
	V1(1),
	V3(3),
	V4(4),
	V5(5),
	V6(6);
	companion object {
		private val map = ServiceDataVersion.values().associateBy(ServiceDataVersion::num)
		fun fromNum(action: Uint8): ServiceDataVersion {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class ServiceDataType(val num: Uint8) {
	STATE(0),
	ERROR(1),
	EXT_STATE(2),
	EXT_ERROR(3),
	UNKNOWN(255);
	companion object {
		private val map = ServiceDataType.values().associateBy(ServiceDataType::num)
		fun fromNum(action: Uint8): ServiceDataType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class UartMode(val num: Uint8) {
	NONE(0),
	RX_ONLY(1),
	RX_AND_TX(3),
	UNKNOWN(255);
	companion object {
		private val map = UartMode.values().associateBy(UartMode::num)
		fun fromNum(action: Uint8): UartMode {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class KeepAliveAction(val num: Uint8) {
	NO_CHANGE(0),
	CHANGE(1),
	UNKNOWN(255);
	companion object {
		private val map = KeepAliveAction.values().associateBy(KeepAliveAction::num)
		fun fromNum(action: Uint8): KeepAliveAction {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class MultiSwitchType(val num: Uint8) {
	LIST(0),
	UNKNOWN(255);
	companion object {
		private val map = MultiSwitchType.values().associateBy(MultiSwitchType::num)
		fun fromNum(action: Uint8): MultiSwitchType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class MultiSwitchIntent(val num: Uint8) {
	SPHERE_ENTER(0),
	SPHERE_EXIT(1),
	ENTER(2),
	EXIT(3),
	MANUAL(4),
	UNKNOWN(255);
	companion object {
		private val map = MultiSwitchIntent.values().associateBy(MultiSwitchIntent::num)
		fun fromNum(action: Uint8): MultiSwitchIntent {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class MultiKeepAliveType(val num: Uint8) {
	SAME_TIMEOUT(1),
	UNKNOWN(255);
	companion object {
		private val map = MultiKeepAliveType.values().associateBy(MultiKeepAliveType::num)
		fun fromNum(action: Uint8): MultiKeepAliveType {
			return map[action] ?: return UNKNOWN
		}
	}
}

class KeepAliveActionSwitch {
	var actionSwitchValue: Uint8 = 255; private set
	fun setAction(switchValue: Uint8) {
		actionSwitchValue = switchValue
	}
	fun clearAction() {
		actionSwitchValue = 255
	}
}

enum class MeshCommandType(val num: Uint8) {
	CONTROL(0),
	BEACON_CONFIG(1),
	CONFIG(2),
	STATE(3),
	UNKNOWN(255);
	companion object {
		private val map = MeshCommandType.values().associateBy(MeshCommandType::num)
		fun fromNum(action: Uint8): MeshCommandType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class ScheduleActionType(val num: Int) {
	SWITCH(0),
	FADE(1),
	TOGGLE(2),
	UNKNOWN(255);
	companion object {
		private val map = ScheduleActionType.values().associateBy(ScheduleActionType::num)
		fun fromNum(action: Int): ScheduleActionType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class ScheduleRepeatType(val num: Int) {
	MINUTES(0),
	DAY(1),
	ONCE(2),
	UNKNOWN(255);
	companion object {
		private val map = ScheduleRepeatType.values().associateBy(ScheduleRepeatType::num)
		fun fromNum(action: Int): ScheduleRepeatType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class ScheduleWeekDayBitPos(val num: Int) {
	SUNDAY(0),
	MONDAY(1),
	TUESDAY(2),
	WEDNESDAY(3),
	THURSDAY(4),
	FRIDAY(5),
	SATURDAY(6),
	ALL_DAYS(7),
	UNKNOWN(255);
	companion object {
		private val map = ScheduleWeekDayBitPos.values().associateBy(ScheduleWeekDayBitPos::num)
		fun fromNum(action: Int): ScheduleWeekDayBitPos {
			return map[action] ?: return UNKNOWN
		}
	}
}

data class IbeaconData(val uuid: UUID, val major: Uint16, val minor: Uint16, val rssiAtOneMeter: Int8) {
	override fun toString(): String {
		return "uuid=$uuid, major=$major, minor=$minor, rssiAtOneMeter=$rssiAtOneMeter"
	}
}

class SwitchState(val state: Uint8) {
	val relay = Util.isBitSet(state,7)
//	val dimmer = state.toInt() and ((1 shl 7).inv())
	val dimmer = state.toInt() and 127
}

class ErrorState() {
	var bitmask: Uint32 = 0; private set
	var overCurrent =       false
	var overCurrentDimmer = false
	var chipTemperature =   false
	var dimmerTemperature = false
	var dimmerOnFailure =   false
	var dimmerOffFailure =  false

	constructor(bitmask: Uint32): this() {
		overCurrent =       Util.isBitSet(bitmask, BluenetProtocol.STATE_ERROR_POS_OVERCURRENT)
		overCurrentDimmer = Util.isBitSet(bitmask, BluenetProtocol.STATE_ERROR_POS_OVERCURRENT_DIMMER)
		chipTemperature =   Util.isBitSet(bitmask, BluenetProtocol.STATE_ERROR_POS_TEMP_CHIP)
		dimmerTemperature = Util.isBitSet(bitmask, BluenetProtocol.STATE_ERROR_POS_TEMP_DIMMER)
		dimmerOnFailure =   Util.isBitSet(bitmask, BluenetProtocol.STATE_ERROR_POS_DIMMER_ON_FAILURE)
		dimmerOffFailure =  Util.isBitSet(bitmask, BluenetProtocol.STATE_ERROR_POS_DIMMER_OFF_FAILURE)
		this.bitmask = bitmask
	}

	fun calcBitMask(): Uint32 {
		bitmask = 0
		if (overCurrent) {       bitmask = Util.setBit(bitmask, BluenetProtocol.STATE_ERROR_POS_OVERCURRENT) }
		if (overCurrentDimmer) { bitmask = Util.setBit(bitmask, BluenetProtocol.STATE_ERROR_POS_OVERCURRENT_DIMMER) }
		if (chipTemperature) {   bitmask = Util.setBit(bitmask, BluenetProtocol.STATE_ERROR_POS_TEMP_CHIP) }
		if (dimmerTemperature) { bitmask = Util.setBit(bitmask, BluenetProtocol.STATE_ERROR_POS_TEMP_DIMMER) }
		if (dimmerOnFailure) {   bitmask = Util.setBit(bitmask, BluenetProtocol.STATE_ERROR_POS_DIMMER_ON_FAILURE) }
		if (dimmerOffFailure) {  bitmask = Util.setBit(bitmask, BluenetProtocol.STATE_ERROR_POS_DIMMER_OFF_FAILURE) }
		return bitmask
	}
}

class ScheduleOverride() {
	var bitmask: Uint8 = 0; private set
	var all =      false
		set(value) { onSet(value, BluenetProtocol.SCHEDULE_OVERRIDE_BIT_POS_ALL) }
	var location = false
		set(value) { onSet(value, BluenetProtocol.SCHEDULE_OVERRIDE_BIT_POS_ALL) }

	constructor(bitmask: Uint8): this() {
		all =      Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_OVERRIDE_BIT_POS_ALL)
		location = Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_OVERRIDE_BIT_POS_LOCATION)
		this.bitmask = bitmask
	}

	fun calcBitMask(): Uint8 {
		bitmask = 0
		if (all) {      bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_OVERRIDE_BIT_POS_ALL) }
		if (location) { bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_OVERRIDE_BIT_POS_LOCATION) }
		return bitmask
	}

	private fun onSet(value: Boolean, bit: Int) {
		when (value) {
			true -> bitmask = Util.setBit(bitmask, bit)
			false -> bitmask = Util.clearBit(bitmask, bit)
		}
	}
}

class ScheduleDayOfWeek() {
	var bitmask: Uint8 = 0; private set
	var sunday =    false
	var monday =    false
	var tuesday =   false
	var wednesday = false
	var thursday =  false
	var friday =    false
	var saturday =  false
	var everyDay =  false

	constructor(bitmask: Uint8): this() {
		sunday =    Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_SUNDAY)
		monday =    Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_MONDAY)
		tuesday =   Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_TUESDAY)
		wednesday = Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_WEDNESDAY)
		thursday =  Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_THURSDAY)
		friday =    Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_FRIDAY)
		saturday =  Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_SATURDAY)
		everyDay =  Util.isBitSet(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_ALL_DAYS)
		this.bitmask = bitmask
	}

	fun calcBitMask(): Uint8 {
		bitmask = 0
		if (sunday) {    bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_SUNDAY) }
		if (monday) {    bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_MONDAY) }
		if (tuesday) {   bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_TUESDAY) }
		if (wednesday) { bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_WEDNESDAY) }
		if (thursday) {  bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_THURSDAY) }
		if (friday) {    bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_FRIDAY) }
		if (saturday) {  bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_SATURDAY) }
		if (everyDay) {  bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_ALL_DAYS) }
		if ((bitmask.toInt() and BluenetProtocol.SCHEDULE_WEEKDAY_MASK_ALL_DAYS) == BluenetProtocol.SCHEDULE_WEEKDAY_MASK_ALL_DAYS) {
			bitmask = Util.setBit(bitmask, BluenetProtocol.SCHEDULE_WEEKDAY_BIT_POS_ALL_DAYS)
//			bitmask = Util.setBit(bitmask, ScheduleWeekDayBitPos.ALL_DAYS.num)
		}
		return bitmask
	}
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
