/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import android.util.Log
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.packets.ControlPacket
import rocks.crownstone.bluenet.packets.SetupPacket
import rocks.crownstone.bluenet.packets.keepAlive.KeepAlivePacket
import rocks.crownstone.bluenet.packets.keepAlive.MultiKeepAlivePacket
import rocks.crownstone.bluenet.packets.meshCommand.MeshCommandPacket
import rocks.crownstone.bluenet.packets.multiSwitch.MultiSwitchPacket
import rocks.crownstone.bluenet.packets.schedule.ScheduleCommandPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus

/**
 * Class to interact with the control characteristic of the crownstone service.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class Control(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	/**
	 * Set the switch value.
	 *
	 * @param value 0-100, where 0 is off, and 100 fully on.
	 * @return Promise
	 */
	@Synchronized
	fun setSwitch(value: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setSwitch $value")
		return writeCommand(ControlType.SWITCH, value)
	}

	/**
	 * Turn the relay on or off.
	 *
	 * @param value True to turn the relay on.
	 * @return Promise
	 */
	@Deprecated("Use setSwitch instead.")
	@Synchronized
	fun setRelay(value: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "setRelay $value")
		return writeCommand(ControlType.RELAY, value)
	}

	/**
	 * Set the dim value.
	 *
	 * @param value 0-100, where 0 is off, and 100 fully on.
	 * @return Promise
	 */
	@Deprecated("Use setSwitch instead.")
	@Synchronized
	fun setDimmer(value: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setDimmer $value")
		return writeCommand(ControlType.PWM, value)
	}

	/**
	 * Toggle the switch.
	 *
	 * @param valueOn The value to set the switch to if the switch is currently off.
	 * @return Promise
	 */
	@Synchronized
	fun toggleSwitch(valueOn: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "toggleSwitch $valueOn")
		val state = State(eventBus, connection)
		return state.getSwitchState()
				.then {
					val switchVal = when (it.value) {
						0 -> valueOn
						else -> 0.toShort()
					}
					 setSwitch(switchVal)
				}.unwrap()
	}

	/**
	 * Toggle the switch.
	 *
	 * @param valueOn The value to set the switch to if the switch is currently off.
	 * @return Promise with the current switch value as result.
	 */
	@Deprecated("It's unlikely the value that's set will be returned in the future, use toggleSwitch() instead.")
	@Synchronized
	fun toggleSwitchReturnValueSet(valueOn: Uint8): Promise<Uint8, Exception> {
		Log.i(TAG, "toggleSwitchReturnValueSet $valueOn")
		val deferred = deferred<Uint8, Exception>()
		val state = State(eventBus, connection)
		var switchVal: Uint8 = 0
		state.getSwitchState()
				.then {
					switchVal = when (it.value) {
						0 -> valueOn
						else -> 0.toShort()
					}
					setSwitch(switchVal)
				}.unwrap()
				.success { deferred.resolve(switchVal) }
				.fail { deferred.reject(it) }
		return deferred.promise
	}

	/**
	 * Set the switch value of multiple crownstones.
	 *
	 * @param packet MultiSwitchPacket, with a MultiSwitchListPacket as payload.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	fun multiSwtich(packet: MultiSwitchPacket): Promise<Unit, Exception> {
		Log.i(TAG, "multiSwtich $packet")
		return writeCommand(ControlPacket(ControlType.MULTI_SWITCH, packet))
	}

	/**
	 * Set the time.
	 *
	 * @param timestamp POSIX timestamp.
	 * @return Promise
	 */
	@Synchronized
	fun setTime(timestamp: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "setTime $timestamp")
		return writeCommand(ControlType.SET_TIME, timestamp)
	}

	/**
	 * Enable or disable dimming.
	 *
	 * @param allow True to allow dimming.
	 * @return Promise
	 */
	@Synchronized
	fun allowDimming(allow: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "allowDimming $allow")
		return writeCommand(ControlType.ALLOW_DIMMING, allow)
	}

	/**
	 * Lock or unlock the switch.
	 *
	 * @param lock True to lock switch.
	 * @return Promise
	 */
	@Synchronized
	fun lockSwitch(lock: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "lockSwitch $lock")
		return writeCommand(ControlType.LOCK_SWITCH, lock)
	}

	/**
	 * Enable or disable switchcraft.
	 *
	 * @param enable True to enable switchcraft.
	 * @return Promise
	 */
	@Synchronized
	fun enableSwitchCraft(enable: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "enableSwitchCraft $enable")
		return writeCommand(ControlType.ENABLE_SWITCHCRAFT, enable)
	}

	/**
	 * Set the UART mode.
	 *
	 * @param mode Which mode to set it to.
	 * @return Promise
	 */
	@Synchronized
	fun setUart(mode: UartMode): Promise<Unit, Exception> {
		Log.i(TAG, "setUart $mode")
		return writeCommand(ControlType.UART_ENABLE, mode)
	}

