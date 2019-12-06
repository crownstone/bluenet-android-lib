/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer

enum class BehaviourType(val num: Uint8) {
	SWITCH(0U),
	TWILIGHT(1U),
	SMART_TIMER(2U),
	UNKNOWN(255U);
	companion object {
		private val map = values().associateBy(BehaviourType::num)
		fun fromNum(action: Uint8): BehaviourType {
			return map[action] ?: return UNKNOWN
		}
	}
}

typealias BehaviourIndex = Uint8
val INDEX_UNKNOWN: BehaviourIndex = 255U

open class BehaviourPacket(type: BehaviourType,
					  switchVal: Uint8,
					  profileId: Uint8,
					  daysOfWeek: DaysOfWeekPacket,
					  from: TimeOfDayPacket,
					  until: TimeOfDayPacket
): PacketInterface {
	var type = type
		private set
	var switchVal = switchVal
		private set
	var profileId = profileId
		private set
	var daysOfWeek = daysOfWeek
		private set
	var from = from
		private set
	var until = until
		private set

	constructor(): this(BehaviourType.UNKNOWN, 0U, 0U, DaysOfWeekPacket(), TimeOfDayPacket(), TimeOfDayPacket())

	companion object {
		const val SIZE = 1 + 1 + 1 + DaysOfWeekPacket.SIZE + TimeOfDayPacket.SIZE + TimeOfDayPacket.SIZE
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		if (type == BehaviourType.UNKNOWN) {
			return false
		}
		bb.putUint8(type.num)
		bb.putUint8(switchVal)
		bb.putUint8(profileId)
		var success = true
		success = success && daysOfWeek.toBuffer(bb)
		success = success && from.toBuffer(bb)
		success = success && until.toBuffer(bb)
		return success
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		type = BehaviourType.fromNum(bb.getUint8())
		switchVal = bb.getUint8()
		profileId = bb.getUint8()
		var success = true
		success = success && daysOfWeek.fromBuffer(bb)
		success = success && from.fromBuffer(bb)
		success = success && until.fromBuffer(bb)
		return success
	}
}
