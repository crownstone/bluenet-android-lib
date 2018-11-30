package rocks.crownstone.bluenet.packets

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface PacketInterface {
	fun getPacketSize(): Int
	fun toBuffer(bb: ByteBuffer): Boolean
	fun fromBuffer(bb: ByteBuffer): Boolean
	fun getArray(): ByteArray {
		val arr = ByteArray(getPacketSize())
		val bb = ByteBuffer.wrap(arr)
		val result = toBuffer(bb)
		if (!result) {
			return ByteArray(0)
			// Or just return null?
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