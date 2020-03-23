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
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
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

	@Synchronized
	fun setState(packet: StatePacketV5, id: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setState packet=$packet")
		return writeMeshCommand(ControlType.UNKNOWN, ControlTypeV4.SET_STATE, packet, listOf(id))
	}

	private fun writeMeshCommand(type: ControlType, type4: ControlTypeV4, ids: List<Uint8> = emptyList()): Promise<Unit, Exception> {
		Log.i(TAG, "writeMeshCommand type=$type type4=$type4")
		val controlClass = Control(eventBus, connection)
		val meshControlPacket = when(getPacketProtocol()) {
			PacketProtocol.V3 -> MeshControlPacket(ControlPacketV3(type), ids)
			else -> MeshControlPacket(ControlPacketV4(type4), ids)
		}
//		return controlClass.writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, meshControlPacket)
		return controlClass.meshCommand(meshControlPacket)
	}

	private inline fun <reified T> writeMeshCommand(type: ControlType, type4: ControlTypeV4, value: T, ids: List<Uint8> = emptyList()): Promise<Unit, Exception> {
		return writeMeshCommand(type, type4, ByteArrayPacket(Conversion.toByteArray(value)), ids)
	}

	private fun writeMeshCommand(type: ControlType, type4: ControlTypeV4, payload: PacketInterface, ids: List<Uint8> = emptyList()): Promise<Unit, Exception> {
		Log.i(TAG, "writeMeshCommand type=$type type4=$type4")
		val controlClass = Control(eventBus, connection)
		val meshControlPacket = when(getPacketProtocol()) {
			PacketProtocol.V3 -> MeshControlPacket(ControlPacketV3(type, payload), ids)
			else -> MeshControlPacket(ControlPacketV4(type4, payload), ids)
		}
//		return controlClass.writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, meshControlPacket)
		return controlClass.meshCommand(meshControlPacket)
	}

	private fun getPacketProtocol(): PacketProtocol {
		return connection.getPacketProtocol()
	}
}