package rocks.crownstone.bluenet

object BluenetConfig {
	// Timeouts in ms
	val TIMEOUT_CONNECT: Long =       10000
	val TIMEOUT_DISCONNECT: Long =    3000
	val TIMEOUT_DISCOVER: Long =      3000
	val TIMEOUT_READ: Long =          3000
	val TIMEOUT_WRITE: Long =         4000
	val TIMEOUT_SUBSCRIBE: Long =     4000
	val TIMEOUT_UNSUBSCRIBE: Long =   4000
	val TIMEOUT_REFRESH_CACHE: Long = 3000

	val TIMEOUT_GET_CONFIG: Long = 5000
	val TIMEOUT_GET_STATE: Long = 5000

	val DELAY_REFRESH_CACHE: Long = 1000
}