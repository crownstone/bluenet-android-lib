/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 23, 2022
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.microapp

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MicroappUploadPacket(header: MicroappHeaderPacket, offset: Int, binaryChunk: ByteArray): PacketInterface {
	val TAG = this.javaClass.simpleName

	val header = header
	val offset = offset
	val binaryChunk = binaryChunk

	override fun getPacketSize(): Int {
		return MicroappHeaderPacket.SIZE + Uint16.SIZE_BYTES + binaryChunk.size
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		if (!header.toBuffer(bb)) {
			return false
		}
		bb.putUint16(offset.toUint16())
		bb.put(binaryChunk)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return "MicroappUploadPacket(header=$header, offset=$offset, binaryChunk=${binaryChunk.contentToString()})"
	}
}
