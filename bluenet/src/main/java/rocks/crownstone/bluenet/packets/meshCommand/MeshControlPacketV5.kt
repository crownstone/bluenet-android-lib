/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Mar 31, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.packets.wrappers.v4.ControlPacketV4
import rocks.crownstone.bluenet.structs.Uint8

class MeshControlPacketV5: MeshCommandPacketV5 {
	constructor(controlPacket: ControlPacketV4,
				flags: MeshCommandFlags,
				transmissions: Uint8,
				timeout: Uint8,
				ids: List<Uint8>
	): super(controlPacket, flags, transmissions, timeout, ids)
}

