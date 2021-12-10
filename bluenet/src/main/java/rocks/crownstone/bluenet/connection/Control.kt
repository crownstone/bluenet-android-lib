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
import rocks.crownstone.bluenet.packets.other.IbeaconConfigIdPacket
import rocks.crownstone.bluenet.packets.schedule.ScheduleCommandPacket
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.packets.wrappers.v4.StatePacketV4
import rocks.crownstone.bluenet.packets.wrappers.v5.ControlPacketV5
import rocks.crownstone.bluenet.packets.wrappers.v5.ResultPacketV5
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.util.*

/**
 * Class to interact with the control characteristic of the crownstone service.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class Control(eventBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val connection = connection
	private val resultClass = Result(eventBus, connection)

//##################################################################################################
//region           Public commands
//##################################################################################################
	/**
	 * Set the switch value.
	 *
	 * @param value 0-100, where 0 is off, and 100 fully on.
	 * @return Promise
	 */
	@Synchronized
	fun setSwitch(value: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setSwitch $value")
		return writeCommandAndCheckResult(ControlType.SWITCH, ControlTypeV4.SWITCH, value)
	}

	/**
	 * Set the switch value.
	 *
	 * @param value Value to set the switch to.
	 * @return Promise
	 */
	@Synchronized
	fun setSwitch(value: SwitchCommandValue): Promise<Unit, Exception> {
		return setSwitch(value.num)
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
		return writeCommandAndCheckResult(ControlType.RELAY, ControlTypeV4.RELAY, value)
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
		return writeCommandAndCheckResult(ControlType.PWM, ControlTypeV4.DIMMER, value)
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
		val state = State(eventBus, connection)
		var switchVal: Uint8 = 0U
		return state.getSwitchState()
				.then {
					switchVal = when (it.value) {
						0 -> valueOn
						else -> 0.toUint8()
					}
					setSwitch(switchVal)
				}.unwrap()
				.then { return@then switchVal }
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
		Log.i(TAG, "multiSwitch $packet")
		when (getPacketProtocol()) {
			PacketProtocol.V1,
			PacketProtocol.V2,
			PacketProtocol.V3 -> {
				return writeCommandAndCheckResult(ControlType.MULTI_SWITCH, ControlTypeV4.UNKNOWN, packet)
			}
			PacketProtocol.V4,
			PacketProtocol.V5 -> {
				val newPacket = MultiSwitchPacket()
				for (item in packet.list) {
					val newItem = MultiSwitchItemPacket(item.id, item.switchValue)
					newPacket.add(newItem)
				}
				return multiSwitch(newPacket)
			}
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
		Log.i(TAG, "multiSwitchOn $packet")
		when (getPacketProtocol()) {
			PacketProtocol.V1,
			PacketProtocol.V2,
			PacketProtocol.V3 -> {
				return writeCommandAndCheckResult(ControlType.MULTI_SWITCH, ControlTypeV4.UNKNOWN, packet)
			}
			PacketProtocol.V4,
			PacketProtocol.V5 -> {
				val newPacket = MultiSwitchPacket()
				for (item in packet.list) {
					val newItem = MultiSwitchItemPacket(item.id, SwitchCommandValue.SMART_ON)
					newPacket.add(newItem)
				}
				return multiSwitch(newPacket)
			}
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
		return writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.MULTI_SWITCH, packet)
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
		return writeCommandAndCheckResult(ControlType.SET_TIME, ControlTypeV4.SET_TIME, timestamp)
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
		return writeCommandAndCheckResult(ControlType.ALLOW_DIMMING, ControlTypeV4.ALLOW_DIMMING, allow)
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
		return writeCommandAndCheckResult(ControlType.LOCK_SWITCH, ControlTypeV4.LOCK_SWITCH, lock)
	}

	/**
	 * Make the crownstone reboot into DFU mode.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun goToDfu(): Promise<Unit, Exception> {
		Log.i(TAG, "goToDfu")
		return writeCommandAndCheckResult(ControlType.GOTO_DFU, ControlTypeV4.GOTO_DFU)
	}

	/**
	 * Reboot the crownstone.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun reset(): Promise<Unit, Exception> {
		Log.i(TAG, "reset")
		return writeCommandAndCheckResult(ControlType.RESET, ControlTypeV4.RESET)
	}

	/**
	 * Send a keep alive without any action.
	 *
	 * This means the action of a previous keep alive with action will be used.
	 * @return Promise
	 */
	@Deprecated("Keep alives are no longer used.")
	@Synchronized
	fun keepAlive(): Promise<Unit, Exception> {
		Log.i(TAG, "keepAlive")
		return writeCommandAndCheckResult(ControlType.KEEP_ALIVE, ControlTypeV4.UNKNOWN)
	}

	/**
	 * Send a keep alive with action.
	 *
	 * @param action         The action.
	 * @param switchValue    The switch value to be set when no keep alive is sent for <timeout> time.
	 * @param timeout        Time in seconds.
	 * @return Promise
	 */
	@Deprecated("Keep alives are no longer used.")
	@Synchronized
	fun keepAliveAction(action: KeepAliveAction, switchValue: Uint8, timeout: Uint16): Promise<Unit, Exception> {
		Log.i(TAG, "keepAliveAction action=$action switchValue=$switchValue timeout=$timeout")
		val keepAlivePacket = KeepAlivePacket(action, switchValue, timeout)
		return writeCommandAndCheckResult(ControlType.KEEP_ALIVE_STATE, ControlTypeV4.UNKNOWN, keepAlivePacket)
	}

	/**
	 * Set a schedule entry.
	 *
	 * You can use getScheduleList() or getAvailableScheduleEntryIndex() to find an available index.
	 *
	 * @param packet         The schedule packet, it will overwrite the existing schedule on the given index.
	 * @return Promise
	 */
	@Deprecated("Schedules have been replaced by behaviours.")
	@Synchronized
	fun setSchedule(packet: ScheduleCommandPacket): Promise<Unit, Exception> {
		Log.i(TAG, "setSchedule $packet")
		return writeCommandAndCheckResult(ControlType.SCHEDULE_ENTRY_SET, ControlTypeV4.UNKNOWN, packet)
	}

	/**
	 * Remove a schedule entry.
	 *
	 * @param id             Index of the schedule entry to be removed.
	 * @return Promise
	 */
	@Deprecated("Schedules have been replaced by behaviours.")
	@Synchronized
	fun removeSchedule(id: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "removeSchedule $id")
		return writeCommandAndCheckResult(ControlType.SCHEDULE_ENTRY_REMOVE, ControlTypeV4.UNKNOWN, id)
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
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.BEHAVIOUR_ADD, behaviour, resultPacket)
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
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.BEHAVIOUR_REPLACE, writePacket, resultPacket)
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
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.BEHAVIOUR_REMOVE, index, resultPacket)
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
		val resultPacket = IndexedBehaviourPacket(INDEX_UNKNOWN, BehaviourGetPacket())
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.BEHAVIOUR_GET, index, resultPacket)
				.then {
					val getPacket: BehaviourGetPacket = it.behaviour as BehaviourGetPacket
					val packet = getPacket.packet
					if (packet == null) {
						return@then Promise.ofFail<IndexedBehaviourPacket, Exception>(Errors.Parse("behaviour"))
					}
					return@then Promise.ofSuccess<IndexedBehaviourPacket, Exception>(IndexedBehaviourPacket(it.index, packet))
				}.unwrap()
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
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.BEHAVIOUR_GET_INDICES, EmptyPacket(), resultPacket)
	}

	/**
	 * Get some info to help debugging behaviours.
	 *
	 * @return Promise with debug info.
	 */
	@Synchronized
	fun getBehaviourDebug(): Promise<BehaviourDebugPacket, Exception> {
		Log.i(TAG, "getBehaviourDebug")
		val resultPacket = BehaviourDebugPacket()
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.BEHAVIOUR_GET_DEBUG, EmptyPacket(), resultPacket)
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
				// Disconnect from our side as well.
				// This also makes sure we resolve after being disconnected.
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
		return writeCommandAndCheckResult(ControlType.NOOP, ControlTypeV4.NOOP)
	}

	/**
	 * Temporarily increase the TX power of the crownstone.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun increaseTx(): Promise<Unit, Exception> {
		Log.i(TAG, "increaseTx")
		return writeCommandAndCheckResult(ControlType.INCREASE_TX, ControlTypeV4.INCREASE_TX)
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
		return writeCommandAndCheckResult(ControlType.RESET_STATE_ERRORS, ControlTypeV4.RESET_STATE_ERRORS, bitmask)
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
		return writeCommandAndCheckResult(ControlType.FACTORY_RESET, ControlTypeV4.FACTORY_RESET, BluenetProtocol.FACTORY_RESET_CODE)
				.then { connection.disconnect(true) }.unwrap()
	}

	/**
	 * Send a keep alive via the mesh.
	 *
	 * This means the action of a previous keep alive on the mesh with switch val will be used.
	 *
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Deprecated("Keep alives are no longer used.")
	@Synchronized
	fun keepAliveMeshRepeat(): Promise<Unit, Exception> {
		Log.i(TAG, "keepAliveMeshRepeat")
		return writeCommandAndCheckResult(ControlType.KEEP_ALIVE_REPEAT_LAST, ControlTypeV4.UNKNOWN)
	}

	/**
	 * Send a keep alive with action via the mesh.
	 *
	 * @param packet A MultiKeepAlivePacket with KeepAliveSameTimeout as payload.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Deprecated("Keep alives are no longer used.")
	@Synchronized
	fun keepAliveMeshAction(packet: MultiKeepAlivePacket): Promise<Unit, Exception> {
		Log.i(TAG, "keepAliveMeshAction $packet")
		return writeCommandAndCheckResult(ControlType.KEEP_ALIVE_MESH, ControlTypeV4.UNKNOWN, packet)
	}

	/**
	 * Set the ID of the ibeacon config.
	 *
	 * To interleave between two ibeacon configs, you can for example
	 * set ID=0 at timestamp=0 with interval=600,
	 * and ID=1 at timestamp=300 with interval=600.
	 */
	@Synchronized
	fun setIbeaconConfigId(packet: IbeaconConfigIdPacket): Promise<Unit, Exception> {
		Log.i(TAG, "setIbeaconConfigId $packet")
		return writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.SET_IBEACON_CONFIG_ID, packet)
	}

	/**
	 * Send a command via the mesh.
	 *
	 * @param packet A MeshCommandPacket with a ControlPacket, ConfigPacket, or BeaconConfigPacket as payload.
	 * @return Promise that resolves when the connected crownstone received the command.
	 */
	@Synchronized
	internal fun meshCommand(packet: MeshCommandPacket): Promise<Unit, Exception> {
		Log.i(TAG, "meshCommand $packet")
		return writeCommandAndCheckResult(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, packet)
	}

	/**
	 * Send hub data, and get the reply.
	 *
	 * @param hubDataPacket  A packet with data to send to the hub.
	 * @param timeoutMs      Timeout in ms.
	 * @return               Promise with the reply data.
	 */
	@Synchronized
	fun hubData(hubDataPacket: HubDataPacket, timeoutMs: Long = 5000): Promise<PacketInterface, Exception> {
		Log.i(TAG, "hubData $hubDataPacket")
		val resultPacket = ByteArrayPacket()
		return writeCommandAndGetResult(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket, resultPacket, timeoutMs)
				.then { return@then Promise.ofSuccess<PacketInterface, Exception>(resultPacket) }.unwrap()


//		val deferred = deferred<PacketInterface, Exception>()
//
//		// Will store the reply from the hub.
//		var replyPacket: PacketInterface? = null
//
//		val resultCallback = fun (resultPacket: ResultPacketV5): ProcessResult {
//			if (resultPacket.type != ControlTypeV4.HUB_DATA) {
//				Log.w(TAG, "Wrong type: ${resultPacket.type}")
//				return ProcessResult.ERROR
//			}
//			if (resultPacket.protocol != ConnectionProtocol.V5) {
//				Log.w(TAG, "Wrong protocol: ${resultPacket.protocol}")
//				return ProcessResult.ERROR
//			}
//			when (resultPacket.resultCode) {
//				ResultType.WAIT_FOR_SUCCESS -> {
//					Log.i(TAG, "Hub data sent to hub, waiting for reply.")
//					return ProcessResult.NOT_DONE
//				}
//				ResultType.SUCCESS,
//				ResultType.SUCCESS_NO_CHANGE -> {
//					replyPacket = resultPacket.getPayloadPacket()
//					Log.i(TAG, "Received reply from hub: $replyPacket")
//					return ProcessResult.DONE
//				}
//				else -> {
//					Log.w(TAG, "Hub data failed: ${resultPacket.resultCode}")
//					return ProcessResult.ERROR
//				}
//			}
//		}
//
//		val writeCommand = fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
//		resultClass.getMultipleResultsV5(
//				writeCommand,
//				resultCallback,
//				timeoutMs,
//				listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS, ResultType.SUCCESS_NO_CHANGE)
//		)
//				.success {
//					deferred.resolve(replyPacket ?: ByteArrayPacket())
//				}
//				.fail {
//					if (it is Errors.NotificationTimeout) {
//						deferred.reject(Errors.HubDataReplyTimeout())
//					}
//					else {
//						deferred.reject(it)
//					}
//				}
//
//		return deferred.promise
	}

	/**
	 * Recover a crownstone.
	 *
	 * This will connect, recover, and disconnect.
	 * Will resolve when already in setup mode.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun recover(): Promise<Unit, Exception> {
		Log.i(TAG, "recover")
		val packet = Conversion.uint32ToByteArray(BluenetProtocol.RECOVERY_CODE)
		// If previous recover attempt succeeded step 1, then only one more write is needed. So also check if in setup mode after second connect.
		// TODO: resolve nicely when already in setup mode, don't use the error hack for that.
		val deferred = deferred<Unit, Exception>()
		connection.connect(false)
				.then { checkIfAlreadyInSetupMode() }.unwrap()
				.then { connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RECOVERY_UUID, packet, AccessLevel.ENCRYPTION_DISABLED) }.unwrap()
				.then { checkRecoveryResult() }.unwrap()
				.then { connection.disconnect(true) }.unwrap()
				.then { connection.wait(2000) }.unwrap() // Wait for the crownstone trying to disconnect us before connecting again. TODO: do this with a waitForDisconnect() function.
				.then { connection.connect(false) }.unwrap()
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
//endregion

//##################################################################################################
//region           Write commands
//##################################################################################################

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

	private fun isValidType(type: ControlType, type4: ControlTypeV4): Boolean {
		when (getPacketProtocol()) {
			PacketProtocol.V1,
			PacketProtocol.V2,
			PacketProtocol.V3 -> return (type != ControlType.UNKNOWN)
			PacketProtocol.V4,
			PacketProtocol.V5 -> return (type4 != ControlTypeV4.UNKNOWN)
		}
	}

	// Commands without payload
	private fun writeCommand(type: ControlType, type4: ControlTypeV4, accessLevel: AccessLevel? = null): Promise<Unit, Exception> {
		return writeCommand(type, type4, EmptyPacket(), accessLevel)
	}

	// Commands with simple value
	internal inline fun <reified T> writeCommand(type: ControlType, type4: ControlTypeV4, value: T, accessLevel: AccessLevel? = null): Promise<Unit, Exception> {
		return writeCommand(type, type4, ByteArrayPacket(Conversion.toByteArray(value)), accessLevel)
	}

	// Commands with packet
	private fun writeCommand(type: ControlType, type4: ControlTypeV4, payload: PacketInterface, accessLevel: AccessLevel? = null): Promise<Unit, Exception> {
		if (!isValidType(type, type4)) {
			return Promise.ofFail(Errors.TypeWrong("$type or $type4"))
		}
		val packet = when (getPacketProtocol()) {
			PacketProtocol.V1,
			PacketProtocol.V2,
			PacketProtocol.V3 -> ControlPacketV3(type, payload)
			PacketProtocol.V4 -> ControlPacketV4(type4, payload)
			PacketProtocol.V5 -> ControlPacketV5(ConnectionProtocol.V5, type4, payload)
		}
		return writeCommand(packet.getArray(), accessLevel)
	}

	private fun writeCommand(array: ByteArray?, accessLevel: AccessLevel? = null): Promise<Unit, Exception> {
		if (array == null) {
			return Promise.ofFail(Errors.ValueWrong())
		}
		Log.i(TAG, "writeCommand ${Conversion.bytesToString(array)}")
		val characteristic = getControlCharacteristic()
		if (connection.mode == CrownstoneMode.SETUP) {
			return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, characteristic, array, accessLevel ?: AccessLevel.SETUP)
		}
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, characteristic, array, accessLevel ?: AccessLevel.HIGHEST_AVAILABLE)
	}
//endregion

//##################################################################################################
//region           Write commands with result
//##################################################################################################

	/**
	 * Write a control command and wait for success.
	 *
	 * For commands with no value.
	 */
	internal fun writeCommandAndCheckResult(
			type: ControlType,
			type4: ControlTypeV4,
			timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT,
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		return writeCommandAndCheckResult(type, type4, EmptyPacket(), timeoutMs, accessLevel)
	}

	/**
	 * Write a control command and wait for success.
	 *
	 * For commands with simple value.
	 */
	internal inline fun <reified V>writeCommandAndCheckResult(
			type: ControlType,
			type4: ControlTypeV4,
			writeValue: V,
			timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT,
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val writePacket = ByteArrayPacket(Conversion.toByteArray(writeValue))
		return writeCommandAndCheckResult(type, type4, writePacket, timeoutMs, accessLevel)
	}

	/**
	 * Write a control command and wait for success.
	 */
	internal fun writeCommandAndCheckResult(
			type: ControlType,
			type4: ControlTypeV4,
			writePacket: PacketInterface,
			timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT,
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val writeCommand = fun (): Promise<Unit, Exception> { return writeCommand(type, type4, writePacket, accessLevel) }
		val protocol = when (getPacketProtocol()) {
			PacketProtocol.V5 -> ConnectionProtocol.V5
			else -> ConnectionProtocol.UNKNOWN
		}
		return resultClass.getSingleResult(writeCommand, protocol, type.num, type4, timeoutMs, getControlService(), getControlCharacteristic(), null, accessLevel)
				.toSuccessVoid()
	}

	/**
	 * Write a control command, wait for success, and get result data.
	 *
	 * For results with simple value.
	 */
	internal inline fun <reified R>writeCommandAndGetResult(
			type: ControlType,
			type4: ControlTypeV4,
			writePacket: PacketInterface,
			timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT,
			accessLevel: AccessLevel? = null):
			Promise<R, Exception> {
		val resultPacket = ByteArrayPacket()
		return writeCommandAndGetResult(type, type4, writePacket, resultPacket, timeoutMs, accessLevel)
				.then {
					val arr = it.getPayload()
					try {
						val value = Conversion.byteArrayTo<R>(arr)
						return@then Promise.ofSuccess<R, Exception>(value)
					} catch (ex: Exception) {
						return@then Promise.ofFail<R, Exception>(ex)
					}
				}.unwrap()
	}

	/**
	 * Write a control command, wait for success, and get result data.
	 *
	 * For commands with simple value.
	 */
	internal inline fun <R: PacketInterface, reified V>writeCommandAndGetResult(
			type: ControlType,
			type4: ControlTypeV4,
			writeValue: V,
			resultPacket: R,
			timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT,
			accessLevel: AccessLevel? = null
	): Promise<R, Exception> {
		val writePacket = ByteArrayPacket(Conversion.toByteArray(writeValue))
		return writeCommandAndGetResult(type, type4, writePacket, resultPacket, timeoutMs, accessLevel)
	}

	/**
	 * Write a control command, wait for success, and get result data.
	 */
	internal fun <R: PacketInterface>writeCommandAndGetResult(
			type: ControlType,
			type4: ControlTypeV4,
			writePacket: PacketInterface,
			resultPacket: R,
			timeoutMs: Long = BluenetConfig.TIMEOUT_CONTROL_RESULT,
			accessLevel: AccessLevel? = null
	): Promise<R, Exception> {
		val writeCommand = fun (): Promise<Unit, Exception> { return writeCommand(type, type4, writePacket, accessLevel) }
		val protocol = when (getPacketProtocol()) {
			PacketProtocol.V5 -> ConnectionProtocol.V5
			else -> ConnectionProtocol.UNKNOWN
		}
		return resultClass.getSingleResult(writeCommand, protocol, type.num, type4, resultPacket, timeoutMs, getControlService(), getControlCharacteristic(), null, accessLevel)
				.then {
					return@then resultPacket
				}
	}
//endregion

//##################################################################################################
//region           Helper functions
//##################################################################################################

	private fun getControlService(): UUID {
		return when (connection.mode == CrownstoneMode.SETUP) {
			true -> BluenetProtocol.SETUP_SERVICE_UUID
			false -> BluenetProtocol.CROWNSTONE_SERVICE_UUID
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
			else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL4_UUID)) {
				return BluenetProtocol.CHAR_SETUP_CONTROL4_UUID
			}
			else {
				return BluenetProtocol.CHAR_SETUP_CONTROL5_UUID
			}
		}
		else {
			if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID)) {
				return BluenetProtocol.CHAR_CONTROL_UUID
			}
			else if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL4_UUID)) {
				return BluenetProtocol.CHAR_CONTROL4_UUID
			}
			else {
				return BluenetProtocol.CHAR_CONTROL5_UUID
			}
		}
	}

	private fun getPacketProtocol(): PacketProtocol {
		return connection.getPacketProtocol()
	}
}
