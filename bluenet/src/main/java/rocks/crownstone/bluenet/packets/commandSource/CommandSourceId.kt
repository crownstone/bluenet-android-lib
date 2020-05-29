/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: May 29, 2020
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.packets.commandSource

import rocks.crownstone.bluenet.structs.Uint8

enum class CommandSourceType(val num: Uint8) {
	ENUM(0U),
	BROADCAST(3U),
	UNKNOWN(255U);
	companion object {
		private val map = values().associateBy(CommandSourceType::num)
		fun fromNum(type: Uint8): CommandSourceType {
			return map[type] ?: return UNKNOWN
		}
	}
}

enum class CommandSourceId(val num: Uint8) {
	NONE(0U),
	INTERNAL(2U),
	UART(3U),
	CONNECTION(4U),
	SWITCHCRAFT(5U),
	TAP_TO_TOGGLE(6U),
	UNKNOWN(255U);
	companion object {
		private val map = values().associateBy(CommandSourceId::num)
		fun fromNum(type: Uint8): CommandSourceId {
			return map[type] ?: return UNKNOWN
		}
	}
}