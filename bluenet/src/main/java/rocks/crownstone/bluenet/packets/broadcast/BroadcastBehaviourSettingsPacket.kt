/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Dec 23, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.broadcast

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.BehaviourSettingsMode
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer

class BroadcastBehaviourSettingsPacket(val mode: BehaviourSettingsMode): PacketInterface {
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
		bb.putUint8(mode.num)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}

	override fun toString(): String {
		return "BroadcastBehaviourSettingsPacket(mode=$mode)"
	}
}