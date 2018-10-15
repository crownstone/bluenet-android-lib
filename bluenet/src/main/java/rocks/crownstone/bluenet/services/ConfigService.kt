package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.services.packets.ConfigPacket
import rocks.crownstone.bluenet.util.Conversion

class ConfigService(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	inline private fun <reified T>getConfigValue(type: ConfigType): Promise<T, Exception> {
		val deferred = deferred<ConfigPacket, Exception>()
		return getConfig(type)
				.then {
					val arr = it.getPayload()
					if (arr == null) {
						return@then Promise.ofFail<T, Exception>(Errors.Parse())
					}
					try {
						val value = Conversion.byteArrayToSigned<T>(arr)
						return@then Promise.ofSuccess<T, Exception>(value)
					}
					catch (ex: Exception) {
						return@then Promise.ofFail<T, Exception>(ex)
					}
				}.unwrap()
	}

	private fun getConfig(type: ConfigType): Promise<ConfigPacket, Exception> {
		val writeCommand = fun (): Promise<Unit, Exception> {
			return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_CONTROL_UUID, ConfigPacket(type).getArray())
		}
		val deferred = deferred<ConfigPacket, Exception>()
		connection.getSingleMergedNotification(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_STATE_READ_UUID, writeCommand)
				.success {
					val statePacket = ConfigPacket()
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