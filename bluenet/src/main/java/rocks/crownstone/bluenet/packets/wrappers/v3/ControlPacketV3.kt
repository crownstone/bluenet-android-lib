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
	constructor(type: ControlType): this(type, null, null)
	constructor(type: ControlType, payload: PacketInterface): this(type, null, payload)
}
