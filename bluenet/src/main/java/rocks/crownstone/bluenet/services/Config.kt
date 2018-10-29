package rocks.crownstone.bluenet.services

import android.util.Log
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.services.packets.ConfigPacket
import rocks.crownstone.bluenet.util.Conversion
import java.util.*

class Config(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	// --- setters only --- //


	fun setCrownstoneId(id: Uint8): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.CROWNSTONE_ID, id)
	}

	fun setAdminKey(key: ByteArray): Promise<Unit, Exception> {
		return setConfig(ConfigPacket(ConfigType.KEY_ADMIN, key))
	}

	fun setMemberKey(key: ByteArray): Promise<Unit, Exception> {
		return setConfig(ConfigPacket(ConfigType.KEY_MEMBER, key))
	}

	fun setGuestKey(key: ByteArray): Promise<Unit, Exception> {
		return setConfig(ConfigPacket(ConfigType.KEY_GUEST, key))
	}

	fun setIbeaconUuid(uuid: UUID): Promise<Unit, Exception> {
		return setConfig(ConfigPacket(ConfigType.IBEACON_PROXIMITY_UUID, Conversion.uuidToBytes(uuid)))
	}

	fun setIbeaconMajor(major: Uint16): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.IBEACON_MAJOR, major)
	}

	fun setIbeaconMinor(minor: Uint16): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.IBEACON_MINOR, minor)
	}


	// --- setters and getters --- //


	fun setMeshAccessAddress(address: Uint32): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.MESH_ACCESS_ADDRESS, address)
	}

	fun getMeshAccessAddress(): Promise<Uint32, Exception> {
		return getConfigValue(ConfigType.MESH_ACCESS_ADDRESS)
	}


	fun setMeshChannel(channel: Uint8): Promise<Unit, Exception> {
		return when (channel.toInt()) {
			37,38,39 -> setConfigValue(ConfigType.MESH_CHANNEL, channel)
			else -> Promise.ofFail(Errors.ValueWrong())
		}
	}

	fun getMeshChannel(): Promise<Uint8, Exception> {
		return getConfigValue(ConfigType.MESH_CHANNEL)
	}


	fun setTxPower(power: Int8): Promise<Unit, Exception> {
		return when (power.toInt()) {
			-40, -20, -16, -12, -8, -4, 0, 4 -> setConfigValue(ConfigType.TX_POWER, power)
			else -> Promise.ofFail(Errors.ValueWrong())
		}
	}

	fun getTxPower(): Promise<Int8, Exception> {
		return getConfigValue(ConfigType.TX_POWER)
	}


	fun setUartEnabled(value: Uint8): Promise<Unit, Exception> {
		return when (value.toInt()) {
			0, 1, 3 -> setConfigValue(ConfigType.UART_ENABLED, value)
			else -> Promise.ofFail(Errors.ValueWrong())
		}
	}

	fun getUartEnabled(): Promise<Uint8, Exception> {
		return getConfigValue(ConfigType.UART_ENABLED)
	}


	fun setSwitchCraftThreshold(value: Float): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.SWITCHCRAFT_THRESHOLD, value)
	}

	fun getSwitchCraftThreshold(): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.SWITCHCRAFT_THRESHOLD, Float)
	}


	fun setRelayHigh(timeMs: Uint16): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.RELAY_HIGH_DURATION, timeMs)
	}

	fun getRelayHigh(): Promise<Uint16, Exception> {
		return getConfigValue(ConfigType.RELAY_HIGH_DURATION)
	}


	// time in μs
	fun setPwmPeriod(timeUs: Uint32): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.PWM_PERIOD, timeUs)
	}

	fun getPwmPeriod(): Promise<Uint32, Exception> {
		return getConfigValue(ConfigType.PWM_PERIOD)
	}


	// time in ms
	fun setBootDelay(timeMs: Uint16): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.BOOT_DELAY, timeMs)
	}

	fun getBootDelay(): Promise<Uint16, Exception> {
		return getConfigValue(ConfigType.BOOT_DELAY)
	}


	// value in mA
	fun setCurrentThreshold(value: Uint16): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.CURRENT_THRESHOLD, value)
	}

	fun getCurrentThreshold(): Promise<Uint16, Exception> {
		return getConfigValue(ConfigType.CURRENT_THRESHOLD)
	}


	fun setCurrentThresholdDimmer(value: Uint16): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.CURRENT_THRESHOLD_DIMMER, value)
	}

	fun getCurrentThresholdDimmer(): Promise<Uint16, Exception> {
		return getConfigValue(ConfigType.CURRENT_THRESHOLD_DIMMER)
	}


	fun setMaxChipTemp(celcius: Int8): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.MAX_CHIP_TEMP, celcius)
	}

	fun getMaxChipTemp(): Promise<Int8, Exception> {
		return getConfigValue(ConfigType.MAX_CHIP_TEMP)
	}


	fun setDimmerTempUpThreshold(value: Float): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.DIMMER_TEMP_UP, value)
	}

	fun getDimmerTempUpThreshold(): Promise<Float, Exception> {
		return getConfigValue(ConfigType.DIMMER_TEMP_UP)
	}


	fun setDimmerTempDownThreshold(value: Float): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.DIMMER_TEMP_DOWN, value)
	}

	fun getDimmerTempDownThreshold(): Promise<Float, Exception> {
		return getConfigValue(ConfigType.DIMMER_TEMP_DOWN)
	}


	fun setVoltageMultiplier(value: Float): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.VOLTAGE_MULTIPLIER, value)
	}

	fun getVoltageMultiplier(): Promise<Float, Exception> {
		return getConfigValue(ConfigType.VOLTAGE_MULTIPLIER)
	}


	fun setCurrentMultiplier(value: Float): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.CURRENT_MULTIPLIER, value)
	}

	fun getCurrentMultiplier(): Promise<Float, Exception> {
		return getConfigValue(ConfigType.CURRENT_MULTIPLIER)
	}


	fun setPowerZero(milliWatt: Int32): Promise<Unit, Exception> {
		return setConfigValue(ConfigType.POWER_ZERO, milliWatt)
	}

	fun getPowerZero(): Promise<Int32, Exception> {
		return getConfigValue(ConfigType.POWER_ZERO)
	}



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
		connection.getSingleMergedNotification(getServiceUuid(), getCharacteristicReadUuid(), writeCommand)
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
		if (config.type != BluenetProtocol.OPCODE_WRITE) {
			return Promise.ofFail(Errors.OpcodeWrong())
		}
		return connection.write(getServiceUuid(), getCharacteristicWriteUuid(), config.getArray())
	}

	private fun getServiceUuid(): UUID {
		return when (connection.isSetupMode) {
			true -> BluenetProtocol.SETUP_SERVICE_UUID
			false -> BluenetProtocol.CROWNSTONE_SERVICE_UUID
		}
	}

	private fun getCharacteristicWriteUuid(): UUID {
		return when (connection.isSetupMode) {
			true -> BluenetProtocol.CHAR_SETUP_CONFIG_CONTROL_UUID
			false -> BluenetProtocol.CHAR_CONFIG_CONTROL_UUID
		}
	}

	private fun getCharacteristicReadUuid(): UUID {
		return when (connection.isSetupMode) {
			true -> BluenetProtocol.CHAR_SETUP_CONFIG_READ_UUID
			false -> BluenetProtocol.CHAR_CONFIG_READ_UUID
		}
	}
}