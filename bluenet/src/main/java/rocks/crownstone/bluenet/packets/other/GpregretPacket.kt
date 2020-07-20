/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jul 20, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint32
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer

class GpregretPacket: PacketInterface {
	var index:          Uint8 = 0U; private set
	var value:          Uint32 = 0U; private set

	companion object {
		const val SIZE = Uint8.SIZE_BYTES + Uint32.SIZE_BYTES
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
		index = bb.getUint8()
		value = bb.getUint32()
		return true
	}

	override fun toString(): String {
		return "GpregretPacket(index=$index, value=$value)"
	}
}