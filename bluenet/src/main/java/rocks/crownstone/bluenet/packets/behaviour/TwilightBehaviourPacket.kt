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

class TwilightBehaviourPacket(
		switchVal: Uint8,
		profileId: Uint8,
		daysOfWeek: DaysOfWeekPacket,
		from: TimeOfDayPacket,
		until: TimeOfDayPacket
): BehaviourPacket(BehaviourType.TWILIGHT, switchVal, profileId, daysOfWeek, from, until) {

	constructor() : this(0U, 0U, DaysOfWeekPacket(), TimeOfDayPacket(), TimeOfDayPacket())
	companion object {
		const val SIZE = BehaviourPacket.SIZE
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		var success = true
		success = success && super.toBuffer(bb)
		success = success && (type == BehaviourType.TWILIGHT)
		return success
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		var success = true
		success = success && super.fromBuffer(bb)
		success = success && (type == BehaviourType.TWILIGHT)
		return success
	}
}
