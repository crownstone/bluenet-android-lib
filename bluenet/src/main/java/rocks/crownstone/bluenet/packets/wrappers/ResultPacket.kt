package rocks.crownstone.bluenet.packets.wrappers

import rocks.crownstone.bluenet.packets.PacketInterface
import rocks.crownstone.bluenet.structs.ResultType

interface ResultPacket {
    fun getCode(): ResultType

    fun getPayload(): ByteArray?
}