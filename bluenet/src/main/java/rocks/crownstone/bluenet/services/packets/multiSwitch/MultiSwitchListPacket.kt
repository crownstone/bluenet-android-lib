package rocks.crownstone.bluenet.services.packets.multiSwitch

import rocks.crownstone.bluenet.services.packets.PacketInterface
import java.nio.ByteBuffer

class MultiSwitchListPacket(var list: List<MultiSwitchListItemPacket>): PacketInterface {

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getSize(): Int {
		return HEADER_SIZE + list.size * MultiSwitchListItemPacket.SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (list.isEmpty() || bb.remaining() < getSize()) {
			return false
		}
		bb.put(list.size.toByte())
		for (it in list) {
			if (!it.toBuffer(bb)) {
				return false
			}
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}
}