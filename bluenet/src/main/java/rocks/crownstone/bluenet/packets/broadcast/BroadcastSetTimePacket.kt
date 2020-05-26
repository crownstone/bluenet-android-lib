/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jun 2, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.bluenet.util.putUint32
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer

enum class BroadcastSetTimeFlagPos(val num: Int) {
	TIMESTAMP_VALID(0),
	SUNTIME_VALID(1),
	UNKNOWN(255);
	companion object {
		private val map = values().associateBy(BroadcastSetTimeFlagPos::num)
		fun fromNum(action: Int): BroadcastSetTimeFlagPos {
			return map[action] ?: return UNKNOWN
		}
	}
}

class BroadcastSetTimePacket(val currentTime: Uint32?, val sunRiseAfterMidnight: Uint32?, val sunSetAfterMidnight: Uint32?): PacketInterface {
	companion object {
		// Sunrise and Sunset are uint 24.
		const val SIZE = Uint8.SIZE_BYTES + Uint32.SIZE_BYTES + 3 + 3
	}
	val flags: Uint8
	init {
		var tempFlags: Uint8 = 0U
		if (currentTime != null) {
			tempFlags = Util.setBit(tempFlags, BroadcastSetTimeFlagPos.TIMESTAMP_VALID.num)
		}
		if (sunRiseAfterMidnight != null && sunSetAfterMidnight != null) {
			tempFlags = Util.setBit(tempFlags, BroadcastSetTimeFlagPos.SUNTIME_VALID.num)
		}
		flags = tempFlags
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putUint8(flags)
		bb.putUint32(currentTime ?: 0U)
		val sunRiseBytes = Conversion.uint24ToByteArray(sunRiseAfterMidnight ?: 0U)
		bb.put(sunRiseBytes)
		val sunSetBytes = Conversion.uint24ToByteArray(sunSetAfterMidnight ?: 0U)
		bb.put(sunSetBytes)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false
	}
}