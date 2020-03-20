/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.CrownstoneMode
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

/**
 * Class to interact with the device information service.
 *
 * Most commands assume you are already connected to the crownstone.
 */
class DeviceInfo(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	/**
	 * Get the firmware version.
	 *
	 * @return Promise with firmware version string as value.
	 */
	@Synchronized
	fun getFirmwareVersion(): Promise<String, Exception> {
		Log.i(TAG, "getFirmwareVersion")
		if (connection.mode != CrownstoneMode.NORMAL && connection.mode != CrownstoneMode.SETUP) {
			// In DFU, the firmware version is actually the bootloader version.
			return Promise.ofFail(Errors.Mode("Not in normal or setup mode"))
		}
		val deferred = deferred<String, Exception>()
		connection.read(BluenetProtocol.DEVICE_INFO_SERVICE_UUID, BluenetProtocol.CHAR_FIRMWARE_REVISION_UUID, false)
				.success {
					val version = String(it)
					deferred.resolve(version)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	/**
	 * Get the hardware version.
	 *
	 * @return Promise with hardware version string as value.
	 */
	@Synchronized
	fun getHardwareVersion(): Promise<String, Exception> {
		Log.i(TAG, "getHardwareVersion")
		// TODO: when in DFU, the hardware version string is shortened.
		val deferred = deferred<String, Exception>()
		connection.read(BluenetProtocol.DEVICE_INFO_SERVICE_UUID, BluenetProtocol.CHAR_HARDWARE_REVISION_UUID, false)
				.success {
					val version = String(it)
					deferred.resolve(version)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

	/**
	 * Get the bootloader version.
	 *
	 * @return Promise with bootloader version string as value.
	 */
	@Synchronized
	fun getBootloaderVersion(): Promise<String, Exception> {
		Log.i(TAG, "getBootloaderVersion")
		if (connection.mode != CrownstoneMode.DFU) {
			// When not in DFU, we can try to read the bootloader version via control command.
			val controlClass = Control(eventBus, connection)
			return controlClass.getBootloaderVersion()
		}
		val deferred = deferred<String, Exception>()
		connection.read(BluenetProtocol.DEVICE_INFO_SERVICE_UUID, BluenetProtocol.CHAR_FIRMWARE_REVISION_UUID, false)
				.success {
					val version = String(it)
					deferred.resolve(version)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}

}