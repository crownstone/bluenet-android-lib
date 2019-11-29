/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer

class BehaviourIndexedAndHashPacket(index: BehaviourIndex, hash: BehaviourHashPacket): PacketInterface {
	var index = index
		private set
	var hash = hash
		private set

	constructor(): this(255, BehaviourHashPacket())

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + hash.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		if (index == 255.toShort()) {
			return false
		}
		bb.putUint8(index)
		return hash.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < HEADER_SIZE) {
			return false
		}
		index = bb.getUint8()
		return hash.fromBuffer(bb)
	}
}