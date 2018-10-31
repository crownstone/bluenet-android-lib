package rocks.crownstone.bluenet.services

import android.util.Log
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.services.packets.CommandResultPacket
import rocks.crownstone.bluenet.services.packets.SetupPacket
import rocks.crownstone.bluenet.util.Util
import kotlin.Exception

class Setup(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	private enum class OldSetupStep(val num: Int) {
		START(1),
		INCREASED_TX(2),
		ID_WRITTEN(3),
		ADMIN_KEY_WRITTEN(4),
		MEMBER_KEY_WRITTEN(5),
		GUEST_KEY_WRITTEN(6),
		MESH_ADDRESS_WRITTEN(7),
		IBEACON_UUID_WRITTEN(8),
		IBEACON_MAJOR_WRITTEN(9),
		IBEACON_MINOR_WRITTEN(10),
		FINALIZE_WRITTEN(11),
		DISCONNECTED(12)
	}

	private enum class FastSetupStep(val num: Int) {
		START(1),
		SUBSCRIBED(3),
		COMMAND_WRITTEN(5),
		WAIT_FOR_SUCCESS(7),
		SUCCESS(10),
		DISCONNECTED(12)
	}

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

		fun sendProgress(progress: OldSetupStep) {
			Log.i(TAG, "progress ${progress.name}")
			val progressDouble = progress.num / 13.0
			eventBus.emit(BluenetEvent.SETUP_PROGRESS, progressDouble)
		}

		sendProgress(OldSetupStep.START)
		// TODO: increase TX?
		return config.setCrownstoneId(id)
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.ID_WRITTEN)
					config.setAdminKey(adminKey)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.ADMIN_KEY_WRITTEN)
					config.setMemberKey(memberKey)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.MEMBER_KEY_WRITTEN)
					config.setGuestKey(guestKey)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.GUEST_KEY_WRITTEN)
					config.setMeshAccessAddress(meshAccessAddress)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.MESH_ADDRESS_WRITTEN)
					config.setIbeaconUuid(ibeaconData.uuid)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.IBEACON_UUID_WRITTEN)
					config.setIbeaconMajor(ibeaconData.major)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.IBEACON_MAJOR_WRITTEN)
					config.setIbeaconMinor(ibeaconData.minor)
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.IBEACON_MINOR_WRITTEN)
					control.validateSetup()
				}.unwrap()
				.then {
					connection.wait(500)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.FINALIZE_WRITTEN)
					connection.disconnect()
				}.unwrap()
				.success {
					sendProgress(OldSetupStep.DISCONNECTED)
				}
	}

	/**
	 * Perform the fast setup
	 *
	 * Subscribes, then writes the setup command, then waits for notifications, then disconnects.
	 * If notifications time out, we assume a success, because the crownstone probably rebooted before sending the notifications.
	 */
	private fun fastSetup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()

		// After subscribing to notifications, the setup command should be written.
		val packet = SetupPacket(0, id, keySet, meshAccessAddress, ibeaconData)
		val control = Control(eventBus, connection)
		val writeCommand = fun (): Promise<Unit, Exception> { return control.setup(packet) }
		var step = FastSetupStep.START

		fun sendProgress(progress: FastSetupStep, isDone: Boolean) {
			Log.i(TAG, "progress ${progress.name}")
			if (!isDone) {
				step = progress
				val progressDouble = progress.num / 13.0
				eventBus.emit(BluenetEvent.SETUP_PROGRESS, progressDouble)
			}
		}

		// Process notifications
		val processCallback = fun (data: ByteArray): ProcessResult {
			val resultPacket = CommandResultPacket()
			if (!resultPacket.fromArray(data)) {
				return ProcessResult.ERROR
			}
			val result = resultPacket.resultCode
			Log.i(TAG, "result: ${result.name}")
			when (result) {
				ResultType.WAIT_FOR_SUCCESS -> {
					sendProgress(FastSetupStep.WAIT_FOR_SUCCESS, deferred.promise.isDone())
					return ProcessResult.NOT_DONE
				}
				ResultType.SUCCESS -> {
					sendProgress(FastSetupStep.SUCCESS, deferred.promise.isDone())
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
		Util.recoverablePromise(
				connection.getMultipleMergedNotifications(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID, writeCommand, processCallback, 3000),
				fun (error: Exception): Promise<Unit, Exception> {
					// If the promise failed with a timeout error, assume it was a success (because we assume the crownstone rebooted before sending the notification).
					if (error is Errors.Timeout) {
//						if (step) ...
						sendProgress(FastSetupStep.SUCCESS, deferred.promise.isDone())
						return Promise.ofSuccess(Unit)
					}
					return Promise.ofFail(error)
				}
		)
				.then {
					connection.disconnect()
				}.unwrap()
				.then {
					sendProgress(FastSetupStep.DISCONNECTED, deferred.promise.isDone())
					deferred.resolve()
				}
				.fail {
					if (!deferred.promise.isDone()) {
						deferred.reject(it)
					}
				}
		return deferred.promise
	}
}