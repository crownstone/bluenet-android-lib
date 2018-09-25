package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object V3 {
	fun parseHeader(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		servicedata.setDeviceTypeFromServiceUuid()
		servicedata.operationMode = OperationMode.NORMAL // TODO: set this before full parse was successful?
		return true
	}

	fun parse(bb: ByteBuffer, servicedata: CrownstoneServiceData, key: ByteArray?): Boolean {
		if (key == null) {
			return parseServiceData(bb, servicedata)
		}
		val decryptedBytes = Encryption.decryptEcb(bb, key)
		if (decryptedBytes == null) {
			return false
		}
		val decryptedBB = ByteBuffer.wrap(decryptedBytes)
		decryptedBB.order(ByteOrder.LITTLE_ENDIAN)
		return parseServiceData(decryptedBB, servicedata)
	}

	private fun parseServiceData(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		servicedata.type = bb.get().toInt()
		when (servicedata.type) {
			0 -> return V3Types.parseType0(bb, servicedata)
			1 -> return V3Types.parseType1(bb, servicedata)
			2 -> return V3Types.parseType2(bb, servicedata)
			3 -> return V3Types.parseType3(bb, servicedata)
			else -> return false
		}
	}

}