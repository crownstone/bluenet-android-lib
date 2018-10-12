package rocks.crownstone.bluenet.services.packets

import rocks.crownstone.bluenet.IbeaconData
import rocks.crownstone.bluenet.Uint32
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.util.Conversion
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SetupPacket(val type: Uint8,
				  val id: Uint8,
				  val keySet: KeySet,
				  val meshAccessAddress: Uint32,
				  val ibeaconData: IbeaconData): PacketInterface {
	companion object {
		const val SIZE = 1+1+16+16+16+4+16+2+2
	}

	override fun getSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			return false
		}
		if (keySet.adminKeyBytes == null || keySet.memberKeyBytes == null || keySet.guestKeyBytes == null) {
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(type.toByte())
		bb.put(id.toByte())
		bb.put(keySet.adminKeyBytes)
		bb.put(keySet.memberKeyBytes)
		bb.put(keySet.guestKeyBytes)
		bb.putInt(meshAccessAddress.toInt())
		bb.put(Conversion.uuidToBytes(ibeaconData.uuid))
		bb.putShort(ibeaconData.major.toShort())
		bb.putShort(ibeaconData.minor.toShort())
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false
	}
}