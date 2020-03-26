package rocks.crownstone.bluenet.packets.other

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.getUint32
import rocks.crownstone.bluenet.util.getUint8
import java.nio.ByteBuffer

class UicrPacket: PacketInterface {
	var board:          Uint32 = 0xFFFFFFFFU; private set
	var productType:    Uint8 = 0xFFU; private set
	var region:         Uint8 = 0xFFU; private set
	var productFamily:  Uint8 = 0xFFU; private set
	var hardwarePatch:  Uint8 = 0xFFU; private set
	var hardwareMinor:  Uint8 = 0xFFU; private set
	var hardwareMajor:  Uint8 = 0xFFU; private set
	var housing:        Uint8 = 0xFFU; private set
	var productionWeek: Uint8 = 0xFFU; private set
	var productionYear: Uint8 = 0xFFU; private set

	companion object {
		const val SIZE = 4 * Uint32.SIZE_BYTES
	}

	override fun getPacketSize(): Int {
		return SIZE
	}

	override fun toBuffer(bb: ByteBuffer): Boolean {
		return false
	}

	override fun fromBuffer(bb: ByteBuffer): Boolean {
		if (bb.remaining() < getPacketSize()) {
			return false
		}
		board = bb.getUint32()
		productType = bb.getUint8()
		region = bb.getUint8()
		productFamily = bb.getUint8()
		bb.getUint8() // Reserved
		hardwarePatch = bb.getUint8()
		hardwareMinor = bb.getUint8()
		hardwareMajor = bb.getUint8()
		bb.getUint8() // Reserved
		housing = bb.getUint8()
		productionWeek = bb.getUint8()
		productionYear = bb.getUint8()
		bb.getUint8() // Reserved
		return true
	}

	override fun toString(): String {
		return "UicrPacket(board=$board, productType=$productType, region=$region, productFamily=$productFamily, hardwarePatch=$hardwarePatch, hardwareMinor=$hardwareMinor, hardwareMajor=$hardwareMajor, housing=$housing, productionWeek=$productionWeek, productionYear=$productionYear)"
	}
}