package rocks.crownstone.bluenet.services.packets.schedule

import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.services.packets.PacketInterface
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class ScheduleEntryPacket(): PacketInterface {
	var repeatType: ScheduleRepeatType = ScheduleRepeatType.UNKNOWN
	var actionType: ScheduleActionType = ScheduleActionType.UNKNOWN
	var overrideMask: Uint8 = 0
	var timestamp: Uint32 = 0

	// Repeat
	var minutes: Uint16 = 0
	var dayOfweekMask: Int = 0

	// Action
	var switchVal: Uint8 = 0
	var fadeDuration: Uint16 = 0

	constructor(repeatType: ScheduleRepeatType, actionType: ScheduleActionType, timestamp: Uint32, overrideMask: Uint8 = 0): this() {
		this.repeatType = repeatType
		this.actionType = actionType
		this.timestamp = timestamp
		this.overrideMask = overrideMask
	}

	companion object {
		const val SIZE = 1+1+1+4+2+3
		const val WEEKDAY_MASK_ALL_DAYS = 0x7F // 01111111
	}

	fun isActive(): Boolean {
		return timestamp != 0L
	}

	override fun getSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getSize()) {
			return false
		}
		bb.put(0) // Reserved
		val type = Conversion.toUint8(repeatType.num + (actionType.num shl 4))
		bb.put(type)
		bb.put(overrideMask)
		bb.putInt(timestamp)
		when (repeatType) {
			ScheduleRepeatType.MINUTES -> {
				if (minutes == 0) {
					return false
				}
				bb.putShort(minutes)
			}
			ScheduleRepeatType.DAY -> {
				if (dayOfweekMask == 0) {
					return false
				}
				if ((dayOfweekMask and WEEKDAY_MASK_ALL_DAYS) == WEEKDAY_MASK_ALL_DAYS) {
					dayOfweekMask = 1 shl ScheduleWeekDayBitPos.ALL_DAYS.num
				}
				bb.put(dayOfweekMask.toByte())
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
				if (fadeDuration == 0) {
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
		if (bb.remaining() < getSize()) {
			return false
		}
		bb.get() // Reserved
		val type = bb.get().toInt()
		repeatType = ScheduleRepeatType.fromNum(type and 0x0F)
		actionType = ScheduleActionType.fromNum((type and 0xF0) shr 4)
		overrideMask = bb.getUint8()
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
				dayOfweekMask = bb.getUint8().toInt()
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