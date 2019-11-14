/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.encryption.MeshKeySet
import rocks.crownstone.bluenet.packets.wrappers.v3.CommandResultPacket
import rocks.crownstone.bluenet.packets.SetupPacket
import rocks.crownstone.bluenet.packets.SetupPacketV2
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.Util
import java.util.*
import kotlin.Exception

/**
 * Class to perform the setup.
 *
 * Assumes you are already connected to the crownstone.
 */
class Setup(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	companion object {
		val OLD_SETUP_WAIT_MS = 500L // Time to wait between steps in ms
	}

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

	/**
	 * Reads the MAC address from the characteristic.
	 *
	 * @return Promise with address as value.
	 */
	@Synchronized
	fun getAddress(): Promise<DeviceAddress, Exception> {
		Log.i(TAG, "getAddress")
		return connection.read(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_MAC_ADDRESS_UUID, false)
				.then { Conversion.bytesToAddress(it) }
	}

	/**
	 * Performs the setup and disconnects.
	 *
	 * Emits progress events.
	 *
	 * @param stoneId           The crownstone id, should be unique per sphere.
	 * @param sphereShortId     The sphere short id, these are not unique, but used as filter.
	 * @param keys              The keys for encryption.
	 * @param meshKeys          The mesh keys.
	 * @param meshDeviceKey     Unique key for this device.
	 * @param meshAccessAddress A unique value, should comply to rules found ... where ??
	 * @param ibeaconData       iBeacon UUID, major, minor, and calibrated rssi.
	 * @return Promise
	 */
	@Synchronized
	fun setup(stoneId: Uint8, sphereShortId: Uint8, keys: KeySet, meshKeys: MeshKeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		Log.i(TAG, "setup stoneId=$stoneId stoneId=$sphereShortId keys=$keys meshKeys=$meshKeys meshAccessAddress=$meshAccessAddress ibeaconData=$ibeaconData")
		if (connection.mode != CrownstoneMode.SETUP) {
			return Promise.ofFail(Errors.NotInMode(CrownstoneMode.SETUP))
		}

		if (
				(stoneId <= 0 || stoneId > 255) ||
				(sphereShortId <= 0 || sphereShortId > 255) ||
				(ibeaconData.major < 0 || ibeaconData.major > 0xFFFF) ||
				(ibeaconData.minor < 0 || ibeaconData.minor > 0xFFFF)
		) {
			return Promise.ofFail(Errors.ValueWrong())
		}

		if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL3_UUID)) {
			return fastSetupV2(stoneId, sphereShortId, keys, meshKeys, ibeaconData)
		}
		else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
			return fastSetup(stoneId, keys, meshAccessAddress, ibeaconData)
		}
		else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
			return oldSetup(stoneId, keys.adminKeyBytes, keys.memberKeyBytes, keys.guestKeyBytes, meshAccessAddress, ibeaconData)
		}
		else {
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
	}

	private fun oldSetup(id: Uint8, adminKey: ByteArray?, memberKey: ByteArray?, guestKey: ByteArray?, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val config = Config(eventBus, connection)
		val control = Control(eventBus, connection)

		if (adminKey == null || memberKey == null || guestKey == null) {
			return Promise.ofFail(Errors.ValueWrong())
		}

		fun sendProgress(progress: OldSetupStep) {
			Log.i(TAG, "progress ${progress.name}")
			val progressDouble = progress.num / 13.0
			eventBus.emit(BluenetEvent.SETUP_PROGRESS, progressDouble)
		}

		sendProgress(OldSetupStep.START)
		// TODO: increase TX?
		return control.increaseTx()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					config.setCrownstoneId(id)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.ID_WRITTEN)
					config.setAdminKey(adminKey)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.ADMIN_KEY_WRITTEN)
					config.setMemberKey(memberKey)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.MEMBER_KEY_WRITTEN)
					config.setGuestKey(guestKey)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.GUEST_KEY_WRITTEN)
					config.setMeshAccessAddress(meshAccessAddress)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.MESH_ADDRESS_WRITTEN)
					config.setIbeaconUuid(ibeaconData.uuid)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.IBEACON_UUID_WRITTEN)
					config.setIbeaconMajor(ibeaconData.major)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.IBEACON_MAJOR_WRITTEN)
					config.setIbeaconMinor(ibeaconData.minor)
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.IBEACON_MINOR_WRITTEN)
					control.validateSetup()
				}.unwrap()
				.then {
					connection.wait(OLD_SETUP_WAIT_MS)
				}.unwrap()
				.then {
					sendProgress(OldSetupStep.FINALIZE_WRITTEN)
					connection.disconnect()
				}.unwrap()
				.success {
					sendProgress(OldSetupStep.DISCONNECTED)
				}
	}

	private fun fastSetup(stoneId: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val packet = SetupPacket(0, stoneId, keySet, meshAccessAddress, ibeaconData)
		val control = Control(eventBus, connection)
		val writeCommand = fun (): Promise<Unit, Exception> { return control.setup(packet) }
		return performFastSetup(writeCommand, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)
	}

	private fun fastSetupV2(stoneId: Uint8, sphereId: Uint8, keySet: KeySet, meshKeys: MeshKeySet, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val packet = SetupPacketV2(stoneId, sphereId, keySet, meshKeys, ibeaconData)
		val control = Control(eventBus, connection)
		val writeCommand = fun (): Promise<Unit, Exception> { return control.setup(packet) }
		return performFastSetup(writeCommand, BluenetProtocol.CHAR_SETUP_CONTROL3_UUID)
	}

	/**
	 * Perform the fast setup
	 *
	 * Subscribes, then writes the setup command, then waits for notifications, then disconnects.
	 * If notifications time out, we assume a success, because the crownstone probably rebooted before sending the notifications.
	 */
	private fun performFastSetup(writeCommand: () -> Promise<Unit, Exception>, characteristicUuid: UUID): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
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
				connection.getMultipleMergedNotifications(BluenetProtocol.SETUP_SERVICE_UUID, characteristicUuid, writeCommand, processCallback, 3000),
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
