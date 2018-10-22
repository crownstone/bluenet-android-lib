package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.services.packets.CommandResultPacket
import rocks.crownstone.bluenet.services.packets.SetupPacket
import rocks.crownstone.bluenet.util.Conversion
import kotlin.Exception

class Setup(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun setup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		if (!connection.isSetupMode) {
			return Promise.ofFail(Errors.NotInSetupMode())
		}
		val adminKey =  keySet.adminKeyBytes
		val memberKey = keySet.memberKeyBytes
		val guestKey =  keySet.guestKeyBytes
		if (adminKey == null || memberKey == null || guestKey == null) {
			return Promise.ofFail(Errors.ValueWrong())
		}

		if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
			return fastSetup(id, keySet, meshAccessAddress, ibeaconData)
		}
		else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
			return oldSetup(id, adminKey, memberKey, guestKey, meshAccessAddress, ibeaconData)
		}
		else {
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
	}

	private fun oldSetup(id: Uint8, adminKey: ByteArray, memberKey: ByteArray, guestKey: ByteArray, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val config = Config(eventBus, connection)
		val control = Control(eventBus, connection)
		return config.setCrownstoneId(id)
				.then {
					config.setAdminKey(adminKey)
				}.unwrap()
				.then {
					config.setMemberKey(memberKey)
				}.unwrap()
				.then {
					config.setGuestKey(guestKey)
				}.unwrap()
				.then {
					config.setMeshAccessAddress(meshAccessAddress)
				}.unwrap()
				.then {
					config.setIbeaconUuid(ibeaconData.uuid)
				}.unwrap()
				.then {
					config.setIbeaconMajor(ibeaconData.major)
				}.unwrap()
				.then {
					config.setIbeaconMinor(ibeaconData.minor)
				}.unwrap()
				.then {
					control.validateSetup()
				}.unwrap()
	}

	private fun fastSetup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()

		// After subscribing to notifications, the setup command should be written.
		val packet = SetupPacket(0, id, keySet, meshAccessAddress, ibeaconData)
		val control = Control(eventBus, connection)
		val writeCommand = fun (): Promise<Unit, Exception> { return control.setup(packet) }

		//
		val processCallback = fun (data: ByteArray): ProcessResult {
			val resultPacket = CommandResultPacket()
			if (!resultPacket.fromArray(data)) {
				return ProcessResult.ERROR
			}
			val result = resultPacket.resultCode
			when (result) {
				ResultType.WAIT_FOR_SUCCESS -> {
					sendProgress(10.0/13, deferred.promise.isDone())
					return ProcessResult.NOT_DONE
				}
				ResultType.SUCCESS -> {
					sendProgress(11.0/13, deferred.promise.isDone())
					return ProcessResult.DONE
				}
				else -> {
					if (!deferred.promise.isDone()) {
						deferred.reject(Errors.Result(result))
					}
					return ProcessResult.ERROR
				}
			}
		}

		// Subscribe for notifications

		connection.getMultipleMergedNotifications(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID, writeCommand, processCallback, 3000)
				.fail {
					if (it is Errors.Timeout) {
						// Assume success.
						sendProgress(13.0/13, deferred.promise.isDone())
						deferred.resolve()
					}
				}
				.then {
					// TODO
					// disconnect
				}
				.then {
					sendProgress(13.0/13, deferred.promise.isDone())
					deferred.resolve()
				}
				.fail {
					if (!deferred.promise.isDone()) {
						deferred.reject(it)
					}
				}
		return deferred.promise
	}

	private fun sendProgress(progress: Double, isDone: Boolean) {
		if (!isDone) {
			eventBus.emit(BluenetEvent.SETUP_PROGRESS, progress)
		}
	}
}