package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.structs.DeviceType
import rocks.crownstone.bluenet.structs.OperationMode
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer

internal object V6 {
	fun parseHeader(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		if (bb.remaining() < 16) {
			return false
		}
		servicedata.deviceType = DeviceType.fromNum(bb.getUint8())
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