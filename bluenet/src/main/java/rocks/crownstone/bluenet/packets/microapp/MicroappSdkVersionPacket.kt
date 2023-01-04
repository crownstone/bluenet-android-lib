/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 23, 2022
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.microapp

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.behaviour.BehaviourType
import rocks.crownstone.bluenet.packets.behaviour.DaysOfWeekPacket
import rocks.crownstone.bluenet.packets.behaviour.TimeOfDayPacket
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.getUint8
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MicroappSdkVersionPacket(major: Uint8, minor: Uint8): PacketInterface {
	val TAG = this.javaClass.simpleName

	var major = major
		private set
	var minor = minor
		private set

	constructor(): this(0U, 0U)

	companion object {
		const val SIZE = Uint8.SIZE_BYTES + Uint8.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putUint8(major)
		bb.putUint8(minor)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.i(TAG, "size=${bb.remaining()} expected=${getPacketSize()}")
			return false
		}
		major = bb.getUint8()
		minor = bb.getUint8()
		return true
	}

	override fun toString(): String {
		return "MicroappSdkVersionPacket(major=$major, minor=$minor)"
	}
}
