package rocks.crownstone.bluenet.services.packets.multiSwitch

import rocks.crownstone.bluenet.MultiSwitchIntent
import rocks.crownstone.bluenet.Uint16
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.services.packets.PacketInterface
import java.nio.ByteBuffer

class MultiSwitchListItemPacket(var id: Uint8, var switchValue: Uint8, val timeout: Uint16, var intent: MultiSwitchIntent): PacketInterface {
	companion object {
		const val SIZE = 5
	}

	override fun getSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getSize()) {
			return false
		}
		bb.put(id.toByte())
		bb.put(switchValue.toByte())
		bb.putShort(timeout.toShort())
		bb.put(intent.num.toByte())
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}
}