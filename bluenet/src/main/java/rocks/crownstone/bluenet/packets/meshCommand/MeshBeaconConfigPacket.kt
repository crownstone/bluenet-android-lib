package rocks.crownstone.bluenet.packets.meshCommand

import rocks.crownstone.bluenet.IbeaconData
import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.putShort
import java.nio.ByteBuffer

class MeshBeaconConfigPacket(beaconData: IbeaconData): MeshCommandPacket(BeaconConfigPacket(beaconData)) {
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