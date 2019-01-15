/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.StatePacket
import rocks.crownstone.bluenet.packets.schedule.ScheduleListPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus

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
		return getStateValue(StateType.RESET_COUNTER)
	}

	/**
	 * Get the switch state.
	 *
	 * @return Promise with SwitchState as value.
	 */
	@Synchronized
	fun getSwitchState(): Promise<SwitchState, Exception> {
//		val deferred = deferred<SwitchState, Exception>()
//		getStateValue<Uint8>(StateType.SWITCH_STATE)
//				.success { deferred.resolve(SwitchState(it)) }
//				.fail { deferred.reject(it) }
//		return deferred.promise
		return getStateValue<Uint8>(StateType.SWITCH_STATE)
				.then { SwitchState(it) }
	}

	/**
	 * Get the list of schedules.
	 *
	 * @return Promise with ScheduleListPacket as value.
	 */
	@Synchronized
	fun getScheduleList(): Promise<ScheduleListPacket, Exception> {
//		return getState(StateType.SCHEDULE)
//				.then {
//					val arr = it.getPayload()
//					if (arr == null) {
//						return@then Promise.ofFail<ScheduleListPacket, Exception>(Errors.Parse("state payload expected"))
//					}
//					val list = ScheduleListPacket()
//					if (!list.fromArray(arr)) {
//						return@then Promise.ofFail<ScheduleListPacket, Exception>(Errors.Parse("state payload expected"))
//					}
//					return@then Promise.ofSuccess<ScheduleListPacket, Exception>(list)
//				}.unwrap()

		// TODO: make this a generic function for payload type T (a ScheduleListPacket in this case)
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
		return getStateValue(StateType.TEMPERATURE)
	}

	/**
	 * Get the time of the crownstone.
	 *
	 * @return Promise with POSIX time as value.
	 */
	@Synchronized
	fun getTime(): Promise<Uint32, Exception> {
		// TODO: time conversion
		return getStateValue(StateType.TIME)
	}

	/**
	 * Get the errors of the crownstone.
	 *
	 * @return Promise with ErrorState as value.
	 */
	@Synchronized
	fun getErrors(): Promise<ErrorState, Exception> {
		val promise: Promise<Uint32, Exception> = getStateValue(StateType.ERRORS)
		return promise.then {
			ErrorState(it)
		}
	}

	inline private fun <reified T>getStateValue(type: StateType): Promise<T, Exception> {
		return getState(type)
				.then {
					val arr = it.getPayload()
					if (arr == null) {
						return@then Promise.ofFail<T, Exception>(Errors.Parse("state payload expected"))
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


	private fun getState(type: StateType): Promise<StatePacket, Exception> {
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
}