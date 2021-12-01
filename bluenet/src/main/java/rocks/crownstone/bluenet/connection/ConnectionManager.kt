package rocks.crownstone.bluenet.connection

import android.os.SystemClock
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.BluenetConfig
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.structs.DeviceAddress
import rocks.crownstone.bluenet.util.EventBus
import rocks.crownstone.bluenet.util.Log
import java.util.*
import kotlin.collections.HashMap

/**
 * Class that manages multiple connections.
 */
class ConnectionManager(eventBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val TAG = this.javaClass.simpleName
	private val connections: HashMap<DeviceAddress, ExtConnection> = HashMap()
	private val eventBus = eventBus
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager

	fun getConnection(address: DeviceAddress): ExtConnection {
//		return connections.get(address) ?: ExtConnection(eventBus, bleCore, encryptionManager)
		val connection = connections.get(address)
		if (connection == null) {
			val newConnection = ExtConnection(address, eventBus, bleCore, encryptionManager)
			connections.put(address, newConnection)
			return newConnection
		}
		else {
			return connection
		}
	}

	fun destroy() {
		for (connection in connections.values) {
			connection.disconnect()
		}
	}



	class QueuedConnect(
			val address: DeviceAddress,
			val deferred: Deferred<Unit, Exception>,
			val timeoutMs: Long,
			val retries: Int,
			val startTimestamp: Long) {

		override fun toString(): String {
			return "QueuedConnect(address='$address', timeoutMs=$timeoutMs, retries=$retries, startTimestamp=$startTimestamp)"
		}
	}

	private val connectQueue = LinkedList<QueuedConnect>()
	private var connectInProgress: QueuedConnect? = null

	/**
	 * Connect to a device.
	 *
	 * @param address        MAC address of the Crownstone.
	 * @param auto           Automatically connect once the device is in range.
	 *                       Note that this will only work when the device is in cache:
	 *                       when it's bonded or when it has been scanned since last phone or bluetooth restart.
	 *                       This may be slower than a non-auto connect when the device is already in range.
	 *                       You can have multiple pending auto connections, but only 1 non-auto connecting at a time.
	 *                       Non-auto connects will be queued.
	 * @param timeoutMs      Optional: timeout in ms.
	 * @param retries        Optional: number of times to retry.
	 * @return Promise that resolves when connected.
	 */
	@Synchronized
	fun connect(address: DeviceAddress, auto: Boolean = false, timeoutMs: Long = BluenetConfig.TIMEOUT_CONNECT, retries: Int = BluenetConfig.CONNECT_RETRIES): Promise<Unit, Exception> {
		if (auto) {
			return getConnection(address).connect(auto, timeoutMs, retries)
		}
		// TODO: don't put in queue when already connected.
		val deferred = deferred<Unit, Exception>()
		val item = QueuedConnect(address, deferred, timeoutMs, retries, SystemClock.elapsedRealtime())
		connectQueue.addLast(item)
		connectNext()
		return deferred.promise
	}

	private fun connectNext() {
		Log.d(TAG, "connectNext")
		if (connectInProgress != null) {
			Log.d(TAG, "connect in progress: $connectInProgress")
			return
		}
		if (connectQueue.isEmpty()) {
			return
		}
		printQueue()
		val item = connectQueue.removeFirst()
		connectInProgress = item
		val address = item.address
		val deferred = item.deferred
		getConnection(address).connect(false, item.timeoutMs, item.retries)
				.success {
					val timestamp = SystemClock.elapsedRealtime()
					Log.i(TAG, "connected to $address dt=${timestamp - item.startTimestamp} ms")
					deferred.resolve(Unit)
					// TODO: also resolve all other connects in queue to this address?
					connectInProgress = null
					connectNext()
				}
				.fail {
					val timestamp = SystemClock.elapsedRealtime()
					Log.w(TAG, "failed to connect to $address: ${it.message} dt=${timestamp - item.startTimestamp} ms")
					deferred.reject(it)
					connectInProgress = null
					connectNext()
				}
	}

	/**
	 * Abort current action (connect, disconnect, write, read, subscribe, unsubscribe) and disconnects.
	 * Mostly made to abort connecting.
	 * Also removes queued connects.
	 *
	 * @param address        MAC address of the Crownstone.
	 * @return Promise that resolves when disconnected.
	 */
	@Synchronized
	fun abort(address: DeviceAddress): Promise<Unit, Exception> {
		Log.i(TAG, "abort $address")
		connectQueue.removeIf { item: QueuedConnect -> item.address == address }
		return getConnection(address).abort()
	}

	private fun printQueue() {
		Log.d(TAG, "Queue:")
		for (item in connectQueue) {
			Log.d(TAG, "    $item")
		}
	}
}