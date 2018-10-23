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

	private enum class OldSetupStep {
		START,
		ID,
		ADMIN_KEY,
		MEMBER_KEY,
		GUEST_KEY,
		MESH_ADDRESS,
		IBEACON_UUID,
		IBEACON_MAJOR,
		IBEACON_MINOR,
		FINALIZE,
		SUCCESS,
		DISCONNECTED
	}

	private enum class FastSetupStep {
		START,
		SUBSCRIBED,
		COMMAND_WRITTEN,
		WAIT_FOR_SUCCESS,
		SUCCESS,
		DISCONNECTED
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
		// TODO: increase TX?
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
				eventBus.emit(BluenetEvent.SETUP_PROGRESS, progress)
			}
		}

		// Process notifications
		val processCallback = fun (data: ByteArray): ProcessResult {
			val resultPacket = CommandResultPacket()
			if (!resultPacket.fromArray(data)) {
				return ProcessResult.ERROR
			}
			val result = resultPacket.resultCode
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
				fun (error: Exception): Boolean {
					// If the promise failed with a timeout error, assume it was a success (because we assume the crownstone rebooted before sending the notification).
					if (error is Errors.Timeout) {
//						if (step) ...
						sendProgress(FastSetupStep.SUCCESS, deferred.promise.isDone())
						return true
					}
					return false
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