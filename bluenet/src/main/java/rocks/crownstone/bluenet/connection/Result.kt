/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.resolve
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.v4.ResultPacketV4
import rocks.crownstone.bluenet.packets.wrappers.v5.ResultPacketV5
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import java.util.*

class Result (evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	@Synchronized
	fun getSingleResult(
			writeCommand: () -> Promise<Unit, Exception>,
			protocol: ConnectionProtocol,
			type: ControlTypeV4,
			payload: PacketInterface,
			timeoutMs: Long,
			acceptedResults: List<ResultType> = listOf(ResultType.SUCCESS),
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		connection.getSingleMergedNotification(getServiceUuid(), getResultCharacteristic(), writeCommand, timeoutMs, accessLevel)
				.success {
					if (connection.getPacketProtocol() == PacketProtocol.V4) {
						val packet = ResultPacketV4(type, ResultType.UNKNOWN, payload)
						val parseSuccess = packet.fromArray(it)
						if (packet.type != type) {
							deferred.reject(Errors.Parse("wrong type, got ${packet.type}, expected $type"))
						}
						else if (packet.resultCode != ResultType.UNKNOWN && !acceptedResults.contains(packet.resultCode)) {
							deferred.reject(Errors.Result(packet.resultCode))
						}
						else if (!parseSuccess) {
							deferred.reject(Errors.Parse("can't make a ResultPacketV4 with payload ${payload.javaClass.simpleName} from ${Conversion.bytesToString(it)}"))
						}
						else {
							deferred.resolve()
						}
					}
					else {
						val packet = ResultPacketV5(protocol, type, ResultType.UNKNOWN, payload)
						val parseSuccess = packet.fromArray(it)
						if (packet.type != type) {
							deferred.reject(Errors.Parse("wrong type, got ${packet.type}, expected $type"))
						}
						else if (packet.resultCode != ResultType.UNKNOWN && !acceptedResults.contains(packet.resultCode)) {
							deferred.reject(Errors.Result(packet.resultCode))
						}
						else if (!parseSuccess) {
							deferred.reject(Errors.Parse("can't make a ResultPacketV5 with payload ${payload.javaClass.simpleName} from ${Conversion.bytesToString(it)}"))
						}
						else {
							deferred.resolve()
						}
					}
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	@Synchronized
	fun getMultipleResultsV4(
			writeCommand: () -> Promise<Unit, Exception>,
			callback: ResultProcessCallbackV4,
			timeoutMs: Long,
			acceptedResults: List<ResultType> = listOf(ResultType.SUCCESS),
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val processCallback = fun (data: ByteArray): ProcessResult {
			val packet = ResultPacketV4()
			if (!packet.fromArray(data)) {
				return ProcessResult.ERROR
			}
			else if (!acceptedResults.contains(packet.resultCode)) {
				return ProcessResult.ERROR
			}
			else {
				return callback(packet)
			}
		}

		return connection.getMultipleMergedNotifications(getServiceUuid(), getResultCharacteristic(), writeCommand, processCallback, timeoutMs, accessLevel)
	}

	/**
	 * Write a command, and get multiple result packets.
	 * 
	 * Uses v5 wrapper packets.
	 *
	 * @param writeCommand        Function to write the command. Example:
	 *                                 fun (): Promise<Unit, Exception> { return writeCommand(ControlType.UNKNOWN, ControlTypeV4.HUB_DATA, hubDataPacket) }
	 * @param callback            Callback function for each result packet. Example:
	 *                                 fun (resultPacket: ResultPacketV5): ProcessResult { return ProcessResult.DONE }
	 * @param acceptedResults     List of result codes that are ok to be processed. Example:
	 *                                 listOf(ResultType.SUCCESS, ResultType.WAIT_FOR_SUCCESS)
	 *                                 TODO: remove this option?
	 * @param accessLevel         Optional access level used for decryption of result packets.
	 * @return                    Promise that resolves or rejects after cleaning up.
	 */
	@Synchronized
	fun getMultipleResultsV5(
			writeCommand: () -> Promise<Unit, Exception>,
			callback: ResultProcessCallbackV5,
			timeoutMs: Long,
			acceptedResults: List<ResultType> = listOf(ResultType.SUCCESS),
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val processCallback = fun (data: ByteArray): ProcessResult {
			val packet = ResultPacketV5()
			if (!packet.fromArray(data)) {
				return ProcessResult.ERROR
			}
			else if (!acceptedResults.contains(packet.resultCode)) {
				return ProcessResult.ERROR
			}
			else {
				return callback(packet)
			}
		}

		return connection.getMultipleMergedNotifications(getServiceUuid(), getResultCharacteristic(), writeCommand, processCallback, timeoutMs, accessLevel)
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
