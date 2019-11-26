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

class SwitchBehaviourPacket(
		var switchVal: Uint8,
		var profileId: Uint8,
		var daysOfWeek: DaysOfWeekPacket,
		var from: TimeOfDayPacket,
		var until: TimeOfDayPacket,
		var presence: PresencePacket
) : PacketInterface {
	constructor(): this(0, 0, DaysOfWeekPacket(), TimeOfDayPacket(), TimeOfDayPacket(), PresencePacket())

	companion object {
		const val SIZE = 1 + 1 + DaysOfWeekPacket.SIZE + TimeOfDayPacket.SIZE + TimeOfDayPacket.SIZE + PresencePacket.SIZE
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putUint8(switchVal)
		bb.putUint8(profileId)
		var success = true
		success = success && daysOfWeek.toBuffer(bb)
		success = success && from.toBuffer(bb)
		success = success && until.toBuffer(bb)
		success = success && presence.toBuffer(bb)
		return success
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		switchVal = bb.getUint8()
		profileId = bb.getUint8()
		var success = true
		success = success && daysOfWeek.fromBuffer(bb)
		success = success && from.fromBuffer(bb)
		success = success && until.fromBuffer(bb)
		success = success && presence.fromBuffer(bb)
		return success
	}
}
