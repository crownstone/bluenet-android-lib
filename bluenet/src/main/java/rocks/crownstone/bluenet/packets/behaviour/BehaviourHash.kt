/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.getUint32
import rocks.crownstone.bluenet.util.putUInt32
import java.nio.ByteBuffer

class BehaviourHashPacket(hash: Uint32): PacketInterface {
	var hash = hash
		private set

	constructor(): this(0U)

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
		bb.putUInt32(hash)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		hash = bb.getUint32()
		return true
	}
}