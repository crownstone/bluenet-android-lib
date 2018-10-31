package rocks.crownstone.bluenet.services.packets.multiSwitch

import rocks.crownstone.bluenet.MultiSwitchType
import rocks.crownstone.bluenet.services.packets.PacketInterface
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

	override fun getSize(): Int {
		val payloadSize = payload?.getSize() ?: 0
		return SIZE + payloadSize
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		val payload = this.payload
		if (payload == null || type == MultiSwitchType.UNKNOWN || bb.remaining() < getSize()) {
			return false
		}
		bb.put(type.num.toByte())
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}