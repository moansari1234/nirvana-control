package com.nirvana.control

import com.nirvana.control.bluetooth.BluetrumConstants
import com.nirvana.control.bluetooth.BluetrumProtocol
import com.nirvana.control.model.AncMode
import com.nirvana.control.model.KeyFunction
import com.nirvana.control.model.SpatialAudioMode
import com.nirvana.control.model.TouchGesture
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BluetrumProtocolTest {
    private lateinit var protocol: BluetrumProtocol

    @Before
    fun setUp() {
        protocol = BluetrumProtocol()
    }

    @Test
    fun testBuildAncModeRequest() {
        val bytes = protocol.buildAncModeRequest(AncMode.ANC_ON)
        assertEquals(6, bytes.size) // 5 header + 1 payload
        assertEquals(0x00, bytes[0].toInt()) // seq 0
        assertEquals(BluetrumConstants.CMD_ANC_MODE, bytes[1])
        assertEquals(BluetrumConstants.TYPE_REQUEST, bytes[2])
        assertEquals(0x00, bytes[3].toInt()) // frameInfo: total 1, index 0
        assertEquals(1, bytes[4].toInt()) // length
        assertEquals(AncMode.ANC_ON.value, bytes[5])
    }

    @Test
    fun testBuildSpatialAudioRequest() {
        val bytes = protocol.buildSpatialAudioRequest(SpatialAudioMode.HEAD_TRACKING)
        assertEquals(6, bytes.size)
        assertEquals(BluetrumConstants.CMD_SPATIAL_AUDIO, bytes[1])
        assertEquals(SpatialAudioMode.HEAD_TRACKING.value, bytes[5])
    }

    @Test
    fun testBuildRecenterCommand() {
        val bytes = protocol.buildRecenterCommand()
        assertEquals(6, bytes.size)
        assertArrayEquals(
            byteArrayOf(0x01, 0xE0.toByte(), 0xFC.toByte(), 0x02, 0xF8.toByte(), 0x00),
            bytes
        )
    }

    @Test
    fun testBuildEqRequest() {
        val gains = intArrayOf(2, 3, 1, 0, -1, 0, 1, 2, 3, 4)
        val bytes = protocol.buildEqRequest(gains)
        assertEquals(5 + 12, bytes.size) // 5 header + 12 payload
        assertEquals(BluetrumConstants.CMD_EQ, bytes[1])
        assertEquals(12, bytes[4].toInt()) // payload length
        assertEquals(10, bytes[5].toInt()) // 10 bands
        assertEquals(32, bytes[6].toInt()) // Custom preset index
        assertEquals(2, bytes[7].toInt()) // band 0
        assertEquals(-1, bytes[11].toInt()) // band 4
    }

    @Test
    fun testBuildKeyRequest() {
        val bytes = protocol.buildKeyRequest(TouchGesture.LEFT_DOUBLE_TAP, KeyFunction.NEXT_TRACK)
        assertEquals(7, bytes.size) // 5 header + 2 payload
        assertEquals(BluetrumConstants.CMD_KEY, bytes[1])
        assertEquals(TouchGesture.LEFT_DOUBLE_TAP.code, bytes[5])
        assertEquals(KeyFunction.NEXT_TRACK.code, bytes[6])
    }

    @Test
    fun testParseTlvBatteryAndCharging() {
        // Tag 1 (power), len 3, Left=85% (0x55), Right=90%+Charging (0x5A | 0x80 = 0xDA), Case=100% (0x64)
        val payload = byteArrayOf(
            BluetrumConstants.TAG_POWER, 3,
            0x55, 0xDA.toByte(), 0x64,
            BluetrumConstants.TAG_ANC_MODE, 1,
            AncMode.ANC_ON.value
        )

        var parsedLeftBattery = -1
        var parsedRightBattery = -1
        var parsedRightCharging = false
        var parsedCaseBattery = -1
        var parsedAncMode: AncMode? = null

        protocol.parseTlv(payload) { tag, value ->
            when (tag) {
                BluetrumConstants.TAG_POWER -> {
                    parsedLeftBattery = value[0].toInt() and 0x7F
                    parsedRightCharging = (value[1].toInt() and 0x80) != 0
                    parsedRightBattery = value[1].toInt() and 0x7F
                    parsedCaseBattery = value[2].toInt() and 0x7F
                }
                BluetrumConstants.TAG_ANC_MODE -> {
                    parsedAncMode = AncMode.fromValue(value[0])
                }
            }
        }

        assertEquals(85, parsedLeftBattery)
        assertEquals(90, parsedRightBattery)
        assertTrue(parsedRightCharging)
        assertEquals(100, parsedCaseBattery)
        assertEquals(AncMode.ANC_ON, parsedAncMode)
    }
}
