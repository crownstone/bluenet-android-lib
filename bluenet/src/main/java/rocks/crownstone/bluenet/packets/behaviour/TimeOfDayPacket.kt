/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 25, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Int32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.put
import java.nio.ByteBuffer

enum class BaseTimeType(val num: Uint8) {
	MIDNIGHT(0),
	SUNDOWN(1),
	SUNRISE(2),
	UNKNOWN(255);
	companion object {
		private val map = values().associateBy(BaseTimeType::num)
		fun fromNum(action: Uint8): BaseTimeType {
			return map[action] ?: return UNKNOWN
		}
	}
}

typealias TimeDifference = Int32

class TimeOfDayPacket(baseTimeType: BaseTimeType, timeOffset: TimeDifference): PacketInterface {
	var baseTimeType = baseTimeType
	var timeOffset = timeOffset

	constructor(): this(BaseTimeType.UNKNOWN, 0)

	companion object {
		const val SIZE = 1+4
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(baseTimeType.num)
		bb.putInt(timeOffset)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		baseTimeType = BaseTimeType.fromNum(bb.getUint8())
		if (baseTimeType == BaseTimeType.UNKNOWN) {
			return false
		}
		timeOffset = bb.getInt()
		return true
	}
}
