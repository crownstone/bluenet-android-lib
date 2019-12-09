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
import rocks.crownstone.bluenet.util.toUint16
import rocks.crownstone.bluenet.util.toUint8
import java.nio.ByteBuffer

class BehaviourIndexAndHashPacket(index: BehaviourIndex, hash: BehaviourHashPacket): PacketInterface {
	var index = index
		private set
	var hash = hash
		private set

	constructor(): this(INDEX_UNKNOWN, BehaviourHashPacket())

	companion object {
		const val SIZE = 1 + BehaviourHashPacket.SIZE // TODO: replace 1 by BehaviourIndex.SIZE
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		if (index == INDEX_UNKNOWN) {
			return false
		}
		bb.putUint8(index)
		return hash.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		index = bb.getUint8()
		return hash.fromBuffer(bb)
	}

	override fun toString(): String {
		return "BehaviourIndexAndHashPacket(index=$index, hash=$hash)"
	}
}