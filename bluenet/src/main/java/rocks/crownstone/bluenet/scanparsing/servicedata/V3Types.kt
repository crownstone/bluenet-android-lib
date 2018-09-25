package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.PartialTime
import rocks.crownstone.bluenet.util.Util
import java.nio.ByteBuffer

object V3Types {
	const val VALIDATION = 0xFA.toByte()

	fun parseType0(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		return parseStatePacket(bb, servicedata, false, false)
	}

	fun parseType1(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		return parseErrorPacket(bb, servicedata, false, false)
	}

	fun parseType2(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		return parseStatePacket(bb, servicedata, true, false)
	}

	fun parseType3(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		return parseErrorPacket(bb, servicedata, true, false)
	}


	internal fun parseStatePacket(bb: ByteBuffer, servicedata: CrownstoneServiceData, external: Boolean, withRssi: Boolean): Boolean {
		servicedata.flagExternalData = external
		servicedata.crownstoneId = bb.get().toInt()
		servicedata.switchState = bb.get().toInt()
		parseFlags(bb.get(), servicedata)
		servicedata.temperature = bb.get()
		parsePowerFactor(bb, servicedata)
		servicedata.powerUsageReal = bb.getShort() / 8.0
		setPowerUsageApparent(servicedata)
		servicedata.energyUsed = bb.getInt() * 64L
		parsePartialTimestamp(bb, servicedata)
		if (external && withRssi) {
			servicedata.externalRssi = bb.get().toInt()
		}
		else {
			bb.get() // reserved
		}
		servicedata.validation = bb.get() == VALIDATION
		return true
	}

	internal fun parseErrorPacket(bb: ByteBuffer, servicedata: CrownstoneServiceData, external: Boolean, withRssi: Boolean): Boolean {
		servicedata.flagExternalData = external
		servicedata.crownstoneId = bb.get().toInt()
		parseErrorBitmask(Conversion.toUint32(bb.getInt()), servicedata)
		servicedata.errorTimestamp = Conversion.toUint32(bb.getInt())
		parseFlags(bb.get(), servicedata)
		servicedata.temperature = bb.get()
		parsePartialTimestamp(bb, servicedata)
		if (external) {
			if (withRssi) {
				servicedata.externalRssi = bb.get().toInt()
			}
			else {
				bb.get() // reserved
			}
			servicedata.validation = bb.get() == VALIDATION
		}
		else {
			servicedata.powerUsageReal = bb.getShort() / 8.0
		}
		return true
	}


	private fun parseFlags(flags: Byte, servicedata: CrownstoneServiceData) {
		servicedata.flagDimmingAvailable = Util.isBitSet(flags, 0)
		servicedata.flagDimmable         = Util.isBitSet(flags, 1)
		servicedata.flagError            = Util.isBitSet(flags, 2)
		servicedata.flagSwitchLocked     = Util.isBitSet(flags, 3)
		servicedata.flagTimeSet          = Util.isBitSet(flags, 4)
		servicedata.flagSwitchCraft      = Util.isBitSet(flags, 5)
	}

	private fun parseErrorBitmask(bitmask: Long, servicedata: CrownstoneServiceData) {
		servicedata.errorOverCurrent       = Util.isBitSet(bitmask,0)
		servicedata.errorOverCurrentDimmer = Util.isBitSet(bitmask,1)
		servicedata.errorChipTemperature   = Util.isBitSet(bitmask,2)
		servicedata.errorDimmerTemperature = Util.isBitSet(bitmask,3)
		servicedata.errorDimmerFailureOn   = Util.isBitSet(bitmask,4)
		servicedata.errorDimmerFailureOff  = Util.isBitSet(bitmask,5)
		servicedata.flagError = true
	}

	private fun parsePowerFactor(bb: ByteBuffer, servicedata: CrownstoneServiceData) {
		val powerFactor = bb.get()
		if (powerFactor == 0.toByte()) {
			servicedata.powerFactor = 1.0
		}
		else {
			servicedata.powerFactor = bb.get() / 127.0
		}
	}

	private fun setPowerUsageApparent(servicedata: CrownstoneServiceData) {
		// See https://kotlinlang.org/docs/reference/equality.html#floating-point-numbers-equality
		if (servicedata.powerFactor == 0.0 || servicedata.powerFactor == -0.0) {
			servicedata.powerUsageApparent = 0.0
		}
		else {
			servicedata.powerUsageApparent = servicedata.powerUsageReal / servicedata.powerFactor
		}
	}

	// Currently assumes flags have been parsed already
	private fun parsePartialTimestamp(bb: ByteBuffer, servicedata: CrownstoneServiceData) {
		val partialTimestamp = Conversion.toUint16(bb.getShort())
		if (servicedata.flagTimeSet) {
			servicedata.timestamp = PartialTime.reconstructTimestamp(partialTimestamp)
		}
		servicedata.changingData = partialTimestamp
	}

}