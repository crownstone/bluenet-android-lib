/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import java.nio.ByteBuffer

class BehaviourIndicesPacket(indices: ArrayList<BehaviourIndexedAndHashPacket>): PacketInterface {
	val indices = indices

	constructor(): this(ArrayList())

	/**
	 * Get a copy of the list of indices.
	 */
	fun getIndices(): List<BehaviourIndexedAndHashPacket> {
		val list = ArrayList<BehaviourIndexedAndHashPacket>()
		list.addAll(indices)
		return list
//		return ArrayList<BehaviourIndex>(indices)
	}

	companion object {
		const val HEADER_SIZE = 0
		const val ITEM_SIZE = 1 // TODO: replace by BehaviourIndex.SIZE
	}

	override fun getPacketSize(): Int {
		return indices.size * ITEM_SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		for (index in indices) {
			if (!index.toBuffer(bb)) {
				return false
			}
		}
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < HEADER_SIZE) {
			return false
		}
		indices.clear()
		while (bb.remaining() > 0) {
			val indexedHash = BehaviourIndexedAndHashPacket()
			if (!indexedHash.fromBuffer(bb)) {
				return false
			}
			indices.add(indexedHash)
		}
		return true
	}
}