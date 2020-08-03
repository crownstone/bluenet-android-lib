/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Aug 03, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.getUint32
import java.nio.ByteBuffer

class RamStatsPacket: PacketInterface {
	var minStackEnd:    Uint32 = 0U; private set
	var maxHeapEnd:     Uint32 = 0U; private set
	var minFree:        Uint32 = 0U; private set
	var numSbrkFails:   Uint32 = 0U; private set

	companion object {
		const val SIZE = 4 * Uint32.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		minStackEnd = bb.getUint32()
		maxHeapEnd = bb.getUint32()
		minFree = bb.getUint32()
		numSbrkFails = bb.getUint32()
		return true
	}

	override fun toString(): String {
//		return "RamStatsPacket(minStackEnd=$minStackEnd, maxHeapEnd=$maxHeapEnd, numSbrkFails=$numSbrkFails)"
		return "RamStatsPacket(minStackEnd=0x%X, maxHeapEnd=0x%X, minFree=$minFree, numSbrkFails=$numSbrkFails)".format(minStackEnd.toLong(), maxHeapEnd.toLong())
	}
}