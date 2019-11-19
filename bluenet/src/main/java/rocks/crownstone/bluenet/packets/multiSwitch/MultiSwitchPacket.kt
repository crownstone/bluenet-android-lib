/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.multiSwitch

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

class MultiSwitchPacket: PacketInterface {
	val list = ArrayList<MultiSwitchItemPacket>()

	companion object {
		const val HEADER_SIZE = 1
	}

	fun add(item: MultiSwitchItemPacket): Boolean {
		list.add(item)
		// TODO: return false when list is too large
		return true
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + list.size * MultiSwitchItemPacket.SIZE
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

class MultiSwitchItemPacket(var id: Uint8, var switchValue: Uint8): PacketInterface {
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