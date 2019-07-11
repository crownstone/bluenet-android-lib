/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface PacketInterface {
	fun getPacketSize(): Int
	fun toBuffer(bb: ByteBuffer): Boolean
	fun fromBuffer(bb: ByteBuffer): Boolean
	fun getArray(): ByteArray? {
		val arr = ByteArray(getPacketSize())
		val bb = ByteBuffer.wrap(arr)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		val result = toBuffer(bb)
		if (!result) {
			//return ByteArray(0)
			return null
		}
		return bb.array()
	}
	fun fromArray(array: ByteArray): Boolean {
		if (array.size < getPacketSize()) {
			return false
		}
		val bb = ByteBuffer.wrap(array)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return fromBuffer(bb)
	}
}
