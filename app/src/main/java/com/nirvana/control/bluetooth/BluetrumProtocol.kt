package com.nirvana.control.bluetooth

import com.nirvana.control.model.AncMode
import com.nirvana.control.model.KeyFunction
import com.nirvana.control.model.SpatialAudioMode
import com.nirvana.control.model.TouchGesture
import java.nio.ByteBuffer
import java.util.UUID

object BluetrumConstants {
    val DEFAULT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val CUSTOM_SPP_UUID: UUID = UUID.fromString("B6632277-0642-458B-A7A0-23FB1DC92C93")

    const val CMD_EQ: Byte = 32              // 0x20
    const val CMD_MUSIC_CONTROL: Byte = 33    // 0x21
    const val CMD_KEY: Byte = 34             // 0x22
    const val CMD_WORK_MODE: Byte = 37       // 0x25 (BEAST Gaming Mode)
    const val CMD_IN_EAR: Byte = 38          // 0x26
    const val CMD_DEVICE_INFO: Byte = 39     // 0x27
    const val CMD_NOTIFY: Byte = 40          // 0x28
    const val CMD_FIND_DEVICE: Byte = 42     // 0x2A
    const val CMD_ANC_MODE: Byte = 44        // 0x2C
    const val CMD_SPATIAL_AUDIO: Byte = 50   // 0x32
    const val CMD_BASS_ENGINE: Byte = 54     // 0x36

    const val TYPE_REQUEST: Byte = 1
    const val TYPE_RESPONSE: Byte = 2
    const val TYPE_NOTIFY: Byte = 3

    // TLV Tags
    const val TAG_POWER: Byte = 1
    const val TAG_FW_VERSION: Byte = 2
    const val TAG_BT_NAME: Byte = 3
    const val TAG_EQ_SETTING: Byte = 4
    const val TAG_KEY_SETTINGS: Byte = 5
    const val TAG_VOLUME: Byte = 6
    const val TAG_PLAY_STATE: Byte = 7
    const val TAG_WORK_MODE: Byte = 8
    const val TAG_IN_EAR_STATUS: Byte = 9
    const val TAG_ANC_MODE: Byte = 12
    const val TAG_TWS_CONNECTED: Byte = 14
    const val TAG_SPATIAL_AUDIO_MODE: Byte = 24
    const val TAG_MULTIPOINT_STATUS: Byte = 25
    const val TAG_BASS_ENGINE: Byte = 30
}

data class BluetrumPacket(
    val seq: Int,
    val command: Byte,
    val commandType: Byte,
    val frameIndex: Int,
    val totalFrames: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BluetrumPacket) return false
        return seq == other.seq &&
                command == other.command &&
                commandType == other.commandType &&
                frameIndex == other.frameIndex &&
                totalFrames == other.totalFrames &&
                payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = seq
        result = 31 * result + command
        result = 31 * result + commandType
        result = 31 * result + frameIndex
        result = 31 * result + totalFrames
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

class BluetrumProtocol {
    private var sequenceNumber: Byte = 0

    @Synchronized
    fun buildFrame(command: Byte, commandType: Byte, payload: ByteArray): ByteArray {
        val totalFrames = 1
        val frameIndex = 0
        val frameInfo = (((totalFrames - 1) shl 4) or (frameIndex and 0x0F)).toByte()

        val buffer = ByteBuffer.allocate(5 + payload.size)
        buffer.put(sequenceNumber)
        buffer.put(command)
        buffer.put(commandType)
        buffer.put(frameInfo)
        buffer.put(payload.size.toByte())
        buffer.put(payload)

        sequenceNumber = ((sequenceNumber + 1) and 0x0F).toByte()
        return buffer.array()
    }

    fun buildAncModeRequest(mode: AncMode): ByteArray =
        buildFrame(BluetrumConstants.CMD_ANC_MODE, BluetrumConstants.TYPE_REQUEST, byteArrayOf(mode.value))

    fun buildSpatialAudioRequest(mode: SpatialAudioMode): ByteArray =
        buildFrame(BluetrumConstants.CMD_SPATIAL_AUDIO, BluetrumConstants.TYPE_REQUEST, byteArrayOf(mode.value))

    fun buildRecenterCommand(): ByteArray {
        // Vendor HCI Recenter opcode packet: 01 E0 FC 02 F8 00
        return byteArrayOf(0x01, 0xE0.toByte(), 0xFC.toByte(), 0x02, 0xF8.toByte(), 0x00)
    }

    fun buildGameModeRequest(enabled: Boolean): ByteArray =
        buildFrame(BluetrumConstants.CMD_WORK_MODE, BluetrumConstants.TYPE_REQUEST, byteArrayOf(if (enabled) 1 else 0))

    fun buildInEarDetectRequest(enabled: Boolean): ByteArray =
        buildFrame(BluetrumConstants.CMD_IN_EAR, BluetrumConstants.TYPE_REQUEST, byteArrayOf(if (enabled) 1 else 0))

    fun buildEqRequest(gains: IntArray): ByteArray {
        // 10 bands: [numBands = 10, preset = 32 (custom), gain0..gain9]
        val payload = ByteArray(12)
        payload[0] = 10.toByte() // 10 bands
        payload[1] = 32.toByte() // Custom EQ profile index
        for (i in 0 until minOf(10, gains.size)) {
            payload[i + 2] = gains[i].toByte()
        }
        return buildFrame(BluetrumConstants.CMD_EQ, BluetrumConstants.TYPE_REQUEST, payload)
    }

    fun buildKeyRequest(gesture: TouchGesture, function: KeyFunction): ByteArray =
        buildFrame(BluetrumConstants.CMD_KEY, BluetrumConstants.TYPE_REQUEST, byteArrayOf(gesture.code, function.code))

    fun buildDeviceInfoQuery(): ByteArray {
        val tagsToQuery = byteArrayOf(
            BluetrumConstants.TAG_POWER,
            BluetrumConstants.TAG_ANC_MODE,
            BluetrumConstants.TAG_SPATIAL_AUDIO_MODE,
            BluetrumConstants.TAG_WORK_MODE,
            BluetrumConstants.TAG_EQ_SETTING,
            BluetrumConstants.TAG_IN_EAR_STATUS,
            BluetrumConstants.TAG_KEY_SETTINGS
        )
        return buildFrame(BluetrumConstants.CMD_DEVICE_INFO, BluetrumConstants.TYPE_REQUEST, tagsToQuery)
    }

    fun parseStream(bytes: ByteArray, onPacket: (BluetrumPacket) -> Unit) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.remaining() >= 5) {
            val seq = (buffer.get().toInt() and 0x0F)
            val cmd = buffer.get()
            val cmdType = buffer.get()
            val frameInfo = buffer.get().toInt() and 0xFF
            val frameIndex = frameInfo and 0x0F
            val totalFrames = ((frameInfo shr 4) and 0x0F) + 1
            val length = buffer.get().toInt() and 0xFF

            if (buffer.remaining() >= length) {
                val payload = ByteArray(length)
                buffer.get(payload)
                onPacket(BluetrumPacket(seq, cmd, cmdType, frameIndex, totalFrames, payload))
            } else {
                break
            }
        }
    }

    fun parseTlv(payload: ByteArray, onTlv: (tag: Byte, value: ByteArray) -> Unit) {
        val buffer = ByteBuffer.wrap(payload)
        while (buffer.remaining() >= 2) {
            val tag = buffer.get()
            val len = buffer.get().toInt() and 0xFF
            if (len <= buffer.remaining()) {
                val value = ByteArray(len)
                buffer.get(value)
                onTlv(tag, value)
            } else {
                break
            }
        }
    }
}
