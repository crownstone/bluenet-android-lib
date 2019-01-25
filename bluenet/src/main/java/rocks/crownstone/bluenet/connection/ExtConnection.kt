/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import android.util.Log
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.SubscriptionId
import rocks.crownstone.bluenet.util.Util
import java.util.*

/**
 * Extends the connection with:
 * - Encryption.
 * - Reading of session data.
 * - Checking of Crownstone mode.
 */
class ExtConnection(evtBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
//	var isConnected = false
//		private set
	var mode = CrownstoneMode.UNKNOWN
		private set

	/**
	 * Connect, discover service and get session data
	 */
	@Synchronized
	fun connect(address: DeviceAddress, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		return bleCore.connect(address, timeoutMs)
				.then {
					mode = CrownstoneMode.UNKNOWN
					bleCore.discoverServices(false)
				}.unwrap()
				.then {
					postConnect(address)
				}.unwrap()
				.success {
//					isConnected = true
				}
	}

	@Synchronized
	fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect clearCache=$clearCache")
		if (clearCache) {
			val deferred = deferred<Unit, Exception>()
			bleCore.disconnect()
					.always {
						bleCore.close(true)
								.success {
									deferred.resolve()
								}
								.fail {
									deferred.reject(it)
								}
					}
			return deferred.promise
		}
		return bleCore.close(false)
	}

	/**
	 * Encrypt and write data.
	 */
	@Synchronized
	fun write(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray, accessLevel: AccessLevel=AccessLevel.HIGHEST_AVAILABLE): Promise<Unit, Exception> {
		Log.i(TAG, "write to $characteristicUuid accessLevel=${accessLevel.name} data=${Conversion.bytesToString(data)}")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		val encryptedData = when (accessLevel) {
			AccessLevel.UNKNOWN, AccessLevel.ENCRYPTION_DISABLED -> {
				data
			}
			else -> {
				encryptionManager.encrypt(address, data, accessLevel)
			}
		}
		if (encryptedData == null) {
			return Promise.ofFail(Errors.Encryption())
		}
		return bleCore.write(serviceUuid, characteristicUuid, encryptedData)
	}

	@Synchronized
	fun read(serviceUuid: UUID, characteristicUuid: UUID, decrypt: Boolean=true): Promise<ByteArray, Exception> {
		Log.i(TAG, "read $characteristicUuid decrypt=$decrypt")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		if (!decrypt) {
			return bleCore.read(serviceUuid, characteristicUuid)
		}
		return bleCore.read(serviceUuid, characteristicUuid)
				.then {
					encryptionManager.decryptPromise(address, it)
				}.unwrap()
	}

	@Synchronized
	fun subscribe(serviceUuid: UUID, characteristicUuid: UUID, callback: (ByteArray) -> Unit): Promise<SubscriptionId, Exception> {
		Log.i(TAG, "subscribe serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		return bleCore.subscribe(serviceUuid, characteristicUuid, callback)
	}

	@Synchronized
	fun unsubscribe(serviceUuid: UUID, characteristicUuid: UUID, subscriptionId: SubscriptionId): Promise<Unit, Exception> {
		Log.i(TAG, "unsubscribe serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid subscriptionId=$subscriptionId")
		return bleCore.unsubscribe(serviceUuid, characteristicUuid, subscriptionId)
	}

	@Synchronized
	fun getSingleMergedNotification(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>, timeoutMs: Long): Promise<ByteArray, Exception> {
		Log.i(TAG, "getSingleMergedNotification serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		return bleCore.getSingleMergedNotification(serviceUuid, characteristicUuid, writeCommand, timeoutMs)
				.then {
					encryptionManager.decryptPromise(address, it)
				}.unwrap()
				.success {
					Log.i(TAG, "received merged notification on $characteristicUuid ${Conversion.bytesToString(it)}")
				}
	}

	@Synchronized
	fun getMultipleMergedNotifications(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>, callback: ProcessCallback, timeoutMs: Long): Promise<Unit, Exception> {
		Log.i(TAG, "getMultipleMergedNotifications serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		val processCallBack = fun (mergedNotification: ByteArray): ProcessResult {
			val decryptedData = encryptionManager.decrypt(address, mergedNotification)
			if (decryptedData == null) {
				return ProcessResult.ERROR
			}
			Log.i(TAG, "received merged notification on $characteristicUuid ${Conversion.bytesToString(mergedNotification)}")
			return callback(decryptedData)
		}
		return bleCore.getMultipleMergedNotifications(serviceUuid, characteristicUuid, writeCommand, processCallBack, timeoutMs)
	}


	/**
	 * @return Whether the service is available.
	 */
	@Synchronized
	fun hasService(serviceUuid: UUID): Boolean {
		return bleCore.hasService(serviceUuid)
	}

	/**
	 * @return Whether the characteristic is available.
	 */
	@Synchronized
	fun hasCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
		return bleCore.hasCharacteristic(serviceUuid, characteristicUuid)
	}

	@Synchronized
	fun wait(timeMs: Long): Promise<Unit, Exception> {
		return bleCore.wait(timeMs)
	}

	@Synchronized
	private fun postConnect(address: DeviceAddress): Promise<Unit, Exception> {
		Log.i(TAG, "postConnect $address")
		checkMode()
		when (mode) {
			CrownstoneMode.SETUP -> {
				Log.i(TAG, "get session data and key")
				return bleCore.read(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_SESSION_NONCE_UUID)
						.then {
							encryptionManager.parseSessionData(address, it, false)
						}.unwrap()
						.then {
							bleCore.read(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_KEY_UUID)
						}.unwrap()
						.then {
							encryptionManager.parseSessionKey(address, it)
						}.unwrap()
			}
			CrownstoneMode.NORMAL -> {
				if (encryptionManager.getKeySetFromAddress(address) == null) {
					Log.i(TAG, "No keys, don't read session data")
					return Promise.ofSuccess(Unit)
				}
				Log.i(TAG, "get session data")
				return bleCore.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_NONCE_UUID)
						.then {
//							// TODO: is it ok to ignore any error in the session data parsing?
//							// Ignore errors in parsing of session data: in case we have no keys for this crownstone, but want to write/read unencrypted data.
//							Util.recoverableUnitPromise(encryptionManager.parseSessionData(address, it, true), { true })
							encryptionManager.parseSessionData(address, it, true)
						}.unwrap()
			}
			CrownstoneMode.DFU -> {
				// Refresh services
				return bleCore.refreshDeviceCache()
			}
			else -> {
				return Promise.ofFail(Errors.Mode())
			}
		}
	}


	@Synchronized
	private fun checkMode() {
		Log.i(TAG, "checkMode")
		if (hasService(BluenetProtocol.SETUP_SERVICE_UUID)) {
			mode = CrownstoneMode.SETUP
		}
		else if (hasService(BluenetProtocol.DFU_SERVICE_UUID)) {
			mode = CrownstoneMode.DFU
		}
		else if (hasService(BluenetProtocol.CROWNSTONE_SERVICE_UUID)) {
			mode = CrownstoneMode.NORMAL
		}
		else {
			mode = CrownstoneMode.UNKNOWN
		}
	}
}