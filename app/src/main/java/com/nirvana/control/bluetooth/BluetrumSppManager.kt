package com.nirvana.control.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.nirvana.control.model.AncMode
import com.nirvana.control.model.ConnectionState
import com.nirvana.control.model.DeviceState
import com.nirvana.control.model.KeyFunction
import com.nirvana.control.model.SpatialAudioMode
import com.nirvana.control.model.TouchGesture
import com.nirvana.control.util.AppLog as Log
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
import java.util.UUID

class BluetrumSppManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "BluetrumSppManager"

        @Volatile
        private var instance: BluetrumSppManager? = null

        fun getInstance(context: Context): BluetrumSppManager {
            return instance ?: synchronized(this) {
                instance ?: BluetrumSppManager(context.applicationContext).also { instance = it }
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
    private var connectJob: Job? = null

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        val devName = device.name ?: "Nirvana Space"
        val devAddress = device.address

        Log.i(TAG, "connect() invoked for '$devName' [$devAddress]")

        if (_deviceState.value.connectionState == ConnectionState.CONNECTING ||
            _deviceState.value.connectionState == ConnectionState.CONNECTED
        ) {
            if (_deviceState.value.deviceAddress == devAddress && socket?.isConnected == true) {
                Log.i(TAG, "Already connected to $devName ($devAddress)")
                return
            }
            disconnect()
        }

        _deviceState.update {
            it.copy(
                connectionState = ConnectionState.CONNECTING,
                deviceName = devName,
                deviceAddress = devAddress
            )
        }

        connectJob?.cancel()
        connectJob = scope.launch {
            // 1. Cancel active discovery so RFCOMM connection bandwidth isn't starved
            try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = btManager?.adapter
                if (adapter?.isDiscovering == true) {
                    adapter.cancelDiscovery()
                    Log.d(TAG, "Cancelled ongoing Bluetooth discovery prior to connect")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check/cancel discovery: ${e.message}")
            }

            // 2. Fetch and log device SDP UUIDs
            try {
                device.fetchUuidsWithSdp()
                val uuids = device.uuids
                if (uuids != null && uuids.isNotEmpty()) {
                    Log.i(TAG, "Discovered ${uuids.size} SDP UUIDs on device:")
                    uuids.forEach { u -> Log.i(TAG, "  -> UUID: ${u.uuid}") }
                } else {
                    Log.d(TAG, "No cached SDP UUIDs on device yet.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching device UUIDs: ${e.message}")
            }

            // 3. Build candidate connection creators
            val connectionAttempts = mutableListOf<Pair<String, () -> BluetoothSocket>>()

            // Try any SDP UUIDs directly advertised by this device first
            val sdpUuids = device.uuids?.map { it.uuid } ?: emptyList()
            for (u in sdpUuids) {
                if (u != BluetrumConstants.CUSTOM_SPP_UUID && u != BluetrumConstants.DEFAULT_SPP_UUID) {
                    connectionAttempts.add("SDP Advertised UUID ($u)" to {
                        device.createInsecureRfcommSocketToServiceRecord(u)
                    })
                }
            }

            // Insecure & Secure with Custom Bluetrum UUID (Official boAt Nirvana Space UUID!)
            connectionAttempts.add("Insecure Custom SPP (${BluetrumConstants.CUSTOM_SPP_UUID})" to {
                device.createInsecureRfcommSocketToServiceRecord(BluetrumConstants.CUSTOM_SPP_UUID)
            })
            connectionAttempts.add("Insecure Default SPP (${BluetrumConstants.DEFAULT_SPP_UUID})" to {
                device.createInsecureRfcommSocketToServiceRecord(BluetrumConstants.DEFAULT_SPP_UUID)
            })
            connectionAttempts.add("Secure Custom SPP (${BluetrumConstants.CUSTOM_SPP_UUID})" to {
                device.createRfcommSocketToServiceRecord(BluetrumConstants.CUSTOM_SPP_UUID)
            })
            connectionAttempts.add("Secure Default SPP (${BluetrumConstants.DEFAULT_SPP_UUID})" to {
                device.createRfcommSocketToServiceRecord(BluetrumConstants.DEFAULT_SPP_UUID)
            })

            // Reflection fallback on standard RFCOMM channels
            for (channel in 1..3) {
                connectionAttempts.add("Reflection Insecure Channel $channel" to {
                    val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                    m.invoke(device, channel) as BluetoothSocket
                })
                connectionAttempts.add("Reflection Secure Channel $channel" to {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    m.invoke(device, channel) as BluetoothSocket
                })
            }

            var connectedSocket: BluetoothSocket? = null
            var connectedMethod = ""

            for ((methodName, creator) in connectionAttempts) {
                var testSocket: BluetoothSocket? = null
                try {
                    Log.d(TAG, "Attempting connection via $methodName...")
                    testSocket = creator()
                    testSocket.connect()
                    connectedSocket = testSocket
                    connectedMethod = methodName
                    Log.i(TAG, ">>> SUCCESS! Connected to $devName via $methodName <<<")
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Method '$methodName' failed: ${e.javaClass.simpleName} - ${e.message}")
                    try {
                        testSocket?.close()
                    } catch (ignored: Exception) {}
                    try {
                        delay(150)
                    } catch (ignored: Exception) {}
                }
            }

            if (connectedSocket == null) {
                Log.e(TAG, "All ${connectionAttempts.size} socket connection attempts failed for $devName [$devAddress].")
                disconnect()
                return@launch
            }

            try {
                socket = connectedSocket
                inputStream = connectedSocket.inputStream
                outputStream = connectedSocket.outputStream

                _deviceState.update {
                    it.copy(connectionState = ConnectionState.CONNECTED)
                }

                Log.i(TAG, "Socket streams established. Starting reader loop...")
                startReaderLoop()

                // Query full device state from firmware
                delay(300)
                Log.i(TAG, "Sending initial Device Info Query...")
                refreshDeviceInfo()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing socket streams: ${e.message}", e)
                disconnect()
            }
        }
    }

    fun disconnect() {
        Log.i(TAG, "Disconnecting RFCOMM socket...")
        connectJob?.cancel()
        connectJob = null

        readerJob?.cancel()
        readerJob = null

        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }

        inputStream = null
        outputStream = null
        socket = null

        _deviceState.update {
            it.copy(connectionState = ConnectionState.DISCONNECTED)
        }
        Log.i(TAG, "Disconnected successfully.")
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = ByteArray(1024)
            Log.d(TAG, "Reader loop active.")
            while (isActive) {
                val stream = inputStream ?: break
                try {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead > 0) {
                        val packetBytes = buffer.copyOf(bytesRead)
                        val hex = packetBytes.joinToString(" ") { "%02X".format(it) }
                        Log.d(TAG, "RX RAW [$bytesRead bytes]: $hex")
                        protocol.parseStream(packetBytes) { packet ->
                            handlePacket(packet)
                        }
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        Log.e(TAG, "Socket read error: ${e.message}")
                        disconnect()
                    }
                    break
                }
            }
        }
    }

    private fun handlePacket(packet: BluetrumPacket) {
        Log.d(TAG, "Received packet Cmd=${packet.command}, Type=${packet.commandType}, PayloadLen=${packet.payload.size}")
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
                    Log.i(TAG, "ANC Mode updated: $mode")
                    _deviceState.update { it.copy(ancMode = mode) }
                }
            }
            BluetrumConstants.CMD_SPATIAL_AUDIO -> {
                if (packet.payload.isNotEmpty()) {
                    val mode = SpatialAudioMode.fromValue(packet.payload[0])
                    Log.i(TAG, "Spatial Audio Mode updated: $mode")
                    _deviceState.update { it.copy(spatialAudioMode = mode) }
                }
            }
            BluetrumConstants.CMD_WORK_MODE -> {
                if (packet.payload.isNotEmpty()) {
                    val isGameMode = packet.payload[0].toInt() == 1
                    Log.i(TAG, "Game Mode updated: $isGameMode")
                    _deviceState.update { it.copy(gameMode = isGameMode) }
                }
            }
            BluetrumConstants.CMD_EQ -> {
                if (packet.payload.size >= 10) {
                    val gains = IntArray(10) { packet.payload[it].toInt() }
                    Log.i(TAG, "EQ Gains updated: ${gains.joinToString()}")
                    _deviceState.update { it.copy(equalizerGains = gains) }
                }
            }
        }
    }

    private fun handleTlv(tag: Byte, value: ByteArray) {
        Log.d(TAG, "TLV Tag=0x%02X, Len=${value.size}".format(tag))
        when (tag) {
            BluetrumConstants.TAG_POWER -> {
                val left = protocol.parseBatteryByte(value.getOrElse(0) { 0 })
                val right = protocol.parseBatteryByte(value.getOrElse(1) { 0 })
                val case = protocol.parseBatteryByte(value.getOrElse(2) { 0 })
                Log.i(TAG, "Battery Telemetry: L=${left.first}% (charging=${left.second}), R=${right.first}% (charging=${right.second}), Case=${case.first}% (charging=${case.second})")
                _deviceState.update {
                    it.copy(
                        leftBattery = left.first,
                        leftCharging = left.second,
                        rightBattery = right.first,
                        rightCharging = right.second,
                        caseBattery = case.first,
                        caseCharging = case.second
                    )
                }
            }
            BluetrumConstants.TAG_ANC_MODE -> {
                if (value.isNotEmpty()) {
                    val mode = AncMode.fromValue(value[0])
                    Log.i(TAG, "TLV ANC Mode: $mode")
                    _deviceState.update { it.copy(ancMode = mode) }
                }
            }
            BluetrumConstants.TAG_SPATIAL_AUDIO_MODE -> {
                if (value.isNotEmpty()) {
                    val mode = SpatialAudioMode.fromValue(value[0])
                    Log.i(TAG, "TLV Spatial Audio: $mode")
                    _deviceState.update { it.copy(spatialAudioMode = mode) }
                }
            }
            BluetrumConstants.TAG_WORK_MODE -> {
                if (value.isNotEmpty()) {
                    val isGame = value[0].toInt() == 1
                    Log.i(TAG, "TLV Game Mode: $isGame")
                    _deviceState.update { it.copy(gameMode = isGame) }
                }
            }
            BluetrumConstants.TAG_IN_EAR_STATUS -> {
                if (value.isNotEmpty()) {
                    val inEar = value[0].toInt() == 1
                    Log.i(TAG, "TLV In-Ear Status: $inEar")
                    _deviceState.update { it.copy(inEarDetection = inEar) }
                }
            }
            BluetrumConstants.TAG_EQ_SETTING -> {
                if (value.size >= 10) {
                    val gains = IntArray(10) { value[it].toInt() }
                    Log.i(TAG, "TLV EQ: ${gains.joinToString()}")
                    _deviceState.update { it.copy(equalizerGains = gains) }
                }
            }
            BluetrumConstants.TAG_KEY_SETTINGS -> {
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
                Log.i(TAG, "TLV Key Settings updated (${mappings.size} gestures)")
                _deviceState.update { it.copy(keyMappings = mappings) }
            }
        }
    }

    private fun sendBytes(bytes: ByteArray) {
        scope.launch {
            try {
                val hex = bytes.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "TX RAW [${bytes.size} bytes]: $hex")
                outputStream?.write(bytes)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error writing bytes: ${e.message}", e)
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
        Log.i(TAG, "Sending IMU Recenter Opcode...")
        sendBytes(protocol.buildRecenterCommand())
    }

    fun setGameMode(enabled: Boolean) {
        _deviceState.update { it.copy(gameMode = enabled) }
        sendBytes(protocol.buildGameModeRequest(enabled))
    }

    fun setInEarDetection(enabled: Boolean) {
        _deviceState.update { it.copy(inEarDetection = enabled) }
        sendBytes(protocol.buildInEarDetectRequest(enabled))
    }

    fun setEqualizerGains(gains: IntArray, presetName: String = "Custom") {
        _deviceState.update { it.copy(equalizerGains = gains.clone(), activePresetName = presetName) }
        sendBytes(protocol.buildEqRequest(gains))
    }

    fun setTouchGesture(gesture: TouchGesture, function: KeyFunction) {
        val updated = _deviceState.value.keyMappings.toMutableMap()
        updated[gesture] = function
        _deviceState.update { it.copy(keyMappings = updated) }
        sendBytes(protocol.buildKeyRequest(gesture, function))
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        _deviceState.update { it.copy(autoConnectEnabled = enabled) }
    }

    fun setAccidentalTouchGuard(enabled: Boolean) {
        _deviceState.update { it.copy(accidentalTouchGuard = enabled) }
        if (enabled) {
            setTouchGesture(TouchGesture.LEFT_SINGLE_TAP, KeyFunction.NONE)
            setTouchGesture(TouchGesture.RIGHT_SINGLE_TAP, KeyFunction.NONE)
        } else {
            setTouchGesture(TouchGesture.LEFT_SINGLE_TAP, KeyFunction.PLAY_PAUSE)
            setTouchGesture(TouchGesture.RIGHT_SINGLE_TAP, KeyFunction.PLAY_PAUSE)
        }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        Log.i(TAG, "setBluetoothEnabled: $enabled")
        _deviceState.update { it.copy(isBluetoothEnabled = enabled) }
        if (!enabled) {
            disconnect()
        }
    }

    fun refreshDeviceInfo() {
        sendBytes(protocol.buildDeviceInfoQuery())
    }
}
