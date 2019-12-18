/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Dec 9, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer

class BroadcastSunTimePacket(val sunRiseAfterMidnight: Uint32, val sunSetAfterMidnight: Uint32): PacketInterface {
	companion object {
		// Sunrise and Sunset are uint 24.
		const val SIZE = 3 + 3
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		val sunRiseBytes = Conversion.uint24ToByteArray(sunRiseAfterMidnight)
		bb.put(sunRiseBytes)
		val sunSetBytes = Conversion.uint24ToByteArray(sunSetAfterMidnight)
		bb.put(sunSetBytes)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}

	override fun toString(): String {
		return "BroadcastSunTimePacket(sunRiseAfterMidnight=$sunRiseAfterMidnight, sunSetAfterMidnight=$sunSetAfterMidnight)"
	}
}