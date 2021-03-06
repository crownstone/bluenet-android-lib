/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.packets.wrappers.v3.ControlPacketV3
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.structs.Uint8

class MeshControlPacketV3: MeshCommandPacketV3 {
	constructor(controlPacket: ControlPacketV3, ids: List<Uint8>): super(controlPacket, ids)
	constructor(controlPacket: ControlPacketV4, ids: List<Uint8>): super(controlPacket, ids)
}

