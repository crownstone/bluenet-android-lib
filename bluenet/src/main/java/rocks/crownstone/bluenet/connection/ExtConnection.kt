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
import rocks.crownstone.bluenet.core.CoreConnection
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.ConnectionEncryption
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.SubscriptionId
import java.util.*

/**
 * Class for a single BLE connection with Bluenet specific implementation:
 * - Reading of session data.
 * - Encryption.
 * - Reconnection attempts.
 * - Checking of Crownstone mode.
 */
class ExtConnection(address: DeviceAddress, eventBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val bleCore = CoreConnection(bleCore)
	private val encryptionManager = encryptionManager
	private val connectionEncryptionManager = ConnectionEncryption(encryptionManager)
	private var protocolVersion: Uint8? = null

	/**
	 * The address of this connection.
	 */
	val address = address

	/**
	 * Will be set to true when connected.
	 */
	var isConnected: Boolean = false
		private set

	/**
	 * Will be set to the correct Crownstone mode when connected.
	 */
	var mode = CrownstoneMode.UNKNOWN
		private set

	/**
	 * Connect, discover service and get session data
	 * Will retry to connect for certain connection errors.
	 *
	 * @param auto           Automatically connect once the device is in range.
	 *                       Note that this will only work when the device is in cache:
	 *                       when it's bonded or when it has been scanned since last phone or bluetooth restart.
	 *                       This may be slower than a non-auto connect when the device is already in range.
	 *                       You can have multiple pending auto connections, but only 1 non-auto connecting at a time.
	 * @param timeoutMs      Optional: timeout in ms.
	 * @param retries        Optional: number of times to retry.
	 * @return Promise that resolves when connected.
	 */
	@Synchronized
	fun connect(auto: Boolean, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT, retries: Int = BluenetConfig.CONNECT_RETRIES, getSessionData: Boolean = true): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address auto=$auto timeoutMs=$timeoutMs retries=$retries")
		val deferred = deferred<Unit, Exception>()

		if (isConnected && address.equals(bleCore.getConnectedAddress())) {
			// Assume we didn't disconnect and reconnect without calling disconnect() or waitForDisconnect().
			// TODO: maybe subscribe to disconnect event to set isReady to false?
			Log.i(TAG, "Already connected")
			deferred.resolve()
			return deferred.promise
		}

		isConnected = false
		connectionAttempt(auto, timeoutMs, retries)
				.then {
					mode = CrownstoneMode.UNKNOWN
					bleCore.discoverServices(false)
				}.unwrap()
				.then {
					postConnect(getSessionData)
				}.unwrap()
				.success {
					isConnected = true
					deferred.resolve()
				}
				.fail {
					disconnect().always { deferred.reject(it) }
				}
		return deferred.promise
	}

	private fun connectionAttempt(auto: Boolean, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT, retries: Int): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val startTime = SystemClock.elapsedRealtime()
		bleCore.connect(address, auto, timeoutMs)
				.success {
					deferred.resolve()
					val curTime = SystemClock.elapsedRealtime()
					Log.i(TAG, "connect success address=$address dt=${curTime - startTime} ms")
				}
				.fail {
					val retry = when (it) {
						is Errors.GattError133 -> true
						else -> false
					}
					val curTime = SystemClock.elapsedRealtime()
					Log.i(TAG, "connect failed address=$address retry=$retry retries=$retries dt=${curTime - startTime} ms")
					if (retry && retries > 0 && (curTime - startTime < BluenetConfig.TIMEOUT_CONNECT_RETRY)) {
						connectionAttempt(auto, timeoutMs, retries - 1)
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

	/**
	 * Abort current action (connect, disconnect, write, read, subscribe, unsubscribe) and disconnects.
	 * Mostly made to abort connecting.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun abort(): Promise<Unit, Exception> {
		return bleCore.abort()
	}

	@Synchronized
	fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
		Log.i(TAG, "disconnect $address clearCache=$clearCache")
		isConnected = false
		return bleCore.close(clearCache)
	}

	@Synchronized
	fun waitForDisconnect(clearCache: Boolean = false, timeoutMs: Long = BluenetConfig.TIMEOUT_WAIT_FOR_DISCONNECT): Promise<Unit, Exception> {
		Log.i(TAG, "waitForDisconnect $address timeoutMs=$timeoutMs clearCache=$clearCache")
		return waitForDisconnectAttempt(clearCache, timeoutMs, BluenetConfig.WAIT_FOR_DISCONNECT_ATTEMPTS)
				.success { isConnected = false }
	}

	/**
	 * Waits for disconnect and retries when core is busy.
	 */
	@Synchronized
	private fun waitForDisconnectAttempt(clearCache: Boolean, timeoutMs: Long, attemptsLeft: Int): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val startTime = SystemClock.elapsedRealtime()
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
					// Decrease timeout.
					val elapsedTime = SystemClock.elapsedRealtime() - startTime
					val newTimeoutMs = timeoutMs - elapsedTime - BluenetConfig.WAIT_FOR_DISCONNECT_ATTEMPT_WAIT
					if (newTimeoutMs < 1) {
						deferred.reject(Errors.Timeout())
						return@fail
					}
					Log.i(TAG, "waitForDisconnect failed: $coreException. Retrying in ${BluenetConfig.WAIT_FOR_DISCONNECT_ATTEMPT_WAIT} ms.")
					bleCore.wait(BluenetConfig.WAIT_FOR_DISCONNECT_ATTEMPT_WAIT).always {
						waitForDisconnectAttempt(clearCache, newTimeoutMs, attemptsLeft - 1)
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
				connectionEncryptionManager.encrypt(address, data, accessLevel)
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
					connectionEncryptionManager.decryptPromise(address, it)
				}.unwrap()
				.success { Log.i(TAG, "read decrypted: ${Conversion.bytesToString(it)}") }
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
					connectionEncryptionManager.decryptPromise(address, it, accessLevel)
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
			val decryptedData = connectionEncryptionManager.decrypt(address, mergedNotification, accessLevel)
			if (decryptedData == null) {
				return ProcessResult(ProcessResultType.ERROR, Errors.Encryption())
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
	private fun postConnect(getSessionData: Boolean): Promise<Unit, Exception> {
		Log.i(TAG, "postConnect $address")
		bleCore.logCharacteristics()
		checkMode()
		connectionEncryptionManager.clearSessionData()
		when (mode) {
			CrownstoneMode.SETUP -> {
				if (!getSessionData) {
					Log.i(TAG, "Skip reading session data")
					return Promise.ofSuccess(Unit)
				}
				val (v5, sessionDataChar, encrypted) =
						when {
							hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_SESSION_DATA_UNENCRYPTED_UUID) -> {
								Triple(true, BluenetProtocol.CHAR_SETUP_SESSION_DATA_UNENCRYPTED_UUID, false)
							}
							hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_SESSION_DATA_UUID) -> {
								Triple(true, BluenetProtocol.CHAR_SETUP_SESSION_DATA_UUID, true)
							}
							else -> {
								Triple(false, BluenetProtocol.CHAR_SETUP_SESSION_NONCE_UUID, false)
							}
						}

				Log.i(TAG, "get session data and key v5=$v5 encrypted=$encrypted sessionDataChar=$sessionDataChar")
				return bleCore.read(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_KEY_UUID)
						.then {
							connectionEncryptionManager.parseSessionKey(address, it)
						}.unwrap()
						.then {
							bleCore.read(BluenetProtocol.SETUP_SERVICE_UUID, sessionDataChar)
						}.unwrap()
						.then {
							connectionEncryptionManager.parseSessionData(address, it, isEncrypted = encrypted, setupMode = true, v5 = v5)
						}.unwrap()
						.then {
							protocolVersion = it.protocolVersion
							return@then Unit
						}
			}
			CrownstoneMode.NORMAL -> {
				if (!getSessionData) {
					Log.i(TAG, "Skip reading session data")
					return Promise.ofSuccess(Unit)
				}
				if (encryptionManager.getKeySetFromAddress(address) == null) {
					Log.w(TAG, "No keys, don't read session data")
					return Promise.ofSuccess(Unit)
				}

				val (v5, sessionDataChar, encrypted) =
						when {
							hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_DATA_UNENCRYPTED_UUID) -> {
								Triple(true, BluenetProtocol.CHAR_SESSION_DATA_UNENCRYPTED_UUID, false)
							}
							hasCharacteristic(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_DATA_UUID) -> {
								Triple(true, BluenetProtocol.CHAR_SESSION_DATA_UUID, true)
							}
							else -> {
								Triple(false, BluenetProtocol.CHAR_SESSION_NONCE_UUID, true)
							}
						}

				Log.i(TAG, "get session data v5=$v5 encrypted=$encrypted sessionDataChar=$sessionDataChar")
				return bleCore.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, sessionDataChar)
						.then {
//							// TODO: is it ok to ignore any error in the session data parsing?
//							// Ignore errors in parsing of session data: in case we have no keys for this crownstone, but want to write/read unencrypted data.
//							Util.recoverableUnitPromise(encryptionManager.parseSessionData(address, it, true), { true })
							connectionEncryptionManager.parseSessionData(address, it, isEncrypted = encrypted, setupMode = false, v5 = v5)
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
		Log.d(TAG, "checkMode")
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
		Log.i(TAG, "mode=$mode protocol=${getPacketProtocol()}")
	}

	@Synchronized
	fun getPacketProtocol(): PacketProtocol {
			if (mode == CrownstoneMode.SETUP) {
				if (hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
					return PacketProtocol.V1
				}
				else if (hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
					return PacketProtocol.V2
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
