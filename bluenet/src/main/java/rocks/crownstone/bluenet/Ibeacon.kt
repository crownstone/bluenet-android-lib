package rocks.crownstone.bluenet

import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

data class IbeaconData(val uuid: UUID, val major: Uint16, val minor: Uint16, val rssiAtOneMeter: Int8) {
	override fun toString(): String {
		return "uuid=$uuid, major=$major, minor=$minor, rssiAtOneMeter=$rssiAtOneMeter"
	}
}

class Ibeacon {
//	var data: IbeaconData? = null
//	var isValid = false
//	var uuid = UUID(0,0)
//	var major = 0
//	var minor = 0
//	var rssiAtOneMeter = 0

	companion object {

//		fun parse(byteArray: ByteArray): Boolean {
		fun parse(byteArray: ByteArray): IbeaconData? {
			val bb = ByteBuffer.wrap(byteArray)

//			bb.order(ByteOrder.LITTLE_ENDIAN)
//
//			if (bb.remaining() < 2) {
//				return null
////				return false
//			}
//			val companyId = Conversion.toUint16(bb.getShort().toInt())
//			if (companyId != BluenetProtocol.APPLE_COMPANY_ID) {
//				return null
////				return false
//			}

			// iBeacon data is in big endian format
			bb.order(ByteOrder.BIG_ENDIAN)

			// advertisement id is actually two separate bytes, first bytes is the iBeacon type (0x02),
			// the second is the iBeacon length (0x15), but they are fixed to these values, so we can
			// compare them together
			if (bb.remaining() < 2) {
				return null
//				return false
			}
			val advertisementId = Conversion.toUint16(bb.getShort().toInt()) // Actually 2 separate fields: type and length
			if (advertisementId == BluenetProtocol.IBEACON_ADVERTISEMENT_ID && bb.remaining() >= 16 + 2 + 2 + 1) {
				val uuid = UUID(bb.getLong(), bb.getLong())
				val major = Conversion.toUint16(bb.getShort())
				val minor = Conversion.toUint16(bb.getShort())
				val rssiAtOneMeter = bb.get()
//				data = IbeaconData(uuid, major, minor, rssiAtOneMeter)
				return IbeaconData(uuid, major, minor, rssiAtOneMeter)
//				return true
			}
			return null
//			return false
		}

		fun toByteArray(data: IbeaconData) {

		}
	}
}