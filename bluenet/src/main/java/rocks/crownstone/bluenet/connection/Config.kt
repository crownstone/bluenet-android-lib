/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.packets.ByteArrayPacket
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.UuidPacket
import rocks.crownstone.bluenet.packets.other.SunTimePacket
import rocks.crownstone.bluenet.packets.wrappers.v3.ConfigPacket
import rocks.crownstone.bluenet.packets.wrappers.v4.StatePacketV4
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.toUint16
import java.util.*

/**
 * Class to interact with the config characteristics of the crownstone service.
 *
 * Most commands assume you are already connected to the crownstone.
 * In order to apply a new config, you have to reboot the crownstone, or set it via a control command.
 */
class Config(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	// -------------------- //
	// --- setters only --- //
	// -------------------- //

	/**
	 * Set the crownstone ID.
	 *
	 * @param id ID of the crownstone.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setCrownstoneId(id: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setCrownstoneId $id")
		val stoneId = id.toUint16() // Is still uint16
		return setConfigValue(ConfigType.CROWNSTONE_ID, StateTypeV4.CROWNSTONE_ID, stoneId)
	}

	/**
	 * Set the admin key.
	 *
	 * @param key The admin key: ByteArray of 16 bytes.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setAdminKey(key: ByteArray): Promise<Unit, Exception> {
		Log.i(TAG, "setAdminKey $key")
		return setConfig(ConfigType.KEY_ADMIN, StateTypeV4.KEY_ADMIN, ByteArrayPacket(key))
	}

	/**
	 * Set the member key.
	 *
	 * @param key The member key: ByteArray of 16 bytes.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setMemberKey(key: ByteArray): Promise<Unit, Exception> {
		Log.i(TAG, "setMemberKey $key")
		return setConfig(ConfigType.KEY_MEMBER, StateTypeV4.KEY_MEMBER, ByteArrayPacket(key))
	}

	/**
	 * Set the guest key.
	 *
	 * @param key The guest key: ByteArray of 16 bytes.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setGuestKey(key: ByteArray): Promise<Unit, Exception> {
		Log.i(TAG, "setGuestKey $key")
		return setConfig(ConfigType.KEY_GUEST, StateTypeV4.KEY_GUEST, ByteArrayPacket(key))
	}

	/**
	 * Set the iBeacon UUID.
	 *
	 * @param uuid The iBeacon UUID.
	 * @return Promise
	 */
	@Synchronized
	fun setIbeaconUuid(uuid: UUID, id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID, persistenceMode: PersistenceModeSet = PersistenceModeSet.STORED): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconUuid $uuid")
		return setConfig(ConfigType.IBEACON_PROXIMITY_UUID, StateTypeV4.IBEACON_PROXIMITY_UUID, UuidPacket(uuid), id = id, persistenceMode = persistenceMode)
	}

	/**
	 * Get the iBeacon UUID.
	 *
	 * @param uuid The iBeacon UUID.
	 * @return Promise
	 */
	@Synchronized
	fun getIbeaconUuid(id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID, persistenceMode: PersistenceModeGet = PersistenceModeGet.CURRENT): Promise<UUID, Exception> {
		Log.i(TAG, "getIbeaconUuid")
		if (getPacketProtocol() == PacketProtocol.V3) {
//			return getConfig(ConfigType.IBEACON_PROXIMITY_UUID)
//					.then {
//						val arr = it.getPayload()
//						if (arr == null || arr.size < 16) {
//							return@then Promise.ofFail<UUID, Exception>(Errors.Parse("payload of 16 expected"))
//						}
//						return@then Promise.ofSuccess<UUID, Exception>(Conversion.bytesToUuid16(arr))
//					}.unwrap()
			return Promise.ofFail(Errors.NotImplemented())
		}
		else {
			val resultPacket = UuidPacket()
			return getState(StateTypeV4.IBEACON_PROXIMITY_UUID, resultPacket, id = id, persistenceMode = persistenceMode)
					.then {
						return@then resultPacket.uuid
					}
		}
	}

	/**
	 * Set the iBeacon major.
	 *
	 * @param major The iBeacon major.
	 * @return Promise
	 */
	@Synchronized
	fun setIbeaconMajor(major: Uint16, id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID, persistenceMode: PersistenceModeSet = PersistenceModeSet.STORED): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconMajor $major")
		return setConfigValue(ConfigType.IBEACON_MAJOR, StateTypeV4.IBEACON_MAJOR, major, id = id, persistenceMode = persistenceMode)
	}

	/**
	 * Set the iBeacon minor.
	 *
	 * @param minor The iBeacon minor.
	 * @return Promise
	 */
	@Synchronized
	fun setIbeaconMinor(minor: Uint16, id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID, persistenceMode: PersistenceModeSet = PersistenceModeSet.STORED): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconMinor $minor")
		return setConfigValue(ConfigType.IBEACON_MINOR, StateTypeV4.IBEACON_MINOR, minor, id = id, persistenceMode = persistenceMode)
	}


	// --------------------------- //
	// --- setters and getters --- //
	// --------------------------- //

	/**
	 * Set the mesh access address.
	 *
	 * @param address The mesh access address.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setMeshAccessAddress(address: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "setMeshAccessAddress $address")
		return setConfigValue(ConfigType.MESH_ACCESS_ADDRESS, StateTypeV4.UNKNOWN, address)
	}

	/**
	 * Get the mesh access address.
	 *
	 * @return Promise with mesh access address as value.
	 */
	@Synchronized
	fun getMeshAccessAddress(): Promise<Uint32, Exception> {
		Log.i(TAG, "getMeshAccessAddress")
		return getConfigValue(ConfigType.MESH_ACCESS_ADDRESS, StateTypeV4.UNKNOWN)
	}

	/**
	 * Set the mesh channel.
	 *
	 * @param channel The channel to use, can be 37, 38, or 39.
	 * @return Promise
	 */
	@Synchronized
	fun setMeshChannel(channel: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setMeshChannel $channel")
		return when (channel.toInt()) {
			37, 38, 39 -> setConfigValue(ConfigType.MESH_CHANNEL, StateTypeV4.UNKNOWN, channel)
			else -> Promise.ofFail(Errors.ValueWrong())
		}
	}

	/**
	 * Get the mesh channel.
	 *
	 * @return Promise with mesh channel as value.
	 */
	@Synchronized
	fun getMeshChannel(): Promise<Uint8, Exception> {
		Log.i(TAG, "getMeshChannel")
		return getConfigValue(ConfigType.MESH_CHANNEL, StateTypeV4.UNKNOWN)
	}

	/**
	 * Set the TX power.
	 *
	 * @param power The TX power to use, can be -40, -20, -16, -12, -8, -4, 0, or 4.
	 * @return Promise
	 */
	@Synchronized
	fun setTxPower(power: Int8): Promise<Unit, Exception> {
		Log.i(TAG, "setTxPower $power")
		return when (power.toInt()) {
			-40, -20, -16, -12, -8, -4, 0, 4 -> setConfigValue(ConfigType.TX_POWER, StateTypeV4.TX_POWER, power)
			else -> Promise.ofFail(Errors.ValueWrong())
		}
	}

	/**
	 * Get the TX power.
	 *
	 * @return Promise with TX power as value.
	 */
	@Synchronized
	fun getTxPower(): Promise<Int8, Exception> {
		Log.i(TAG, "getTxPower")
		return getConfigValue(ConfigType.TX_POWER, StateTypeV4.TX_POWER)
	}

	/**
	 * Set the UART mode.
	 *
	 * @param mode Mode to set it to.
	 * @return Promise
	 */
	@Synchronized
	fun setUartEnabled(mode: UartMode): Promise<Unit, Exception> {
		Log.i(TAG, "setUartEnabled $mode")
		if (mode == UartMode.UNKNOWN) {
			return Promise.ofFail(Errors.ValueWrong())
		}
		if (getPacketProtocol() == PacketProtocol.V3) {
			val controlClass = Control(eventBus, connection)
			return controlClass.writeCommand(ControlType.UART_ENABLE, ControlTypeV4.UNKNOWN, mode.num)
		}
		else {
			return setConfigValue(ConfigType.UART_ENABLED, StateTypeV4.UART_ENABLED, mode.num)
		}
	}

	/**
	 * Get the UART mode.
	 *
	 * @return Promise with UartMode as value as value.
	 */
	@Synchronized
	fun getUartEnabled(): Promise<UartMode, Exception> {
		Log.i(TAG, "getUartEnabled")
		return getConfigValue<Uint8>(ConfigType.UART_ENABLED, StateTypeV4.UART_ENABLED)
				.then { UartMode.fromNum(it) }
	}

	/**
	 * Set relay high duration.
	 *
	 * @param timeMs Time in ms.
	 * @return Promise
	 */
	@Synchronized
	fun setRelayHigh(timeMs: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "setRelayHigh $timeMs")
		return setConfigValue(ConfigType.RELAY_HIGH_DURATION, StateTypeV4.RELAY_HIGH_DURATION, timeMs)
	}

	/**
	 * Get relay high duration.
	 *
	 * @return Promise with time in ms as value.
	 */
	@Synchronized
	fun getRelayHigh(): Promise<Uint16, Exception> {
		Log.i(TAG, "getRelayHigh")
		return getConfigValue(ConfigType.RELAY_HIGH_DURATION, StateTypeV4.RELAY_HIGH_DURATION)
	}


	/**
	 * Set PWM period.
	 *
	 * @param timeUs Time in μs.
	 * @return Promise
	 */
	@Synchronized
	fun setPwmPeriod(timeUs: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "setPwmPeriod $timeUs")
		return setConfigValue(ConfigType.PWM_PERIOD, StateTypeV4.PWM_PERIOD, timeUs)
	}

	/**
	 * Set PWM period.
	 *
	 * @return Promise with time in μs as value.
	 */
	@Synchronized
	fun getPwmPeriod(): Promise<Uint32, Exception> {
		Log.i(TAG, "getPwmPeriod")
		return getConfigValue(ConfigType.PWM_PERIOD, StateTypeV4.PWM_PERIOD)
	}


	/**
	 * Set boot delay.
	 *
	 * @param timeMs Time in ms.
	 * @return Promise
	 */
	@Synchronized
	fun setBootDelay(timeMs: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "setBootDelay $timeMs")
		return setConfigValue(ConfigType.BOOT_DELAY, StateTypeV4.BOOT_DELAY, timeMs)
	}

	/**
	 * Get boot delay.
	 *
	 * @return Promise with time in ms as value.
	 */
	@Synchronized
	fun getBootDelay(): Promise<Uint16, Exception> {
		Log.i(TAG, "getBootDelay")
		return getConfigValue(ConfigType.BOOT_DELAY, StateTypeV4.BOOT_DELAY)
	}


	/**
	 * Set current threshold.
	 *
	 * @param value Threshold in mA.
	 * @return Promise
	 */
	@Synchronized
	fun setCurrentThreshold(value: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "setCurrentThreshold $value")
		return setConfigValue(ConfigType.CURRENT_THRESHOLD, StateTypeV4.CURRENT_THRESHOLD, value)
	}

	/**
	 * Get current threshold.
	 *
	 * @return Promise with threshold in mA as value.
	 */
	@Synchronized
	fun getCurrentThreshold(): Promise<Uint16, Exception> {
		Log.i(TAG, "getCurrentThreshold")
		return getConfigValue(ConfigType.CURRENT_THRESHOLD, StateTypeV4.CURRENT_THRESHOLD)
	}

	/**
	 * Set current threshold of dimmer.
	 *
	 * @param value Threshold in mA.
	 * @return Promise
	 */
	@Synchronized
	fun setCurrentThresholdDimmer(value: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "setCurrentThresholdDimmer $value")
		return setConfigValue(ConfigType.CURRENT_THRESHOLD_DIMMER, StateTypeV4.CURRENT_THRESHOLD_DIMMER, value)
	}

	/**
	 * Get current threshold of dimmer in mA.
	 *
	 * @return Promise with threshold in mA as value.
	 */
	@Synchronized
	fun getCurrentThresholdDimmer(): Promise<Uint16, Exception> {
		Log.i(TAG, "getCurrentThresholdDimmer")
		return getConfigValue(ConfigType.CURRENT_THRESHOLD_DIMMER, StateTypeV4.CURRENT_THRESHOLD_DIMMER)
	}


	/**
	 * Set chip temperature threshold.
	 *
	 * @param celcius The threshold in °C.
	 * @return Promise
	 */
	@Synchronized
	fun setMaxChipTemp(celcius: Int8): Promise<Unit, Exception> {
		Log.i(TAG, "setMaxChipTemp $celcius")
		return setConfigValue(ConfigType.MAX_CHIP_TEMP, StateTypeV4.MAX_CHIP_TEMP, celcius)
	}

	/**
	 * Get chip temperature threshold.
	 *
	 * @return Promise with threshold in °C as value.
	 */
	@Synchronized
	fun getMaxChipTemp(): Promise<Int8, Exception> {
		Log.i(TAG, "getMaxChipTemp")
		return getConfigValue(ConfigType.MAX_CHIP_TEMP, StateTypeV4.MAX_CHIP_TEMP)
	}

	/**
	 * Set dimmer temperature upper threshold.
	 *
	 * @param value The threshold in V.
	 * @return Promise
	 */
	@Synchronized
	fun setDimmerTempUpThreshold(value: Float): Promise<Unit, Exception> {
		Log.i(TAG, "setDimmerTempUpThreshold $value")
		return setConfigValue(ConfigType.DIMMER_TEMP_UP, StateTypeV4.DIMMER_TEMP_UP, value)
	}

	/**
	 * Get dimmer temperature upper threshold.
	 *
	 * @return Promise with threshold in V as value.
	 */
	@Synchronized
	fun getDimmerTempUpThreshold(): Promise<Float, Exception> {
		Log.i(TAG, "getDimmerTempUpThreshold")
		return getConfigValue(ConfigType.DIMMER_TEMP_UP, StateTypeV4.DIMMER_TEMP_UP)
	}

	/**
	 * Set dimmer temperature lower threshold.
	 *
	 * @param value The threshold in V.
	 * @return Promise
	 */
	@Synchronized
	fun setDimmerTempDownThreshold(value: Float): Promise<Unit, Exception> {
		Log.i(TAG, "setDimmerTempDownThreshold $value")
		return setConfigValue(ConfigType.DIMMER_TEMP_DOWN, StateTypeV4.DIMMER_TEMP_DOWN, value)
	}

	/**
	 * Get dimmer temperature lower threshold.
	 *
	 * @return Promise with threshold in V as value.
	 */
	@Synchronized
	fun getDimmerTempDownThreshold(): Promise<Float, Exception> {
		Log.i(TAG, "getDimmerTempDownThreshold")
		return getConfigValue(ConfigType.DIMMER_TEMP_DOWN, StateTypeV4.DIMMER_TEMP_DOWN)
	}

	/**
	 * Set voltage multiplier.
	 *
	 * @param value The multiplier.
	 * @return Promise
	 */
	@Synchronized
	fun setVoltageMultiplier(value: Float): Promise<Unit, Exception> {
		Log.i(TAG, "setVoltageMultiplier $value")
		return setConfigValue(ConfigType.VOLTAGE_MULTIPLIER, StateTypeV4.VOLTAGE_MULTIPLIER, value)
	}

	/**
	 * Get voltage multiplier.
	 *
	 * @return Promise with multiplier as value.
	 */
	@Synchronized
	fun getVoltageMultiplier(): Promise<Float, Exception> {
		Log.i(TAG, "getVoltageMultiplier")
		return getConfigValue(ConfigType.VOLTAGE_MULTIPLIER, StateTypeV4.VOLTAGE_MULTIPLIER)
	}

	/**
	 * Set current multiplier.
	 *
	 * @param value The multiplier.
	 * @return Promise
	 */
	@Synchronized
	fun setCurrentMultiplier(value: Float): Promise<Unit, Exception> {
		Log.i(TAG, "setCurrentMultiplier $value")
		return setConfigValue(ConfigType.CURRENT_MULTIPLIER, StateTypeV4.CURRENT_MULTIPLIER, value)
	}

	/**
	 * Get current multiplier.
	 *
	 * @return Promise with multiplier as value.
	 */
	@Synchronized
	fun getCurrentMultiplier(): Promise<Float, Exception> {
		Log.i(TAG, "getCurrentMultiplier")
		return getConfigValue(ConfigType.CURRENT_MULTIPLIER, StateTypeV4.CURRENT_MULTIPLIER)
	}

	/**
	 * Set voltage zero offset.
	 *
	 * @param value The voltage zero offset
	 * @return Promise
	 */
	@Synchronized
	fun setVoltageZero(value: Int32): Promise<Unit, Exception> {
		Log.i(TAG, "setVoltageZero $value")
		return setConfigValue(ConfigType.VOLTAGE_ZERO, StateTypeV4.VOLTAGE_ZERO, value)
	}

	/**
	 * Get voltage zero offset.
	 *
	 * @return Promise with voltage zero offset as value.
	 */
	@Synchronized
	fun getVoltageZero(): Promise<Int32, Exception> {
		Log.i(TAG, "getVoltageZero")
		return getConfigValue(ConfigType.VOLTAGE_ZERO, StateTypeV4.VOLTAGE_ZERO)
	}

	/**
	 * Set current zero offset.
	 *
	 * @param value The current zero offset
	 * @return Promise
	 */
	@Synchronized
	fun setCurrentZero(value: Int32): Promise<Unit, Exception> {
		Log.i(TAG, "setCurrentZero $value")
		return setConfigValue(ConfigType.CURRENT_ZERO, StateTypeV4.CURRENT_ZERO, value)
	}

	/**
	 * Get current zero offset.
	 *
	 * @return Promise with current zero offset as value.
	 */
	@Synchronized
	fun getCurrentZero(): Promise<Int32, Exception> {
		Log.i(TAG, "getCurrentZero")
		return getConfigValue(ConfigType.CURRENT_ZERO, StateTypeV4.CURRENT_ZERO)
	}

	/**
	 * Set power measurement offset.
	 *
	 * @param value The offset in mW.
	 * @return Promise
	 */
	@Synchronized
	fun setPowerZero(milliWatt: Int32): Promise<Unit, Exception> {
		Log.i(TAG, "setPowerZero $milliWatt")
		return setConfigValue(ConfigType.POWER_ZERO, StateTypeV4.POWER_ZERO, milliWatt)
	}

	/**
	 * Get power measurement offset.
	 *
	 * @return Promise with offset in mW as value.
	 */
	@Synchronized
	fun getPowerZero(): Promise<Int32, Exception> {
		Log.i(TAG, "getPowerZero")
		return getConfigValue(ConfigType.POWER_ZERO, StateTypeV4.POWER_ZERO)
	}

	/**
	 * Enable or disable switchcraft.
	 *
	 * @param enable True to enable switchcraft.
	 * @return Promise
	 */
	@Synchronized
	fun setSwitchCraftEnabled(enable: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "setSwitchCraftEnabled $enable")
		if (getPacketProtocol() == PacketProtocol.V3) {
			val controlClass = Control(eventBus, connection)
			return controlClass.writeCommand(ControlType.ENABLE_SWITCHCRAFT, ControlTypeV4.UNKNOWN, enable)
		}
		else {
			return setConfigValue(ConfigType.UNKNOWN, StateTypeV4.SWITCHCRAFT_ENABLED, enable)
		}
	}

	/**
	 * Get switchcraft enabled.
	 *
	 * @return Promise with enabled as value.
	 */
	@Synchronized
	fun getSwitchCraftEnabled(enable: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "getSwitchCraftEnabled $enable")
		return getConfigValue(ConfigType.SWITCHCRAFT_ENABLED, StateTypeV4.SWITCHCRAFT_ENABLED)
	}

	/**
	 * Set switchcraft threshold.
	 *
	 * @param value The threshold.
	 * @return Promise
	 */
	@Synchronized
	fun setSwitchCraftThreshold(value: Float): Promise<Unit, Exception> {
		Log.i(TAG, "setSwitchCraftThreshold $value")
		return setConfigValue(ConfigType.SWITCHCRAFT_THRESHOLD, StateTypeV4.SWITCHCRAFT_THRESHOLD, value)
	}

	/**
	 * Get switchcraft threshold.
	 *
	 * @return Promise with threshold as value.
	 */
	@Synchronized
	fun getSwitchCraftThreshold(): Promise<Float, Exception> {
		Log.i(TAG, "getSwitchCraftThreshold")
		return getConfigValue(ConfigType.SWITCHCRAFT_THRESHOLD, StateTypeV4.SWITCHCRAFT_THRESHOLD)
	}

	/**
	 * Enable or disable tap to toggle.
	 *
	 * @param value True to enable.
	 * @return Promise
	 */
	@Synchronized
	fun setTapToToggleEnabled(value: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "setTapToToggleEnabled $value")
		return setConfigValue(ConfigType.UNKNOWN, StateTypeV4.TAP_TO_TOGGLE_ENABLED, value)
	}

	/**
	 * Get tap to toggle enabled.
	 *
	 * @return Promise with enabled as value.
	 */
	@Synchronized
	fun getTapToToggleEnabled(): Promise<Boolean, Exception> {
		Log.i(TAG, "getTapToToggleEnabled")
		return getConfigValue(ConfigType.UNKNOWN, StateTypeV4.TAP_TO_TOGGLE_ENABLED)
	}

	/**
	 * Set tap to toggle rssi threshold offset.
	 *
	 * @param value The offset.
	 * @return Promise
	 */
	@Synchronized
	fun setTapToToggleRssiThresholdOffset(value: Int8): Promise<Unit, Exception> {
		Log.i(TAG, "setTapToToggleRssiThresholdOffset $value")
		return setConfigValue(ConfigType.UNKNOWN, StateTypeV4.TAP_TO_TOGGLE_RSSI_THRESHOLD_OFFSET, value)
	}

	/**
	 * Get tap to toggle rssi threshold offset.
	 *
	 * @return Promise with offset as value.
	 */
	@Synchronized
	fun getTapToToggleRssiThresholdOffset(): Promise<Int8, Exception> {
		Log.i(TAG, "getTapToToggleRssiThresholdOffset")
		return getConfigValue(ConfigType.UNKNOWN, StateTypeV4.TAP_TO_TOGGLE_RSSI_THRESHOLD_OFFSET)
	}

	/**
	 * Set sun time.
	 *
	 * @param sunRiseAfterMidnight     Seconds after midnight at which the sun rises.
	 * @param sunSetAfterMidnight      Seconds after midnight at which the sun sets.
	 * @return Promise
	 */
	@Synchronized
	fun setSunTime(sunRiseAfterMidnight: Uint32, sunSetAfterMidnight: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "setSunTime $sunRiseAfterMidnight $sunSetAfterMidnight")
		if (getPacketProtocol() == PacketProtocol.V3) {
			Log.w(TAG, "Old protocol: no suntime will be written.")
			return Promise.ofSuccess(Unit)
		}
		return setConfig(ConfigType.UNKNOWN, StateTypeV4.SUN_TIME, SunTimePacket(sunRiseAfterMidnight, sunSetAfterMidnight))
	}

	/**
	 * Get sun time.
	 *
	 * @return Promise with sun time packet as value.
	 */
	@Synchronized
	fun getSunTime(): Promise<SunTimePacket, Exception> {
		Log.i(TAG, "getSunTime")
		return getState(StateTypeV4.SUN_TIME, SunTimePacket())
	}

	/**
	 * Set time.
	 *
	 * @param currentTime     POSIX timestamp.
	 * @return Promise
	 */
	@Synchronized
	@Deprecated("Use control command instead.")
	fun setTime(currentTime: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "setTime $currentTime")
		return setConfigValue(ConfigType.UNKNOWN, StateTypeV4.TIME, currentTime)
	}

	/**
	 * Get current time.
	 *
	 * @return Promise with POSIX timestamp as value.
	 */
	@Synchronized
	fun getTime(): Promise<Uint32, Exception> {
		Log.i(TAG, "getTime")
		return getConfigValue(ConfigType.UNKNOWN, StateTypeV4.TIME)
	}

	/**
	 * Set soft on speed.
	 *
	 * @param speed          Speed ranging from 1 until 100.
	 * @return Promise
	 */
	@Synchronized
	fun setSoftOnSpeed(speed: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setSoftOnSpeed $speed")
		return setConfigValue(ConfigType.UNKNOWN, StateTypeV4.SOFT_ON_SPEED, speed)
	}

	/**
	 * Get current soft on speed.
	 *
	 * @return Promise with soft on speed as value.
	 */
	@Synchronized
	fun getSoftOnSpeed(): Promise<Uint8, Exception> {
		Log.i(TAG, "getSoftOnSpeed")
		return getConfigValue(ConfigType.UNKNOWN, StateTypeV4.SOFT_ON_SPEED)
	}

	/**
	 * Set uart key.
	 *
	 * @param uartKey        Key to encrypt the UART (16 byte long).
	 * @return Promise
	 */
	@Synchronized
	fun setUartKey(uartKey: ByteArray): Promise<Unit, Exception> {
		Log.i(TAG, "setUartKey $uartKey")
		if (uartKey.size != 16) {
			return Promise.ofFail(Errors.SizeWrong())
		}
		return setConfig(ConfigType.UNKNOWN, StateTypeV4.UART_KEY, ByteArrayPacket(uartKey))
	}

	/**
	 * Set behaviour settings.
	 * This should normally not be done via config, but via a broadcast.
	 *
	 * @param settings       The new behaviour settings.
	 * @return Promise
	 */
	@Synchronized
	fun setBehaviourSettings(settings: BehaviourSettings): Promise<Unit, Exception> {
		Log.i(TAG, "setBehaviourSettings $settings")
		return setConfigValue(ConfigType.UNKNOWN, StateTypeV4.BEHAVIOUR_SETTINGS, settings.num)
	}

	// ------------------------ //
	// --- helper functions --- //
	// ------------------------ //

	private inline fun <reified T>getConfigValue(
			type: ConfigType,
			type4: StateTypeV4,
			id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID,
			persistenceMode: PersistenceModeGet = PersistenceModeGet.CURRENT
	): Promise<T, Exception> {
		if (getPacketProtocol() == PacketProtocol.V3) {
			return getConfig(type)
					.then {
						val arr = it.getPayload()
						if (arr == null) {
							return@then Promise.ofFail<T, Exception>(Errors.Parse("config payload expected"))
						}
						try {
							val value = Conversion.byteArrayTo<T>(arr)
							return@then Promise.ofSuccess<T, Exception>(value)
						} catch (ex: Exception) {
							return@then Promise.ofFail<T, Exception>(ex)
						}
					}.unwrap()
		}
		else {
			return getStateValue(type4, id, persistenceMode)
		}
	}

	internal inline fun <reified T>getStateValue(
			type: StateTypeV4,
			id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID,
			persistenceMode: PersistenceModeGet = PersistenceModeGet.CURRENT
	): Promise<T, Exception> {
		Log.i(TAG, "getStateValue value type=${T::class}")
		val arrPacket = ByteArrayPacket()
		return getState(type, arrPacket, id, persistenceMode)
				.then {
					try {
						Log.i(TAG, "class: ${T::class}")
						val value = Conversion.byteArrayTo<T>(it.getPayload())
						return@then Promise.ofSuccess<T, Exception>(value)
					} catch (ex: Exception) {
						return@then Promise.ofFail<T, Exception>(ex)
					}
				}.unwrap()
	}

	private fun getConfig(type: ConfigType): Promise<ConfigPacket, Exception> {
		val writeCommand = fun (): Promise<Unit, Exception> {
			return connection.write(getServiceUuid(), getCharacteristicWriteUuid(), ConfigPacket(type).getArray())
		}
		val deferred = deferred<ConfigPacket, Exception>()
		connection.getSingleMergedNotification(getServiceUuid(), getCharacteristicReadUuid(), writeCommand, BluenetConfig.TIMEOUT_GET_CONFIG)
				.success {
					val configPacket = ConfigPacket()
					if (!configPacket.fromArray(it) || configPacket.type != type.num) {
						deferred.reject(Errors.Parse("can't make a ConfigPacket from ${Conversion.bytesToString(it)}"))
						return@success
					}
					deferred.resolve(configPacket)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	internal fun <T : PacketInterface>getState(
			stateType: StateTypeV4,
			statePayloadPacket: T,
			id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID,
			persistenceMode: PersistenceModeGet = PersistenceModeGet.CURRENT
	): Promise<T, Exception> {
		val resultClass = Result(eventBus, connection)
		val controlClass = Control(eventBus, connection)
		val deferred = deferred<T, Exception>()

		if (getPacketProtocol() == PacketProtocol.V4) {
			val writeCommand = fun(): Promise<Unit, Exception> {
				val statePacket = StatePacketV4(stateType, id, null)
				return controlClass.writeGetState(statePacket)
			}
			val statePacket = StatePacketV4(stateType, id, statePayloadPacket)
			resultClass.getSingleResult(writeCommand, ConnectionProtocol.UNKNOWN, ControlTypeV4.GET_STATE, statePacket, BluenetConfig.TIMEOUT_GET_CONFIG)
					.success {
						if (statePacket.type != stateType) {
							deferred.reject(Errors.Parse("Wrong state type: req=$stateType rec=${statePacket.type}"))
							return@success
						}
						deferred.resolve(statePayloadPacket)
					}
					.fail {
						deferred.reject(it)
					}
		}
		else {
			val writeCommand = fun(): Promise<Unit, Exception> {
				val statePacket = StatePacketV5(stateType, id, persistenceMode.num, null)
				return controlClass.writeGetState(statePacket)
			}
			val statePacket = StatePacketV5(stateType, id, persistenceMode.num, statePayloadPacket)
			resultClass.getSingleResult(writeCommand, ConnectionProtocol.V5, ControlTypeV4.GET_STATE, statePacket, BluenetConfig.TIMEOUT_GET_CONFIG)
					.success {
						if (statePacket.type != stateType) {
							deferred.reject(Errors.Parse("Wrong state type: req=$stateType rec=${statePacket.type}"))
							return@success
						}
						deferred.resolve(statePayloadPacket)
					}
					.fail {
						deferred.reject(it)
					}
		}
		return deferred.promise
	}

	private inline fun <reified T>setConfigValue(
			type: ConfigType,
			type4: StateTypeV4,
			value: T,
			id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID,
			persistenceMode: PersistenceModeSet = PersistenceModeSet.STORED
	): Promise<Unit, Exception> {
		return setConfig(type, type4, ByteArrayPacket(Conversion.toByteArray(value)), id, persistenceMode)
	}

	private fun setConfig(type: ConfigType,
						  type4: StateTypeV4,
						  packet: PacketInterface,
						  id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID,
						  persistenceMode: PersistenceModeSet = PersistenceModeSet.STORED
	): Promise<Unit, Exception> {
		if (getPacketProtocol() == PacketProtocol.V3) {
			val configPacket = ConfigPacket(type, packet)
			Log.i(TAG, "setConfig $configPacket")
			return connection.write(getServiceUuid(), getCharacteristicWriteUuid(), configPacket.getArray())
		}
		else {
			return setState(type4, packet, id, persistenceMode)
		}
	}

	internal fun setState(type: StateTypeV4,
						  packet: PacketInterface,
						  id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID,
						  persistenceMode: PersistenceModeSet = PersistenceModeSet.STORED
	): Promise<Unit, Exception> {
		val controlClass = Control(eventBus, connection)
		if (getPacketProtocol() == PacketProtocol.V4) {
			val statePacket = StatePacketV4(type, id, packet)
			Log.i(TAG, "setState $statePacket")
			return controlClass.writeSetState(statePacket)
		}
		else {
			val statePacket = StatePacketV5(type, id, persistenceMode.num, packet)
			Log.i(TAG, "setState $statePacket")
			return controlClass.writeSetState(statePacket)
		}
	}

	private fun getServiceUuid(): UUID {
		return when (connection.mode) {
			CrownstoneMode.SETUP -> BluenetProtocol.SETUP_SERVICE_UUID
			else -> BluenetProtocol.CROWNSTONE_SERVICE_UUID
		}
	}

	private fun getCharacteristicWriteUuid(): UUID {
		val packetProtocol = getPacketProtocol()
		if (connection.mode == CrownstoneMode.SETUP) {
			return when (packetProtocol) {
				PacketProtocol.V3 -> BluenetProtocol.CHAR_SETUP_CONFIG_CONTROL_UUID
				PacketProtocol.V4 -> BluenetProtocol.CHAR_SETUP_CONTROL4_UUID
				PacketProtocol.V5 -> BluenetProtocol.CHAR_SETUP_CONTROL5_UUID
			}
		}
		else {
			return when (packetProtocol) {
				PacketProtocol.V3 -> BluenetProtocol.CHAR_CONFIG_CONTROL_UUID
				PacketProtocol.V4 -> BluenetProtocol.CHAR_CONTROL4_UUID
				PacketProtocol.V5 -> BluenetProtocol.CHAR_CONTROL5_UUID
			}
		}
	}

	private fun getCharacteristicReadUuid(): UUID {
		val packetProtocol = getPacketProtocol()
		if (connection.mode == CrownstoneMode.SETUP) {
			return when (packetProtocol) {
				PacketProtocol.V3 -> BluenetProtocol.CHAR_SETUP_CONFIG_READ_UUID
				PacketProtocol.V4 -> BluenetProtocol.CHAR_SETUP_RESULT_UUID
				PacketProtocol.V5 -> BluenetProtocol.CHAR_SETUP_RESULT5_UUID
			}
		}
		else {
			return when (packetProtocol) {
				PacketProtocol.V3 -> BluenetProtocol.CHAR_CONFIG_READ_UUID
				PacketProtocol.V4 -> BluenetProtocol.CHAR_RESULT_UUID
				PacketProtocol.V5 -> BluenetProtocol.CHAR_RESULT5_UUID
			}
		}
	}

	private fun getPacketProtocol(): PacketProtocol {
		return connection.getPacketProtocol()
	}
}
