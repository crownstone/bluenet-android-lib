/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.packets.wrappers.v3.ConfigPacket
import rocks.crownstone.bluenet.structs.Uint8

// Also never used?
@Deprecated("Deprecated, use state set command instead")
class MeshConfigPacket(configPacket: ConfigPacket, ids: List<Uint8>): MeshCommandPacketV3(configPacket, ids)