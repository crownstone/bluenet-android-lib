/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.packets.wrappers.v3.ControlPacketV3
import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4

class MeshControlPacket: MeshCommandPacket {
	constructor(controlPacket: ControlPacketV3): super(controlPacket)
	constructor(controlPacket: ControlPacketV4): super(controlPacket)
}

