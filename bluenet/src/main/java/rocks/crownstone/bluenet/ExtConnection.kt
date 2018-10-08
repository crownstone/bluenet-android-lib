package rocks.crownstone.bluenet

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.core.CoreConnection
import rocks.crownstone.bluenet.encryption.EncryptionManager

class ExtConnection(evtBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	val TAG = this.javaClass.simpleName
	val eventBus = evtBus
	val bleCore = bleCore
	val encryptionManager = encryptionManager

	fun connect(address: DeviceAddress) {
		return bleCore.connect(address, 9999)
				.then {
					bleCore.discoverServices(false)
				}.unwrap()
				.then {
					getSessionNonce(address)
				}.unwrap()
	}




	private fun getSessionNonce(address: DeviceAddress): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		bleCore.read(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_SESSION_NONCE_UUID)
				.success {
					encryptionManager.parseSessionData(address, it, true)
				}
				.fail {
					deferred.reject(it)
				}
		return deferred.promise
	}
}