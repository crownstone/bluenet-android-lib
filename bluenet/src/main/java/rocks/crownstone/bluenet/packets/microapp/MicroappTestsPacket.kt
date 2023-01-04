/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 23, 2022
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.microapp

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

class MicroappTestsPacket(): PacketInterface {
	val TAG = this.javaClass.simpleName

	var raw = arrayOf<Uint8>(0U, 0U)
		private set
	var hasData: Boolean = false
		private set

	// 0=untested, 1=trying, 2=failed, 3=passed.
	var checksum: Int = 0
		private set
	var enabled: Boolean = false
		private set

	// 0=untested, 1=trying, 2=failed, 3=passed.
	var boot: Int = 0
		private set

	// 0=ok, 1=excessive
	var memory: Int = 0
		private set
	var reserved: Int = 0
		private set

	companion object {
		const val SIZE = Uint8.SIZE_BYTES * 2 // Size of raw
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.i(TAG, "size=${bb.remaining()} expected=${getPacketSize()}")
			return false
		}

		for (i in raw.indices) {
			raw[i] = bb.getUint8()
		}

		var data1 = raw[0].toInt()

		// 1 bit hasData
		hasData = data1 and 0x01 != 0
		data1 shr 1

		// 2 bits checksum
		checksum = data1 and 0x03
		data1 shr 2

		// 1 bit enabled
		enabled = data1 and 0x01 != 0
		data1 shr 1

		// 2 bits boot
		boot = data1 and 0x03
		data1 shr 2

		// 1 bit memory
		memory = data1 and 0x01
		data1 shr 1

		// 9 bits reserved
		reserved = data1 and 0x01 + raw[1].toInt()

		return true
	}

	override fun toString(): String {
		return "MicroappTestsPacket(raw=${raw.contentToString()}, hasData=$hasData, checksum=$checksum, enabled=$enabled, boot=$boot, memory=$memory, reserved=$reserved)"
	}
}
