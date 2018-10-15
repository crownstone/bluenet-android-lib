package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.KeySet
import java.lang.Exception

class SetupService(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun setup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		if (!connection.isSetupMode) {
			return Promise.ofFail(Errors.NotInSetupMode())
		}
		if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
			return fastSetup(id, keySet, meshAccessAddress, ibeaconData)
		}
		else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
			return oldSetup(id, keySet, meshAccessAddress, ibeaconData)
		}
		else {
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
	}

	private fun oldSetup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		// TODO
		return Promise.ofFail(Exception("TODO"))
	}

	private fun fastSetup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		// TODO
		return Promise.ofFail(Exception("TODO"))
	}
}