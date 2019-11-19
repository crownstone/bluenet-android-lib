/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Nov 14, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v4

import rocks.crownstone.bluenet.packets.ByteArrayPacket
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.packets.wrappers.PayloadWrapperPacket
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class ControlPacketV4(type: ControlTypeV4, payload: PacketInterface?): PayloadWrapperPacket(payload) {
	constructor():                                         this(ControlTypeV4.UNKNOWN, null)
	constructor(type: ControlTypeV4):                      this(type, null)
	constructor(type: ControlTypeV4, data: ByteArray):     this(type, ByteArrayPacket(data))
	constructor(type: ControlTypeV4, byte: Byte):          this(type, byteArrayOf(byte))
	constructor(type: ControlTypeV4, short: Short):        this(type, Conversion.int16ToByteArray(short))
	constructor(type: ControlTypeV4, int: Int):            this(type, Conversion.int32ToByteArray(int))
	constructor(type: ControlTypeV4, float: Float):        this(type, Conversion.floatToByteArray(float))

	override val TAG = this.javaClass.simpleName
	companion object {
		const val SIZE = 2+2
	}
	var type: ControlTypeV4 = type
		protected set
	protected var dataSize: Uint16 = 0
	init {
		dataSize = payload?.getPacketSize() ?: 0
	}

	override fun getHeaderSize(): Int {
		return SIZE
	}

	override fun getPayloadSize(): Int? {
		return dataSize
	}

	override fun headerToBuffer(bb: ByteBuffer): Boolean {
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.putShort(type.num)
		bb.putShort(dataSize)
		return true
	}

	override fun headerFromBuffer(bb: ByteBuffer): Boolean {
		type = ControlTypeV4.fromNum(bb.getUint16())
		dataSize = bb.getUint16()
		return true
	}

	override fun toString(): String {
		return "type=$type dataSize=$dataSize data=${Conversion.bytesToString(getPayload())}"
	}
}
