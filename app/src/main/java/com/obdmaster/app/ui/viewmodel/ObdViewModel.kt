package com.obdmaster.app.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.obdmaster.app.core.protocol.DtcItem
import com.obdmaster.app.core.protocol.DtcService
import com.obdmaster.app.core.protocol.Elm327Protocol
import com.obdmaster.app.core.protocol.ObdPid
import com.obdmaster.app.core.protocol.VehicleInfo
import com.obdmaster.app.core.protocol.VehicleInfoService
import com.obdmaster.app.core.transport.BluetoothSppTransport
import com.obdmaster.app.core.transport.ObdTransport
import com.obdmaster.app.core.transport.WifiTcpTransport
import com.obdmaster.app.data.AppSettings
import com.obdmaster.app.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    data class Connected(val adapterInfo: String, val protocol: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

enum class ConnectionType {
    BLUETOOTH,
    WIFI
}

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val _appSettings = MutableStateFlow(settingsRepo.loadSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    fun updateSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        settingsRepo.saveSettings(newSettings)
    }

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.BLUETOOTH)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val _pids = MutableStateFlow(ObdPid.getAllPids())
    val pids: StateFlow<List<ObdPid>> = _pids.asStateFlow()

    private val _chartPid = MutableStateFlow<ObdPid>(_pids.value.first())
    val chartPid: StateFlow<ObdPid> = _chartPid.asStateFlow()

    private val _chartHistory = MutableStateFlow<List<Float>>(emptyList())
    val chartHistory: StateFlow<List<Float>> = _chartHistory.asStateFlow()

    private val _dtcList = MutableStateFlow<List<DtcItem>>(emptyList())
    val dtcList: StateFlow<List<DtcItem>> = _dtcList.asStateFlow()

    private val _isDtcScanning = MutableStateFlow(false)
    val isDtcScanning: StateFlow<Boolean> = _isDtcScanning.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("OBD-Master v1.2.0 инициализирован."))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice.asStateFlow()

    private val _vehicleInfo = MutableStateFlow(VehicleInfo())
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private var activeTransport: ObdTransport? = null
    private var protocol: Elm327Protocol? = null
    private var dtcService: DtcService? = null
    private var vehicleInfoService: VehicleInfoService? = null
    private var pollingJob: Job? = null

    init {
        refreshPairedDevices()
    }

    fun setConnectionType(type: ConnectionType) {
        _connectionType.value = type
    }

    fun selectDevice(device: BluetoothDevice) {
        _selectedDevice.value = device
    }

    fun selectChartPid(pid: ObdPid) {
        _chartPid.value = pid
        _chartHistory.value = emptyList()
    }

    fun refreshPairedDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            try {
                val devices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                _pairedDevices.value = devices
                if (_selectedDevice.value == null && devices.isNotEmpty()) {
                    val elm = devices.find { it.name?.contains("OBD", ignoreCase = true) == true || it.name?.contains("ELM", ignoreCase = true) == true }
                    _selectedDevice.value = elm ?: devices.first()
                }
            } catch (e: SecurityException) {
                _pairedDevices.value = emptyList()
            }
        }
    }

    fun connectBluetooth(device: BluetoothDevice) {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting
            appendTerminal("Подключение к Bluetooth: ${device.name} (${device.address})...")

            val transport = BluetoothSppTransport(device)
            val success = transport.connect()

            if (success) {
                activeTransport = transport
                setupProtocol(transport)
            } else {
                _connectionStatus.value = ConnectionStatus.Error("Не удалось подключиться к Bluetooth устройству")
                appendTerminal("Ошибка подключения к Bluetooth.")
            }
        }
    }

    fun connectWifi(ip: String = "192.168.0.10", port: Int = 35000) {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting
            appendTerminal("Подключение к Wi-Fi: $ip:$port...")

            val transport = WifiTcpTransport(ip, port)
            val success = transport.connect()

            if (success) {
                activeTransport = transport
                setupProtocol(transport)
            } else {
                _connectionStatus.value = ConnectionStatus.Error("Не удалось подключиться по Wi-Fi к $ip:$port")
                appendTerminal("Ошибка Wi-Fi сокета.")
            }
        }
    }

    private suspend fun setupProtocol(transport: ObdTransport) {
        appendTerminal("Инициализация ELM327 протокола...")
        val proto = Elm327Protocol(transport)
        protocol = proto
        dtcService = DtcService(proto)
        vehicleInfoService = VehicleInfoService(proto)

        val connected = proto.initialize()
        if (connected) {
            _connectionStatus.value = ConnectionStatus.Connected(
                adapterInfo = proto.batteryVoltage,
                protocol = proto.detectedProtocol
            )
            appendTerminal("Подключено! Протокол: ${proto.detectedProtocol}, АКБ: ${proto.batteryVoltage}")
            startPolling()
            loadVehicleInfo()
        } else {
            _connectionStatus.value = ConnectionStatus.Error("ELM327 ответил, но связь с ЭБУ не установлена (зажигание включено?)")
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        activeTransport?.disconnect()
        activeTransport = null
        protocol = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        appendTerminal("Отключено от адаптера.")
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            // Real-time critical dashboard PIDs:
            // 0C = RPM, 0D = Speed, 04 = Engine Load, 11 = Throttle
            val fastPidHexes = setOf("0C", "0D", "04", "11")
            var slowPidIndex = 0

            while (isActive && activeTransport?.isConnected == true) {
                val proto = protocol ?: break
                val allPids = _pids.value

                // Filter supported PIDs (auto-excludes unsupported ones that cause timeouts)
                val supportedPids = allPids.filter { it.isSupported }
                val currentChartHex = _chartPid.value.pidHex

                val cyclePids = mutableListOf<ObdPid>()

                // 1. Fast PIDs + chart PID (queried every cycle)
                for (p in supportedPids) {
                    if (p.pidHex in fastPidHexes || p.pidHex == currentChartHex) {
                        cyclePids.add(p)
                    }
                }

                // 2. Round-robin: exactly 1 secondary PID per cycle (Coolant, Fuel, Voltage, Temps...)
                val slowPids = supportedPids.filter { it.pidHex !in fastPidHexes && it.pidHex != currentChartHex }
                if (slowPids.isNotEmpty()) {
                    val slowPid = slowPids[slowPidIndex % slowPids.size]
                    cyclePids.add(slowPid)
                    slowPidIndex++
                }

                // 3. Query each PID and update UI IMMEDIATELY
                for (pid in cyclePids) {
                    if (!isActive) break
                    val rawResp = proto.sendCommand("01 ${pid.pidHex}")
                    val success = pid.decode(rawResp)
                    if (success) {
                        if (pid.pidHex == _chartPid.value.pidHex) {
                            val currentList = _chartHistory.value.toMutableList()
                            if (currentList.size > 50) currentList.removeAt(0)
                            currentList.add(pid.currentValue)
                            _chartHistory.value = currentList
                        }
                        // Emit new list instance immediately on every packet so UI reacts with zero lag!
                        _pids.value = ArrayList(allPids)
                    }
                }

                val interval = _appSettings.value.pollingIntervalMs.coerceAtLeast(10L)
                delay(interval)
            }
        }
    }

    fun scanDtcs() {
        viewModelScope.launch {
            val service = dtcService ?: return@launch
            _isDtcScanning.value = true
            appendTerminal("Запуск сканирования кодов ошибок DTC...")

            try {
                val stored = service.readStoredDtcs()
                val pending = service.readPendingDtcs()
                val permanent = service.readPermanentDtcs()

                val all = (stored + pending + permanent).distinctBy { it.code }
                _dtcList.value = all
                appendTerminal("Сканирование завершено: найдено ошибок: ${all.size}")
            } catch (e: Exception) {
                appendTerminal("Ошибка сканирования DTC: ${e.localizedMessage}")
            } finally {
                _isDtcScanning.value = false
            }
        }
    }

    fun clearDtcs(onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val service = dtcService ?: return@launch
            appendTerminal("Отправка команды сброса кодов ошибок (Mode 04)...")
            val success = service.clearDtcs()
            if (success) {
                _dtcList.value = emptyList()
                appendTerminal("Команды сброса выполнены успешно!")
            } else {
                appendTerminal("Не удалось сбросить ошибки.")
            }
            onCompleted(success)
        }
    }

    fun loadVehicleInfo() {
        viewModelScope.launch {
            val service = vehicleInfoService ?: return@launch
            try {
                val info = service.readVehicleInfo()
                _vehicleInfo.value = info
                appendTerminal("Данные авто загружены: VIN=${info.vin}")
            } catch (e: Exception) {
                appendTerminal("Не удалось прочитать VIN: ${e.localizedMessage}")
            }
        }
    }

    fun sendTerminalCommand(cmd: String) {
        viewModelScope.launch {
            val proto = protocol
            if (proto == null || activeTransport?.isConnected != true) {
                appendTerminal("Ошибка: нет подключения к адаптеру!")
                return@launch
            }

            appendTerminal("> $cmd")
            val resp = proto.sendCommand(cmd)
            appendTerminal(resp.ifBlank { "OK (пустой ответ)" })
        }
    }

    private fun appendTerminal(msg: String) {
        val current = _terminalLogs.value.toMutableList()
        if (current.size > 200) current.removeAt(0)
        current.add(msg)
        _terminalLogs.value = current
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
