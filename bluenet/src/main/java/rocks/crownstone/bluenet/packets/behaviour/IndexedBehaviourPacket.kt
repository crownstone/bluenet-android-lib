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

class IndexedBehaviourPacket(index: BehaviourIndex, behaviour: BehaviourPacket): PacketInterface, Comparable<IndexedBehaviourPacket> {
	var index = index
		private set
	var behaviour = behaviour
		private set

	constructor(): this(INDEX_UNKNOWN, BehaviourPacket())

	companion object {
		const val HEADER_SIZE = 1
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + behaviour.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		if (index == INDEX_UNKNOWN) {
			return false
		}
		bb.putUint8(index)
		return behaviour.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < HEADER_SIZE) {
			return false
		}
		index = bb.getUint8()
		return behaviour.fromBuffer(bb)
	}

	override fun compareTo(other: IndexedBehaviourPacket): Int {
		return (this.index - other.index).toInt()
	}

	override fun toString(): String {
		return "IndexedBehaviourPacket(index=$index, behaviour=$behaviour)"
	}
}