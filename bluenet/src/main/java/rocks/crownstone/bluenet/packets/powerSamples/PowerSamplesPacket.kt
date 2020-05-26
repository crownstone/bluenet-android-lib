/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 25, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.powerSamples

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class PowerSamplesPacket: PacketInterface {
	var type: PowerSamplesType = PowerSamplesType.UNKNOWN
		private set
	var index: Uint8 = 0U
		private set
	var count: Uint16 = 0U
		private set
	var timestamp: Uint32 = 0U
		private set
	var delayUs: Uint16 = 0U
		private set
	var sampleIntervalUs: Uint16 = 0U
		private set
	var offset: Int16 = 0
		private set
	var multiplier: Float = 0.0F
		private set
	var samples: ArrayList<Int16> = ArrayList()
		private set

	companion object {
		const val HEADER_SIZE =
						Uint8.SIZE_BYTES +
						Uint8.SIZE_BYTES +
						Uint16.SIZE_BYTES +
						Uint32.SIZE_BYTES +
						Uint16.SIZE_BYTES +
						Uint16.SIZE_BYTES +
						Uint16.SIZE_BYTES + // Reserved
						Int16.SIZE_BYTES +
						4
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + samples.size * Int16.SIZE_BYTES
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < HEADER_SIZE) {
			return false
		}
		type = PowerSamplesType.fromNum(bb.getUint8())
		index = bb.getUint8()
		count = bb.getUint16()
		timestamp = bb.getUint32()
		delayUs = bb.getUint16()
		sampleIntervalUs = bb.getUint16()
		bb.getUint16() // Reserved
		offset = bb.getInt16()
		multiplier = bb.getFloat()
		if (bb.remaining() < count.toInt() * Int16.SIZE_BYTES) {
			return false
		}
		for (i in 0 until count.toInt()) {
			samples.add(bb.getInt16())
		}
		return true
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return "PowerSamplesPacket(type=$type, index=$index, count=$count, timestamp=$timestamp, delayUs=$delayUs, sampleIntervalUs=$sampleIntervalUs, offset=$offset, multiplier=$multiplier, samples=$samples)"
	}
}