//	fun uartMessage(): Promise<Unit, Exception> // TODO


	/**
	 * Make the crownstone reboot into DFU mode.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun goToDfu(): Promise<Unit, Exception> {
		Log.i(TAG, "goToDfu")
		return writeCommand(ControlType.GOTO_DFU)
	}

	/**
	 * Reboot the crownstone.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun reset(): Promise<Unit, Exception> {
		Log.i(TAG, "reset")
		return writeCommand(ControlType.RESET)
	}

	/**
	 * Send a keep alive with action.
	 *
	 * @param action         The action.
	 * @param switchValue    The switch value to be set when no keep alive is sent for <timeout> time.
	 * @param timeout        Time in seconds.
	 * @return Promise
	 */
	@Synchronized
	fun keepAliveAction(action: KeepAliveAction, switchValue: Uint8, timeout: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "keepAliveAction action=$action switchValue=$switchValue timeout=$timeout")
		val keepAlivePacket = KeepAlivePacket(action, switchValue, timeout)
		return writeCommand(ControlPacket(ControlType.KEEP_ALIVE_STATE, keepAlivePacket))
	}

	/**
	 * Send a keep alive without any action.
	 *
	 * This means the action of a previous keep alive with action will be used.
	 * @return Promise
	 */
	@Synchronized
	fun keepAlive(): Promise<Unit, Exception> {
		Log.i(TAG, "keepAlive")
		return writeCommand(ControlType.KEEP_ALIVE)
	}

	/**
	 * Set a schedule entry.
	 *
	 * You can use getScheduleList() or getAvailableScheduleEntryIndex() to find an available index.
	 *
	 * @param packet         The schedule packet, it will overwrite the existing schedule on the given index.
	 * @return Promise
	 */
	@Synchronized
	fun setSchedule(packet: ScheduleCommandPacket): Promise<Unit, Exception> {
		Log.i(TAG, "setSchedule $packet")
		return writeCommand(ControlPacket(ControlType.SCHEDULE_ENTRY_SET, packet))
	}

	/**
	 * Remove a schedule entry.
	 *
	 * @param id             Index of the schedule entry to be removed.
	 * @return Promise
	 */
	@Synchronized
	fun removeSchedule(id: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "removeSchedule $id")
		return writeCommand(ControlType.SCHEDULE_ENTRY_REMOVE, id)
	}

	/**
	 * Make the crownstone break the connection.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun disconnect(): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect")
		return writeCommand(ControlType.DISCONNECT)
	}

	/**
	 * Send a command that does nothing.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun noop(): Promise<Unit, Exception> {
		Log.i(TAG, "noop")
		return writeCommand(ControlType.NOOP)
	}

	/**
	 * Temporarily increase the TX power of the crownstone.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun increaseTx(): Promise<Unit, Exception> {
		Log.i(TAG, "increaseTx")
		return writeCommand(ControlType.INCREASE_TX)
	}

	/**
	 * Reset all errors.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun resetErrors(): Promise<Unit, Exception> {
		return resetErrors(0xFFFFFFFF)
	}

	/**
	 * Reset specific errors.
	 *
	 * @param errorState     Which errors to reset.
	 * @return Promise
	 */
	@Synchronized
	fun resetErrors(errorState: ErrorState): Promise<Unit, Exception> {
		return resetErrors(errorState.bitmask)
	}

	private fun resetErrors(bitmask: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "resetErrors $bitmask")
		return writeCommand(ControlType.RESET_STATE_ERRORS, bitmask)
	}

	/**
	 * Factory reset the crownstone.
	 *
	 * Clears all configurations and makes the crownstone go into setup mode.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun factoryReset(): Promise<Unit, Exception> {
		Log.i(TAG, "factoryReset")
		return writeCommand(ControlType.FACTORY_RESET, BluenetProtocol.FACTORY_RESET_CODE)
	}

	/**
	 * Send a keep alive via the mesh.
	 *
	 * This means the action of a previous keep alive on the mesh with switch val will be used.
	 *
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	fun keepAliveMeshRepeat(): Promise<Unit, Exception> {
		Log.i(TAG, "keepAliveMeshRepeat")
		return writeCommand(ControlType.KEEP_ALIVE_REPEAT_LAST)
	}

	/**
	 * Send a keep alive with action via the mesh.
	 *
	 * @param packet A MultiKeepAlivePacket with KeepAliveSameTimeout as payload.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	fun keepAliveMeshAction(packet: MultiKeepAlivePacket): Promise<Unit, Exception> {
		Log.i(TAG, "keepAliveMeshAction $packet")
		return writeCommand(ControlPacket(ControlType.KEEP_ALIVE_MESH, packet))
	}

	/**
	 * Send a command via the mesh.
	 *
	 * @param packet A MeshCommandPacket with a ControlPacket, ConfigPacket, or BeaconConfigPacket as payload.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	fun meshCommand(packet: MeshCommandPacket): Promise<Unit, Exception> {
		Log.i(TAG, "meshCommand $packet")
		return writeCommand(ControlPacket(ControlType.MESH_COMMAND, packet))
	}

	/**
	 * Recover a crownstone.
	 *
	 * This will connect, recover, and disconnect.
	 *
	 * @param address MAC address of the crownstone.
	 * @return Promise
	 */
	@Synchronized
	fun recover(address: DeviceAddress): Promise<Unit, Exception> {
		Log.i(TAG, "recover $address")
		val packet = Conversion.uint32ToByteArray(BluenetProtocol.RECOVERY_CODE)
		// TODO: check if already in setup mode, resolve in that case.
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, packet, AccessLevel.ENCRYPTION_DISABLED)
//				.then { connection.wait(500) }.unwrap() // We have to delay the read a bit until the result is written to the characteristic
				.then { checkRecoveryResult() }.unwrap()
				.then { connection.disconnect(false) }.unwrap()
				.then { connection.connect(address) }.unwrap()
				.then { connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, packet, AccessLevel.ENCRYPTION_DISABLED) }.unwrap()
				.then { checkRecoveryResult() }.unwrap()
				.then { connection.disconnect(true) }.unwrap()
				.fail { connection.disconnect(true) }
	}

	private fun checkRecoveryResult(): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		connection.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, false)
				.success {
					val returnCode: Uint32 = Conversion.byteArrayTo(it)
					when (returnCode) {
						1L -> deferred.resolve()
						2L -> deferred.reject(Errors.RecoveryDisabled())
						else -> deferred.reject(Errors.RecoveryRebootRequired())
					}
				}
				.fail { deferred.reject(it) }
		return deferred.promise
	}

	@Synchronized
	internal fun validateSetup(): Promise<Unit, Exception> {
		return writeCommand(ControlType.VALIDATE_SETUP)
	}

	@Synchronized
	internal fun setup(packet: SetupPacket): Promise<Unit, Exception> {
		val controlPacket = ControlPacket(ControlType.SETUP, packet)
		return writeCommand(controlPacket)
	}

	// Commands without payload
	private fun writeCommand(type: ControlType): Promise<Unit, Exception> {
		return writeCommand(ControlPacket(type))
	}

	// Commands with simple value
	private inline fun <reified T> writeCommand(type: ControlType, value: T): Promise<Unit, Exception> {
		val packet = ControlPacket(type, Conversion.toByteArray(value))
		return writeCommand(packet)
	}

	private fun writeCommand(packet: ControlPacket): Promise<Unit, Exception> {
		val array = packet.getArray()
		Log.i(TAG, "writeCommand ${Conversion.bytesToString(array)}")
		if (connection.mode == CrownstoneMode.SETUP) {
			if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
				return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID, array, AccessLevel.SETUP)
			}
			else {
				return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID, array, AccessLevel.SETUP)
			}
		}
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID, array, AccessLevel.HIGHEST_AVAILABLE)
	}
}