/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.wrappers.v3

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion

open class ControlPacketV3(type: ControlType, data: ByteArray?, payload: PacketInterface?): StreamPacket(type.num, data, payload, OpcodeType.WRITE) {


	constructor(): this(ControlType.UNKNOWN, null, null)
	constructor(type: ControlType): this(type, null, null)
	constructor(type: ControlType, data: ByteArray): this(type, data, null)
	constructor(type: ControlType, payload: PacketInterface): this(type, null, payload)
	constructor(type: ControlType, byte: Byte):               this(type, byteArrayOf(byte), null)
	constructor(type: ControlType, short: Short):             this(type, Conversion.int16ToByteArray(short), null)
	constructor(type: ControlType, int: Int):                 this(type, Conversion.int32ToByteArray(int), null)
	constructor(type: ControlType, float: Float):             this(type, Conversion.floatToByteArray(float), null)
}
