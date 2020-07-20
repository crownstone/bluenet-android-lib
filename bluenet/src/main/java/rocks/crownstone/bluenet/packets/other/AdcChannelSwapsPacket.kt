/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jul 20, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.getUint32
import java.nio.ByteBuffer

class AdcChannelSwapsPacket: PacketInterface {
	var count:          Uint32 = 0U; private set
	var lastTimestamp:  Uint32 = 0U; private set

	companion object {
		const val SIZE = 2 * Uint32.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		count = bb.getUint32()
		lastTimestamp = bb.getUint32()
		return true
	}

	override fun toString(): String {
		return "AdcSwapsPacket(count=$count, lastTimestamp=$lastTimestamp)"
	}
}