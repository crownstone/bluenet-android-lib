/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.encryption

import rocks.crownstone.bluenet.structs.Uint8

enum class AccessLevel(val num: Uint8) {
	ADMIN(0),
	MEMBER(1),
	GUEST(2),
	SETUP(100),
	SERVICE_DATA(101),
	UNKNOWN(201),
	HIGHEST_AVAILABLE(202),
	ENCRYPTION_DISABLED(254);

	companion object {
		private val map = AccessLevel.values().associateBy(AccessLevel::num)
		//		fun fromInt(type: Int) = map.getOrDefault(type, UNKNOWN)
		//@JvmStatic
		fun fromNum(type: Uint8): AccessLevel {
			return map[type] ?: return UNKNOWN
		}
	}
}