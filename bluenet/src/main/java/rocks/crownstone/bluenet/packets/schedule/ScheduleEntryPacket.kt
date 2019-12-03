/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.schedule

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class ScheduleEntryPacket(): PacketInterface {
	var repeatType: ScheduleRepeatType = ScheduleRepeatType.UNKNOWN
	var actionType: ScheduleActionType = ScheduleActionType.UNKNOWN
//	var overrideMask: Uint8 = 0
	var overrideMask = ScheduleOverride()
	var timestamp: Uint32 = 0U

	// Repeat
	var minutes: Uint16 = 0U
//	var dayOfWeekMask: Int = 0
	var dayOfWeekMask = ScheduleDayOfWeek()

	// Action
	var switchVal: Uint8 = 0U
	var fadeDuration: Uint16 = 0U

	constructor(repeatType: ScheduleRepeatType, actionType: ScheduleActionType, timestamp: Uint32, overrideMask: ScheduleOverride = ScheduleOverride()): this() {
		this.repeatType = repeatType
		this.actionType = actionType
		this.timestamp = timestamp
		this.overrideMask = overrideMask
	}

	companion object {
		const val SIZE = 1+1+1+4+2+3
	}

	fun isActive(): Boolean {
		return timestamp != 0U
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.put(0) // Reserved
		val type = Conversion.toUint8(repeatType.num + (actionType.num shl 4))
		bb.put(type)
		bb.put(overrideMask.calcBitMask())
		bb.putInt(timestamp)
		when (repeatType) {
			ScheduleRepeatType.MINUTES -> {
				if (minutes == 0.toUint16()) {
					return false
				}
				bb.putShort(minutes)
			}
			ScheduleRepeatType.DAY -> {
				val dayOfWeekBitmask = dayOfWeekMask.calcBitMask()
				if (dayOfWeekBitmask == 0.toUint8()) {
					return false
				}
				bb.put(dayOfWeekMask.bitmask)
				bb.put(0) // Reserved
			}
			ScheduleRepeatType.ONCE -> {
				bb.putShort(0) // Reserved
			}
			else -> return false
		}
		when (actionType) {
			ScheduleActionType.SWITCH -> {
				bb.put(switchVal)
				bb.putShort(0) // Reserved
			}
			ScheduleActionType.FADE -> {
				if (fadeDuration == 0.toUint16()) {
					return false
				}
				bb.put(switchVal)
				bb.putShort(fadeDuration)
			}
			ScheduleActionType.TOGGLE -> {
				bb.put(0) // Reserved
				bb.putShort(0) // Reserved
			}
			else -> return false
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.get() // Reserved
		val type = bb.get().toInt()
		repeatType = ScheduleRepeatType.fromNum(type and 0x0F)
		actionType = ScheduleActionType.fromNum((type and 0xF0) shr 4)
		overrideMask = ScheduleOverride(bb.getUint8())
		timestamp = bb.getUint32()
		if (!isActive()) {
			// skip remaining 5 bytes
			bb.position(bb.position() + 5)
			return true
		}
		when (repeatType) {
			ScheduleRepeatType.MINUTES -> {
				minutes = bb.getUint16()
			}
			ScheduleRepeatType.DAY -> {
				dayOfWeekMask = ScheduleDayOfWeek(bb.getUint8())
				bb.get() // Reserved
			}
			ScheduleRepeatType.ONCE -> {
				bb.getShort() // Reserved
			}
			else -> return false
		}
		when (actionType) {
			ScheduleActionType.SWITCH -> {
				switchVal = bb.getUint8()
				bb.getShort() // Reserved
			}
			ScheduleActionType.FADE -> {
				switchVal = bb.getUint8()
				fadeDuration = bb.getUint16()
			}
			ScheduleActionType.TOGGLE -> {
				bb.get() // Reserved
				bb.getShort() // Reserved
			}
			else -> return false
		}
		return true
	}
}