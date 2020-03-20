/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 19, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.connection

import nl.komponents.kovenant.Promise
import rocks.crownstone.bluenet.packets.ByteArrayPacket
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.meshCommand.MeshCommandPacket
import rocks.crownstone.bluenet.packets.meshCommand.MeshControlPacket
import rocks.crownstone.bluenet.packets.wrappers.v3.ControlPacketV3
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

class Mesh(evtBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = evtBus
	private val connection = connection

	/**
	 * Send a command that does nothing.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun noop(): Promise<Unit, Exception> {
		Log.i(TAG, "noop")
		return writeMeshCommand(ControlType.NOOP, ControlTypeV4.NOOP)
	}

	/**
	 * Set the time.
	 *
	 * @param timestamp POSIX timestamp.
	 * @return Promise
	 */
	@Synchronized
	fun setTime(timestamp: Uint32): Promise<Unit, Exception> {
		Log.i(TAG, "setTime $timestamp")
		return writeMeshCommand(ControlType.SET_TIME, ControlTypeV4.SET_TIME, timestamp)
	}



	private fun writeMeshCommand(type: ControlType, type4: ControlTypeV4): Promise<Unit, Exception> {
		val controlClass = Control(eventBus, connection)
		val meshControlPacket = when(getPacketProtocol()) {
			PacketProtocol.V3 -> MeshControlPacket(ControlPacketV3(type))
			else -> MeshControlPacket(ControlPacketV4(type4))
		}
		return controlClass.writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, MeshCommandPacket(meshControlPacket))
	}

	private inline fun <reified T> writeMeshCommand(type: ControlType, type4: ControlTypeV4, value: T): Promise<Unit, Exception> {
		return writeMeshCommand(type, type4, ByteArrayPacket(Conversion.toByteArray(value)))
	}

	private fun writeMeshCommand(type: ControlType, type4: ControlTypeV4, payload: PacketInterface): Promise<Unit, Exception> {
		val controlClass = Control(eventBus, connection)
		val meshControlPacket = when(getPacketProtocol()) {
			PacketProtocol.V3 -> MeshControlPacket(ControlPacketV3(type, payload))
			else -> MeshControlPacket(ControlPacketV4(type4, payload))
		}
		return controlClass.writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, MeshCommandPacket(meshControlPacket))
	}

	private fun getPacketProtocol(): PacketProtocol {
		return connection.getPacketProtocol()
	}
}