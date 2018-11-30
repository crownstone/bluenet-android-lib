package rocks.crownstone.bluenet.packets.multiSwitch

import rocks.crownstone.bluenet.structs.MultiSwitchType
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class MultiSwitchPacket(val payload: PacketInterface): PacketInterface {
	val type: MultiSwitchType
	companion object {
		val SIZE = 1
	}
	init {
		when (payload::class) {
			MultiSwitchListPacket::class -> type = MultiSwitchType.LIST
			else -> type = MultiSwitchType.UNKNOWN
		}
	}

	override fun getPacketSize(): Int {
		val payloadSize = payload?.getPacketSize() ?: 0
		return SIZE + payloadSize
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		val payload = this.payload
		if (payload == null || type == MultiSwitchType.UNKNOWN || bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(type.num)
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}