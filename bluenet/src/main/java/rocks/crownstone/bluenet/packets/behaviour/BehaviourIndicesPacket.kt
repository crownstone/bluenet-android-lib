/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.behaviour

import rocks.crownstone.bluenet.packets.PacketInterface
import java.nio.ByteBuffer

class BehaviourIndicesPacket(indicesWithHash: ArrayList<BehaviourIndexAndHashPacket>): PacketInterface {
	val indicesWithHash = indicesWithHash

	constructor(): this(ArrayList())

	companion object {
		const val HEADER_SIZE = 0
		const val ITEM_SIZE = BehaviourIndexAndHashPacket.SIZE
	}

	override fun getPacketSize(): Int {
		return indicesWithHash.size * ITEM_SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		for (index in indicesWithHash) {
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
		indicesWithHash.clear()
		while (bb.remaining() > 0) {
			val indexedHash = BehaviourIndexAndHashPacket()
			if (!indexedHash.fromBuffer(bb)) {
				return false
			}
			indicesWithHash.add(indexedHash)
		}
		return true
	}
}