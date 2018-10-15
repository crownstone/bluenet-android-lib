package rocks.crownstone.bluenet.services.packets

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface PacketInterface {
	fun getSize(): Int
	fun toBuffer(bb: ByteBuffer): Boolean
	fun fromBuffer(bb: ByteBuffer): Boolean
	fun getArray(): ByteArray {
		val arr = ByteArray(getSize())
		val bb = ByteBuffer.wrap(arr)
		val result = toBuffer(bb)
		if (!result) {
			return ByteArray(0)
			// Or just return null?
		}
		return bb.array()
	}
	fun fromArray(array: ByteArray): Boolean {
		val bb = ByteBuffer.wrap(array)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		return fromBuffer(bb)
	}
}