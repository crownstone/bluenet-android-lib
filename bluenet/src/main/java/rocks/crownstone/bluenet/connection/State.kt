/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.wrappers.v3.StatePacket
import rocks.crownstone.bluenet.packets.schedule.ScheduleListPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

/**
 * Class to interact with the state characteristics of the crownstone service.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class State(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	/**
	 * Get the reset count.
	 *
	 * @return Promise with the reset count as value.
	 */
	@Synchronized
	fun getResetCount(): Promise<Uint16, Exception> {
		Log.i(TAG, "getResetCount")
		return getStateValue(StateType.RESET_COUNTER, StateTypeV4.RESET_COUNTER)
	}

	/**
	 * Get the switch state.
	 *
	 * @return Promise with SwitchState as value.
	 */
	@Synchronized
	fun getSwitchState(): Promise<SwitchState, Exception> {
		Log.i(TAG, "getSwitchState")
//		val deferred = deferred<SwitchState, Exception>()
//		getStateValue<Uint8>(StateType.SWITCH_STATE)
//				.success { deferred.resolve(SwitchState(it)) }
//				.fail { deferred.reject(it) }
//		return deferred.promise
		return getStateValue<Uint8>(StateType.SWITCH_STATE, StateTypeV4.SWITCH_STATE)
				.then { SwitchState(it) }
	}

	/**
	 * Get the list of schedules.
	 *
	 * @return Promise with ScheduleListPacket as value.
	 */
	@Synchronized
	fun getScheduleList(): Promise<ScheduleListPacket, Exception> {
		Log.i(TAG, "getScheduleList")
		val type = StateType.SCHEDULE
		val writeCommand = fun (): Promise<Unit, Exception> {
			return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_CONTROL_UUID, StatePacket(type).getArray())
		}
		val deferred = deferred<ScheduleListPacket, Exception>()
		connection.getSingleMergedNotification(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_READ_UUID, writeCommand, BluenetConfig.TIMEOUT_GET_STATE)
				.success {
					val scheduleList = ScheduleListPacket()
					val statePacket = StatePacket(type, scheduleList)

					if (!statePacket.fromArray(it) || statePacket.type != type.num) {
						deferred.reject(Errors.Parse("can't make a ScheduleListPacket from ${Conversion.bytesToString(it)}"))
					}
					deferred.resolve(scheduleList)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	/**
	 * Get an empty index in the schedule list.
	 *
	 * @return Promise with an empty index as value.
	 */
	@Synchronized
	fun getAvailableScheduleEntryIndex(): Promise<Int, Exception> {
		Log.i(TAG, "getAvailableScheduleEntryIndex")
		return getScheduleList()
				.then { scheduleList ->
					for (i in 0 until scheduleList.list.size) {
						val entry = scheduleList.list[i]
						if (!entry.isActive()) {
							return@then Promise.ofSuccess<Int, Exception>(i)
						}
					}
					return@then Promise.ofFail<Int, Exception>(Errors.Full())
				}.unwrap()
	}

	/**
	 * Get the chip temperature.
	 *
	 * @return Promise with temperature in °C as value.
	 */
	@Synchronized
	fun getTemperature(): Promise<Int32, Exception> {
		Log.i(TAG, "getTemperature")
		return getStateValue(StateType.TEMPERATURE, StateTypeV4.TEMPERATURE)
	}

	/**
	 * Get the time of the crownstone.
	 *
	 * @return Promise with POSIX time as value.
	 */
	@Synchronized
	fun getTime(): Promise<Uint32, Exception> {
		Log.i(TAG, "getTime")
		val deferred = deferred<Uint32, Exception>()
		getStateValue<Uint32>(StateType.TIME, StateTypeV4.TIME)
				.success() {
					deferred.resolve(it)
				}
				.fail() {
					Log.i(TAG, "Failed to get state time, trying via control command. Error: $it")
					val control = Control(eventBus, connection)
					control.writeCommandAndGetResult<Uint32>(ControlTypeV4.GET_TIME, EmptyPacket())
							.success() {
								deferred.resolve(it)
							}
							.fail {
								deferred.reject(it)
							}
				}
		return deferred.promise
	}

	/**
	 * Get the errors of the crownstone.
	 *
	 * @return Promise with ErrorState as value.
	 */
	@Synchronized
	fun getErrors(): Promise<ErrorState, Exception> {
		Log.i(TAG, "getErrors")
		val promise: Promise<Uint32, Exception> = getStateValue(StateType.ERRORS, StateTypeV4.ERRORS)
		return promise.then {
			ErrorState(it)
		}
	}


	private inline fun <reified T>getStateValue(type: StateType, type4: StateTypeV4, id: Uint16 = BluenetProtocol.STATE_DEFAULT_ID): Promise<T, Exception> {
		if (getPacketProtocol() == PacketProtocol.V3) {
			return getStateV3(type)
					.then {
						val arr = it.getPayload()
						if (arr == null) {
							return@then Promise.ofFail<T, Exception>(Errors.Parse("state payload expected"))
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
			val configClass = Config(eventBus, connection)
			return configClass.getStateValue(type4, id)
		}
	}


	private fun getStateV3(type: StateType): Promise<StatePacket, Exception> {
		val writeCommand = fun (): Promise<Unit, Exception> {
			return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_CONTROL_UUID, StatePacket(type).getArray())
		}
		val deferred = deferred<StatePacket, Exception>()
		connection.getSingleMergedNotification(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_READ_UUID, writeCommand, BluenetConfig.TIMEOUT_GET_STATE)
				.success {
					val statePacket = StatePacket()
					if (!statePacket.fromArray(it) || statePacket.type != type.num) {
						deferred.reject(Errors.Parse("can't make a StatePacket from ${Conversion.bytesToString(it)}"))
					}
					deferred.resolve(statePacket)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}


	private fun getPacketProtocol(): PacketProtocol {
		return connection.getPacketProtocol()
	}
}
