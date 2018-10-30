package rocks.crownstone.bluenet.services.packets.keepAlive

import rocks.crownstone.bluenet.Uint16
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.services.packets.PacketInterface
import java.nio.ByteBuffer

class KeepAliveSameTimeout(val timeout: Uint16): PacketInterface {
	val list = ArrayList<KeepAliveSameTimeoutItem>()

	companion object {
		const val HEADER_SIZE = 4
	}

	fun add(item: KeepAliveSameTimeoutItem) {
		list.add(item)
	}

	override fun getSize(): Int {
		return HEADER_SIZE + list.size * KeepAliveSameTimeoutItem.SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (list.isEmpty() || bb.remaining() < getSize()) {
			return false
		}
		bb.putShort(timeout.toShort())
		bb.put(list.size.toByte())
		bb.put(0) // reserved
		for (it in list) {
			if (!it.toBuffer(bb)) {
				return false
			}
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}


class KeepAliveSameTimeoutItem(val id: Uint8, val actionSwitchValue: Uint8): PacketInterface {
	companion object {
		const val SIZE = 2
	}

	override fun getSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getSize()) {
			return false
		}
		bb.put(id.toByte())
		bb.put(actionSwitchValue.toByte())
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}