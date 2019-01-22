/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import android.util.Log
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.packets.ConfigPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
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
		val stoneId = Conversion.toUint16(id) // Is still uint16
		return setConfigValue(ConfigType.CROWNSTONE_ID, stoneId)
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
		return setConfig(ConfigPacket(ConfigType.KEY_ADMIN, key))
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
		return setConfig(ConfigPacket(ConfigType.KEY_MEMBER, key))
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
		return setConfig(ConfigPacket(ConfigType.KEY_GUEST, key))
	}

	/**
	 * Set the iBeacon UUID.
	 *
	 * @param uuid The iBeacon UUID.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setIbeaconUuid(uuid: UUID): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconUuid $uuid")
		return setConfig(ConfigPacket(ConfigType.IBEACON_PROXIMITY_UUID, Conversion.uuidToBytes(uuid)))
	}

	/**
	 * Set the iBeacon major.
	 *
	 * @param major The iBeacon major.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setIbeaconMajor(major: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconMajor $major")
		return setConfigValue(ConfigType.IBEACON_MAJOR, major)
	}

	/**
	 * Set the iBeacon minor.
	 *
	 * @param minor The iBeacon minor.
	 * @return Promise
	 */
	@Deprecated("Should be set during setup")
	@Synchronized
	fun setIbeaconMinor(minor: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconMinor $minor")
		return setConfigValue(ConfigType.IBEACON_MINOR, minor)
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
		return setConfigValue(ConfigType.MESH_ACCESS_ADDRESS, address)
	}

	/**
	 * Get the mesh access address.
	 *
	 * @return Promise with mesh access address as value.
	 */
	@Synchronized
	fun getMeshAccessAddress(): Promise<Uint32, Exception> {
		Log.i(TAG, "getMeshAccessAddress")
		return getConfigValue(ConfigType.MESH_ACCESS_ADDRESS)
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
			37, 38, 39 -> setConfigValue(ConfigType.MESH_CHANNEL, channel)
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
		return getConfigValue(ConfigType.MESH_CHANNEL)
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
			-40, -20, -16, -12, -8, -4, 0, 4 -> setConfigValue(ConfigType.TX_POWER, power)
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
		return getConfigValue(ConfigType.TX_POWER)
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
		return when (mode) {
			UartMode.UNKNOWN -> Promise.ofFail(Errors.ValueWrong())
			else -> setConfigValue(ConfigType.UART_ENABLED, mode.num)
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
		return getConfigValue<Uint8>(ConfigType.UART_ENABLED)
				.then { UartMode.fromNum(it) }
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
		return setConfigValue(ConfigType.SWITCHCRAFT_THRESHOLD, value)
	}

	/**
	 * Set switchcraft threshold.
	 *
	 * @return Promise with threshold as value.
	 */
	@Synchronized
	fun getSwitchCraftThreshold(): Promise<Unit, Exception> {
		Log.i(TAG, "getSwitchCraftThreshold")
		return setConfigValue(ConfigType.SWITCHCRAFT_THRESHOLD, Float)
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
		return setConfigValue(ConfigType.RELAY_HIGH_DURATION, timeMs)
	}

	/**
	 * Get relay high duration.
	 *
	 * @return Promise with time in ms as value.
	 */
	@Synchronized
	fun getRelayHigh(): Promise<Uint16, Exception> {
		Log.i(TAG, "getRelayHigh")
		return getConfigValue(ConfigType.RELAY_HIGH_DURATION)
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
		return setConfigValue(ConfigType.PWM_PERIOD, timeUs)
	}

	/**
	 * Set PWM period.
	 *
	 * @return Promise with time in μs as value.
	 */
	@Synchronized
	fun getPwmPeriod(): Promise<Uint32, Exception> {
		Log.i(TAG, "getPwmPeriod")
		return getConfigValue(ConfigType.PWM_PERIOD)
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
		return setConfigValue(ConfigType.BOOT_DELAY, timeMs)
	}

	/**
	 * Get boot delay.
	 *
	 * @return Promise with time in ms as value.
	 */
	@Synchronized
	fun getBootDelay(): Promise<Uint16, Exception> {
		Log.i(TAG, "getBootDelay")
		return getConfigValue(ConfigType.BOOT_DELAY)
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
		return setConfigValue(ConfigType.CURRENT_THRESHOLD, value)
	}

	/**
	 * Get current threshold.
	 *
	 * @return Promise with threshold in mA as value.
	 */
	@Synchronized
	fun getCurrentThreshold(): Promise<Uint16, Exception> {
		Log.i(TAG, "getCurrentThreshold")
		return getConfigValue(ConfigType.CURRENT_THRESHOLD)
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
		return setConfigValue(ConfigType.CURRENT_THRESHOLD_DIMMER, value)
	}

	/**
	 * Get current threshold of dimmer in mA.
	 *
	 * @return Promise with threshold in mA as value.
	 */
	@Synchronized
	fun getCurrentThresholdDimmer(): Promise<Uint16, Exception> {
		Log.i(TAG, "getCurrentThresholdDimmer")
		return getConfigValue(ConfigType.CURRENT_THRESHOLD_DIMMER)
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
		return setConfigValue(ConfigType.MAX_CHIP_TEMP, celcius)
	}

	/**
	 * Get chip temperature threshold.
	 *
	 * @return Promise with threshold in °C as value.
	 */
	@Synchronized
	fun getMaxChipTemp(): Promise<Int8, Exception> {
		Log.i(TAG, "getMaxChipTemp")
		return getConfigValue(ConfigType.MAX_CHIP_TEMP)
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
		return setConfigValue(ConfigType.DIMMER_TEMP_UP, value)
	}

	/**
	 * Get dimmer temperature upper threshold.
	 *
	 * @return Promise with threshold in V as value.
	 */
	@Synchronized
	fun getDimmerTempUpThreshold(): Promise<Float, Exception> {
		Log.i(TAG, "getDimmerTempUpThreshold")
		return getConfigValue(ConfigType.DIMMER_TEMP_UP)
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
		return setConfigValue(ConfigType.DIMMER_TEMP_DOWN, value)
	}

	/**
	 * Get dimmer temperature lower threshold.
	 *
	 * @return Promise with threshold in V as value.
	 */
	@Synchronized
	fun getDimmerTempDownThreshold(): Promise<Float, Exception> {
		Log.i(TAG, "getDimmerTempDownThreshold")
		return getConfigValue(ConfigType.DIMMER_TEMP_DOWN)
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
		return setConfigValue(ConfigType.VOLTAGE_MULTIPLIER, value)
	}

	/**
	 * Get voltage multiplier.
	 *
	 * @return Promise with multiplier as value.
	 */
	@Synchronized
	fun getVoltageMultiplier(): Promise<Float, Exception> {
		Log.i(TAG, "getVoltageMultiplier")
		return getConfigValue(ConfigType.VOLTAGE_MULTIPLIER)
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
		return setConfigValue(ConfigType.CURRENT_MULTIPLIER, value)
	}

	/**
	 * Get current multiplier.
	 *
	 * @return Promise with multiplier as value.
	 */
	@Synchronized
	fun getCurrentMultiplier(): Promise<Float, Exception> {
		Log.i(TAG, "getCurrentMultiplier")
		return getConfigValue(ConfigType.CURRENT_MULTIPLIER)
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
		return setConfigValue(ConfigType.POWER_ZERO, milliWatt)
	}

	/**
	 * Get power measurement offset.
	 *
	 * @return Promise with offset in mW as value.
	 */
	@Synchronized
	fun getPowerZero(): Promise<Int32, Exception> {
		Log.i(TAG, "getPowerZero")
		return getConfigValue(ConfigType.POWER_ZERO)
	}


	// ------------------------ //
	// --- helper functions --- //
	// ------------------------ //

	private inline fun <reified T>getConfigValue(type: ConfigType): Promise<T, Exception> {
		return getConfig(type)
				.then {
					val arr = it.getPayload()
					if (arr == null) {
						return@then Promise.ofFail<T, Exception>(Errors.Parse("config payload expected"))
					}
					try {
						val value = Conversion.byteArrayTo<T>(arr)
						return@then Promise.ofSuccess<T, Exception>(value)
					}
					catch (ex: Exception) {
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
					}
					deferred.resolve(configPacket)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	private inline fun <reified T>setConfigValue(type: ConfigType, value: T): Promise<Unit, Exception> {
		val config = ConfigPacket(type, Conversion.toByteArray(value))
		return setConfig(config)
	}

	private fun setConfig(config: ConfigPacket): Promise<Unit, Exception> {
		Log.i(TAG, "setConfig $config")
		if (config.opCode != OpcodeType.WRITE) {
			return Promise.ofFail(Errors.OpcodeWrong())
		}
		return connection.write(getServiceUuid(), getCharacteristicWriteUuid(), config.getArray())
	}

	private fun getServiceUuid(): UUID {
		return when (connection.mode) {
			CrownstoneMode.SETUP -> BluenetProtocol.SETUP_SERVICE_UUID
			else -> BluenetProtocol.CROWNSTONE_SERVICE_UUID
		}
	}

	private fun getCharacteristicWriteUuid(): UUID {
		return when (connection.mode) {
			CrownstoneMode.SETUP -> BluenetProtocol.CHAR_SETUP_CONFIG_CONTROL_UUID
			else -> BluenetProtocol.CHAR_CONFIG_CONTROL_UUID
		}
	}

	private fun getCharacteristicReadUuid(): UUID {
		return when (connection.mode) {
			CrownstoneMode.SETUP -> BluenetProtocol.CHAR_SETUP_CONFIG_READ_UUID
			else -> BluenetProtocol.CHAR_CONFIG_READ_UUID
		}
	}
}