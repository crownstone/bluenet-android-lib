package rocks.crownstone.bluenet.encryption

enum class AccessLevel(val num: Int) {
	ADMIN(0),
	MEMBER(1),
	GUEST(2),
	SETUP(100),
	UNKNOWN(201),
	HIGHEST_AVAILABLE(202),
	ENCRYPTION_DISABLED(255);

	companion object {
		private val map = AccessLevel.values().associateBy(AccessLevel::num)
		//		fun fromInt(type: Int) = map.getOrDefault(type, UNKNOWN)
		//@JvmStatic
		fun fromInt(type: Int): AccessLevel {
			return map[type] ?: return UNKNOWN
		}
	}
}