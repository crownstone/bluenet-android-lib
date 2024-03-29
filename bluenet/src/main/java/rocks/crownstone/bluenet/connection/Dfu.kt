/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import android.content.Context
import nl.komponents.kovenant.*
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuBaseService
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.Util
import java.util.*


/**
 * Class to interact with DFU service.
 */
class Dfu(eventBus: EventBus, connection: ExtConnection, context: Context) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val connection = connection
	private val context = context
	private var dfuDeferred: Deferred<Unit, Exception>? = null

	private val progressListener = object: DfuProgressListener {
		override fun onDeviceConnecting(deviceAddress: String) {
			Log.d(TAG, "onDeviceConnecting")
		}

		override fun onDeviceConnected(deviceAddress: String) {
			Log.d(TAG, "onDeviceConnected")
		}

		override fun onEnablingDfuMode(deviceAddress: String) {
			Log.d(TAG, "onEnablingDfuMode")
		}

		override fun onDfuProcessStarting(deviceAddress: String) {
			Log.d(TAG, "onDfuProcessStarting")
		}

		override fun onDfuProcessStarted(deviceAddress: String) {
			Log.d(TAG, "onDfuProcessStarted")
		}

		override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
			Log.d(TAG, "onProgressChanged percent=$percent ($currentPart / $partsTotal) speed=$speed ($avgSpeed avg)")
			onProgress(percent, speed, avgSpeed, currentPart, partsTotal)
		}

		override fun onFirmwareValidating(deviceAddress: String) {
			Log.d(TAG, "onFirmwareValidating")
		}

		override fun onDeviceDisconnecting(deviceAddress: String) {
			Log.d(TAG, "onDeviceDisconnecting")
		}

		override fun onDeviceDisconnected(deviceAddress: String) {
			Log.d(TAG, "onDeviceDisconnected")
		}

		override fun onDfuCompleted(deviceAddress: String) {
			Log.d(TAG, "onDfuCompleted")
			onDfuCompleted()
		}

		override fun onDfuAborted(deviceAddress: String) {
			Log.d(TAG, "onDfuAborted")
			onDfuAborted()
		}

		override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
			Log.d(TAG, "onError error=$error errorType=$errorType msg=$message")
			onDfuError(error, errorType, message)
		}
	}

	init {
		DfuServiceListenerHelper.registerProgressListener(context, progressListener)
	}

	/**
	 * Reset the device, which will make it go out of DFU mode.
	 *
	 * Also resolves when already in normal mode.
	 */
	@Synchronized
	fun reset(): Promise<Unit, Exception> {
		Log.i(TAG, "reset")
		if (connection.hasService(BluenetProtocol.CROWNSTONE_SERVICE_UUID) || connection.hasService(BluenetProtocol.SETUP_SERVICE_UUID)) {
			Log.i(TAG, "already out of DFU mode")
			return connection.disconnect(true)
		}
		if (connection.hasService(BluenetProtocol.DFU_SERVICE_UUID)) {
			return connection.subscribe(BluenetProtocol.DFU_SERVICE_UUID, BluenetProtocol.CHAR_DFU_CONTROL_UUID, {})
					.then {
						Util.recoverableUnitPromise(
								connection.write(BluenetProtocol.DFU_SERVICE_UUID, BluenetProtocol.CHAR_DFU_CONTROL_UUID, BluenetProtocol.DFU_RESET_COMMAND, AccessLevel.ENCRYPTION_DISABLED),
								{
									Log.i(TAG, "Write error expected, as bootloader resets.")
									true
								}
						)
					}.unwrap()
					.then {
						// Disconnect and clear cache, as services are expected to change.
						connection.disconnect(true)
					}.unwrap()
		}
		else if (connection.hasService(BluenetProtocol.DFU2_SERVICE_UUID)) {
			return Util.recoverableUnitPromise(
					connection.write(BluenetProtocol.DFU2_SERVICE_UUID, BluenetProtocol.CHAR_DFU2_CONTROL_UUID, BluenetProtocol.DFU2_RESET_COMMAND, AccessLevel.ENCRYPTION_DISABLED),
					{
						Log.i(TAG, "Write error expected, as bootloader resets.")
						true
					}
			)
					.then {
						// Disconnect and clear cache, as services are expected to change.
						connection.disconnect(true)
					}.unwrap()
		}
		return Promise.ofFail(Errors.CharacteristicNotFound(BluenetProtocol.DFU2_SERVICE_UUID))
	}

	/**
	 * Start the dfu process.
	 *
	 * Connects, performs dfu, and disconnects.
	 * Emits progress events.
	 *
	 * @param fileName Filename of the zip with the firmware.
	 * @param service  Implementation of [DfuBaseService], see https://github.com/NordicSemiconductor/Android-DFU-Library/tree/release/documentation
	 * @return Promise that resolves when dfu was successful.
	 */
	@Synchronized
	fun startDfu(fileName: String, service: Class<out DfuBaseService>): Promise<Unit, Exception> {
		Log.i(TAG, "startDfu address=${connection.address} fileName=$fileName")
		if (dfuDeferred != null) {
			return Promise.ofFail(Errors.Busy("busy with dfu"))
		}
		val deferred = deferred<Unit, Exception>()
		val dfuServiceInitiator = DfuServiceInitiator(connection.address)
//				.setDeviceName(name)
				.setKeepBond(false)
				.setDisableNotification(true)
				.setForeground(false)
				.setRestoreBond(false)
				.setPacketsReceiptNotificationsEnabled(false)
//				.setForceDfu(false)
				.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(false)
				.setZip(null, fileName)
				.setMtu(23) // Same as iOS
				.setPacketsReceiptNotificationsEnabled(true)
				.setNumberOfRetries(2)
		// Controller can be used to pause / resume / abort.
		val dfuServiceController = dfuServiceInitiator.start(context, service)
		dfuDeferred = deferred
		return deferred.promise
	}

	@Synchronized
	private fun onProgress(percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
		eventBus.emit(BluenetEvent.DFU_PROGRESS, DfuProgress(percent, speed, avgSpeed, currentPart, partsTotal))
	}

	@Synchronized
	private fun onDfuCompleted() {
		val deferred = dfuDeferred ?: return
		deferred.resolve(Unit)
		dfuDeferred = null
	}

	@Synchronized
	private fun onDfuAborted() {
		val deferred = dfuDeferred ?: return
		deferred.reject(Errors.Aborted())
		dfuDeferred = null
	}

	@Synchronized
	private fun onDfuError(error: Int, errorType: Int, message: String?) {
		val deferred = dfuDeferred ?: return
		val type = when (errorType) {
			DfuBaseService.ERROR_TYPE_COMMUNICATION_STATE -> "ERROR_TYPE_COMMUNICATION_STATE"
			DfuBaseService.ERROR_TYPE_COMMUNICATION -> "ERROR_TYPE_COMMUNICATION"
			DfuBaseService.ERROR_TYPE_DFU_REMOTE -> "ERROR_TYPE_DFU_REMOTE"
			DfuBaseService.ERROR_TYPE_OTHER -> "ERROR_TYPE_OTHER"
			else -> "unknown"
		}
		deferred.reject(Exception("dfu error $error type=$type msg=$message"))
		dfuDeferred = null
	}
}