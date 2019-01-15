/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.schedule

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class ScheduleListPacket: PacketInterface {
	// To be used as read only
	val list = ArrayList<ScheduleEntryPacket>()

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + list.size * ScheduleEntryPacket.SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(Conversion.toUint8(list.size))
		for (entry in list) {
			if (!entry.toBuffer(bb)) {
				return false
			}
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		list.clear()
		val size = bb.getUint8()
		for (i in 0 until size) {
			val entry = ScheduleEntryPacket()
			if (!entry.fromBuffer(bb)) {
				list.clear()
				return false
			}
			list.add(entry)
		}
		return true
	}
}