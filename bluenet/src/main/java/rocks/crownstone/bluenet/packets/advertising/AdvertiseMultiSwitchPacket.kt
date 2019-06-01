/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 1, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.advertising

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class AdvertiseMultiSwitchPacket: PacketInterface {
	val list = ArrayList<AdvertiseMultiSwitchItemPacket>()

	companion object {
		const val HEADER_SIZE = 1
	}

	fun add(item: AdvertiseMultiSwitchItemPacket): Boolean {
		list.add(item)
		return true
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + list.size * AdvertiseMultiSwitchItemPacket.SIZE
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


class AdvertiseMultiSwitchItemPacket(var id: Uint8, var switchValue: Uint8): PacketInterface {
	companion object {
		const val SIZE = 2
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
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		// Not implemented yet (no need?)
		return false
	}
}
