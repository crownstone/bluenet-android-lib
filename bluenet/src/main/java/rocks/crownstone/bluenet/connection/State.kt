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

class State(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun getResetCount(): Promise<Uint16, Exception> {
		return getStateValue(StateType.RESET_COUNTER)
	}

	fun getSwitchState(): Promise<Uint8, Exception> {
		// TODO: class for switchstate
		return getStateValue(StateType.SWITCH_STATE)
	}

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

	fun getTemperature(): Promise<Int32, Exception> {
		return getStateValue(StateType.TEMPERATURE)
	}

	fun getTime(): Promise<Uint32, Exception> {
		// TODO: time conversion
		return getStateValue(StateType.TIME)
	}

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