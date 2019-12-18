/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Dec 18, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer


/**
 * Class that makes getting a behaviour easier.
 *
 * Depending on type, it will create a specific behaviour packet.
 */
class BehaviourGetPacket: BehaviourPacket() {
	var packet: BehaviourPacket? = null
		private set
	override fun getPacketSize(): Int {
		return 1 // Behaviour type
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		// Peak the type
		val oldPos = bb.position()
		val type = BehaviourType.fromNum(bb.getUint8())
		bb.position(oldPos)
		when (type) {
			BehaviourType.UNKNOWN -> return false
			BehaviourType.SWITCH -> {
				packet = SwitchBehaviourPacket()
				return packet?.fromBuffer(bb) ?: false
			}
			BehaviourType.TWILIGHT -> {
				packet = TwilightBehaviourPacket()
				return packet?.fromBuffer(bb) ?: false
			}
			BehaviourType.SMART_TIMER -> {
				packet = SmartTimerBehaviourPacket()
				return packet?.fromBuffer(bb) ?: false
			}
		}
	}
}