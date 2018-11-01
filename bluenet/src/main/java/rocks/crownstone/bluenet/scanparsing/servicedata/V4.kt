package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import java.nio.ByteBuffer

internal object V4 {
	fun parseHeader(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		servicedata.setDeviceTypeFromServiceUuid()
		servicedata.operationMode = OperationMode.SETUP
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