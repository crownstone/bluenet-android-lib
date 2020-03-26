/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class StringPacket(data: String): PacketInterface {
	constructor():                         this("")

	val TAG = this.javaClass.simpleName
	val charset = Charsets.US_ASCII
	var string: String = data
		protected set
	var arr: ByteArray = string.toByteArray(charset)

	override fun getPacketSize(): Int {
		return arr.size
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(arr)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		arr = ByteArray(bb.remaining())
		bb.get(arr)
		string = String(arr, charset)
		return true
	}

//	override fun getArray(): ByteArray? {
//		return arr
//	}

//	override fun fromArray(array: ByteArray): Boolean {
//		arr = array
//		string = String(arr, charset)
//		return true
//	}

//	fun getPayload(): ByteArray {
//		return arr
//	}

	override fun toString(): String {
		return string
	}
}
