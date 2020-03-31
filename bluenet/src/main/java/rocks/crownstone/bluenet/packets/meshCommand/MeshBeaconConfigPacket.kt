/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.structs.IbeaconData
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

// Also never used?
@Deprecated("Deprecated, use state set command instead")
class MeshBeaconConfigPacket(beaconData: IbeaconData, ids: List<Uint8> = emptyList()): MeshCommandPacketV3(BeaconConfigPacket(beaconData), ids) {
	internal class BeaconConfigPacket(val beaconData: IbeaconData) : PacketInterface {
		companion object {
			const val SIZE = 21
		}

		override fun getPacketSize(): Int {
			return SIZE
		}

		override fun toBuffer(bb: ByteBuffer): Boolean {
			if (bb.remaining() < getPacketSize()) {
				return false
			}
			bb.putShort(beaconData.major)
			bb.putShort(beaconData.minor)
			bb.put(Conversion.uuidToBytes(beaconData.uuid))
			bb.put(beaconData.rssiAtOneMeter)
			return true
		}

		override fun fromBuffer(bb: ByteBuffer): Boolean {
			return false // Not implemented yet (no need?)
		}
	}
}