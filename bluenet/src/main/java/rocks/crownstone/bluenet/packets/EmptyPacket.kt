/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import java.nio.ByteBuffer

open class EmptyPacket(): PacketInterface {
	val TAG = this.javaClass.simpleName

	override fun getPacketSize(): Int {
		return 0
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return true
	}

	override fun getArray(): ByteArray? {
		return null
	}

	override fun fromArray(array: ByteArray): Boolean {
		return true
	}

	override fun toString(): String {
		return "empty"
	}
}
