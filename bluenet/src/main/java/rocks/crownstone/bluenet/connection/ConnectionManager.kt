package rocks.crownstone.bluenet.connection

import rocks.crownstone.bluenet.BleCore
import rocks.crownstone.bluenet.encryption.EncryptionManager
import rocks.crownstone.bluenet.structs.DeviceAddress
import rocks.crownstone.bluenet.util.EventBus

/**
 * Class that manages multiple connections.
 */
class ConnectionManager(eventBus: EventBus, bleCore: BleCore, encryptionManager: EncryptionManager) {
	private val connections: HashMap<DeviceAddress, ExtConnection> = HashMap()
	private val eventBus = eventBus
	private val bleCore = bleCore
	private val encryptionManager = encryptionManager

	fun getConnection(address: DeviceAddress): ExtConnection {
//		return connections.get(address) ?: ExtConnection(eventBus, bleCore, encryptionManager)
		val connection = connections.get(address)
		if (connection == null) {
			val newConnection = ExtConnection(eventBus, bleCore, encryptionManager)
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
}