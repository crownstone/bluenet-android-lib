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

class SessionDataPacket: PacketInterface {
	private val TAG = this.javaClass.simpleName
	var protocol: Uint8 = 0U; private set
	var sessionNonce = ByteArray(BluenetProtocol.SESSION_NONCE_LENGTH); private set
	var validationKey = ByteArray(BluenetProtocol.VALIDATION_KEY_LENGTH); private set

	companion object {
		const val SIZE =
						Uint8.SIZE_BYTES +
						BluenetProtocol.SESSION_NONCE_LENGTH +
						BluenetProtocol.VALIDATION_KEY_LENGTH
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
		protocol = bb.getUint8()
		bb.get(sessionNonce)
		bb.get(validationKey)
		return true
	}

	override fun toString(): String {
		return "SessionDataPacket(protocol=$protocol, sessionNonce=${sessionNonce.contentToString()}, validationKey=${validationKey.contentToString()})"
	}
}