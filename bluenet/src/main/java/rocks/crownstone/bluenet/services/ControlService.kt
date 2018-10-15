package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.services.packets.ControlPacket

class ControlService(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun setSwitch(value: Uint8): Promise<Unit, Exception> {
		return writePacket(ControlPacket(ControlType.SWITCH, value))
	}

	fun setRelay(value: Boolean): Promise<Unit, Exception> {
		return writePacket(ControlPacket(ControlType.RELAY, if (value) 0 else 1))
	}

	fun setPwm(value: Uint8): Promise<Unit, Exception> {
		return writePacket(ControlPacket(ControlType.PWM, value))
	}


	private fun writePacket(packet: ControlPacket): Promise<Unit, Exception> {
		if (connection.isSetupMode) {
			return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID, packet.getArray(), AccessLevel.SETUP)
		}
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID, packet.getArray(), AccessLevel.HIGHEST_AVAILABLE)
	}
}