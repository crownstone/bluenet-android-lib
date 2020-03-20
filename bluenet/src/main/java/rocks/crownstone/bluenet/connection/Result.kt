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
			timeoutMs: Long,
			acceptedResults: List<ResultType> = listOf(ResultType.SUCCESS),
			accessLevel: AccessLevel? = null
	): Promise<ResultPacketV4, Exception> {
		val deferred = deferred<ResultPacketV4, Exception>()
		connection.getSingleMergedNotification(getServiceUuid(), getResultCharacteristic(), writeCommand, timeoutMs, accessLevel)
				.success {
					val packet = ResultPacketV4()
					if (!packet.fromArray(it)) {
						deferred.reject(Errors.Parse("can't make a ResultPacketV4 from ${Conversion.bytesToString(it)}"))
					}
					else if (!acceptedResults.contains(packet.resultCode)) {
						deferred.reject(Errors.Result(packet.resultCode))
					}
					else {
						deferred.resolve(packet)
					}
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	@Synchronized
	fun getSingleResult(
			writeCommand: () -> Promise<Unit, Exception>,
			type: ControlTypeV4,
			payload: PacketInterface,
			timeoutMs: Long,
			acceptedResults: List<ResultType> = listOf(ResultType.SUCCESS),
			accessLevel: AccessLevel? = null
	): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		connection.getSingleMergedNotification(getServiceUuid(), getResultCharacteristic(), writeCommand, timeoutMs, accessLevel)
				.success {
					val packet = ResultPacketV4(type, ResultType.UNKNOWN, payload)
					if (!packet.fromArray(it) || packet.type != type) {
						deferred.reject(Errors.Parse("can't make a ResultPacketV4 from ${Conversion.bytesToString(it)}"))
					}
					else if (!acceptedResults.contains(packet.resultCode)) {
						deferred.reject(Errors.Result(packet.resultCode))
					}
					else {
						deferred.resolve()
					}
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	@Synchronized
	fun getMultipleResults(
			writeCommand: () -> Promise<Unit, Exception>,
			callback: ResultProcessCallback,
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
