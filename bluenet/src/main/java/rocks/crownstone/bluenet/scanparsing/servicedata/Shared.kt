/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.scanparsing.servicedata

import rocks.crownstone.bluenet.structs.Uint32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.structs.OperationMode
import rocks.crownstone.bluenet.structs.SwitchState
import rocks.crownstone.bluenet.util.*
import java.nio.ByteBuffer

internal object Shared {
	private const val VALIDATION = 0xFA.toByte()

	internal fun parseStatePacket(bb: ByteBuffer, servicedata: CrownstoneServiceData, external: Boolean, withRssi: Boolean): Boolean {
		servicedata.flagExternalData = external
		servicedata.crownstoneId = bb.getUint8()
		servicedata.switchState = SwitchState(bb.getUint8())
		parseFlags(bb.getUint8(), servicedata)
		servicedata.temperature = bb.get()
		parsePowerFactor(bb, servicedata)
		servicedata.powerUsageReal = bb.getShort() / 8.0
		setPowerUsageApparent(servicedata)
		servicedata.energyUsed = bb.getInt() * 64L
		parsePartialTimestamp(bb, servicedata)
		if (external && withRssi) {
			servicedata.externalRssi = bb.get()
		}
		else {
			parseExtraFlags(bb.getUint8(), servicedata)
		}
		servicedata.validation = bb.get() == VALIDATION
		return true
	}

	internal fun parseAltStatePacket(bb: ByteBuffer, servicedata: CrownstoneServiceData): Boolean {
		servicedata.flagExternalData = false
		servicedata.crownstoneId = bb.getUint8()
		servicedata.switchState = SwitchState(bb.getUint8())
		parseFlags(bb.getUint8(), servicedata)
		servicedata.behaviourHash = bb.getUint16()
		bb.getUint16() // Reserved
		bb.getUint32() // Reserved
		parsePartialTimestamp(bb, servicedata)
		bb.getUint8() // Reserved
		servicedata.validation = bb.get() == VALIDATION
		return true
	}

	internal fun parseErrorPacket(bb: ByteBuffer, servicedata: CrownstoneServiceData, external: Boolean, withRssi: Boolean): Boolean {
		servicedata.flagExternalData = external
		servicedata.crownstoneId = bb.getUint8()
		parseErrorBitmask(bb.getUint32(), servicedata)
		servicedata.errorTimestamp = bb.getUint32()
		parseFlags(bb.getUint8(), servicedata)
		servicedata.flagError = true // Force flag error to be set.
		servicedata.temperature = bb.get()
		parsePartialTimestamp(bb, servicedata)
		if (external) {
			if (withRssi) {
				servicedata.externalRssi = bb.get()
			}
			else {
				bb.get() // reserved
			}
			servicedata.validation = bb.get() == VALIDATION
		}
		else {
			servicedata.powerUsageReal = bb.getShort() / 8.0
			servicedata.validation = true // No validation to perform, so assume it's true
		}
		return true
	}

	internal fun parseHubDataPacket(bb: ByteBuffer, serviceData: CrownstoneServiceData): Boolean {
		serviceData.crownstoneId = bb.getUint8()
		parseHubFlags(bb.getUint8(), serviceData)
		bb.get(serviceData.hubData)
		parsePartialTimestamp(bb, serviceData)
		bb.getUint8() // Reserved
		serviceData.validation = bb.get() == VALIDATION
		if (serviceData.operationMode == OperationMode.SETUP) {
			serviceData.changingData = serviceData.count.toInt()
		}
		return true
	}

	internal fun parseSetupPacket(bb: ByteBuffer, servicedata: CrownstoneServiceData, external: Boolean, withRssi: Boolean): Boolean {
		servicedata.switchState = SwitchState(bb.getUint8())
		parseFlags(bb.getUint8(), servicedata)
		servicedata.temperature = bb.get()
		parsePowerFactor(bb, servicedata)
		servicedata.powerUsageReal = bb.getShort() / 8.0
		setPowerUsageApparent(servicedata)
		parseErrorBitmask(bb.getUint32(), servicedata)
		val count = bb.getUint8()
		servicedata.count = count.toUint16()
		servicedata.changingData = count.toInt()
		bb.getInt() // reserved
		servicedata.validation = true // No need for validation so set to true
		return true
	}

	private fun parseFlags(flags: Uint8, servicedata: CrownstoneServiceData) {
		servicedata.flagDimmerReady         = Util.isBitSet(flags, 0)
		servicedata.flagDimmable            = Util.isBitSet(flags, 1)
		servicedata.flagError               = Util.isBitSet(flags, 2)
		servicedata.flagSwitchLocked        = Util.isBitSet(flags, 3)
		servicedata.flagTimeSet             = Util.isBitSet(flags, 4)
		servicedata.flagSwitchCraft         = Util.isBitSet(flags, 5)
		servicedata.flagTapToToggleEnabled  = Util.isBitSet(flags, 6)
		servicedata.flagBehaviourOverridden = Util.isBitSet(flags, 7)
	}

	private fun parseExtraFlags(flags: Uint8, servicedata: CrownstoneServiceData) {
		servicedata.flagBehaviourEnabled    = Util.isBitSet(flags, 0)
	}

	private fun parseErrorBitmask(bitmask: Uint32, servicedata: CrownstoneServiceData) {
		servicedata.errorOverCurrent       = Util.isBitSet(bitmask,0)
		servicedata.errorOverCurrentDimmer = Util.isBitSet(bitmask,1)
		servicedata.errorChipTemperature   = Util.isBitSet(bitmask,2)
		servicedata.errorDimmerTemperature = Util.isBitSet(bitmask,3)
		servicedata.errorDimmerFailureOn   = Util.isBitSet(bitmask,4)
		servicedata.errorDimmerFailureOff  = Util.isBitSet(bitmask,5)
	}

	private fun parseHubFlags(flags: Uint8, serviceData: CrownstoneServiceData) {
		serviceData.hubFlagUartAlive                     = Util.isBitSet(flags, 0)
		serviceData.hubFlagUartAliveEncrypted            = Util.isBitSet(flags, 1)
		serviceData.hubFlagUartEncryptionRequiredByStone = Util.isBitSet(flags, 2)
		serviceData.hubFlagUartEncryptionRequiredByHub   = Util.isBitSet(flags, 3)
		serviceData.hubFlagHasBeenSetup                  = Util.isBitSet(flags, 4)
		serviceData.hubFlagHasInternet                   = Util.isBitSet(flags, 5)
		serviceData.hubFlagHasError                      = Util.isBitSet(flags, 6)
		serviceData.flagTimeSet                          = Util.isBitSet(flags, 7)
	}

	private fun parsePowerFactor(bb: ByteBuffer, servicedata: CrownstoneServiceData) {
		val powerFactor = bb.get()
		if (powerFactor == 0.toByte()) {
			servicedata.powerFactor = 1.0
		}
		else {
			servicedata.powerFactor = powerFactor / 127.0
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
		val partialTimestamp = bb.getUint16()
		if (servicedata.flagTimeSet) {
			servicedata.timestamp = PartialTime.reconstructTimestamp(partialTimestamp)
		}
		else {
			servicedata.count = partialTimestamp
		}
//		servicedata.changingData = partialTimestamp.toInt()
	}
}
