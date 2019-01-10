package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.structs.MeshCommandType
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.packets.ConfigPacket
import rocks.crownstone.bluenet.packets.ControlPacket
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer

open class MeshCommandPacket(val payload: PacketInterface): PacketInterface {
	val ids = ArrayList<Uint8>()
	var type: MeshCommandType
	var bitmask: Uint8 = 0
	init {
		type = when (payload::class) {
			ControlPacket::class -> MeshCommandType.CONTROL
			ConfigPacket::class -> MeshCommandType.CONFIG
			MeshBeaconConfigPacket.BeaconConfigPacket::class -> MeshCommandType.BEACON_CONFIG
			else -> MeshCommandType.UNKNOWN
		}
	}

	companion object {
		const val HEADER_SIZE = 3
		const val ID_SIZE = 1
	}

	fun addId(id: Uint8) {
		ids.add(id)
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + ids.size * ID_SIZE + payload.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (ids.isEmpty() || bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(type.num)
		bb.put(bitmask)
		bb.put(Conversion.toUint8(ids.size))
		for (id in ids) {
			bb.put(id)
		}
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}