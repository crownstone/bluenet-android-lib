package rocks.crownstone.bluenet.services.packets

import android.util.Log
import rocks.crownstone.bluenet.IbeaconData
import rocks.crownstone.bluenet.Uint32
import rocks.crownstone.bluenet.Uint8
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.put
import rocks.crownstone.bluenet.util.putInt
import rocks.crownstone.bluenet.util.putShort
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
	private val TAG = this.javaClass.simpleName

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < SIZE) {
			Log.w(TAG, "buffer too small: ${bb.remaining()} < $SIZE")
			return false
		}
		if (keySet.adminKeyBytes == null || keySet.memberKeyBytes == null || keySet.guestKeyBytes == null) {
			Log.w(TAG, "missing key: admin=${keySet.adminKeyBytes} member=${keySet.memberKeyBytes} guest=${keySet.guestKeyBytes}")
			return false
		}
		bb.order(ByteOrder.LITTLE_ENDIAN)
		bb.put(type)
		bb.put(id)
		bb.put(keySet.adminKeyBytes)
		bb.put(keySet.memberKeyBytes)
		bb.put(keySet.guestKeyBytes)
		bb.putInt(meshAccessAddress)
		bb.put(Conversion.uuidToBytes(ibeaconData.uuid))
		bb.putShort(ibeaconData.major)
		bb.putShort(ibeaconData.minor)
		return true
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		return false // Not implemented yet (no need?)
	}
}