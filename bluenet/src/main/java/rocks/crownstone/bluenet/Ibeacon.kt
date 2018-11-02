package rocks.crownstone.bluenet

import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.getUint16
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

data class IbeaconData(val uuid: UUID, val major: Uint16, val minor: Uint16, val rssiAtOneMeter: Int8) {
	override fun toString(): String {
		return "uuid=$uuid, major=$major, minor=$minor, rssiAtOneMeter=$rssiAtOneMeter"
	}
}

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
			val advertisementId = bb.getUint16()
			if (advertisementId == BluenetProtocol.IBEACON_ADVERTISEMENT_ID && bb.remaining() >= 16 + 2 + 2 + 1) {
				val uuid = UUID(bb.getLong(), bb.getLong())
				val major = Conversion.toUint16(bb.getShort())
				val minor = Conversion.toUint16(bb.getShort())
				val rssiAtOneMeter = bb.get()
				return IbeaconData(uuid, major, minor, rssiAtOneMeter)
			}
			return null
		}
	}
}