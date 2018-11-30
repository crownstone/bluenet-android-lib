package rocks.crownstone.bluenet.scanparsing

import rocks.crownstone.bluenet.structs.BluenetProtocol
import rocks.crownstone.bluenet.structs.IbeaconData
import rocks.crownstone.bluenet.structs.Uint16
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.getUint16
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class Ibeacon {
	companion object {
		fun parse(byteArray: ByteArray): IbeaconData? {
			val bb = ByteBuffer.wrap(byteArray)

			// iBeacon data is in big endian format
			bb.order(ByteOrder.BIG_ENDIAN)

			// Advertisement id is actually two separate bytes, first bytes is the iBeacon type (0x02),
			// the second is the iBeacon length (0x15), but they are fixed to these values, so we can
			// compare them together
			if (bb.remaining() < 2) {
				return null
			}
			if (bb.getUint8().toInt() != BluenetProtocol.IBEACON_TYPE) {
				return null
			}
			if (bb.getUint8().toInt() != BluenetProtocol.IBEACON_SIZE) {
				return null
			}
			if (bb.remaining() < BluenetProtocol.IBEACON_SIZE) {
				return null
			}
			val uuid = UUID(bb.getLong(), bb.getLong())
			val major = bb.getUint16()
			val minor = bb.getUint16()
			val rssiAtOneMeter = bb.get()
			return IbeaconData(uuid, major, minor, rssiAtOneMeter)
		}
	}
}