package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.DeviceType
import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import java.nio.ByteBuffer

internal object V6 {
	fun parseHeader(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		servicedata.deviceType = DeviceType.fromInt(bb.get().toInt())
		servicedata.operationMode = OperationMode.SETUP // TODO: set this before full parse was successful?
		return true
	}

	fun parse(bb: ByteBuffer, servicedata: CrownstoneServiceData, key: ByteArray?): Boolean {
		servicedata.type = bb.get().toInt()
		when (servicedata.type) {
			0 -> return Shared.parseSetupPacket(bb, servicedata, false, false)
			else -> return false
		}
	}
}