package rocks.crownstone.bluenet

import java.lang.Exception

object Errors {
	class PromiseTypeWrong: Exception("wrong promise type")
	class ActionTypeWrong: Exception("wrong action")

	class BleNotReady: Exception("BLE not ready")
	class AddressInvalid: Exception("invalid address")
	class GattNull: Exception("gatt is null")
	class NotConnected: Exception("not connected")

	open class Busy(msg: String = "busy"): Exception(msg)
	class BusyWrongState: Busy("wrong state: busy")
	class BusyOtherDevice: Busy("busy with another device")

	class DiscoveryStart: Exception("failed to start discovery")

	class CharacteristicNotFound: Exception("characteristic not found")
	class NotInSetupMode: Exception("not in setup mode")

	class WriteFailed: Exception("write failed")
	class ReadFailed: Exception("read failed")

	open class SubscribeFailed(msg: String=""): Exception("subscribe failed: $msg")
	class SubscribeAlreadySubscribed : SubscribeFailed("already subscribed")
	class UnsubscribeFailed: Exception("unsubscribe failed")

	class ProcessCallback: Exception("process callback error")
	class Parse: Exception("parse failed")
	class UnexpectedValue: Exception("unexpected value")
	class SessionDataMissing: Exception("no session data")

	open class Encryption(msg: String="encryption failed"): Exception(msg)
	class EncryptionKeyMissing: Encryption("missing key")

	open class Gatt(status: Int): Exception("gatt error $status") // Make a class per error?
}