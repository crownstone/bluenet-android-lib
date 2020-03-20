/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v5

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.PayloadWrapperPacket
import rocks.crownstone.bluenet.structs.PersistenceMode
import rocks.crownstone.bluenet.structs.StateTypeV4
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class StatePacketV5(type: StateTypeV4, id: Uint16, persistenceMode: PersistenceMode, payload: PacketInterface?): PayloadWrapperPacket(payload) {
	override val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 2+2+1+1
	}
	var type = type
		protected set
	var id = id
		protected set
	var persistenceMode = persistenceMode
		protected set

	override fun getHeaderSize(): Int {
		return SIZE
	}

	override fun getPayloadSize(): Int? {
		return null
	}

	override fun headerToBuffer(bb: ByteBuffer): Boolean {
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putUint16(type.num)
		bb.putUint16(id)
		bb.putUint8(persistenceMode.num)
		bb.putUint8(0U) // Reserved
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		type = StateTypeV4.fromNum(bb.getUint16())
		id = bb.getUint16()
		persistenceMode = PersistenceMode.fromNum(bb.getUint8())
		bb.getUint8() // Reserved
		return true
	}

	override fun toString(): String {
		return "type=$type, id=$id, persistenceMode=$persistenceMode, ${super.toString()}"
	}
}