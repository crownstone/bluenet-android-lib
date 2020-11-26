/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 26, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.putUint8
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HubDataPacket(encryptType: EncryptType, payload: PacketInterface): PacketInterface {
	val TAG = this.javaClass.simpleName
	companion object {
		const val HEADER_SIZE = 1 + 1
	}

	val encryptType = encryptType
	val payload = payload

	enum class EncryptType(val num: Uint8) {
		NOT_ENCRYPTED(0U),
		ENCRYPT_IF_ENABLED(1U),
		ENCRYPT_OR_FAIL(2U),
		UNKNOWN(0xFFU);
		companion object {
			private val map = values().associateBy(EncryptType::num)
			fun fromNum(type: Uint8): EncryptType {
				return map[type] ?: return UNKNOWN
			}
		}
	}

	override fun getPacketSize(): Int {
		return HEADER_SIZE + payload.getPacketSize()
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < ${getPacketSize()}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putUint8(encryptType.num)
		bb.putUint8(0U) // Reserved
		return payload.toBuffer(bb)
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun toString(): String {
		return "HubDataPacket(encryptType=$encryptType, payload=$payload)"
	}
}
