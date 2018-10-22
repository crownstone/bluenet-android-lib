package rocks.crownstone.bluenet.services

import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.AccessLevel
import rocks.crownstone.bluenet.services.packets.ControlPacket
import rocks.crownstone.bluenet.services.packets.SetupPacket
import rocks.crownstone.bluenet.util.Conversion

class Control(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	fun setSwitch(value: Uint8): Promise<Unit, Exception> {
		return writeCommand(ControlType.SWITCH, value)
	}

	fun setRelay(value: Boolean): Promise<Unit, Exception> {
		return writeCommand(ControlType.RELAY, value)
	}

	fun setPwm(value: Uint8): Promise<Unit, Exception> {
		return writeCommand(ControlType.PWM, value)
	}

	internal fun validateSetup(): Promise<Unit, Exception> {
		return writeCommand(ControlType.VALIDATE_SETUP)
	}

	internal fun setup(packet: SetupPacket): Promise<Unit, Exception> {
		return writeCommand(ControlType.SETUP, packet)
	}


	// Commands without payload
	private fun writeCommand(type: ControlType): Promise<Unit, Exception> {
		return writeCommand(ControlPacket(type))
	}

	// Commands with simple value
	private inline fun <reified T> writeCommand(type: ControlType, value: T): Promise<Unit, Exception> {
		val packet = ControlPacket(type, Conversion.toByteArray(value))
		return writeCommand(packet)
	}

	private fun writeCommand(packet: ControlPacket): Promise<Unit, Exception> {
		if (connection.isSetupMode) {
			return connection.write(BluenetProtocol.SETUP_SERVICE_UUID, BluenetProtocol.CHAR_SETUP_CONTROL_UUID, packet.getArray(), AccessLevel.SETUP)
		}
		return connection.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID, packet.getArray(), AccessLevel.HIGHEST_AVAILABLE)
	}
}