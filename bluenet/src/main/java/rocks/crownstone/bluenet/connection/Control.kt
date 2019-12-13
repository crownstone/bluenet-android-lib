/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.packets.*
import rocks.crownstone.bluenet.packets.wrappers.v3.ControlPacketV3
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.packets.keepAlive.KeepAlivePacket
import rocks.crownstone.bluenet.packets.keepAlive.MultiKeepAlivePacket
import rocks.crownstone.bluenet.packets.meshCommand.MeshCommandPacket
import rocks.crownstone.bluenet.packets.multiSwitch.MultiSwitchItemPacket
import rocks.crownstone.bluenet.packets.multiSwitch.MultiSwitchLegacyPacket
import rocks.crownstone.bluenet.packets.multiSwitch.MultiSwitchPacket
import rocks.crownstone.bluenet.packets.schedule.ScheduleCommandPacket
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.packets.wrappers.v4.StatePacketV4
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.util.*

/**
 * Class to interact with the control characteristic of the crownstone service.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class Control(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection
	private val resultClass = Result(eventBus, connection)

	/**
	 * Set the switch value.
	 *
	 * @param value 0-100, where 0 is off, and 100 fully on.
	 * @return Promise
	 */
	@Synchronized
	fun setSwitch(value: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setSwitch $value")
		return writeCommand(ControlType.SWITCH, ControlTypeV4.SWITCH, value)
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
		return writeCommand(ControlType.RELAY, ControlTypeV4.RELAY, value)
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
		return writeCommand(ControlType.PWM, ControlTypeV4.DIMMER, value)
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
						else -> 0.toUint8()
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
	@Synchronized
	fun toggleSwitchReturnValueSet(valueOn: Uint8): Promise<Uint8, Exception> {
		Log.i(TAG, "toggleSwitchReturnValueSet $valueOn")
		val deferred = deferred<Uint8, Exception>()
		val state = State(eventBus, connection)
		var switchVal: Uint8 = 0U
		state.getSwitchState()
				.then {
					switchVal = when (it.value) {
						0 -> valueOn
						else -> 0.toUint8()
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
	 * @param packet Packet with switch values.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Deprecated("This is the legacy multi switch")
	@Synchronized
	fun multiSwitch(packet: MultiSwitchLegacyPacket): Promise<Unit, Exception> {
		Log.i(TAG, "multiSwtich $packet")
		if (getPacketProtocol() == 3) {
			return writeCommand(ControlType.MULTI_SWITCH, ControlTypeV4.UNKNOWN, packet)
		}
		else {
			val newPacket = MultiSwitchPacket()
			for (item in packet.list) {
				val newItem = MultiSwitchItemPacket(item.id, item.switchValue)
				newPacket.add(newItem)
			}
			return multiSwitch(newPacket)
		}
	}

	/**
	 * Turn switch on of multiple crownstones.
	 *
	 * @param packet Packet with switch values.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	fun multiSwitchOn(packet: MultiSwitchLegacyPacket): Promise<Unit, Exception> {
		Log.i(TAG, "multiSwtichOn $packet")
		if (getPacketProtocol() == 3) {
			return writeCommand(ControlType.MULTI_SWITCH, ControlTypeV4.UNKNOWN, packet)
		}
		else {
			val newPacket = MultiSwitchPacket()
			for (item in packet.list) {
				val newItem = MultiSwitchItemPacket(item.id, BluenetProtocol.TURN_SWITCH_ON)
				newPacket.add(newItem)
			}
			return multiSwitch(newPacket)
		}
	}

	/**
	 * Set the switch value of multiple crownstones.
	 *
	 * @param packet Packet with switch values.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	fun multiSwitch(packet: MultiSwitchPacket): Promise<Unit, Exception> {
		Log.i(TAG, "multiSwtich $packet")
		return writeCommand(ControlType.UNKNOWN, ControlTypeV4.MULTI_SWITCH, packet)
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
		if (getPacketProtocol() == 3) {
			return writeCommand(ControlType.SET_TIME, ControlTypeV4.UNKNOWN, timestamp)
		}
		else {
			val config = Config(eventBus, connection)
			return config.setTime(timestamp)
		}
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
		return writeCommand(ControlType.ALLOW_DIMMING, ControlTypeV4.ALLOW_DIMMING, allow)
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
		return writeCommand(ControlType.LOCK_SWITCH, ControlTypeV4.LOCK_SWITCH, lock)
	}

	/**
	 * Make the crownstone reboot into DFU mode.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun goToDfu(): Promise<Unit, Exception> {
		Log.i(TAG, "goToDfu")
		return writeCommand(ControlType.GOTO_DFU, ControlTypeV4.GOTO_DFU)
	}

	/**
	 * Reboot the crownstone.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun reset(): Promise<Unit, Exception> {
		Log.i(TAG, "reset")
		return writeCommand(ControlType.RESET, ControlTypeV4.RESET)
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
		return writeCommand(ControlType.KEEP_ALIVE_STATE, ControlTypeV4.UNKNOWN, keepAlivePacket)
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
		return writeCommand(ControlType.KEEP_ALIVE, ControlTypeV4.UNKNOWN)
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
		return writeCommand(ControlType.SCHEDULE_ENTRY_SET, ControlTypeV4.UNKNOWN, packet)
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
		return writeCommand(ControlType.SCHEDULE_ENTRY_REMOVE, ControlTypeV4.UNKNOWN, id)
	}

	/**
	 * Add a behaviour.
	 *
	 * @param behaviour      The behaviour to store.
	 * @return Promise with index where the behaviour is stored, and the hash of all behaviours.
	 */
	@Synchronized
	fun addBehaviour(behaviour: BehaviourPacket): Promise<BehaviourIndexAndHashPacket, Exception> {
		Log.i(TAG, "addBehaviour behaviour=$behaviour")
		val resultPacket = BehaviourIndexAndHashPacket()
		return writeCommandAndGetResult(ControlTypeV4.BEHAVIOUR_ADD, behaviour, resultPacket)
	}

	/**
	 * Replace a behaviour.
	 *
	 * @param index          Index at which to store the behaviour.
	 * @param behaviour      The behaviour to store.
	 * @return Promise with index where the behaviour is stored, and the hash of all behaviours.
	 */
	@Synchronized
	fun replaceBehaviour(index: BehaviourIndex, behaviour: BehaviourPacket): Promise<BehaviourIndexAndHashPacket, Exception> {
		Log.i(TAG, "replaceBehaviour index=$index behaviour=$behaviour")
		val writePacket = IndexedBehaviourPacket(index, behaviour)
		val resultPacket = BehaviourIndexAndHashPacket()
		return writeCommandAndGetResult(ControlTypeV4.BEHAVIOUR_REPLACE, writePacket, resultPacket)
	}

	/**
	 * Remove a behaviour.
	 *
	 * @param index          Index at which to remove a behaviour.
	 * @return Promise with index where the behaviour was removed, and the hash of all behaviours.
	 */
	@Synchronized
	fun removeBehaviour(index: BehaviourIndex): Promise<BehaviourIndexAndHashPacket, Exception> {
		Log.i(TAG, "removeBehaviour index=$index")
		val resultPacket = BehaviourIndexAndHashPacket()
		return writeCommandAndGetResult(ControlTypeV4.BEHAVIOUR_REMOVE, index, resultPacket)
	}

	/**
	 * Get a behaviour.
	 *
	 * @param index          Index of the behaviour to get.
	 * @return Promise with requested index, and the behaviour at that index.
	 */
	@Synchronized
	fun getBehaviour(index: BehaviourIndex): Promise<IndexedBehaviourPacket, Exception> {
		Log.i(TAG, "getBehaviour index=$index")
		val resultPacket = IndexedBehaviourPacket()
		return writeCommandAndGetResult(ControlTypeV4.BEHAVIOUR_GET, index, resultPacket)
	}

	/**
	 * Get a list of indices that hold a behaviour.
	 *
	 * @return List of all indices at which a behaviour is stored.
	 */
	@Synchronized
	fun getBehaviourIndices(): Promise<BehaviourIndicesPacket, Exception> {
		Log.i(TAG, "getBehaviourIndices")
		val resultPacket = BehaviourIndicesPacket()
		return writeCommandAndGetResult(ControlTypeV4.BEHAVIOUR_GET_INDICES, EmptyPacket(), resultPacket)
	}

	/**
	 * Make the crownstone break the connection.
	 *
	 * Resolves also when already disconnected.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect")
		val deferred = deferred<Unit, Exception>()
		Util.recoverableUnitPromise(
			writeCommand(ControlType.DISCONNECT, ControlTypeV4.DISCONNECT),
			fun (error: Exception): Boolean {
				return when(error) {
					is Errors.Timeout -> true // Assume the write callback was never called because the ack didn't arrive?
					is Errors.GattDisconnectedByPeer -> true // The crownstone disconnected us, this was a result of the write.
					is Errors.GattError133 -> true // Often the case when disconnected by peer, but the real error (19) is only given in the connection state change.
					is Errors.NotConnected -> true
					else -> false
				}
			}
		)
				// Disconnect afterwards, so that resolve only happens after being disconnected.
				// TODO: do this with a waitForDisconnect() function?
				.success {
					connection.disconnect(clearCache).always { deferred.resolve() }
				}
				.fail {
					connection.disconnect(clearCache).always { deferred.reject(it) }
				}
		return deferred.promise
	}

	/**
	 * Send a command that does nothing.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun noop(): Promise<Unit, Exception> {
		Log.i(TAG, "noop")
		return writeCommand(ControlType.NOOP, ControlTypeV4.NOOP)
	}

	/**
	 * Temporarily increase the TX power of the crownstone.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun increaseTx(): Promise<Unit, Exception> {
		Log.i(TAG, "increaseTx")
		return writeCommand(ControlType.INCREASE_TX, ControlTypeV4.INCREASE_TX)
	}

	/**
	 * Reset all errors.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun resetErrors(): Promise<Unit, Exception> {
		return resetErrors(0xFFFFFFFFU)
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
		return writeCommand(ControlType.RESET_STATE_ERRORS, ControlTypeV4.RESET_STATE_ERRORS, bitmask)
	}

	/**
	 * Factory reset the crownstone.
	 *
	 * Clears all configurations and makes the crownstone go into setup mode.
	 * Disconnects afterwards.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun factoryReset(): Promise<Unit, Exception> {
		Log.i(TAG, "factoryReset")
		return writeCommand(ControlType.FACTORY_RESET, ControlTypeV4.FACTORY_RESET, BluenetProtocol.FACTORY_RESET_CODE)
				.then { connection.disconnect(true) }.unwrap()
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
		return writeCommand(ControlType.KEEP_ALIVE_REPEAT_LAST, ControlTypeV4.UNKNOWN)
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
		return writeCommand(ControlType.KEEP_ALIVE_MESH, ControlTypeV4.UNKNOWN, packet)
	}

	/**
	 * Send a command via the mesh.
	 *
	 * @param packet A MeshCommandPacket with a ControlPacket, ConfigPacket, or BeaconConfigPacket as payload.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	@Deprecated("Use Mesh class instead")
	fun meshCommand(packet: MeshCommandPacket): Promise<Unit, Exception> {
		Log.i(TAG, "meshCommand $packet")
		return writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, packet)
	}

	/**
	 * Recover a crownstone.
	 *
	 * This will connect, recover, and disconnect.
	 * Will resolve when already in setup mode.
	 *
	 * @param address MAC address of the crownstone.
	 * @return Promise
	 */
	@Synchronized
	fun recover(address: DeviceAddress): Promise<Unit, Exception> {
		Log.i(TAG, "recover $address")
		val packet = Conversion.uint32ToByteArray(BluenetProtocol.RECOVERY_CODE)
		// If previous recover attempt succeeded step 1, then only one more write is needed. So also check if in setup mode after second connect.
		// TODO: resolve nicely when already in setup mode, don't use the error hack for that.
		val deferred = deferred<Unit, Exception>()
		connection.connect(address)
				.then { checkIfAlreadyInSetupMode() }.unwrap()
				.then { connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, packet, AccessLevel.ENCRYPTION_DISABLED) }.unwrap()
				.then { checkRecoveryResult() }.unwrap()
				.then { connection.disconnect(true) }.unwrap()
				.then { connection.wait(2000) }.unwrap() // Wait for the crownstone trying to disconnect us before connecting again. TODO: do this with a waitForDisconnect() function.
				.then { connection.connect(address) }.unwrap()
				.then { checkIfAlreadyInSetupMode() }.unwrap()
				.then { connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, packet, AccessLevel.ENCRYPTION_DISABLED) }.unwrap()
				.then { checkRecoveryResult() }.unwrap()
				.success {
					connection.disconnect(true).always { deferred.resolve() }
				}
				.fail {
					when (it) {
						is Errors.AlreadySetupInMode -> connection.disconnect(true).always { deferred.resolve() }
						else -> connection.disconnect(true).always { deferred.reject(it) }
					}
				}
		return deferred.promise
	}

	// Bit of a hack: return an error when already in setup mode.
	private fun checkIfAlreadyInSetupMode(): Promise<Unit, Exception> {
		if (connection.mode == CrownstoneMode.SETUP) {
			return Promise.ofFail(Errors.AlreadySetupInMode())
		}
		return Promise.ofSuccess(Unit)
	}

	private fun checkRecoveryResult(): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		connection.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, false)
				.success {
					val returnCode: Uint32 = Conversion.byteArrayTo(it)
					when (returnCode) {
						1U -> deferred.resolve()
						2U -> deferred.reject(Errors.RecoveryDisabled())
						else -> deferred.reject(Errors.RecoveryRebootRequired())
					}
				}
				.fail {
					when (it) {
						is Errors.GattDisconnectedByPeer -> deferred.resolve() // The crownstone disconnected us, assume that this was a result of the write.
						is Errors.GattError133 -> deferred.resolve() // Currently, the read promise returns error 133, even when disconnected by peer, see TODO in CoreConnection.onGattCharacteristicRead()
						else -> deferred.reject(it)
					}
				}
		return deferred.promise
	}

	@Synchronized
	internal fun validateSetup(): Promise<Unit, Exception> {
		return writeCommand(ControlType.VALIDATE_SETUP, ControlTypeV4.UNKNOWN)
	}

	@Synchronized
	internal fun writeSetup(packet: SetupPacket): Promise<Unit, Exception> {
		return writeCommand(ControlType.SETUP, ControlTypeV4.UNKNOWN, packet)
	}

	@Synchronized
	internal fun writeSetup(packet: SetupPacketV2): Promise<Unit, Exception> {
		return writeCommand(ControlType.SETUP, ControlTypeV4.SETUP, packet)
	}

	@Synchronized
	internal fun writeSetState(packet: StatePacketV4): Promise<Unit, Exception> {
		return writeCommand(ControlType.UNKNOWN, ControlTypeV4.SET_STATE, packet)
	}

	@Synchronized
	internal fun writeGetState(type: StateTypeV4): Promise<Unit, Exception> {
		return writeCommand(ControlType.UNKNOWN, ControlTypeV4.GET_STATE, type.num)
	}

