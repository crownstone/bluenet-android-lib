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

	class WriteFailed: Exception("write failed")
	class ReadFailed: Exception("read failed")

	open class Gatt(status: Int): Exception("gatt error $status") // Make a class per error?
}