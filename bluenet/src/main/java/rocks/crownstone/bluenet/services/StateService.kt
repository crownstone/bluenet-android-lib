package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.services.packets.StatePacket
import rocks.crownstone.bluenet.util.Conversion

class StateService(evtBus: EventBus, connection: ExtConnection) {
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

	// TODO: schedules
//	fun getScheduleList(): Promise<, Exception> {
//		return getState(StateType.SCHEDULE)
//	}

	fun getTemperature(): Promise<Int32, Exception> {
		return getStateValue(StateType.TEMPERATURE)
	}

	fun getTime(): Promise<Uint32, Exception> {
		// TODO: time conversion
		return getStateValue(StateType.TIME)
	}

	fun getErrors(): Promise<Uint32, Exception> {
		// TODO: class for errors
		return getStateValue(StateType.ERRORS)
	}

	inline private fun <reified T>getStateValue(type: StateType): Promise<T, Exception> {
		val deferred = deferred<StatePacket, Exception>()
		return getState(type)
				.then {
					val arr = it.getPayload()
					if (arr == null) {
						return@then Promise.ofFail<T, Exception>(Errors.Parse())
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
		connection.getSingleMergedNotification(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_READ_UUID, writeCommand)
				.success {
					val statePacket = StatePacket()
					if (!statePacket.fromArray(it) || statePacket.type != type.num) {
						deferred.reject(Errors.Parse())
					}
					deferred.resolve(statePacket)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}
}