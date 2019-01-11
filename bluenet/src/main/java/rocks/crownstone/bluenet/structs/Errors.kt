package rocks.crownstone.bluenet.structs

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

	// Wrong crownstone mode
	open class Mode(msg: String = "wrong mode"): Exception(msg)
	class NotInMode(expectedMode: CrownstoneMode): Mode("not in ${expectedMode.name} mode")

	// Recovery
	class RecoveryRebootRequired: Exception("reboot of stone is required before you can recover")
	class RecoveryDisabled: Exception("recovery is disabled on this stone")

	// parsing
	class ProcessCallback: Exception("process callback error")
	class Parse(msg: String): Exception("parse failed: $msg")
	class OpcodeWrong: Exception("wrong opcode")
	class SizeWrong: Exception("wrong size")
	class ValueWrong: Exception("wrong value")

	class Full: Exception("full")

	class Result(result: ResultType): Exception("result: ${result.name}")

	class Timeout: Exception("timed out")

	class Aborted: Exception("aborted")

	// encryption errors
	open class Encryption(msg: String="encryption failed"): Exception(msg)
	class EncryptionKeyMissing: Encryption("missing key")


}