package rocks.crownstone.bluenet

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.encryption.EncryptionManager
import java.util.*

/**
 * Extends the connection with encryption.
 */
class ExtConnection(evtBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager
	var isSetupMode = false
		private set

	fun connect(address: DeviceAddress): Promise<Unit, Exception> {
		return bleCore.connect(address, 9999)
				.then {
					isSetupMode = false
					bleCore.discoverServices(false)
				}.unwrap()
				.then {
					checkSetupMode()
					getSessionData(address)
				}.unwrap()
	}

	fun write(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray, accessLevel: AccessLevel): Promise<Unit, Exception> {
		val encryptedData = when (accessLevel) {
			AccessLevel.UNKNOWN, AccessLevel.SETUP -> data
			else -> {
				val address = bleCore.getConnectedAddress()
				if (address == null) {
					return Promise.ofFail(Errors.NotConnected())
				}
				encryptionManager.encrypt(address, data, accessLevel)
			}
		}
		if (encryptedData == null) {
			return Promise.ofFail(Errors.Encryption())
		}
		return bleCore.write(serviceUuid, characteristicUuid, encryptedData)
	}

	/**
	 * @return Whether the service is available.
	 */
	fun hasService(serviceUuid: UUID): Boolean {
		return bleCore.hasService(serviceUuid)
	}

	/**
	 * @return Whether the characteristic is available.
	 */
	fun hasCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
		return bleCore.hasCharacteristic(serviceUuid, characteristicUuid)
	}

	private fun getSessionData(address: DeviceAddress): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		// TODO: different in setup mode
		bleCore.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_NONCE_UUID)
				.success {
					encryptionManager.parseSessionData(address, it, true)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}


	private fun checkSetupMode() {
		if (bleCore.hasService(BluenetProtocol.SETUP_SERVICE_UUID)) {
			isSetupMode = true
		}
	}
}