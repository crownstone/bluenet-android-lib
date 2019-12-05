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
	fun getSingleResult(writeCommand: () -> Promise<Unit, Exception>, timeoutMs: Long): Promise<ResultPacketV4, Exception> {
		val deferred = deferred<ResultPacketV4, Exception>()
		connection.getSingleMergedNotification(getServiceUuid(), getResultCharacteristic(), writeCommand, timeoutMs)
				.success {
					val packet = ResultPacketV4()
					if (packet.fromArray(it)) {
						deferred.resolve(packet)
					}
					else {
						deferred.reject(Errors.Parse("can't make a ResultPacketV4 from ${Conversion.bytesToString(it)}"))
					}
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	@Synchronized
	fun getSingleResult(writeCommand: () -> Promise<Unit, Exception>, type: ControlTypeV4, payload: PacketInterface, timeoutMs: Long): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		connection.getSingleMergedNotification(getServiceUuid(), getResultCharacteristic(), writeCommand, timeoutMs)
				.success {
					val packet = ResultPacketV4(type, ResultType.UNKNOWN, payload)
					if (!packet.fromArray(it) || packet.type != type) {
						deferred.reject(Errors.Parse("can't make a ResultPacketV4 from ${Conversion.bytesToString(it)}"))
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
	fun getMultipleResults(writeCommand: () -> Promise<Unit, Exception>, callback: ResultProcessCallback, timeoutMs: Long): Promise<Unit, Exception> {
		val processCallback = fun (data: ByteArray): ProcessResult {
			val packet = ResultPacketV4()
			if (packet.fromArray(data)) {
				return callback(packet)
			}
			else {
				return ProcessResult.ERROR
			}
		}

		return connection.getMultipleMergedNotifications(getServiceUuid(), getResultCharacteristic(), writeCommand, processCallback, timeoutMs)
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
				// Try anyway
				return BluenetProtocol.CHAR_SETUP_RESULT_UUID
			}
		}
		else {
			if (connection.hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_RESULT_UUID)) {
				return BluenetProtocol.CHAR_RESULT_UUID
			}
			else {
				// Try anyway
				return BluenetProtocol.CHAR_RESULT_UUID
			}
		}
	}
}
