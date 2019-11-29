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

open class SwitchBehaviourPacket(
		switchVal: Uint8,
		profileId: Uint8,
		daysOfWeek: DaysOfWeekPacket,
		from: TimeOfDayPacket,
		until: TimeOfDayPacket,
		presence: PresencePacket
): BehaviourPacket(BehaviourType.SWITCH, switchVal, profileId, daysOfWeek, from, until) {
	var presence = presence
		private set

	constructor(): this(0, 0, DaysOfWeekPacket(), TimeOfDayPacket(), TimeOfDayPacket(), PresencePacket())
//	constructor(other: SwitchBehaviourPacket): this(other.switchVal, other.profileId, other.daysOfWeek, other.from, other.until, other.presence)

	companion object {
		const val SIZE = BehaviourPacket.SIZE + PresencePacket.SIZE
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
		success = success && presence.toBuffer(bb)
		success = success && (type == BehaviourType.SWITCH)
		return success
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		var success = true
		success = success && super.fromBuffer(bb)
		success = success && presence.fromBuffer(bb)
		success = success && (type == BehaviourType.SWITCH)
		return success
	}
}
