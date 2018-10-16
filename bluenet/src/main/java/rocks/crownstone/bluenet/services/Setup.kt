package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.KeySet
import java.lang.Exception

class Setup(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun setup(id: Uint8, keySet: KeySet, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		if (!connection.isSetupMode) {
			return Promise.ofFail(Errors.NotInSetupMode())
		}
		val adminKey =  keySet.adminKeyBytes
		val memberKey = keySet.memberKeyBytes
		val guestKey =  keySet.guestKeyBytes
		if (adminKey == null || memberKey == null || guestKey == null) {
			return Promise.ofFail(Errors.ValueWrong())
		}

		if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL2_UUID)) {
			return fastSetup(id, adminKey, memberKey, guestKey, meshAccessAddress, ibeaconData)
		}
		else if (connection.hasCharacteristic(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID)) {
			return oldSetup(id, adminKey, memberKey, guestKey, meshAccessAddress, ibeaconData)
		}
		else {
			return Promise.ofFail(Errors.CharacteristicNotFound())
		}
	}

	private fun oldSetup(id: Uint8, adminKey: ByteArray, memberKey: ByteArray, guestKey: ByteArray, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		val config = Config(eventBus, connection)
		val control = Control(eventBus, connection)
		return config.setCrownstoneId(id)
				.then {
					config.setAdminKey(adminKey)
				}.unwrap()
				.then {
					config.setMemberKey(memberKey)
				}.unwrap()
				.then {
					config.setGuestKey(guestKey)
				}.unwrap()
				.then {
					config.setMeshAccessAddress(meshAccessAddress)
				}.unwrap()
				.then {
					config.setIbeaconUuid(ibeaconData.uuid)
				}.unwrap()
				.then {
					config.setIbeaconMajor(ibeaconData.major)
				}.unwrap()
				.then {
					config.setIbeaconMinor(ibeaconData.minor)
				}.unwrap()
				.then {
					control.validateSetup()
				}.unwrap()
	}

	private fun fastSetup(id: Uint8, adminKey: ByteArray, memberKey: ByteArray, guestKey: ByteArray, meshAccessAddress: Uint32, ibeaconData: IbeaconData): Promise<Unit, Exception> {
		// TODO
		return Promise.ofFail(Exception("TODO"))
	}
}