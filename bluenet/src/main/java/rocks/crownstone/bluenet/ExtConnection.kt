package rocks.crownstone.bluenet

import android.util.Log
import nl.komponents.kovenant.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.util.Conversion
import java.util.*

/**
 * Extends the connection with encryption.
 */
class ExtConnection(evtBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	var isConnected = false
		private set
	var isSetupMode = false
		private set

	/**
	 * Connect, discover service and get session data
	 */
	@Synchronized fun connect(address: DeviceAddress, timeout: Int=100000): Promise<Unit, Exception> {
		Log.i(TAG, "connect $address")
		return bleCore.connect(address, timeout)
				.then {
					isSetupMode = false
					bleCore.discoverServices(false)
				}.unwrap()
				.then {
					checkSetupMode()
					getSessionData(address)
				}.unwrap()
				.success {
					isConnected = true
				}
	}

	@Synchronized fun disconnect(clearCache: Boolean = false): Promise<Unit, Exception> {
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
	@Synchronized fun write(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray, accessLevel: AccessLevel=AccessLevel.HIGHEST_AVAILABLE): Promise<Unit, Exception> {
		Log.i(TAG, "write to $characteristicUuid accessLevel=${accessLevel.name}")
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

	@Synchronized fun read(serviceUuid: UUID, characteristicUuid: UUID, decrypt: Boolean=true): Promise<ByteArray, Exception> {
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

	@Synchronized fun getSingleMergedNotification(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>): Promise<ByteArray, Exception> {
		val address = bleCore.getConnectedAddress()
		if (address == null) {
			return Promise.ofFail(Errors.NotConnected())
		}
		return bleCore.getSingleMergedNotification(serviceUuid, characteristicUuid, writeCommand)
				.then {
					encryptionManager.decryptPromise(address, it)
				}.unwrap()
				.success {
					Log.i(TAG, "received merged notification on $characteristicUuid ${Conversion.bytesToString(it)}")
				}
	}

	@Synchronized fun getMultipleMergedNotifications(serviceUuid: UUID, characteristicUuid: UUID, writeCommand: () -> Promise<Unit, Exception>, callback: ProcessCallback, timeoutMs: Long=0): Promise<Unit, Exception> {
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
	@Synchronized fun hasService(serviceUuid: UUID): Boolean {
		return bleCore.hasService(serviceUuid)
	}

	/**
	 * @return Whether the characteristic is available.
	 */
	@Synchronized fun hasCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
		return bleCore.hasCharacteristic(serviceUuid, characteristicUuid)
	}

	@Synchronized private fun getSessionData(address: DeviceAddress): Promise<Unit, Exception> {
		Log.i(TAG, "get session data $address")
		if (isSetupMode) {
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
		else {
			return bleCore.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_NONCE_UUID)
					.then {
						encryptionManager.parseSessionData(address, it, true)
					}.unwrap()
		}
	}


	@Synchronized private fun checkSetupMode() {
		Log.i(TAG, "checkSetupMode: ${bleCore.hasService(BluenetProtocol.SETUP_SERVICE_UUID)}")
		if (bleCore.hasService(BluenetProtocol.SETUP_SERVICE_UUID)) {
			isSetupMode = true
		}
	}
}