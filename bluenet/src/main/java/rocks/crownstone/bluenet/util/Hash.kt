package rocks.crownstone.bluenet.util

import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint32
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Hash {
	fun fletcher32(array: ByteArray, initialHash: Uint32 = 0U): Uint32 {
		val bb = ByteBuffer.wrap(array)
		return fletcher32(bb, initialHash)
	}

	fun fletcher32(bb: ByteBuffer, initialHash: Uint32 = 0U): Uint32 {
		var C0: Uint16 = (initialHash shr 0).toUint16()
		var C1: Uint16 = (initialHash shr 16).toUint16()

		bb.order(ByteOrder.LITTLE_ENDIAN)
		while (bb.remaining() > 0) {
			val num: Uint16 =
					if (bb.remaining() >= Uint16.SIZE_BYTES) {
						bb.getShort().toUint16()
					}
					else {
						bb.get().toUint16()
					}
			C0 = ((C0 + num) % 0xFFFFU).toUint16()
			C1 = ((C1 + C0) % 0xFFFFU).toUint16()
		}
		return (C1.toUint32() shl 16) + C0.toUint32()
	}
}