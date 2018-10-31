package rocks.crownstone.bluenet.services.packets.schedule

import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.services.packets.PacketInterface
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class ScheduleCommandPacket(private val index: Uint8, private val entry: ScheduleEntryPacket): PacketInterface {
	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + entry.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(index)
		return entry.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}