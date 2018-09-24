package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.scanparsing.ServiceDataType
import rocks.crownstone.bluenet.util.Util
import java.nio.ByteBuffer

internal object V1 {
	fun parseV1Header(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		servicedata.type = ServiceDataType.V1
		servicedata.setDeviceTypeFromServiceUuid()

		// Can't just parse header, since we can only determine if stone is in setup mode after parsing all data.
		if (bb.remaining() < 16) {
			return false
		}
		parseV1EncryptedData(bb, servicedata)
		// Check if it's in setup mode
		if (servicedata.flagSetup &&
				servicedata.crownstoneId == 0 &&
				servicedata.switchState == 0 &&
				servicedata.powerUsageReal == 0.0 &&
				servicedata.energyUsed == 0L) {
			return true
		}
		if (encrypted)
		return true
	}

	fun parseV1(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
			return true
	}

	private fun parseV1EncryptedData(bb: ByteBuffer, servicedata: CrownstoneServiceData) {
		servicedata.crownstoneId = Conversion.toUint16(bb.getShort().toInt())
		servicedata.switchState = Conversion.toUint8(bb.get())
		val flags = bb.get()
		servicedata.flagExternalData = (Util.isBitSet(flags, 1))
		servicedata.flagError = (Util.isBitSet(flags, 2))
		servicedata.flagSetup = (Util.isBitSet(flags, 7))
		servicedata.temperature = bb.get()
		servicedata.powerUsageReal = bb.getInt() / 1000.0
		servicedata.energyUsed = bb.getInt().toLong()
		val byteArray = ByteArray(3)
		bb.get(byteArray)
		servicedata.uniqueBytes = byteArray
	}
}