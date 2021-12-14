/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import android.os.SystemClock
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.ResultPacket
import rocks.crownstone.bluenet.packets.wrappers.v3.ResultPacketV3
import rocks.crownstone.bluenet.packets.wrappers.v4.ResultPacketV4
import rocks.crownstone.bluenet.packets.wrappers.v5.ResultPacketV5
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.toUint16
import java.util.*

class Result (eventBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val connection = connection

	/**
	 * Execute write command, wait for success, and set result payload.
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param protocol            Expected protocol version, may be checked against the received protocol.
	 * @param type                Control / config / state type that was used in the write command, may be checked against the received type.
	 * @param type4               Expected data type, may be checked against the received type.
	 * @param payload             Expected result payload, will be filled on success.
	 * @param timeoutMs           Timeout in ms.
	 * @param serviceUuid         The service UUID to get the result from, or null to use the default.
	 * @param characteristicUuid  The characteristic UUID to get the result from, or null to use the default.
	 * @param acceptedResults     List of result codes that are ok to be processed, or null to use the default. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.SUCCESS_NO_CHANGE, ResultType.WAIT_FOR_SUCCESS)
	 * @param accessLevel         Access level used for decryption of result packets, or null to use the default.
	 */
	@Synchronized
	fun getSingleResult(
			writeCommand: () -> Promise<Unit, Exception>,
			protocol: ConnectionProtocol,
			type: Uint8,
			type4: ControlTypeV4,
			payload: PacketInterface,
			timeoutMs: Long,
			serviceUuid: UUID? = null,
			characteristicUuid: UUID? = null,
			acceptedResults: List<ResultType>? = null,
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		return getSingleResult(writeCommand, protocol, type, type4, timeoutMs, serviceUuid, characteristicUuid, acceptedResults, accessLevel)
				.then {
					if (!payload.fromArray(it)) {
						return@then Promise.ofFail<Unit, Exception>(Errors.Parse("can't make a ${payload.javaClass.simpleName} from ${Conversion.bytesToString(it)}"))
					}
					return@then Promise.ofSuccess<Unit, Exception>(Unit)
				}.unwrap()
	}

	/**
	 * Execute write command, wait for success, and return result data.
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param protocol            Expected protocol version, may be checked against the received protocol.
	 * @param type                Control / config / state type that was used in the write command, may be checked against the received type.
	 * @param type4               Expected data type, may be checked against the received type.
	 * @param timeoutMs           Timeout in ms.
	 * @param serviceUuid         The service UUID to get the result from, or null to use the default.
	 * @param characteristicUuid  The characteristic UUID to get the result from, or null to use the default.
	 * @param acceptedResults     List of result codes that are ok to be processed, or null to use the default. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.SUCCESS_NO_CHANGE, ResultType.WAIT_FOR_SUCCESS)
	 * @param accessLevel         Access level used for decryption of result packets, or null to use the default.
	 * @return                    The result payload as byte array.
	 */
	@Synchronized
	fun getSingleResult(
			writeCommand: () -> Promise<Unit, Exception>,
			protocol: ConnectionProtocol,
			type: Uint8,
			type4: ControlTypeV4,
			timeoutMs: Long,
			serviceUuid: UUID? = null,
			characteristicUuid: UUID? = null,
			acceptedResults: List<ResultType>? = null,
			accessLevel: AccessLevel? = null
	): Promise<ByteArray, Exception> {
		val acceptedResults = (acceptedResults ?: listOf(ResultType.SUCCESS, ResultType.SUCCESS_NO_CHANGE)) + ResultType.WAIT_FOR_SUCCESS
		val serviceUuid = serviceUuid ?: BluenetProtocol.CROWNSTONE_SERVICE_UUID
		val characteristicUuid = characteristicUuid ?: BluenetProtocol.CHAR_CONTROL_UUID
		var resultData = ByteArray(0)

		val callback = fun (packet: ResultPacket): ProcessResult {
			when (packet.getCode()) {
				ResultType.WAIT_FOR_SUCCESS -> return ProcessResult(ProcessResultType.NOT_DONE)
				in acceptedResults -> {
					resultData = packet.getPayload() ?: ByteArray(0)
					return ProcessResult(ProcessResultType.DONE)
				}
				else -> return ProcessResult(ProcessResultType.ERROR, Errors.Result(packet.getCode()))
			}
		}

		when (connection.getPacketProtocol()) {
			PacketProtocol.V1 -> {
				return checkResultCodeV1(writeCommand, type, timeoutMs, serviceUuid, characteristicUuid, acceptedResults)
						.then { return@then Promise.ofSuccess<ByteArray, Exception>(ByteArray(0)) }.unwrap()
			}
			PacketProtocol.V2,
			PacketProtocol.V3 -> {
				// It seems like v3 control does not actually set the result, so we can't use it.
				val assumeSuccess = true
				return checkResultCodeV2V3(writeCommand, type, timeoutMs, serviceUuid, characteristicUuid, assumeSuccess, acceptedResults)
						.then { return@then Promise.ofSuccess<ByteArray, Exception>(ByteArray(0)) }.unwrap()
			}
			PacketProtocol.V4 -> {
				return getMultipleResultsV4(writeCommand, callback, timeoutMs, acceptedResults, accessLevel)
						.then { return@then Promise.ofSuccess<ByteArray, Exception>(resultData) }.unwrap()
			}
			PacketProtocol.V5 -> {
				return getMultipleResultsV5(writeCommand, callback, timeoutMs, acceptedResults, accessLevel)
						.then { return@then Promise.ofSuccess<ByteArray, Exception>(resultData) }.unwrap()
			}
		}
	}

	/**
	 * Write a command, and get multiple result packets.
	 *
	 * Uses v4 result packets.
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param callback            Callback function for each result packet. Example:
	 *                                 fun (resultPacket: ResultPacketV4): ProcessResult { return ProcessResult.DONE }
	 * @param timeoutMs           Timeout in ms.
	 * @param acceptedResults     List of result codes that are ok to be processed. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS)
	 * @param accessLevel         Optional access level used for decryption of result packets.
	 * @return                    Promise that resolves or rejects after cleaning up.
	 */
	@Synchronized
	fun getMultipleResultsV4(
			writeCommand: () -> Promise<Unit, Exception>,
			callback: ResultProcessCallbackV4,
			timeoutMs: Long,
			acceptedResults: List<ResultType>? = null,
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val acceptedResults = acceptedResults ?: listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS, ResultType.SUCCESS_NO_CHANGE)
		val processCallback = fun (data: ByteArray): ProcessResult {
			val packet = ResultPacketV4()
			if (!packet.fromArray(data)) {
				return ProcessResult(ProcessResultType.ERROR, Errors.Parse("Can't make result packet v4 from $data"))
			}
			if (!acceptedResults.contains(packet.resultCode)) {
				return ProcessResult(ProcessResultType.ERROR, Errors.Result(packet.resultCode))
			}
			return callback(packet)
		}

		return connection.getMultipleMergedNotifications(getServiceUuid(), getResultCharacteristic(), writeCommand, processCallback, timeoutMs, accessLevel)
	}

	/**
	 * Write a command, and get multiple result packets.
	 * 
	 * Uses v5 result packets.
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param callback            Callback function for each result packet. Example:
	 *                                 fun (resultPacket: ResultPacketV5): ProcessResult { return ProcessResult.DONE }
	 * @param timeoutMs           Timeout in ms.
	 * @param acceptedResults     List of result codes that are ok to be processed. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS)
	 * @param accessLevel         Optional access level used for decryption of result packets.
	 * @return                    Promise that resolves or rejects after cleaning up.
	 */
	@Synchronized
	fun getMultipleResultsV5(
			writeCommand: () -> Promise<Unit, Exception>,
			callback: ResultProcessCallbackV5,
			timeoutMs: Long,
			acceptedResults: List<ResultType>? = null,
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val acceptedResults = acceptedResults ?: listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS, ResultType.SUCCESS_NO_CHANGE)
		val processCallback = fun (data: ByteArray): ProcessResult {
			val packet = ResultPacketV5()
			if (!packet.fromArray(data)) {
				return ProcessResult(ProcessResultType.ERROR, Errors.Parse("Can't make result packet v5 from $data"))
			}
			if (!acceptedResults.contains(packet.resultCode)) {
				return ProcessResult(ProcessResultType.ERROR, Errors.Result(packet.resultCode))
			}
			return callback(packet)
		}

		return connection.getMultipleMergedNotifications(getServiceUuid(), getResultCharacteristic(), writeCommand, processCallback, timeoutMs, accessLevel)
	}



	/**
	 * Executes write command, then checks result.
	 *
	 * TODO: This function assumes success, because of decryption errors (validation mismatch, most likely because the read access level is 2 (member) instead of 100 (setup).
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param type                Control / config / state type that was used in the write command.
	 * @param timeoutMs           Timeout in ms.
	 * @param serviceUuid         The service UUID to get the result from.
	 * @param characteristicUuid  The characteristic UUID to get the result from.
	 * @param acceptedResults     List of result codes that are ok to be processed. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS)
	 */
	@Synchronized
	fun checkResultCodeV1(
			writeCommand: () -> Promise<Unit, Exception>,
			type: Uint8,
			timeoutMs: Long,
			serviceUuid: UUID,
			characteristicUuid: UUID,
			acceptedResults: List<ResultType>? = null,
	): Promise<Unit, Exception> {
		return writeCommand()
				.then {
					connection.wait(BluenetConfig.DELAY_READ_AFTER_COMMAND)
				}.unwrap()
	}

	/**
	 * Executes write command, then checks result.
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param type                Control / config / state type that was used in the write command.
	 * @param timeoutMs           Timeout in ms.
	 * @param serviceUuid         The service UUID to get the result from.
	 * @param characteristicUuid  The characteristic UUID to get the result from.
	 * @param assumeSuccess       Instead of checking the result, simply assume success.
	 * @param acceptedResults     List of result codes that are ok to be processed. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS)
	 */
	@Synchronized
	fun checkResultCodeV2V3(
			writeCommand: () -> Promise<Unit, Exception>,
			type: Uint8,
			timeoutMs: Long,
			serviceUuid: UUID,
			characteristicUuid: UUID,
			assumeSuccess: Boolean,
			acceptedResults: List<ResultType>? = null,
	): Promise<Unit, Exception> {
		val acceptedResults = acceptedResults ?: listOf(ResultType.SUCCESS, ResultType.SUCCESS_NO_CHANGE)
		if (assumeSuccess) {
			return connection.wait(BluenetConfig.DELAY_READ_AFTER_COMMAND)
		}
		return writeCommand()
				.then {
					checkResultCodeAttempt(type, timeoutMs, serviceUuid, characteristicUuid, acceptedResults)
				}.unwrap()
	}

	/**
	 * Waits and then reads the result code.
	 */
	private fun checkResultCodeAttempt(
			type: Uint8,
			timeoutMs: Long,
			serviceUuid: UUID,
			characteristicUuid: UUID,
			acceptedResults: List<ResultType>
	): Promise<Unit, Exception> {
		val startTime = SystemClock.elapsedRealtime()
		Log.d(TAG, "checkResultCodeAttempt timeoutMs=$timeoutMs")
		// TODO: can we get notifications instead?
		return connection.wait(BluenetConfig.DELAY_READ_AFTER_COMMAND)
				.then {
					connection.read(serviceUuid, characteristicUuid, true)
				}.unwrap()
				.then {
					// We cannot distinguish between V1, V2, or V3.
					// So just try the V2/V3 packet first, otherwise try the V1 result code only.
					val resultPacket3 = ResultPacketV3()
					val resultCode: ResultType =
							if (resultPacket3.fromArray(it)) {
								Log.d(TAG, "Result packet: $resultPacket3")
								resultPacket3.resultCode
								// TODO: check type?
							}
							else {
								Log.d(TAG, "Result uint16: ${Conversion.byteArrayToShort(it).toUint16()}")
								ResultType.fromNum(Conversion.byteArrayToShort(it).toUint16())
							}
					Log.d(TAG, "Result code: $resultCode")
					when (resultCode) {
						ResultType.WAIT_FOR_SUCCESS -> {
							val elapsedTime = SystemClock.elapsedRealtime() - startTime
							val newTimeoutMs = timeoutMs - elapsedTime - BluenetConfig.DELAY_READ_AFTER_COMMAND
							if (newTimeoutMs < 1) {
								return@then Promise.ofFail(Errors.Timeout())
							}
							return@then checkResultCodeAttempt(type, newTimeoutMs, serviceUuid, characteristicUuid, acceptedResults)
						}
						in acceptedResults -> return@then Promise.ofSuccess<Unit, Exception>(Unit)
						else -> return@then Promise.ofFail(Errors.Result(resultCode))
					}
				}.unwrap()
	}

	private fun getServiceUuid(): UUID {
		if (connection.mode == CrownstoneMode.SETUP) {
			return BluenetProtocol.SETUP_SERVICE_UUID
		}
		else {
			return BluenetProtocol.CROWNSTONE_SERVICE_UUID
		}
	}

	private fun getResultCharacteristic(): UUID {
		if (connection.mode == CrownstoneMode.SETUP) {
			if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_RESULT_UUID)) {
				return BluenetProtocol.CHAR_SETUP_RESULT_UUID
			}
			else {
				return BluenetProtocol.CHAR_SETUP_RESULT5_UUID
			}
		}
		else {
			if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RESULT_UUID)) {
				return BluenetProtocol.CHAR_RESULT_UUID
			}
			else {
				return BluenetProtocol.CHAR_RESULT5_UUID
			}
		}
	}
}
