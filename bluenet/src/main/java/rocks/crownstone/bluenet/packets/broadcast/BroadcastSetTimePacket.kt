/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.putUint32
import java.nio.ByteBuffer

class BroadcastSetTimePacket(val currentTime: Uint32, val sunRiseAfterMidnight: Uint32, val sunSetAfterMidnight: Uint32): PacketInterface {
	companion object {
		// Sunrise and Sunset are uint 24.
		const val SIZE = Uint32.SIZE_BYTES + 3 + 3
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putUint32(currentTime)
		val sunRiseBytes = Conversion.uint24ToByteArray(sunRiseAfterMidnight)
		bb.put(sunRiseBytes)
		val sunSetBytes = Conversion.uint24ToByteArray(sunSetAfterMidnight)
		bb.put(sunSetBytes)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}