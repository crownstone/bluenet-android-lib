/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Dec 23, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.BehaviourSettings
import rocks.crownstone.bluenet.util.putUint32
import java.nio.ByteBuffer

class BroadcastBehaviourSettingsPacket(val mode: BehaviourSettings): PacketInterface {
	companion object {
		const val SIZE = 4
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.putUint32(mode.num)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return "BroadcastBehaviourSettingsPacket(mode=$mode)"
	}
}