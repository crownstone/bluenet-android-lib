/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 25, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.toInt
import java.nio.ByteBuffer

enum class DayOfWeekBitPos(val num: Int) {
	SUN(0),
	MON(1),
	TUE(2),
	WED(3),
	THU(4),
	FRI(5),
	SAT(6),
}

class DaysOfWeekPacket(
		sun: Boolean,
		mon: Boolean,
		tue: Boolean,
		wed: Boolean,
		thu: Boolean,
		fri: Boolean,
		sat: Boolean
): PacketInterface {
	var sun = sun
		private set
	var mon = mon
		private set
	var tue = tue
		private set
	var wed = wed
		private set
	var thu = thu
		private set
	var fri = fri
		private set
	var sat = sat
		private set

	constructor(): this(false, false, false, false, false, false, false)

	companion object {
		const val SIZE = 1
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		val bitmask: Int =
				(sun.toInt() shl DayOfWeekBitPos.SUN.num) +
				(mon.toInt() shl DayOfWeekBitPos.MON.num) +
				(tue.toInt() shl DayOfWeekBitPos.TUE.num) +
				(wed.toInt() shl DayOfWeekBitPos.WED.num) +
				(thu.toInt() shl DayOfWeekBitPos.THU.num) +
				(fri.toInt() shl DayOfWeekBitPos.FRI.num) +
				(sat.toInt() shl DayOfWeekBitPos.SAT.num)
		bb.put(Conversion.toUint8(bitmask))
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		val bitmask = bb.get().toInt()
		sun = (bitmask and (1 shl DayOfWeekBitPos.SUN.num)) == 1
		mon = (bitmask and (1 shl DayOfWeekBitPos.MON.num)) == 1
		tue = (bitmask and (1 shl DayOfWeekBitPos.TUE.num)) == 1
		wed = (bitmask and (1 shl DayOfWeekBitPos.WED.num)) == 1
		thu = (bitmask and (1 shl DayOfWeekBitPos.THU.num)) == 1
		fri = (bitmask and (1 shl DayOfWeekBitPos.FRI.num)) == 1
		sat = (bitmask and (1 shl DayOfWeekBitPos.SAT.num)) == 1
		return true
	}

	override fun toString(): String {
		return "DaysOfWeekPacket(sun=$sun, mon=$mon, tue=$tue, wed=$wed, thu=$thu, fri=$fri, sat=$sat)"
	}
}
