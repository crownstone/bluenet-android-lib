package rocks.crownstone.bluenet.services.packets.multiSwitch

import rocks.crownstone.bluenet.MultiSwitchIntent
import rocks.crownstone.bluenet.Uint16
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.services.packets.PacketInterface
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

class MultiSwitchListPacket: PacketInterface {
	val list = ArrayList<MultiSwitchListItemPacket>()

	companion object {
		const val HEADER_SIZE = 1
	}

	fun add(item: MultiSwitchListItemPacket) {
		list.add(item)
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + list.size * MultiSwitchListItemPacket.SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (list.isEmpty() || bb.remaining() < getPacketSize()) {
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

class MultiSwitchListItemPacket(var id: Uint8, var switchValue: Uint8, val timeout: Uint16, var intent: MultiSwitchIntent): PacketInterface {
	companion object {
		const val SIZE = 5
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(id)
		bb.put(switchValue)
		bb.putShort(timeout)
		bb.put(intent.num)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}
}