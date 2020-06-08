/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 25, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.powerSamples

import rocks.crownstone.bluenet.structs.Uint8

enum class PowerSamplesType(val num: Uint8) {
	SWITCHCRAFT(0U),
	SWITCHCRAFT_NON_TRIGGERED(1U),
	NOW(3U),
	NOW_UNFILTERED(4U),
	UNKNOWN(255U);
	companion object {
		private val map = values().associateBy(PowerSamplesType::num)
		fun fromNum(type: Uint8): PowerSamplesType {
			return map[type] ?: return UNKNOWN
		}
	}
}

fun PowerSamplesIndices(type: PowerSamplesType): MutableList<Uint8> {
	return when (type) {
		PowerSamplesType.SWITCHCRAFT -> arrayListOf(0U, 1U, 2U)
		PowerSamplesType.SWITCHCRAFT_NON_TRIGGERED -> arrayListOf(0U, 1U, 2U)
		PowerSamplesType.NOW -> arrayListOf(0U, 1U)
		PowerSamplesType.NOW_UNFILTERED -> arrayListOf(0U, 1U)
		PowerSamplesType.UNKNOWN -> arrayListOf()
	}
}
