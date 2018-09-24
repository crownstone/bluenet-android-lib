package rocks.crownstone.bluenet.encryption


class EncryptionManager() {
	private val TAG = this::class.java.canonicalName

	private val keys = HashMap<String, KeySet>()
	private val sessionData: SessionData? = null

	fun getKeyset(id: String): KeySet? {
		return keys.get(id)
	}
}