package rocks.crownstone.bluenet.services.packets.keepAlive

import rocks.crownstone.bluenet.KeepAliveAction
import rocks.crownstone.bluenet.Uint16
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.services.packets.PacketInterface
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class KeepAlivePacket(var action: KeepAliveAction, var switchValue: Uint8, var timeout: Uint16): PacketInterface {
	companion object {
		val SIZE = 4
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		bb.put(action.num)
		bb.put(switchValue)
		bb.putShort(timeout)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		action = KeepAliveAction.fromNum(Conversion.toUint8(bb.get()))
		if (action == KeepAliveAction.UNKNOWN) {
			return false
		}
		switchValue = bb.getUint8()
		timeout = bb.getUint16()
		return true
	}
}