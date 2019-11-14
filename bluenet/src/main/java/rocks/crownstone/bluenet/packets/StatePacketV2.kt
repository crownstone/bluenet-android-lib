/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets

import rocks.crownstone.bluenet.structs.StateTypeV2
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Log
import rocks.crownstone.bluenet.util.getUint16
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class StatePacketV2(type: StateTypeV2, payload: PacketInterface?): PayloadWrapperPacket(payload) {
	override val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 2
	}
	var type: Uint16 = type.num
		protected set

	override fun getHeaderSize(): Int {
		return SIZE
	}

	override fun getPayloadSize(): Int? {
		return null
	}

	override fun headerToBuffer(bb: ByteBuffer): Boolean {
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(type)
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		type = bb.getUint16()
		return true
	}

	override fun toString(): String {
		return "type=$type ${super.toString()}}"
	}
}