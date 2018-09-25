package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.util.Util
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object V1 {
	fun parseHeader(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		servicedata.setDeviceTypeFromServiceUuid()

		// Can't just parse header, since we can only determine if stone is in setup mode after parsing all data.
		parseServiceData(bb, servicedata)
		// Check if it's in setup mode
		if (servicedata.flagSetup &&
				servicedata.crownstoneId == 0 &&
				servicedata.switchState == 0 &&
				servicedata.powerUsageReal == 0.0 &&
				servicedata.energyUsed == 0L) {
			servicedata.operationMode = OperationMode.SETUP
		}
		return true
	}

	fun parse(bb: ByteBuffer, servicedata: CrownstoneServiceData, key: ByteArray?): Boolean {
		if (servicedata.operationMode == OperationMode.SETUP) {
			return true
		}
		if (key == null) {
			// No decryption, data is already parsed when header was parsed, just set operation mode.
			when (servicedata.flagSetup) {
				true -> servicedata.operationMode = OperationMode.SETUP
				false -> servicedata.operationMode = OperationMode.NORMAL
			}
			return true
		}
		val decryptedBytes = Encryption.decryptEcb(bb, key)
		if (decryptedBytes == null) {
			return false
		}
		val decryptedBB = ByteBuffer.wrap(decryptedBytes)
		decryptedBB.order(ByteOrder.LITTLE_ENDIAN)
		parseServiceData(decryptedBB, servicedata)
		return true
	}

	private fun parseServiceData(bb: ByteBuffer, servicedata: CrownstoneServiceData) {
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