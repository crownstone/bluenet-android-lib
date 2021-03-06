/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import android.os.SystemClock
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
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
	private var protocolVersion: Uint8? = null
	var isReady: Boolean = false
		private set
	var mode = CrownstoneMode.UNKNOWN
		private set

	/**
	 * Connect, discover service and get session data
	 *
	 * Will retry to connect for certain connection errors.
	 */
	@Synchronized
	fun connect(address: DeviceAddress, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT, retries: Int = BluenetConfig.CONNECT_RETRIES): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		val deferred = deferred<Unit, Exception>()

		if (isReady && address.equals(bleCore.getConnectedAddress())) {
			// Assume we didn't disconnect and reconnect without calling disconnect() or waitForDisconnect().
			// TODO: maybe subscribe to disconnect event to set isReady to false?
			Log.i(TAG, "Already connected")
			deferred.resolve()
			return deferred.promise
		}

		isReady = false
		connectionAttempt(address, timeoutMs, retries)
				.then {
					mode = CrownstoneMode.UNKNOWN
					bleCore.discoverServices(false)
				}.unwrap()
				.then {
					postConnect(address)
				}.unwrap()
				.success {
					isReady = true
					deferred.resolve()
				}
				.fail {
					disconnect().always { deferred.reject(it) }
				}
		return deferred.promise
	}

	private fun connectionAttempt(address: DeviceAddress, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT, retries: Int): Promise<Unit, Exception> {
		// TODO: check time
		val deferred = deferred<Unit, Exception>()
		val startTime = SystemClock.elapsedRealtime()
		bleCore.connect(address, timeoutMs)
				.success { deferred.resolve() }
				.fail {
					val retry = when (it) {
						is Errors.GattError133 -> true
						else -> false
					}
					val curTime = SystemClock.elapsedRealtime()
					Log.i(TAG, "retry=$retry retries=$retries dt=${curTime - startTime}")
					if (retry && retries > 0 && (curTime - startTime < BluenetConfig.TIMEOUT_CONNECT_RETRY)) {
						connectionAttempt(address, timeoutMs, retries - 1)
								.success { deferred.resolve() }
								.fail { exception: Exception ->
									deferred.reject(exception)
								}
					}
					else {
						deferred.reject(it)
					}
				}
		return deferred.promise
	}

	@Synchronized
	fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect clearCache=$clearCache")
		isReady = false
		return bleCore.close(clearCache)
	}

	@Synchronized
	fun waitForDisconnect(clearCache: Boolean = false, timeoutMs: Long = BluenetConfig.TIMEOUT_WAIT_FOR_DISCONNECT): Promise<Unit, Exception> {
		Log.i(TAG, "waitForDisconnect timeoutMs=$timeoutMs clearCache=$clearCache")
		return waitForDisconnectAttempt(clearCache, timeoutMs, BluenetConfig.WAIT_FOR_DISCONNECT_ATTEMPTS)
				.success { isReady = false }
	}

	@Synchronized
	private fun waitForDisconnectAttempt(clearCache: Boolean, timeoutMs: Long, attemptsLeft: Int): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		bleCore.waitForDisconnect(clearCache, timeoutMs)
				.success {
					deferred.resolve()
				}
				.fail { coreException ->
					if (attemptsLeft < 1) {
						deferred.reject(coreException)
						return@fail
					}
					if (coreException !is Errors.Busy) {
						deferred.reject(coreException)
						return@fail
					}
					// Retry after waiting.
					bleCore.wait(BluenetConfig.WAIT_FOR_DISCONNECT_ATTEMPT_WAIT).always {
						waitForDisconnectAttempt(clearCache, timeoutMs, attemptsLeft - 1)
								.success { deferred.resolve() }
								.fail { recursiveException ->
									deferred.reject(recursiveException)
								}
					}
				}
		return deferred.promise
	}

	/**
	 * Encrypt and write data.
	 */
	@Synchronized
	fun write(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray?, accessLevel: AccessLevel=AccessLevel.HIGHEST_AVAILABLE): Promise<Unit, Exception> {
		Log.i(TAG, "write to $characteristicUuid accessLevel=${accessLevel.name} data=${Conversion.bytesToString(data)}")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		if (data == null) {
			return Promise.ofFail(Errors.ValueWrong())
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
	fun getSingleMergedNotification(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>, timeoutMs: Long, accessLevel: AccessLevel? = null): Promise<ByteArray, Exception> {
		Log.i(TAG, "getSingleMergedNotification serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		return bleCore.getSingleMergedNotification(serviceUuid, characteristicUuid, writeCommand, timeoutMs)
				.then {
					encryptionManager.decryptPromise(address, it, accessLevel)
				}.unwrap()
				.success {
					Log.i(TAG, "received merged notification on $characteristicUuid ${Conversion.bytesToString(it)}")
				}
	}

	@Synchronized
	fun getMultipleMergedNotifications(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>, callback: ProcessCallback, timeoutMs: Long, accessLevel: AccessLevel? = null): Promise<Unit, Exception> {
		Log.i(TAG, "getMultipleMergedNotifications serviceUuid=$serviceUuid characteristicUuid=$characteristicUuid")
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		val processCallBack = fun (mergedNotification: ByteArray): ProcessResult {
			val decryptedData = encryptionManager.decrypt(address, mergedNotification, accessLevel)
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
		encryptionManager.clearSessionData()
		when (mode) {
			CrownstoneMode.SETUP -> {
				Log.i(TAG, "get session data and key")
				val v5 = hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_SESSION_DATA_UUID)
				val sessionDataChar = when (v5) {
					true -> BluenetProtocol.CHAR_SETUP_SESSION_DATA_UUID
					false -> BluenetProtocol.CHAR_SETUP_SESSION_NONCE_UUID
				}
				return bleCore.read(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_KEY_UUID)
						.then {
							encryptionManager.parseSessionKey(address, it)
						}.unwrap()
						.then {
							bleCore.read(BluenetProtocol.SETUP_SERVICE_UUID, sessionDataChar)
						}.unwrap()
						.then {
							encryptionManager.parseSessionData(address, it, isEncrypted = v5, setupMode = true, v5 = v5)
						}.unwrap()
						.then {
							protocolVersion = it.protocolVersion
							return@then Unit
						}
			}
			CrownstoneMode.NORMAL -> {
				if (encryptionManager.getKeySetFromAddress(address) == null) {
					Log.w(TAG, "No keys, don't read session data")
					return Promise.ofSuccess(Unit)
				}
				Log.i(TAG, "get session data")
				val v5 = hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_DATA_UUID)
				val sessionDataChar = when (v5) {
					true -> BluenetProtocol.CHAR_SESSION_DATA_UUID
					false -> BluenetProtocol.CHAR_SESSION_NONCE_UUID
				}
				return bleCore.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, sessionDataChar)
						.then {
//							// TODO: is it ok to ignore any error in the session data parsing?
//							// Ignore errors in parsing of session data: in case we have no keys for this crownstone, but want to write/read unencrypted data.
//							Util.recoverableUnitPromise(encryptionManager.parseSessionData(address, it, true), { true })
							encryptionManager.parseSessionData(address, it, isEncrypted = true, setupMode = false, v5 = v5)
						}.unwrap()
						.then {
							protocolVersion = it.protocolVersion
							return@then Unit
						}
			}
			CrownstoneMode.DFU -> {
				// Refresh services
				return bleCore.refreshDeviceCache()
			}
			else -> {
				Log.w(TAG, "unknown mode")
				bleCore.logCharacteristics()
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
		else if (hasService(BluenetProtocol.CROWNSTONE_SERVICE_UUID)) {
			mode = CrownstoneMode.NORMAL
		}
		else if (hasService(BluenetProtocol.DFU_SERVICE_UUID) || hasService(BluenetProtocol.DFU2_SERVICE_UUID)) {
			mode = CrownstoneMode.DFU
		}
		else {
			mode = CrownstoneMode.UNKNOWN
		}
	}

	@Synchronized
	fun getPacketProtocol(): PacketProtocol {
			if (mode == CrownstoneMode.SETUP) {
				if (hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
					return PacketProtocol.V3
				}
				else if (hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
					return PacketProtocol.V3
				}
				else if (hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL3_UUID)) {
					return PacketProtocol.V3
				}
				else if (hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL4_UUID)) {
					return PacketProtocol.V4
				}
				else {
					return PacketProtocol.V5
				}
			}
			else {
				if (hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID)) {
					return PacketProtocol.V3
				}
				else if (hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL4_UUID)) {
					return PacketProtocol.V4
				}
				else {
					return PacketProtocol.V5
				}
			}
	}
}
