/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.getUint32
import rocks.crownstone.bluenet.util.putUint32
import java.nio.ByteBuffer

class SunTimePacket(sunRiseAfterMidnight: Uint32, sunSetAfterMidnight: Uint32): PacketInterface {
	var sunRiseAfterMidnight = sunRiseAfterMidnight
		private set
	var sunSetAfterMidnight = sunSetAfterMidnight
		private set

	constructor(): this(0U, 0U)

	companion object {
		const val SIZE = Uint32.SIZE_BYTES + Uint32.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putUint32(sunRiseAfterMidnight)
		bb.putUint32(sunSetAfterMidnight)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		sunRiseAfterMidnight = bb.getUint32()
		sunSetAfterMidnight = bb.getUint32()
		return true
	}

	override fun toString(): String {
		return "SunTimePacket(sunRiseAfterMidnight=$sunRiseAfterMidnight, sunSetAfterMidnight=$sunSetAfterMidnight)"
	}
}