package com.nirvana.control.model

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}

enum class AncMode(val value: Byte, val label: String) {
    OFF(0, "Normal"),
    ANC_ON(1, "Noise Cancelling"),
    TRANSPARENCY(2, "Transparency");

    companion object {
        fun fromValue(value: Byte): AncMode = entries.find { it.value == value } ?: OFF
    }
}

enum class SpatialAudioMode(val value: Byte, val label: String) {
    OFF(0, "Off"),
    FIXED(1, "Fixed 3D"),
    HEAD_TRACKING(2, "Head Tracking");

    companion object {
        fun fromValue(value: Byte): SpatialAudioMode = entries.find { it.value == value } ?: OFF
    }
}

enum class TouchGesture(val code: Byte, val label: String, val isLeft: Boolean) {
    LEFT_SINGLE_TAP(1, "Left Single Tap", true),
    RIGHT_SINGLE_TAP(2, "Right Single Tap", false),
    LEFT_DOUBLE_TAP(3, "Left Double Tap", true),
    RIGHT_DOUBLE_TAP(4, "Right Double Tap", false),
    LEFT_TRIPLE_TAP(5, "Left Triple Tap", true),
    RIGHT_TRIPLE_TAP(6, "Right Triple Tap", false),
    LEFT_LONG_PRESS(7, "Left Long Press", true),
    RIGHT_LONG_PRESS(8, "Right Long Press", false);

    companion object {
        fun fromCode(code: Byte): TouchGesture? = entries.find { it.code == code }
    }
}

enum class KeyFunction(val code: Byte, val label: String) {
    NONE(0, "Disable / None"),
    RECALL(1, "Redial / Recall"),
    VOICE_ASSISTANT(2, "Voice Assistant"),
    PREVIOUS_TRACK(3, "Previous Track"),
    NEXT_TRACK(4, "Next Track"),
    VOLUME_UP(5, "Volume Up"),
    VOLUME_DOWN(6, "Volume Down"),
    PLAY_PAUSE(7, "Play / Pause"),
    BEAST_MODE(8, "BEAST™ Mode Toggle"),
    ANC_MODE(9, "Cycle ANC Modes"),
    SPATIAL_AUDIO(10, "Toggle Spatial Audio"),
    QUICK_SWITCH(11, "Quick Switch Device");

    companion object {
        fun fromCode(code: Byte): KeyFunction = entries.find { it.code == code } ?: NONE
    }
}

data class EqPreset(
    val name: String,
    val gains: IntArray // 10 bands (-6 to +6 dB)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqPreset) return false
        return name == other.name && gains.contentEquals(other.gains)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + gains.contentHashCode()
}

val DEFAULT_EQ_PRESETS = listOf(
    EqPreset("Signature", intArrayOf(1, 3, 2, 1, 0, 0, 1, 2, 3, 2)),
    EqPreset("Balanced", intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    EqPreset("Bass Boost", intArrayOf(5, 4, 3, 2, 1, 0, 0, 0, 0, 0)),
    EqPreset("Treble Boost", intArrayOf(0, 0, 0, 0, 0, 1, 2, 3, 4, 5)),
    EqPreset("Pop", intArrayOf(-1, 1, 2, 3, 2, 0, -1, 1, 2, 3)),
    EqPreset("Rock", intArrayOf(4, 3, 1, 0, -1, 0, 1, 2, 3, 4)),
    EqPreset("Club", intArrayOf(3, 4, 2, 1, 0, 0, 1, 2, 2, 0)),
    EqPreset("Jazz", intArrayOf(2, 3, 1, 2, -1, -1, 0, 1, 2, 3))
)

val EQ_BAND_FREQUENCIES = intArrayOf(31, 63, 125, 250, 400, 1000, 2000, 4000, 8000, 16000)

val EQ_BAND_LABELS = listOf("31Hz", "63Hz", "125Hz", "250Hz", "400Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

data class DeviceState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val deviceName: String = "boAt Nirvana Space",
    val deviceAddress: String = "",
    val leftBattery: Int = -1,
    val leftCharging: Boolean = false,
    val rightBattery: Int = -1,
    val rightCharging: Boolean = false,
    val caseBattery: Int = -1,
    val caseCharging: Boolean = false,
    val ancMode: AncMode = AncMode.OFF,
    val spatialAudioMode: SpatialAudioMode = SpatialAudioMode.OFF,
    val gameMode: Boolean = false,
    val inEarDetection: Boolean = true,
    val equalizerGains: IntArray = IntArray(10) { 0 },
    val activePresetName: String = "Balanced",
    val keyMappings: Map<TouchGesture, KeyFunction> = mapOf(
        TouchGesture.LEFT_DOUBLE_TAP to KeyFunction.PREVIOUS_TRACK,
        TouchGesture.RIGHT_DOUBLE_TAP to KeyFunction.NEXT_TRACK,
        TouchGesture.LEFT_TRIPLE_TAP to KeyFunction.VOICE_ASSISTANT,
        TouchGesture.RIGHT_TRIPLE_TAP to KeyFunction.BEAST_MODE,
        TouchGesture.LEFT_LONG_PRESS to KeyFunction.ANC_MODE,
        TouchGesture.RIGHT_LONG_PRESS to KeyFunction.ANC_MODE
    ),
    val headYaw: Float = 0f,
    val headPitch: Float = 0f,
    val autoConnectEnabled: Boolean = true,
    val accidentalTouchGuard: Boolean = false
) {
    val isConnected: Boolean get() = connectionState == ConnectionState.CONNECTED

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceState) return false
        return connectionState == other.connectionState &&
                deviceName == other.deviceName &&
                deviceAddress == other.deviceAddress &&
                leftBattery == other.leftBattery &&
                leftCharging == other.leftCharging &&
                rightBattery == other.rightBattery &&
                rightCharging == other.rightCharging &&
                caseBattery == other.caseBattery &&
                caseCharging == other.caseCharging &&
                ancMode == other.ancMode &&
                spatialAudioMode == other.spatialAudioMode &&
                gameMode == other.gameMode &&
                inEarDetection == other.inEarDetection &&
                equalizerGains.contentEquals(other.equalizerGains) &&
                activePresetName == other.activePresetName &&
                keyMappings == other.keyMappings &&
                headYaw == other.headYaw &&
                headPitch == other.headPitch &&
                autoConnectEnabled == other.autoConnectEnabled &&
                accidentalTouchGuard == other.accidentalTouchGuard
    }

    override fun hashCode(): Int {
        var result = connectionState.hashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + deviceAddress.hashCode()
        result = 31 * result + leftBattery
        result = 31 * result + leftCharging.hashCode()
        result = 31 * result + rightBattery
        result = 31 * result + rightCharging.hashCode()
        result = 31 * result + caseBattery
        result = 31 * result + caseCharging.hashCode()
        result = 31 * result + ancMode.hashCode()
        result = 31 * result + spatialAudioMode.hashCode()
        result = 31 * result + gameMode.hashCode()
        result = 31 * result + inEarDetection.hashCode()
        result = 31 * result + equalizerGains.contentHashCode()
        result = 31 * result + activePresetName.hashCode()
        result = 31 * result + keyMappings.hashCode()
        result = 31 * result + headYaw.hashCode()
        result = 31 * result + headPitch.hashCode()
        result = 31 * result + autoConnectEnabled.hashCode()
        result = 31 * result + accidentalTouchGuard.hashCode()
        return result
    }
}

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isPaired: Boolean,
    val isNirvana: Boolean
)
