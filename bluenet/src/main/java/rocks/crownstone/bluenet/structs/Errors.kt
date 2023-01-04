/**
 * Author: Crownstone Team
 * Copyright: Crownstone (https://crownstone.rocks)
 * Date: Jan 15, 2019
 * License: LGPLv3+, Apache License 2.0, and/or MIT (triple-licensed)
 */

package rocks.crownstone.bluenet.structs

import java.util.*
import kotlin.Exception

object Errors {
	// core promises
	class PromisesWrongType: Exception("wrong promise type")
	class PromisesWrongActionType: Exception("wrong action")
	open class Busy(msg: String = "busy"): Exception(msg)
	class BusyWrongState: Busy("wrong state: busy")
	class BusyOtherDevice: Busy("busy with another device")
	class BusyAlready(msg: String): Busy("busy: already $msg")

	// BluetoothGatt errors, see https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
	open class Gatt(status: Int, type: String = ""): Exception("gatt error $status: $type") // Make a class per error?
	class GattDisconnected(status: Int): Gatt(status)
	class GattDisconnectedByTimeout: Gatt(8, "timeout")
	class GattDisconnectedByPeer: Gatt(19, "disconnected by peer")
	class GattDisconnectedByHost: Gatt(22, "disconnected by host")
	class GattConnectionFail: Gatt(62, "connection failed to establish")


	class GattNoResources: Gatt(128)
	class GattInternal: Gatt(129)
	class GattWrongState: Gatt(130)
	class GattDbFull: Gatt(131)
	class GattBusy: Gatt(132)
	class GattError133: Gatt(133, "error")
	class GattCmdStarted: Gatt(134)
	class GattIllegalParameter: Gatt(135)
	class GattPending: Gatt(136)
	class GattAuthFail: Gatt(137)
	class GattMore: Gatt(138)
	class GattInvalidCfg: Gatt(139)
	class GattServiceStarted: Gatt(140)
	class GattEncryptedNoMitm: Gatt(141)
	class GattNotEncrypted: Gatt(142)
	class GattCongested: Gatt(143)
	class GattCccCfg: Gatt(253)
	class GattPrcInProgress: Gatt(254)
	class GattOutOfRange: Gatt(255)


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
	class ConnectionNotClosed: Exception("connection is not closed, disconnect and try again")
	class DeviceNotInCache: Exception("device is not in cache, scan in order to get it in cache")
	class NotConnected: Exception("not connected")
	class CharacteristicNotFound(uuid: UUID): Exception("characteristic not found: $uuid")
	class SessionDataMissing: Exception("no session data")
	class NotInitialized: Exception("Not initialized")

	// Wrong crownstone mode
	open class Mode(msg: String = "wrong mode"): Exception(msg)
	class NotInMode(expectedMode: CrownstoneMode): Mode("not in ${expectedMode.name} mode")
	class AlreadySetupInMode: Mode("already in setup mode")

	// Recovery
	class RecoveryRebootRequired: Exception("reboot of stone is required before you can recover")
	class RecoveryDisabled: Exception("recovery is disabled on this stone")

	// parsing
	class ProcessCallback: Exception("process callback error")
	class Parse(msg: String): Exception("parse failed: $msg")
	class OpcodeWrong: Exception("wrong opcode")
	class SizeWrong(msg: String = "wrong size"): Exception(msg)
	class ValueWrong(msg: String = "wrong value"): Exception(msg)
	class TypeWrong(type: String): Exception("wrong type: $type")

	class Full: Exception("full")

	class NotImplemented: Exception("Not implemented")

	class Unsupported(msg: String): Exception("Unsupported: $msg")

	class Result(val result: ResultType): Exception("result: ${result.name}")

	open class Timeout(msg: String =""): Exception("timed out: ($msg)")
	class NotificationTimeout: Timeout("notification") // Expected a notification from a BLE connection, but didn't get one before timeout.
	class HubDataReplyTimeout: Timeout("hub reply") // Expected a reply from the hub, but didn't get one before timeout.

	class Aborted: Exception("aborted")

	class Unknown: Exception("unknown")

	// encryption errors
	open class Encryption(msg: String="encryption failed"): Exception(msg)
	class EncryptionKeyMissing: Encryption("missing key")

	fun gattConnectionError(status: Int): Gatt {
		return when (status) {
			8 -> GattDisconnectedByTimeout()
			19 -> GattDisconnectedByPeer()
			22 -> GattDisconnectedByHost()
			62 -> GattConnectionFail()
			else -> GattDisconnected(status)
		}
	}

	fun gattError(status: Int): Gatt {
		return when (status) {
			8 -> GattDisconnectedByTimeout()
			19 -> GattDisconnectedByPeer()
			22 -> GattDisconnectedByHost()
			128 -> GattNoResources()
			129 -> GattInternal()
			130 -> GattWrongState()
			131 -> GattDbFull()
			132 -> GattBusy()
			133 -> GattError133()
			134 -> GattCmdStarted()
			135 -> GattIllegalParameter()
			136 -> GattPending()
			137 -> GattAuthFail()
			138 -> GattMore()
			139 -> GattInvalidCfg()
			140 -> GattServiceStarted()
			141 -> GattEncryptedNoMitm()
			142 -> GattNotEncrypted()
			143 -> GattCongested()
			253 -> GattCccCfg()
			254 -> GattPrcInProgress()
			255 -> GattOutOfRange()
			else -> Gatt(status)
		}
	}

}