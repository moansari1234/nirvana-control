package com.nirvana.control.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.nirvana.control.util.AppLog as Log
import com.nirvana.control.model.AncMode
import com.nirvana.control.model.ConnectionState
import com.nirvana.control.model.DeviceState
import com.nirvana.control.model.KeyFunction
import com.nirvana.control.model.SpatialAudioMode
import com.nirvana.control.model.TouchGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException

class BluetrumSppManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "BluetrumSppManager"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: BluetrumSppManager? = null

        fun getInstance(context: Context): BluetrumSppManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetrumSppManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val protocol = BluetrumProtocol()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var readerJob: Job? = null

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (_deviceState.value.connectionState == ConnectionState.CONNECTING ||
            _deviceState.value.connectionState == ConnectionState.CONNECTED
        ) {
            if (_deviceState.value.deviceAddress == device.address) return
            disconnect()
        }

        _deviceState.update {
            it.copy(
                connectionState = ConnectionState.CONNECTING,
                deviceName = device.name ?: "boAt Nirvana Space",
                deviceAddress = device.address
            )
        }

        scope.launch {
            try {
                Log.d(TAG, "Attempting SPP connection to  using default UUID...")
                var connectedSocket: BluetoothSocket? = null
                
                // Try Default SPP UUID first
                try {
                    connectedSocket = device.createRfcommSocketToServiceRecord(BluetrumConstants.DEFAULT_SPP_UUID)
                    connectedSocket.connect()
                } catch (e: Exception) {
                    Log.w(TAG, "Standard SPP failed: . Trying custom Bluetrum UUID...")
                    try {
                        connectedSocket = device.createRfcommSocketToServiceRecord(BluetrumConstants.CUSTOM_SPP_UUID)
                        connectedSocket.connect()
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fallback SPP failed: ")
                        throw e2
                    }
                }

                socket = connectedSocket
                inputStream = connectedSocket.inputStream
                outputStream = connectedSocket.outputStream

                _deviceState.update {
                    it.copy(connectionState = ConnectionState.CONNECTED)
                }

                Log.i(TAG, "Successfully connected to !")
                startReaderLoop()

                // Query full device state
                delay(300)
                refreshDeviceInfo()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect: ")
                disconnect()
            }
        }
    }

    fun disconnect() {
        readerJob?.cancel()
        readerJob = null

        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ")
        }

        inputStream = null
        outputStream = null
        socket = null

        _deviceState.update {
            it.copy(connectionState = ConnectionState.DISCONNECTED)
        }
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                val stream = inputStream ?: break
                try {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead > 0) {
                        val packetBytes = buffer.copyOf(bytesRead)
                        protocol.parseStream(packetBytes) { packet ->
                            handlePacket(packet)
                        }
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        Log.e(TAG, "Socket read error: ")
                        disconnect()
                    }
                    break
                }
            }
        }
    }

    private fun handlePacket(packet: BluetrumPacket) {
        when (packet.command) {
            BluetrumConstants.CMD_DEVICE_INFO,
            BluetrumConstants.CMD_NOTIFY -> {
                protocol.parseTlv(packet.payload) { tag, value ->
                    handleTlv(tag, value)
                }
            }
            BluetrumConstants.CMD_ANC_MODE -> {
                if (packet.payload.isNotEmpty()) {
                    val mode = AncMode.fromValue(packet.payload[0])
                    _deviceState.update { it.copy(ancMode = mode) }
                }
            }
            BluetrumConstants.CMD_SPATIAL_AUDIO -> {
                if (packet.payload.isNotEmpty()) {
                    val mode = SpatialAudioMode.fromValue(packet.payload[0])
                    _deviceState.update { it.copy(spatialAudioMode = mode) }
                }
            }
            BluetrumConstants.CMD_WORK_MODE -> {
                if (packet.payload.isNotEmpty()) {
                    val isGameMode = packet.payload[0].toInt() == 1
                    _deviceState.update { it.copy(gameMode = isGameMode) }
                }
            }
        }
    }

    private fun handleTlv(tag: Byte, value: ByteArray) {
        when (tag) {
            BluetrumConstants.TAG_POWER -> {
                if (value.size >= 3) {
                    val leftCharging = (value[0].toInt() and 0x80) != 0
                    val leftBattery = value[0].toInt() and 0x7F
                    val rightCharging = (value[1].toInt() and 0x80) != 0
                    val rightBattery = value[1].toInt() and 0x7F
                    val caseCharging = (value[2].toInt() and 0x80) != 0
                    val caseBattery = value[2].toInt() and 0x7F

                    _deviceState.update {
                        it.copy(
                            leftBattery = leftBattery,
                            leftCharging = leftCharging,
                            rightBattery = rightBattery,
                            rightCharging = rightCharging,
                            caseBattery = caseBattery,
                            caseCharging = caseCharging
                        )
                    }
                }
            }
            BluetrumConstants.TAG_ANC_MODE -> {
                if (value.isNotEmpty()) {
                    val mode = AncMode.fromValue(value[0])
                    _deviceState.update { it.copy(ancMode = mode) }
                }
            }
            BluetrumConstants.TAG_SPATIAL_AUDIO_MODE -> {
                if (value.isNotEmpty()) {
                    val mode = SpatialAudioMode.fromValue(value[0])
                    _deviceState.update { it.copy(spatialAudioMode = mode) }
                }
            }
            BluetrumConstants.TAG_WORK_MODE -> {
                if (value.isNotEmpty()) {
                    val isGame = value[0].toInt() == 1
                    _deviceState.update { it.copy(gameMode = isGame) }
                }
            }
            BluetrumConstants.TAG_IN_EAR_STATUS -> {
                if (value.isNotEmpty()) {
                    val inEar = value[0].toInt() == 1
                    _deviceState.update { it.copy(inEarDetection = inEar) }
                }
            }
            BluetrumConstants.TAG_EQ_SETTING -> {
                if (value.size >= 10) {
                    val gains = IntArray(10) { i -> value[i].toInt() }
                    _deviceState.update { it.copy(equalizerGains = gains) }
                }
            }
            BluetrumConstants.TAG_KEY_SETTINGS -> {
                // Key settings map
                val mappings = _deviceState.value.keyMappings.toMutableMap()
                var i = 0
                while (i + 1 < value.size) {
                    val gesture = TouchGesture.fromCode(value[i])
                    val function = KeyFunction.fromCode(value[i + 1])
                    if (gesture != null) {
                        mappings[gesture] = function
                    }
                    i += 2
                }
                _deviceState.update { it.copy(keyMappings = mappings) }
            }
        }
    }

    private fun sendBytes(bytes: ByteArray) {
        scope.launch {
            try {
                outputStream?.write(bytes)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error writing bytes: ")
            }
        }
    }

    fun setAncMode(mode: AncMode) {
        _deviceState.update { it.copy(ancMode = mode) }
        sendBytes(protocol.buildAncModeRequest(mode))
    }

    fun setSpatialAudioMode(mode: SpatialAudioMode) {
        _deviceState.update { it.copy(spatialAudioMode = mode) }
        sendBytes(protocol.buildSpatialAudioRequest(mode))
    }

    fun recenterHeadTracking() {
        sendBytes(protocol.buildRecenterCommand())
        _deviceState.update { it.copy(headYaw = 0f, headPitch = 0f) }
    }

    fun setGameMode(enabled: Boolean) {
        _deviceState.update { it.copy(gameMode = enabled) }
        sendBytes(protocol.buildGameModeRequest(enabled))
    }

    fun setEqualizerGains(gains: IntArray, presetName: String = "Custom") {
        _deviceState.update { it.copy(equalizerGains = gains, activePresetName = presetName) }
        sendBytes(protocol.buildEqRequest(gains))
    }

    fun setTouchGesture(gesture: TouchGesture, function: KeyFunction) {
        val updated = _deviceState.value.keyMappings.toMutableMap()
        updated[gesture] = function
        _deviceState.update { it.copy(keyMappings = updated) }
        sendBytes(protocol.buildKeyRequest(gesture, function))
    }

    fun setInEarDetection(enabled: Boolean) {
        _deviceState.update { it.copy(inEarDetection = enabled) }
        sendBytes(protocol.buildInEarDetectRequest(enabled))
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        _deviceState.update { it.copy(autoConnectEnabled = enabled) }
    }

    fun setAccidentalTouchGuard(enabled: Boolean) {
        _deviceState.update { it.copy(accidentalTouchGuard = enabled) }
        if (enabled) {
            // Disable single tap on both buds to prevent accidental touches
            setTouchGesture(TouchGesture.LEFT_SINGLE_TAP, KeyFunction.NONE)
            setTouchGesture(TouchGesture.RIGHT_SINGLE_TAP, KeyFunction.NONE)
        } else {
            // Restore default Play/Pause for single tap
            setTouchGesture(TouchGesture.LEFT_SINGLE_TAP, KeyFunction.PLAY_PAUSE)
            setTouchGesture(TouchGesture.RIGHT_SINGLE_TAP, KeyFunction.PLAY_PAUSE)
        }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        _deviceState.update { it.copy(isBluetoothEnabled = enabled) }
        if (!enabled) {
            disconnect()
        }
    }

    fun refreshDeviceInfo() {
        sendBytes(protocol.buildDeviceInfoQuery())
    }
}
