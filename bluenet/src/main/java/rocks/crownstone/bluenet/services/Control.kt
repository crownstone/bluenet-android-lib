package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.services.packets.ControlPacket

class Control(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun setSwitch(value: Uint8): Promise<Unit, Exception> {
		return writePacket(ControlPacket(ControlType.CMD_SWITCH, value))
	}


	private fun writePacket(packet: ControlPacket): Promise<Unit, Exception> {
		if (connection.isSetupMode) {
			return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID, packet.getArray(), AccessLevel.SETUP)
		}
		// TODO: use different char in setup mode
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID, packet.getArray(), AccessLevel.HIGHEST_AVAILABLE)
	}
}