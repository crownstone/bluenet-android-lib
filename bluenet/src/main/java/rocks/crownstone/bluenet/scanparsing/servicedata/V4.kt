package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.structs.OperationMode
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.structs.ServiceDataType
import rocks.crownstone.bluenet.util.Conversion
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
		servicedata.type = ServiceDataType.fromNum(Conversion.toUint8(bb.get()))
		when (servicedata.type) {
			ServiceDataType.STATE -> return Shared.parseSetupPacket(bb, servicedata, false, false)
			else -> return false
		}
	}
}