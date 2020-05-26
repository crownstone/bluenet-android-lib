/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 25, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.powerSamples

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class PowerSamplesRequestPacket(type: PowerSamplesType, index: Uint8 = 0U): PacketInterface {
	val type = type
	val index = index

	companion object {
		const val SIZE = Uint8.SIZE_BYTES +	Uint8.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		bb.putUint8(type.num)
		bb.putUint8(index)
		return true
	}

	override fun toString(): String {
		return "PowerSamplesRequestPacket(type=$type, index=$index)"
	}
}