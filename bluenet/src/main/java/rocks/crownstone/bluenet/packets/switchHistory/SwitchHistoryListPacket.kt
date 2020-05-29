/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 29, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.switchHistory

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.commandSource.CommandSourcePacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class SwitchHistoryListPacket: PacketInterface {
	// To be used as read only
	val list = ArrayList<SwitchHistoryItemPacket>()

	companion object {
		val HEADER_SIZE = Uint8.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + list.size * SwitchHistoryItemPacket.SIZE
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		list.clear()
		if (bb.remaining() < HEADER_SIZE) {
			return false
		}
		val count = bb.getUint8().toInt()
		if (bb.remaining() < count * SwitchHistoryItemPacket.SIZE) {
			return false
		}
		for (i in 0 until count) {
			val item = SwitchHistoryItemPacket()
			if (!item.fromBuffer(bb)) {
				list.clear()
				return false
			}
			list.add(item)
		}
		return true
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return "SwitchHistoryListPacket(list=$list)"
	}
}