//	private fun checkValidType(type: ControlType, type4: ControlTypeV4): Promise<Unit, Exception> {
//		when (getPacketProtocol()) {
//			3 -> {
//				if (type == ControlType.UNKNOWN) {
//					return Promise.ofFail(Errors.TypeWrong(type.name))
//				}
//			}
//			else -> {
//				if (type4 == ControlTypeV4.UNKNOWN) {
//					return Promise.ofFail(Errors.TypeWrong(type4.name))
//				}
//			}
//		}
//		return Promise.ofSuccess(Unit)
//	}

	private fun isValidType(type: ControlType, type4: ControlTypeV4): Boolean {
		when (getPacketProtocol()) {
			3 -> return (type != ControlType.UNKNOWN)
			else -> return (type4 != ControlTypeV4.UNKNOWN)
		}
	}

	// Commands without payload
	private fun writeCommand(type: ControlType, type4: ControlTypeV4): Promise<Unit, Exception> {
		if (!isValidType(type, type4)) {
			return Promise.ofFail(Errors.TypeWrong("$type or $type4"))
		}
		val packet = when (getPacketProtocol()) {
			3 -> ControlPacketV3(type)
			else -> ControlPacketV4(type4)
		}
		return writeCommand(packet.getArray())
	}

	// Commands with simple value
	internal inline fun <reified T> writeCommand(type: ControlType, type4: ControlTypeV4, value: T): Promise<Unit, Exception> {
		return writeCommand(type, type4, ByteArrayPacket(Conversion.toByteArray(value)))
	}

	// Commands with packet
	private fun writeCommand(type: ControlType, type4: ControlTypeV4, packet: PacketInterface): Promise<Unit, Exception> {
		if (!isValidType(type, type4)) {
			return Promise.ofFail(Errors.TypeWrong("$type or $type4"))
		}
		val packet = when (getPacketProtocol()) {
			3 -> ControlPacketV3(type, packet)
			else -> ControlPacketV4(type4, packet)
		}
		return writeCommand(packet.getArray())
	}

	private fun writeCommand(array: ByteArray?): Promise<Unit, Exception> {
		if (array == null) {
			return Promise.ofFail(Errors.ValueWrong())
		}
		Log.i(TAG, "writeCommand ${Conversion.bytesToString(array)}")
		val characteristic = getControlCharacteristic()
		if (connection.mode == CrownstoneMode.SETUP) {
			return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, characteristic, array, AccessLevel.SETUP)
		}
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, characteristic, array, AccessLevel.HIGHEST_AVAILABLE)
	}


	// Results with simple value
	private inline fun <reified R>writeCommandAndGetResult(type: ControlTypeV4, writePacket: PacketInterface, timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT): Promise<R, Exception> {
		val resultPacket = ByteArrayPacket()
		return writeCommandAndGetResult(type, writePacket, resultPacket, timeoutMs)
				.then {
					val arr = it.getPayload()
//					if (arr == null) {
//						return@then Promise.ofFail<R, Exception>(Errors.Parse("payload missing"))
//					}
					try {
						val value = Conversion.byteArrayTo<R>(arr)
						return@then Promise.ofSuccess<R, Exception>(value)
					} catch (ex: Exception) {
						return@then Promise.ofFail<R, Exception>(ex)
					}
				}.unwrap()
	}

	// Commands with simple value, and no result payload.
	private inline fun <reified V>writeCommandAndGetResult(type: ControlTypeV4, writeValue: V, timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT): Promise<Unit, Exception> {
		val writePacket = ByteArrayPacket(Conversion.toByteArray(writeValue))
		val resultPacket = EmptyPacket()
		return writeCommandAndGetResult(type, writePacket, resultPacket, timeoutMs)
				.then {
					return@then Promise.ofSuccess<Unit, Exception>(Unit)
				}.unwrap()
	}

	// Commands with simple value
	private inline fun <T: PacketInterface, reified V>writeCommandAndGetResult(type: ControlTypeV4, writeValue: V, resultPacket: T, timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT): Promise<T, Exception> {
		val writePacket = ByteArrayPacket(Conversion.toByteArray(writeValue))
		return writeCommandAndGetResult(type, writePacket, resultPacket, timeoutMs)
	}

	private fun <T: PacketInterface>writeCommandAndGetResult(type: ControlTypeV4, writePacket: PacketInterface, resultPacket: T, timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT): Promise<T, Exception> {
		val writeCommand = fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, type, writePacket) }
		return resultClass.getSingleResult(writeCommand, type, resultPacket, timeoutMs)
				.then {
					return@then resultPacket
				}
	}

	private fun getControlCharacteristic(): UUID {
		if (connection.mode == CrownstoneMode.SETUP) {
			if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
				return BluenetProtocol.CHAR_SETUP_CONTROL_UUID
			}
			else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
				return BluenetProtocol.CHAR_SETUP_CONTROL2_UUID
			}
			else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL3_UUID)) {
				return BluenetProtocol.CHAR_SETUP_CONTROL3_UUID
			}
			else {
				return BluenetProtocol.CHAR_SETUP_CONTROL4_UUID
			}
		}
		else {
			if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID)) {
				return BluenetProtocol.CHAR_CONTROL_UUID
			}
			else {
				return BluenetProtocol.CHAR_CONTROL4_UUID
			}
		}
	}

	private fun getPacketProtocol(): Int {
		if (connection.mode == CrownstoneMode.SETUP) {
			if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
				return 3
			}
			else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
				return 3
			}
			else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL3_UUID)) {
				return 3
			}
			else {
				return 4
			}
		}
		else {
			if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID)) {
				return 3
			}
			else {
				return 4
			}
		}
	}
}
