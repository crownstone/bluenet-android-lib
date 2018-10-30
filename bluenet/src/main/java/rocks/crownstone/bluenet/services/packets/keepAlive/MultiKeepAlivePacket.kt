package rocks.crownstone.bluenet.services.packets.keepAlive

import rocks.crownstone.bluenet.MultiKeepAliveType
import rocks.crownstone.bluenet.services.packets.PacketInterface
import java.nio.ByteBuffer

class MultiKeepAlivePacket(val payload: PacketInterface): PacketInterface {
	var type: MultiKeepAliveType
	init {
		type = when (payload::class) {
			KeepAliveSameTimeout::class -> MultiKeepAliveType.SAME_TIMEOUT
			else -> MultiKeepAliveType.UNKNOWN
		}
	}

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getSize(): Int {
		return HEADER_SIZE + payload.getSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (type == MultiKeepAliveType.UNKNOWN || bb.remaining() < getSize()) {
			return false
		}
		bb.put(type.num.toByte())
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}