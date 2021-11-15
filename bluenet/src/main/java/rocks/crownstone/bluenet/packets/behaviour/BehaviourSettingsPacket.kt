/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Oct 29, 2021
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class BehaviourSettingsPacket: PacketInterface {
	var enabled: Boolean = true
		private set

	companion object {
		const val SIZE = Uint32.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		val asInt = bb.getUint32()
		enabled = Util.isBitSet(asInt, 0)
		return true
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		var asInt: Uint32 = 0U
		if (enabled) { asInt = Util.setBit(asInt, 0) }

		bb.putUint32(asInt)
		return true
	}

	override fun toString(): String {
		return "BehaviourSettingsPacket(enabled=$enabled)"
	}
}