/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.structs

import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import java.util.UUID

object BluenetProtocol {
	val SERVICE_DATA_UUID_CROWNSTONE_PLUG    = UUID.fromString("0000C001-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_CROWNSTONE_BUILTIN = UUID.fromString("0000C002-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_GUIDESTONE         = UUID.fromString("0000C003-0000-1000-8000-00805F9B34FB")
	val SERVICE_DATA_UUID_DFU                = UUID.fromString("00001530-1212-efde-1523-785feabcd123")
	val SERVICE_DATA_UUID_DFU2               = Conversion.stringToUuid("FE59")!!

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
	// protocol v4
	val CHAR_CONTROL4_UUID =       UUID.fromString("24f0000a-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_RESULT_UUID =         UUID.fromString("24f0000b-7d10-4805-bfc1-7663a01c3bff")

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
	val CHAR_SETUP_CONTROL3_UUID =       UUID.fromString("24f10009-7d10-4805-bfc1-7663a01c3bff")
	// protocol v4
	val CHAR_SETUP_CONTROL4_UUID =       UUID.fromString("24f1000a-7d10-4805-bfc1-7663a01c3bff")
	val CHAR_SETUP_RESULT_UUID =         UUID.fromString("24f1000b-7d10-4805-bfc1-7663a01c3bff")

	// Device Information Service
	val DEVICE_INFO_SERVICE_UUID =    UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
	val CHAR_HARDWARE_REVISION_UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
	val CHAR_FIRMWARE_REVISION_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

	// DFU service
	val DFU_SERVICE_UUID = UUID.fromString("00001530-1212-efde-1523-785feabcd123")
	val CHAR_DFU_CONTROL_UUID = UUID.fromString("00001531-1212-efde-1523-785feabcd123")
	val DFU_RESET_COMMAND = byteArrayOf(0x06)

	// DFU service 2
	val DFU2_SERVICE_UUID = Conversion.stringToUuid("FE59")!!
	val CHAR_DFU2_CONTROL_UUID = UUID.fromString("8EC90001-F315-4F60-9FB8-838830DAEA50")
	val DFU2_RESET_COMMAND = byteArrayOf(0x0C)

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

	const val RC5_ROUNDS = 12
	const val RC5_NUM_SUBKEYS = (2*(RC5_ROUNDS+1))
	const val RC5_KEYLEN = 16
	const val RC5_WORD_SIZE = 2

	const val MULTIPART_NOTIFICATION_MAX_SIZE = 512
	const val MULTIPART_NOTIFICATION_LAST_NR = 255

	const val RECOVERY_CODE: Uint32 = 0xDEADBEEFU
	const val FACTORY_RESET_CODE: Uint32 = 0xDEADBEEFU

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

	const val TURN_SWITCH_ON: Uint8 = 0xFFU

	const val STATE_DEFAULT_ID: Uint16 = 0U
}

enum class OpcodeType(val num: Uint8) {
	READ(0U),
	WRITE(1U),
	NOTIFY(2U),
	RESULT(3U),
	UNKNOWN(255U);
	companion object {
		private val map = OpcodeType.values().associateBy(OpcodeType::num)
		fun fromNum(type: Uint8): OpcodeType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class ResultType(val num: Uint16) {
	SUCCESS(                 0x00U),
	WAIT_FOR_SUCCESS(        0x01U),
	BUFFER_UNASSIGNED(       0x10U),
	BUFFER_LOCKED(           0x11U),
	BUFFER_TOO_SMALL(        0x12U),
	WRONG_PAYLOAD_LENGTH(    0x20U),
	WRONG_PARAMETER(         0x21U),
	INVALID_MESSAGE(         0x22U),
	UNKNOWN_OP_CODE(         0x23U),
	UNKNOWN_TYPE(            0x24U),
	NOT_FOUND(               0x25U),
	NO_SPACE(                0x26U),
	BUSY(                    0x27U),
	WRONG_STATE(             0x28U),
	NO_ACCESS(               0x30U),
	NOT_AVAILABLE(           0x40U),
	NOT_IMPLEMENTED(         0x41U),
	NOT_INITIALIZED(         0x43U),
	WRITE_DISABLED(          0x50U),
	ERR_WRITE_NOT_ALLOWED(   0x51U),
	ADC_INVALID_CHANNEL(     0x60U),
	EVENT_UNHANDLED(         0x70U),
	UNKNOWN(                 0xFFFFU);
	companion object {
		private val map = ResultType.values().associateBy(ResultType::num)
		fun fromNum(type: Uint16): ResultType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class ControlType(val num: Uint8) {
	SWITCH(0U),
	PWM(1U),
	SET_TIME(2U),
	GOTO_DFU(3U),
	RESET(4U),
	FACTORY_RESET(5U),
	KEEP_ALIVE_STATE(6U),
	KEEP_ALIVE(7U),
	ENABLE_MESH(8U),
	ENABLE_ENCRYPTION(9U),
	ENABLE_IBEACON(10U),
	ENABLE_CONT_POWER_MEASURE(11U),
	ENABLE_SCANNER(12U),
	SCAN_DEVICES(13U),
	USER_FEEDBACK(14U),
	SCHEDULE_ENTRY_SET(15U),
	RELAY(16U),
	VALIDATE_SETUP(17U),
	REQUEST_SERVICE_DATA(18U),
	DISCONNECT(19U),
	NOOP(21U),
	INCREASE_TX(22U),
	RESET_STATE_ERRORS(23U),
	KEEP_ALIVE_REPEAT_LAST(24U),
	MULTI_SWITCH(25U),
	SCHEDULE_ENTRY_REMOVE(26U),
	KEEP_ALIVE_MESH(27U),
	MESH_COMMAND(28U),
	ALLOW_DIMMING(29U),
	LOCK_SWITCH(30U),
	SETUP(31U),
	ENABLE_SWITCHCRAFT(32U),
	UART_MSG(33U),
	UART_ENABLE(34U),
	UNKNOWN(255U);
	companion object {
		private val map = ControlType.values().associateBy(ControlType::num)
		fun fromNum(type: Uint8): ControlType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class ControlTypeV4(val num: Uint16) {
	SETUP(0U),
	FACTORY_RESET(1U),
	GET_STATE(2U),
	SET_STATE(3U),
	RESET(10U),
	GOTO_DFU(11U),
	NOOP(12U),
	DISCONNECT(13U),
	SWITCH(20U),
	MULTI_SWITCH(21U),
	DIMMER(22U),
	RELAY(23U),
	SET_TIME(30U),
	INCREASE_TX(31U),
	RESET_STATE_ERRORS(32U),
	MESH_COMMAND(33U),
	ALLOW_DIMMING(40U),
	LOCK_SWITCH(41U),
	UART_MSG(50U),
	BEHAVIOUR_ADD(60U),
	BEHAVIOUR_REPLACE(61U),
	BEHAVIOUR_REMOVE(62U),
	BEHAVIOUR_GET(63U),
	BEHAVIOUR_GET_INDICES(64U),
	BEHAVIOUR_GET_DEBUG(69U),
	UNKNOWN(0xFFFFU);
	companion object {
		private val map = ControlTypeV4.values().associateBy(ControlTypeV4::num)
		fun fromNum(type: Uint16): ControlTypeV4 {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class CommandBroadcastType(val num: Uint8) {
	NO_OP(0U),
	MULTI_SWITCH(1U),
	SET_TIME(2U),
	BEHAVIOUR_SETTINGS(3U),
	UNKNOWN(255U);
	companion object {
		private val map = values().associateBy(CommandBroadcastType::num)
		fun fromNum(action: Uint8): CommandBroadcastType {
			return map[action] ?: return UNKNOWN
		}
	}
}


enum class StateType(val num: Uint8) {
	RESET_COUNTER(128U),
	SWITCH_STATE(129U),
	ACCUMULATED_ENERGY(130U),
	POWER_USAGE(131U),
	TRACKED_DEVICES(132U),
	SCHEDULE(133U),
	OPERATION_MODE(134U),
	TEMPERATURE(135U),
	TIME(136U),
	ERRORS(139U),
	SWITCHCRAFT_LAST_BUF1(149U),
	SWITCHCRAFT_LAST_BUF2(150U),
	SWITCHCRAFT_LAST_BUF3(151U),
	UNKNOWN(255U);
	companion object {
		private val map = StateType.values().associateBy(StateType::num)
		fun fromNum(type: Uint8): StateType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class ConfigType(val num: Uint8) {
	NAME(0U),
	DEVICE_TYPE(1U),
	ROOM(2U),
	FLOOR(3U),
	NEARBY_TIMEOUT(4U),
	PWM_PERIOD(5U),
	IBEACON_MAJOR(6U),
	IBEACON_MINOR(7U),
	IBEACON_PROXIMITY_UUID(8U),
	IBEACON_TXPOWER(9U),
	WIFI_SETTINGS(10U),
	TX_POWER(11U),
	ADV_INTERVAL(12U),
	PASSKEY(13U),
	MIN_ENV_TEMP(14U),
	MAX_ENV_TEMP(15U),
	SCAN_DURATION(16U),
	SCAN_SEND_DELAY(17U),
	SCAN_BREAK_DURATION(18U),
	BOOT_DELAY(19U),
	MAX_CHIP_TEMP(20U),
	SCAN_FILTER(21U),
	SCAN_FILTER_SEND_FRACTION(22U),
	MESH_ENABLED(24U),
	ENCRYPTION_ENABLED(25U),
	IBEACON_ENABLED(26U),
	SCANNER_ENABLED(27U),
	CONT_POWER_SAMPLER_ENABLED(28U),
	TRACKER_ENABLED(29U),
	ADC_SAMPLE_RATE(30U),
	POWER_SAMPLE_BURST_INTERVAL(31U),
	POWER_SAMPLE_CONT_INTERVAL(32U),
	POWER_SAMPLE_CONT_NUM_SAMPLES(33U),
	CROWNSTONE_ID(34U),
	KEY_ADMIN(35U),
	KEY_MEMBER(36U),
	KEY_GUEST(37U),
	DEFAULT_ON(38U),
	SCAN_INTERVAL(39U),
	SCAN_WINDOW(40U),
	RELAY_HIGH_DURATION(41U),
	LOW_TX_POWER(42U),
	VOLTAGE_MULTIPLIER(43U),
	CURRENT_MULTIPLIER(44U),
	VOLTAGE_ZERO(45U),
	CURRENT_ZERO(46U),
	POWER_ZERO(47U),
	POWER_AVG_WINDOW(48U),
	MESH_ACCESS_ADDRESS(49U),
	CURRENT_THRESHOLD(50U),
	CURRENT_THRESHOLD_DIMMER(51U),
	DIMMER_TEMP_UP(52U),
	DIMMER_TEMP_DOWN(53U),
	DIMMING_ALLOWED(54U),
	SWITCH_LOCKED(55U),
	SWITCHCRAFT_ENABLED(56U),
	SWITCHCRAFT_THRESHOLD(57U),
	MESH_CHANNEL(58U),
	UART_ENABLED(59U),
	UNKNOWN(255U);
	companion object {
		private val map = ConfigType.values().associateBy(ConfigType::num)
		fun fromNum(type: Uint8): ConfigType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class StateTypeV4(val num: Uint16) {
	PWM_PERIOD(5U),
	IBEACON_MAJOR(6U),
	IBEACON_MINOR(7U),
	IBEACON_PROXIMITY_UUID(8U),
	IBEACON_TXPOWER(9U),
	TX_POWER(11U),
	ADV_INTERVAL(12U),
	SCAN_DURATION(16U),
	SCAN_BREAK_DURATION(18U),
	BOOT_DELAY(19U),
	MAX_CHIP_TEMP(20U),
	MESH_ENABLED(24U),
	ENCRYPTION_ENABLED(25U),
	IBEACON_ENABLED(26U),
	SCANNER_ENABLED(27U),
	POWER_SAMPLE_CONT_NUM_SAMPLES(33U),
	CROWNSTONE_ID(34U),
	KEY_ADMIN(35U),
	KEY_MEMBER(36U),
	KEY_GUEST(37U),
	DEFAULT_ON(38U),
	SCAN_INTERVAL(39U),
	SCAN_WINDOW(40U),
	RELAY_HIGH_DURATION(41U),
	LOW_TX_POWER(42U),
	VOLTAGE_MULTIPLIER(43U),
	CURRENT_MULTIPLIER(44U),
	VOLTAGE_ZERO(45U),
	CURRENT_ZERO(46U),
	POWER_ZERO(47U),
	CURRENT_THRESHOLD(50U),
	CURRENT_THRESHOLD_DIMMER(51U),
	DIMMER_TEMP_UP(52U),
	DIMMER_TEMP_DOWN(53U),
	DIMMING_ALLOWED(54U),
	SWITCH_LOCKED(55U),
	SWITCHCRAFT_ENABLED(56U),
	SWITCHCRAFT_THRESHOLD(57U),
	UART_ENABLED(59U),
	NAME(60U),
	KEY_SERVICE_DATA(61U),
	KEY_MESH_DEVICE(62U),
	KEY_MESH_APP(63U),
	KEY_MESH_NETWORK(64U),
	KEY_LOCALIZATION(65U),
	START_DIMMER_ON_ZERO_CROSSING(66U),
	TAP_TO_TOGGLE_ENABLED(67U),
	TAP_TO_TOGGLE_RSSI_THRESHOLD_OFFSET(68U),

	RESET_COUNTER(128U),
	SWITCH_STATE(129U),
	ACCUMULATED_ENERGY(130U),
	POWER_USAGE(131U),
	OPERATION_MODE(134U),
	TEMPERATURE(135U),
	TIME(136U),
	ERRORS(139U),
	SUN_TIME(149U),

	UNKNOWN(0xFFFFU);
	companion object {
		private val map = StateTypeV4.values().associateBy(StateTypeV4::num)
		fun fromNum(type: Uint16): StateTypeV4 {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class DeviceType(val num: Uint8) {
	UNKNOWN(0U),
	CROWNSTONE_PLUG(1U),
	GUIDESTONE(2U),
	CROWNSTONE_BUILTIN(3U),
	CROWNSTONE_DONGLE(4U),
	CROWNSTONE_BUILTIN_ONE(5U);
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
	UNKNOWN(0U),
	V1(1U),
	V3(3U),
	V4(4U),
	V5(5U),
	V6(6U),
	V7(7U);
	companion object {
		private val map = ServiceDataVersion.values().associateBy(ServiceDataVersion::num)
		fun fromNum(action: Uint8): ServiceDataVersion {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class ServiceDataType(val num: Uint8) {
	STATE(0U), // Service data with state info
	ERROR(1U), // Service data with error info
	EXT_STATE(2U), // Service data of another crownstone with state info
	EXT_ERROR(3U), // Service data of another crownstone with error info
	UNKNOWN(255U);
	companion object {
		private val map = ServiceDataType.values().associateBy(ServiceDataType::num)
		fun fromNum(action: Uint8): ServiceDataType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class UartMode(val num: Uint8) {
	NONE(0U),
	RX_ONLY(1U),
	RX_AND_TX(3U),
	UNKNOWN(255U);
	companion object {
		private val map = UartMode.values().associateBy(UartMode::num)
		fun fromNum(action: Uint8): UartMode {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class KeepAliveAction(val num: Uint8) {
	NO_CHANGE(0U),
	CHANGE(1U),
	UNKNOWN(255U);
	companion object {
		private val map = KeepAliveAction.values().associateBy(KeepAliveAction::num)
		fun fromNum(action: Uint8): KeepAliveAction {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class MultiSwitchType(val num: Uint8) {
	LIST(0U),
	UNKNOWN(255U);
	companion object {
		private val map = MultiSwitchType.values().associateBy(MultiSwitchType::num)
		fun fromNum(action: Uint8): MultiSwitchType {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class MultiSwitchIntent(val num: Uint8) {
	SPHERE_ENTER(0U),
	SPHERE_EXIT(1U),
	ENTER(2U),
	EXIT(3U),
	MANUAL(4U),
	UNKNOWN(255U);
	companion object {
		private val map = MultiSwitchIntent.values().associateBy(MultiSwitchIntent::num)
		fun fromNum(action: Uint8): MultiSwitchIntent {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class MultiKeepAliveType(val num: Uint8) {
	SAME_TIMEOUT(1U),
	UNKNOWN(255U);
	companion object {
		private val map = MultiKeepAliveType.values().associateBy(MultiKeepAliveType::num)
		fun fromNum(action: Uint8): MultiKeepAliveType {
			return map[action] ?: return UNKNOWN
		}
	}
}

class KeepAliveActionSwitch() {
	var actionSwitchValue: Uint8 = 255U; private set
	constructor(action: Uint8): this() {
		this.setAction(action)
	}
	fun setAction(switchValue: Uint8) {
		actionSwitchValue = switchValue
	}
	fun clearAction() {
		actionSwitchValue = 255U
	}
}

enum class MeshCommandType(val num: Uint8) {
	CONTROL(0U),
	BEACON_CONFIG(1U),
	CONFIG(2U),
	STATE(3U),
	UNKNOWN(255U);
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
		private val map = values().associateBy(ScheduleWeekDayBitPos::num)
		fun fromNum(action: Int): ScheduleWeekDayBitPos {
			return map[action] ?: return UNKNOWN
		}
	}
}

enum class BehaviourSettings(val num: Uint32) {
	DUMB(0U),
	SMART(1U),
	UNKNOWN(0xFFFFFFFFU);
	companion object {
		private val map = values().associateBy(BehaviourSettings::num)
		fun fromNum(action: Uint32): BehaviourSettings {
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
	val relay = Util.isBitSet(state,7) // State of the relay, true when on.
//	val dimmer = state.toInt() and ((1 shl 7).inv())
	val dimmer = state.toInt() and 127 // State of the dimmer: 0 (off) - 100 (on)
	val value = if (state.toInt() > 100) 100 else state.toInt() // State of the switch: 0 (off) - 100 (on)
	override fun toString(): String {
		return "$state"
	}
}

class ErrorState() {
	var bitmask: Uint32 = 0U; private set
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
		bitmask = 0U
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
	var bitmask: Uint8 = 0U; private set
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
		bitmask = 0U
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
	var bitmask: Uint8 = 0U; private set
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
		bitmask = 0U
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

//
//object BluenetConfigOld {
//
//	val BLE_DEVICE_ADDRESS_LENGTH = 6
//	val BLE_MAX_MULTIPART_NOTIFICATION_LENGTH = 512
//
//	//
//	// UUID string should be written with lower case!
//	//
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Dfu service
//	val DFU_SERVICE_UUID = "00001530-1212-efde-1523-785feabcd123"
//	val DFU_CONTROL_UUID = "00001531-1212-efde-1523-785feabcd123"
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Crownstone Service
//	val CROWNSTONE_SERVICE_UUID = "24f00000-7d10-4805-bfc1-7663a01c3bff"
//	// Crownstone Service - Characteristics
//	val CHAR_CONTROL_UUID = "24f00001-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_MESH_CONTROL_UUID = "24f00002-7d10-4805-bfc1-7663a01c3bff"
//	// public static final String CHAR_MESH_READ_UUID =                    "24f00003-7d10-4805-bfc1-7663a01c3bff";
//	val CHAR_CONFIG_CONTROL_UUID = "24f00004-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_CONFIG_READ_UUID = "24f00005-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_STATE_CONTROL_UUID = "24f00006-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_STATE_READ_UUID = "24f00007-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SESSION_NONCE_UUID = "24f00008-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_RECOVERY_UUID = "24f00009-7d10-4805-bfc1-7663a01c3bff"
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Setup Service
//	val SETUP_SERVICE_UUID = "24f10000-7d10-4805-bfc1-7663a01c3bff"
//	// Setup Service - Characteristics
//	val CHAR_SETUP_CONTROL_UUID = "24f10001-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_MAC_ADDRESS_UUID = "24f10002-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SESSION_KEY_UUID = "24f10003-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SETUP_CONFIG_CONTROL_UUID = "24f10004-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SETUP_CONFIG_READ_UUID = "24f10005-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SETUP_GOTO_DFU_UUID = "24f10006-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SETUP_SESSION_NONCE_UUID = "24f10008-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SETUP_CONTROL2_UUID = "24f10007-7d10-4805-bfc1-7663a01c3bff"
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// General Service
//	val GENERAL_SERVICE_UUID = "24f20000-7d10-4805-bfc1-7663a01c3bff"
//	// General Service - Characteristics
//	val CHAR_TEMPERATURE_UUID = "24f20001-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_RESET_UUID = "24f20002-7d10-4805-bfc1-7663a01c3bff"
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Power Service
//	val POWER_SERVICE_UUID = "24f30000-7d10-4805-bfc1-7663a01c3bff"
//	// Power Service - Characteristics
//	val CHAR_PWM_UUID = "24f30001-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_RELAY_UUID = "24f30002-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_POWER_SAMPLES_UUID = "24f30003-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_POWER_CONSUMPTION_UUID = "24f30004-7d10-4805-bfc1-7663a01c3bff"
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Indoor Localization Service
//	val INDOOR_LOCALIZATION_SERVICE_UUID = "24f40000-7d10-4805-bfc1-7663a01c3bff"
//	// Indoor Localization Service - Characteristics
//	val CHAR_TRACK_CONTROL_UUID = "24f40001-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_TRACKED_DEVICES_UUID = "24f40002-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SCAN_CONTROL_UUID = "24f40003-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_SCANNED_DEVICES_UUID = "24f40004-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_RSSI_UUID = "24f40005-7d10-4805-bfc1-7663a01c3bff"
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Schedule Service
//	val SCHEDULE_SERVICE_UUID = "24f50000-7d10-4805-bfc1-7663a01c3bff"
//	// Alert Service - Characteristics
//	val CHAR_CURRENT_TIME_UUID = "24f50001-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_WRITE_SCHEDULE_ENTRY = "24f50002-7d10-4805-bfc1-7663a01c3bff"
//	val CHAR_LIST_SCHEDULE_ENTRIES = "24f50003-7d10-4805-bfc1-7663a01c3bff"
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Mesh Service
//	val MESH_SERVICE_UUID = "0000fee4-0000-1000-8000-00805f9b34fb"
//	// Mesh Service - Characteristics
//	val MESH_META_CHARACTERISTIC_UUID = "2a1e0004-fd51-d882-8ba8-b98c0000cd1e"
//	val MESH_DATA_CHARACTERISTIC_UUID = "2a1e0005-fd51-d882-8ba8-b98c0000cd1e"
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Device Information Service
//	//	public static final String DEVICE_INFO_SERVICE_UUID =               "0x180a";
//	val DEVICE_INFO_SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb"
//	// Device Information Service - Characteristics
//	//	public static final String CHAR_HARDWARE_REVISION_UUID =            "0x2a27";
//	//	public static final String CHAR_SOFTWARE_REVISION_UUID =            "0x2a26";
//	val CHAR_HARDWARE_REVISION_UUID = "00002a27-0000-1000-8000-00805f9b34fb"
//	val CHAR_SOFTWARE_REVISION_UUID = "00002a26-0000-1000-8000-00805f9b34fb"
//
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Mesh handles (channel)
//	val MESH_HANDLE_KEEP_ALIVE: Char = 1.toChar()    // 0x01
//	val MESH_HANDLE_STATE_BROADCAST: Char = 2.toChar()    // 0x02
//	val MESH_HANDLE_STATE_CHANGE: Char = 3.toChar()    // 0x03
//	val MESH_HANDLE_COMMAND: Char = 4.toChar()    // 0x04
//	val MESH_HANDLE_COMMAND_REPLY: Char = 5.toChar()    // 0x05
//	val MESH_HANDLE_SCAN_RESULT: Char = 6.toChar()    // 0x06
//	val MESH_HANDLE_BIG_DATA: Char = 7.toChar()    // 0x07
//	val MESH_HANDLE_MULTI_SWITCH: Char = 8.toChar()    // 0x08
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Mesh command types
//	val MESH_MAX_PAYLOAD_SIZE = 92 // bytes
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Mesh command types
//	val MESH_CMD_CONTROL: Char = 0.toChar()    // 0x00;
//	val MESH_CMD_BEACON: Char = 1.toChar()    // 0x01;
//	val MESH_CMD_CONFIG: Char = 2.toChar()    // 0x02;
//	val MESH_CMD_STATE: Char = 3.toChar()    // 0x03;
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Mesh reply types
//	val MESH_REPLY_STATUS: Char = 0.toChar()    // 0x00;
//	val MESH_REPLY_CONFIG: Char = 1.toChar()    // 0x01;
//	val MESH_REPLY_STATE: Char = 2.toChar()    // 0x02;
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Switch intent types
//	val SWITCH_INTENT_SPHERE_ENTER: Char = 0.toChar()    // 0x00;
//	val SWITCH_INTENT_SPHERE_EXIT: Char = 1.toChar()    // 0x01;
//	val SWITCH_INTENT_ENTER: Char = 2.toChar()    // 0x02;
//	val SWITCH_INTENT_EXIT: Char = 3.toChar()    // 0x03;
//	val SWITCH_INTENT_MANUAL: Char = 4.toChar()    // 0x04;
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// State error bits
//	val STATE_ERROR_POS_OVERCURRENT: Char = 0.toChar()
//	val STATE_ERROR_POS_OVERCURRENT_DIMMER: Char = 1.toChar()
//	val STATE_ERROR_POS_TEMP_CHIP: Char = 2.toChar()
//	val STATE_ERROR_POS_TEMP_DIMMER: Char = 3.toChar()
//	val STATE_ERROR_POS_DIMMER_ON_FAILURE: Char = 4.toChar()
//	val STATE_ERROR_POS_DIMMER_OFF_FAILURE: Char = 5.toChar()
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// iBeacon Identifiers
//	val APPLE_COMPANY_ID = 0x004c
//	val IBEACON_ADVERTISEMENT_ID = 0x0215 // Actually 2 separate fields: type and length
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Crownstone
//	val CROWNSTONE_PLUG_SERVICE_DATA_UUID = 0xC001
//	val CROWNSTONE_BUILTIN_SERVICE_DATA_UUID = 0xC002
//	val GUIDESTONE_SERVICE_DATA_UUID = 0xC003
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Reset OP codes
//	val RESET_DEFAULT = 1     // 0x01
//	val RESET_DFU = 66    // 0x42
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Recovery code
//	val RECOVERY_CODE = 0xDEADBEEF // Kotlin made this -0x21524111
//	val FACTORY_RESET_CODE = 0xDEADBEEF // Kotlin made this -0x21524111
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Verification code for ECB encryption
//	val CAFEBABE = 0xCAFEBABE // Kotlin made this -0x35014542
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Device Type Identifiers
//	val DEVICE_UNDEF = 0
//	val DEVICE_CROWNSTONE_PLUG = 1
//	val DEVICE_GUIDESTONE = 2
//	val DEVICE_CROWNSTONE_BUILTIN = 3
//	val DEVICE_CROWNSTONE_DONGLE = 4
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// constant used to convert the advertisement interval from ms to the unit expected by the
//	// characteristic (increments of 0.625 ms)
//	val ADVERTISEMENT_INCREMENT = 0.625
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	val PWM_ON = 100
//	val PWM_OFF = 0
//	val RELAY_ON = 128 // Can be 100 too actually
//	val RELAY_OFF = 0
//	val SWITCH_ON = 100 // Fully on
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	val KEEP_ALIVE_NO_ACTION = 255
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Error codes
//	val ERR_SUCCESS = 0x00
//	val ERR_WAIT_FOR_SUCCESS = 0x01
//
//	val ERR_BUFFER_UNASSIGNED = 0x10
//	val ERR_BUFFER_LOCKED = 0x11
//
//	val ERR_WRONG_PAYLOAD_LENGTH = 0x20
//	val ERR_WRONG_PARAMETER = 0x21
//	val ERR_INVALID_MESSAGE = 0x22
//	val ERR_UNKNOWN_OP_CODE = 0x23
//	val ERR_UNKNOWN_TYPE = 0x24
//	val ERR_NOT_FOUND = 0x25
//
//	val ERR_NO_ACCESS = 0x30
//
//	val ERR_NOT_AVAILABLE = 0x40
//	val ERR_NOT_IMPLEMENTED = 0x41
//	val ERR_WRONG_SETTING = 0x42
//
//	val ERR_WRITE_DISABLED = 0x50
//	val ERR_WRITE_NOT_ALLOWED = 0x51
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////
//}
