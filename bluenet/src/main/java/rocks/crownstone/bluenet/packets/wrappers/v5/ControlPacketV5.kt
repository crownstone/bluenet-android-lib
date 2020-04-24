/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Apr 23, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v5

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.PayloadWrapperPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class ControlPacketV5(protocol: ConnectionProtocol, type: ControlTypeV4, payload: PacketInterface?): PayloadWrapperPacket(payload) {
	constructor(protocol: ConnectionProtocol, type: ControlTypeV4): this(protocol, type, null)

	override val TAG = this.javaClass.simpleName
	companion object {
		const val HEADER_SIZE = 1+2+2
	}
	var protocol: ConnectionProtocol = protocol
		protected set
	var type: ControlTypeV4 = type
		protected set
	protected var dataSize: Uint16 = 0U
	init {
		dataSize = payload?.getPacketSize()?.toUint16() ?: 0U
	}

	override fun getHeaderSize(): Int {
		return HEADER_SIZE
	}

	override fun getPayloadSize(): Int? {
		return dataSize.toInt()
	}

	override fun headerToBuffer(bb: ByteBuffer): Boolean {
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putUint8(protocol.num)
		bb.putUint16(type.num)
		bb.putUint16(dataSize)
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		protocol = ConnectionProtocol.fromNum(bb.getUint8())
		type = ControlTypeV4.fromNum(bb.getUint16())
		dataSize = bb.getUint16()
		return true
	}

	override fun toString(): String {
		return "ControlPacketV5(protocol=$protocol, type=$type, dataSize=$dataSize, data=${Conversion.bytesToString(getPayload())})"
	}
}
