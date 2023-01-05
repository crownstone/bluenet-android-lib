/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 23, 2022
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.packets.EmptyPacket
import rocks.crownstone.bluenet.packets.microapp.MicroappHeaderPacket
import rocks.crownstone.bluenet.packets.microapp.MicroappInfoPacket
import rocks.crownstone.bluenet.packets.microapp.MicroappUploadPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.toUint8
import kotlin.math.min

/**
 * Class to handle microapp commands.
 *
 * These commands assume you are already connected to the crownstone.
 */
class Microapp(eventBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val connection = connection

	companion object {
		const val MAX_CHUNK_SIZE = 128 // 256 was too much
		const val PROTOCOL: Uint8 = 1U
	}

	/**
	 * Upload, validate, and enable a microapp.
	 * First retrieves the microapp info to check compatibility.
	 *
	 * @param appIndex         At which index to place the microapp.
	 * @param microappBinary   The microapp binary.
	 * @return Promise
	 */
	@Synchronized
	fun uploadAndEnable(appIndex: Int, microappBinary: ByteArray): Promise<Unit, Exception> {
		Log.i(TAG, "uploadAndEnable")
		var remove = false
		return getMicroappInfo()
				.then {
					if (it.protocol != PROTOCOL) {
						return@then Promise.ofFail<Unit, Exception>(Errors.Unsupported("protocol=${it.protocol}"))
					}
					if (appIndex >= it.maxApps.toInt()) {
						return@then Promise.ofFail<Unit, Exception>(Errors.ValueWrong("maxApps=${it.maxApps}"))
					}
					if (it.maxAppSize.toInt() < microappBinary.size) {
						return@then Promise.ofFail<Unit, Exception>(Errors.SizeWrong("maxAppSize=${it.maxAppSize}"))
					}
					val chunkSize = it.maxChunkSize.toInt()
					if (it.appsStatus[appIndex].tests.hasData) {
						// If there's any data on this index, it has to be removed first.
						removeMicroapp(appIndex)
								.then {
									uploadMicroapp(appIndex, microappBinary, chunkSize)
								}.unwrap()
					}
					else {
						uploadMicroapp(appIndex, microappBinary, chunkSize)
					}
				}.unwrap()
				.then {
					validateMicroapp(appIndex)
				}.unwrap()
				.then {
					enableMicroapp(appIndex)
				}.unwrap()
	}

	/**
	 * Get the microapp info: what microapp support this crownstone has.
	 *
	 * @return Promise with microapp info as value.
	 */
	@Synchronized
	fun getMicroappInfo(): Promise<MicroappInfoPacket, Exception> {
		Log.i(TAG, "getInfo")
		if (connection.mode != CrownstoneMode.NORMAL) {
			return Promise.ofFail(Errors.Mode("Not in normal mode"))
		}
		val control = Control(eventBus, connection)
		val infoPacket = MicroappInfoPacket()
		return control.writeCommandAndGetResult<MicroappInfoPacket>(ControlType.UNKNOWN, ControlTypeV4.MICROAPP_GET_INFO, EmptyPacket(), infoPacket)
	}

	/**
	 * Enable a microapp.
	 *
	 * @param appIndex         The index of the microapp.
	 * @return Promise
	 */
	@Synchronized
	fun enableMicroapp(appIndex: Int): Promise<Unit, Exception> {
		val control = Control(eventBus, connection)
		val header = MicroappHeaderPacket(PROTOCOL, appIndex.toUint8())
		return control.writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.MICROAPP_ENABLE, header)
	}

	/**
	 * Disable a microapp.
	 *
	 * @param appIndex         The index of the microapp.
	 * @return Promise
	 */
	@Synchronized
	fun disableMicroapp(appIndex: Int): Promise<Unit, Exception> {
		val control = Control(eventBus, connection)
		val header = MicroappHeaderPacket(PROTOCOL, appIndex.toUint8())
		return control.writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.MICROAPP_DISABLE, header)
	}

	/**
	 * Validate a microapp.
	 *
	 * @param appIndex         The index of the microapp.
	 * @return Promise
	 */
	@Synchronized
	fun validateMicroapp(appIndex: Int): Promise<Unit, Exception> {
		val control = Control(eventBus, connection)
		val header = MicroappHeaderPacket(PROTOCOL, appIndex.toUint8())
		return control.writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.MICROAPP_VALIDATE, header)
	}

	/**
	 * Remove a microapp.
	 *
	 * @param appIndex         The index of the microapp.
	 * @return Promise
	 */
	@Synchronized
	fun removeMicroapp(appIndex: Int): Promise<Unit, Exception> {
		val control = Control(eventBus, connection)
		val header = MicroappHeaderPacket(PROTOCOL, appIndex.toUint8())
		return control.writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.MICROAPP_REMOVE, header)
	}

	/**
	 * Upload a microapp.
	 * Will upload the binary in multiple chunks when needed.
	 *
	 * @param appIndex         At which index to place the microapp.
	 * @param microappBinary   The microapp binary.
	 * @param chunkSize        The chunk size to use.
	 * @return Promise
	 */
	@Synchronized
	fun uploadMicroapp(appIndex: Int, microappBinary: ByteArray, chunkSize: Int = MAX_CHUNK_SIZE): Promise<Unit, Exception> {
		Log.i(TAG, "uploadMicroapp appIndex=$appIndex chunkSize=$chunkSize data=${microappBinary.contentToString()}")
		return uploadMicroappChunkNext(appIndex, microappBinary, 0, chunkSize)
	}

	@Synchronized
	private fun uploadMicroappChunkNext(appIndex: Int, data: ByteArray, offset: Int, chunkSize: Int = MAX_CHUNK_SIZE): Promise<Unit, Exception> {
		Log.i(TAG, "uploadMicroappChunkNext appIndex=$appIndex chunkSize=$chunkSize offset=$offset data=${data.contentToString()}")
		if (offset >= data.size) {
			return Promise.ofSuccess(Unit)
		}

		val maxChunkSize = min(chunkSize, MAX_CHUNK_SIZE)
		val actualChunkSize = min(maxChunkSize, data.size - offset)
		val chunk = data.slice(offset until (offset + actualChunkSize)).toMutableList()

		// Pad the chunk with 0xFF, so the size is a multiple of 4.
		if (chunk.size % 4 != 0) {
			for (j in 0 until 4 - chunk.size % 4) {
				chunk.add(0xFF.toByte())
			}
		}
		return uploadMicroappChunk(appIndex, chunk.toByteArray(), offset)
				.then {
					// Recursive call, with offset increased.
					uploadMicroappChunkNext(appIndex, data, offset + actualChunkSize, chunkSize)
				}.unwrap()
	}

	@Synchronized
	private fun uploadMicroappChunk(appIndex: Int, data: ByteArray, offset: Int): Promise<Unit, Exception> {
		Log.i(TAG, "uploadMicroappChunk appIndex=$appIndex offset=$offset data=${data.contentToString()}")
		val control = Control(eventBus, connection)
		val header = MicroappHeaderPacket(PROTOCOL, appIndex.toUint8())
		val packet = MicroappUploadPacket(header, offset, data)
		return control.writeCommandAndCheckResult(ControlType.UNKNOWN, ControlTypeV4.MICROAPP_UPLOAD, packet)
	}
}