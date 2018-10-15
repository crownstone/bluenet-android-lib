package rocks.crownstone.bluenet

import java.lang.Exception

object Errors {
	// core promises
	class PromisesWrongType: Exception("wrong promise type")
	class PromisesWrongActionType: Exception("wrong action")
	open class Busy(msg: String = "busy"): Exception(msg)
	class BusyWrongState: Busy("wrong state: busy")
	class BusyOtherDevice: Busy("busy with another device")

	// gatt errors
	open class Gatt(status: Int): Exception("gatt error $status") // Make a class per error?

	// connection issues
	class WriteFailed: Exception("write failed")
	class ReadFailed: Exception("read failed")
	class DiscoveryStart: Exception("failed to start discovery")
	open class SubscribeFailed(msg: String=""): Exception("subscribe failed: $msg")
	class SubscribeAlreadySubscribed : SubscribeFailed("already subscribed")
	class UnsubscribeFailed(msg: String=""): Exception("unsubscribe failed: $msg")

	// wrong state
	class BleNotReady: Exception("BLE not ready")
	class AddressInvalid: Exception("invalid address")
	class GattNull: Exception("gatt is null")
	class NotConnected: Exception("not connected")
	class CharacteristicNotFound: Exception("characteristic not found")
	class SessionDataMissing: Exception("no session data")
	class NotInSetupMode: Exception("not in setup mode")

	// parsing
	class ProcessCallback: Exception("process callback error")
	class Parse: Exception("parse failed")

	// encryption errors
	open class Encryption(msg: String="encryption failed"): Exception(msg)
	class EncryptionKeyMissing: Encryption("missing key")


}