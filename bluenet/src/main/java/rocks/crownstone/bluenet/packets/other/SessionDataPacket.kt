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
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer

class SessionDataPacket: PacketInterface {
	var validation = ByteArray(BluenetProtocol.VALIDATION_KEY_LENGTH); private set
	var protocol: Uint8 = 0U; private set
	var sessionNonce = ByteArray(BluenetProtocol.SESSION_NONCE_LENGTH); private set
	var validationKey = ByteArray(BluenetProtocol.VALIDATION_KEY_LENGTH); private set
	var padding = ByteArray(PADDING_SIZE); private set

	companion object {
		const val SIZE = BluenetProtocol.AES_BLOCK_SIZE
		const val PADDING_SIZE = SIZE -
				(BluenetProtocol.VALIDATION_KEY_LENGTH +
						Uint8.SIZE_BYTES +
						BluenetProtocol.SESSION_NONCE_LENGTH +
						BluenetProtocol.VALIDATION_KEY_LENGTH)
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		bb.get(validation)
		protocol = bb.getUint8()
		bb.get(sessionNonce)
		bb.get(validationKey)
		bb.get(padding)
		return true
	}

	override fun toString(): String {
		return "SessionDataPacket(protocol=$protocol, sessionNonce=${Conversion.bytesToString(sessionNonce)}, validationKey=${Conversion.bytesToString(validationKey)}, padding=${Conversion.bytesToString(padding)})"
	}
}