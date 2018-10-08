package rocks.crownstone.bluenet.services.packets

import rocks.crownstone.bluenet.BluenetProtocol
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer


open class Int8Packet(type: Byte, value: Byte): BasePacket(type, SIZE) {
	companion object {
		const val SIZE = 1
	}
	private var value: Byte = value

	override fun toBuffer(bb: ByteBuffer): Boolean {
		bb.put(value)
		return true
	}
}

class Uint8Packet(type: Byte, value: Short): Int8Packet(type, value.toByte())


// Etc


