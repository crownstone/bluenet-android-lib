/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Apr 23, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer

class SessionDataEncryptedPacket: PacketInterface {
	private val TAG = this.javaClass.simpleName
	var validation = ByteArray(BluenetProtocol.VALIDATION_KEY_LENGTH); private set
	var sessionData = SessionDataPacket(); private set
	var padding = ByteArray(PADDING_SIZE); private set

	companion object {
		const val SIZE = BluenetProtocol.AES_BLOCK_SIZE
		const val PADDING_SIZE = SIZE -	(BluenetProtocol.VALIDATION_KEY_LENGTH + SessionDataPacket.SIZE)
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
		bb.get(validation)
		if (!sessionData.fromBuffer(bb)) {
			return false
		}
		bb.get(padding)
		return true
	}

	override fun toString(): String {
		return "SessionDataEncryptedPacket(validation=${validation.contentToString()}, sessionData=$sessionData, padding=${padding.contentToString()})"
	}
}
