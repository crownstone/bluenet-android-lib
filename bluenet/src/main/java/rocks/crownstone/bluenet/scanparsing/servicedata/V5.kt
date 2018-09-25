package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.DeviceType
import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.encryption.Encryption
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object V5 {
	fun parseHeader(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 17) {
			return false
		}
		servicedata.deviceType = DeviceType.fromInt(bb.get().toInt())
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
			0 -> return Shared.parseStatePacket(bb, servicedata, false, false)
			1 -> return Shared.parseErrorPacket(bb, servicedata, false, false)
			2 -> return Shared.parseStatePacket(bb, servicedata, true, true)
			3 -> return Shared.parseErrorPacket(bb, servicedata, true, true)
			else -> return false
		}
	}
}