/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import java.nio.ByteBuffer

class SmartTimerBehaviourPacket(
		var switchBehaviourPacket: SwitchBehaviourPacket,
		var presence: PresencePacket,
		var timeOffset: TimeDifference
) : PacketInterface {
	constructor(): this(SwitchBehaviourPacket(), PresencePacket(), 0)

	companion object {
		const val SIZE = SwitchBehaviourPacket.SIZE + PresencePacket.SIZE + TimeDifference.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		var success = true
		success = success && switchBehaviourPacket.toBuffer(bb)
		success = success && presence.toBuffer(bb)
		bb.putInt(timeOffset)
		return success
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		var success = true
		success = success && switchBehaviourPacket.fromBuffer(bb)
		success = success && presence.fromBuffer(bb)
		timeOffset = bb.getInt()
		return success
	}
}
