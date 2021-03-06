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
	/**
	 * Get size of the packet in bytes.
	 */
	fun getPacketSize(): Int

	/**
	 * Serialize packet to buffer.
	 */
	fun toBuffer(bb: ByteBuffer): Boolean

	/**
	 * Deserialize packet from buffer.
	 */
	fun fromBuffer(bb: ByteBuffer): Boolean

	/**
	 * Serialize packet to byte array.
	 */
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

	/**
	 * Deserialize packet from byte array.
	 */
	fun fromArray(array: ByteArray): Boolean {
		// Let the packets do the size check themselves, so they can parse partially.
//		if (array.size < getPacketSize()) {
//			return false
//		}
		val bb = ByteBuffer.wrap(array)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return fromBuffer(bb)
	}
}
