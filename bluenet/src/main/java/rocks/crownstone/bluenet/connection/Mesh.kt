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
import rocks.crownstone.bluenet.packets.meshCommand.MeshCommandFlags
import rocks.crownstone.bluenet.packets.meshCommand.MeshControlPacketV3
import rocks.crownstone.bluenet.packets.meshCommand.MeshControlPacketV5
import rocks.crownstone.bluenet.packets.other.IbeaconConfigIdPacket
import rocks.crownstone.bluenet.packets.wrappers.v3.ControlPacketV3
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.packets.wrappers.v5.ControlPacketV5
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log

class Mesh(eventBus: EventBus, connection: ExtConnection) {
	private val TAG = this.javaClass.simpleName
	private val eventBus = eventBus
	private val connection = connection

	/**
	 * Send a command that does nothing.
	 *
	 * @return Promise
	 */
	@Synchronized
	fun noop(): Promise<Unit, Exception> {
		Log.i(TAG, "noop")
		return writeMeshCommand(
				ControlType.NOOP,
				ControlTypeV4.NOOP,
				MeshCommandFlags(broadcast = true, acked = false, useKnownIds = false)
		)
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
		return writeMeshCommand(
				ControlType.SET_TIME,
				ControlTypeV4.SET_TIME,
				timestamp,
				MeshCommandFlags(broadcast = true, acked = false, useKnownIds = false)
		)
	}

	@Synchronized
	fun setState(packet: StatePacketV5, id: Uint8): Promise<Unit, Exception> {
		Log.i(TAG, "setState packet=$packet")
		return writeMeshCommand(
				ControlType.UNKNOWN,
				ControlTypeV4.SET_STATE,
				packet,
				MeshCommandFlags(broadcast = false, acked = true, useKnownIds = false),
				ids = listOf(id))
	}

	@Synchronized
	fun setIbeaconConfigId(packet: IbeaconConfigIdPacket, ids: List<Uint8>): Promise<Unit, Exception> {
		return writeMeshCommand(
				ControlType.UNKNOWN,
				ControlTypeV4.SET_IBEACON_CONFIG_ID,
				packet,
				MeshCommandFlags(broadcast = true, acked = true, useKnownIds = false),
				ids = ids)
	}



	private fun writeMeshCommand(
			type: ControlType,
			type4: ControlTypeV4,
			flags: MeshCommandFlags,
			transmissions: Uint8 = 0U,
			timeout: Uint8 = 0U,
			ids: List<Uint8> = emptyList()
	): Promise<Unit, Exception> {
		Log.i(TAG, "writeMeshCommand type=$type type4=$type4")
		val controlClass = Control(eventBus, connection)
		val meshControlPacket = when(getPacketProtocol()) {
			PacketProtocol.V1,
			PacketProtocol.V2,
			PacketProtocol.V3 -> MeshControlPacketV3(ControlPacketV3(type), ids)
			PacketProtocol.V4 -> MeshControlPacketV3(ControlPacketV4(type4), ids)
			PacketProtocol.V5 -> MeshControlPacketV5(ControlPacketV5(ConnectionProtocol.V5, type4), flags, transmissions, timeout, ids)
		}
//		return controlClass.writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, meshControlPacket)
		return controlClass.meshCommand(meshControlPacket)
	}

	private inline fun <reified T> writeMeshCommand(
			type: ControlType,
			type4: ControlTypeV4,
			value: T,
			flags: MeshCommandFlags,
			transmissions: Uint8 = 0U,
			timeout: Uint8 = 0U,
			ids: List<Uint8> = emptyList()
	): Promise<Unit, Exception> {
		return writeMeshCommand(type, type4, ByteArrayPacket(Conversion.toByteArray(value)), flags, transmissions, timeout, ids)
	}

	private fun writeMeshCommand(
			type: ControlType,
			type4: ControlTypeV4,
			payload: PacketInterface,
			flags: MeshCommandFlags,
			transmissions: Uint8 = 0U,
			timeout: Uint8 = 0U,
			ids: List<Uint8> = emptyList()
	): Promise<Unit, Exception> {
		Log.i(TAG, "writeMeshCommand type=$type type4=$type4")
		val controlClass = Control(eventBus, connection)
		val meshControlPacket = when(getPacketProtocol()) {
			PacketProtocol.V1,
			PacketProtocol.V2,
			PacketProtocol.V3 -> MeshControlPacketV3(ControlPacketV3(type, payload), ids)
			PacketProtocol.V4 -> MeshControlPacketV3(ControlPacketV4(type4, payload), ids)
			PacketProtocol.V5 -> MeshControlPacketV5(ControlPacketV5(ConnectionProtocol.V5, type4, payload), flags, transmissions, timeout, ids)
		}
//		return controlClass.writeCommand(ControlType.MESH_COMMAND, ControlTypeV4.MESH_COMMAND, meshControlPacket)
		return controlClass.meshCommand(meshControlPacket)
	}

	private fun getPacketProtocol(): PacketProtocol {
		return connection.getPacketProtocol()
	}
}