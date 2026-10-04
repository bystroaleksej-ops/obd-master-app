package com.obdmaster.app.ui.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data class Connecting(val message: String = "Подключение...") : ConnectionStatus()
    data class Connected(val adapterInfo: String, val protocol: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus() {
        val error: String get() = message
    }
sealed class AutoTestUiState {
    data object Idle : AutoTestUiState()
    data class Running(val step: String, val progress: Float) : AutoTestUiState()
    data class Completed(val report: com.obdmaster.app.core.protocol.AutoTestReport) : AutoTestUiState()
}

enum class ConnectionType {
    BLUETOOTH,
    WIFI
}

class ObdViewModel : ViewModel() {

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _autoTestState = MutableStateFlow<AutoTestUiState>(AutoTestUiState.Idle)
    val autoTestState: StateFlow<AutoTestUiState> = _autoTestState.asStateFlow()

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

    private val _vehicleInfo = MutableStateFlow(VehicleInfo())
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("Терминал готов к отправке команд."))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _appSettings = MutableStateFlow(com.obdmaster.app.data.AppSettings())
    val appSettings: StateFlow<com.obdmaster.app.data.AppSettings> = _appSettings.asStateFlow()

    fun updateSettings(newSettings: com.obdmaster.app.data.AppSettings) {
        _appSettings.value = newSettings
    }

    fun togglePidSelection(pidHex: String) {
        val current = _appSettings.value.selectedPidHexes.toMutableSet()
        if (current.contains(pidHex)) {
            if (current.size > 1) { // keep at least 1 sensor active
                current.remove(pidHex)
            }
        } else {
            current.add(pidHex)
        }
        updateSettings(_appSettings.value.copy(selectedPidHexes = current))
    }

    fun setPidSelectionPreset(presetHexes: Set<String>) {
        updateSettings(_appSettings.value.copy(selectedPidHexes = presetHexes))
    }

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

    fun setChartPid(pid: ObdPid) {
        _chartPid.value = pid
        _chartHistory.value = emptyList()
    }

    fun selectChartPid(pid: ObdPid) = setChartPid(pid)

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                _pairedDevices.value = adapter.bondedDevices.toList()
            }
        } catch (e: Exception) {
            _pairedDevices.value = emptyList()
        }
    }

    fun connectBluetooth(device: BluetoothDevice) {
        viewModelScope.launch {
            disconnect()
            _connectionStatus.value = ConnectionStatus.Connecting("Подключение к ${device.name ?: device.address}...")

            val transport = BluetoothSppTransport(device)
            activeTransport = transport

            if (!transport.connect()) {
                _connectionStatus.value = ConnectionStatus.Error("Не удалось открыть Bluetooth-сокет SPP")
                return@launch
            }

            initializeProtocol(transport)
        }
    }

    fun connectWifi(host: String, port: Int) {
        viewModelScope.launch {
            disconnect()
            _connectionStatus.value = ConnectionStatus.Connecting("Подключение к $host:$port...")

            val transport = WifiTcpTransport(host, port)
            activeTransport = transport

            if (!transport.connect()) {
                _connectionStatus.value = ConnectionStatus.Error("Не удалось подключиться к Wi-Fi адаптеру ($host:$port)")
                return@launch
            }

            initializeProtocol(transport)
        }
    }

    private suspend fun initializeProtocol(transport: ObdTransport) {
        _connectionStatus.value = ConnectionStatus.Connecting("Инициализация ELM327 и определение протокола...")
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
            while (isActive && activeTransport?.isConnected == true) {
                val proto = protocol ?: break
                val allPids = _pids.value
                val selectedHexes = _appSettings.value.selectedPidHexes
                val currentChartHex = _chartPid.value.pidHex

                // Only poll sensors selected by the user (and supported)
                // Plus the active chart sensor if viewing the Charts screen
                val cyclePids = allPids.filter { 
                    it.isSupported && (it.pidHex in selectedHexes || it.pidHex == currentChartHex) 
                }

                if (cyclePids.isEmpty()) {
                    delay(200)
                    continue
                }

                // Query only the selected PIDs and update UI IMMEDIATELY on each packet!
                for (pid in cyclePids) {
                    if (!isActive) break
                    val rawResp = proto.sendCommand("01 ${pid.pidHex}")
                    val success = pid.decode(rawResp)
                    if (success) {
                        if (pid.pidHex == currentChartHex) {
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
                appendTerminal("ERR: Нет активного подключения к адаптеру!")
                return@launch
            }
            appendTerminal("> $cmd")
            val resp = proto.sendCommand(cmd)
            appendTerminal(resp)
        }
    }

    private fun appendTerminal(msg: String) {
        val list = _terminalLogs.value.toMutableList()
        if (list.size > 200) list.removeAt(0)
        list.add(msg)
        _terminalLogs.value = list
    fun runAutoTest() {
        viewModelScope.launch {
            val proto = protocol
            if (proto == null || activeTransport?.isConnected != true) {
                appendTerminal("Автотест: сначала подключитесь к адаптеру!")
                return@launch
            }
            pollingJob?.cancel()
            _autoTestState.value = AutoTestUiState.Running("Запуск экспресс-автотеста...", 0f)
            try {
                val report = proto.runAutoTest(_pids.value) { step, prog ->
                    _autoTestState.value = AutoTestUiState.Running(step, prog)
                }
                _autoTestState.value = AutoTestUiState.Completed(report)
                appendTerminal("Автотест: найдено ${report.supportedPidCount}/${report.totalTestedCount} датчиков, пинг: ${report.pingMs} мс")
            } catch (e: Exception) {
                appendTerminal("Ошибка автотеста: ${e.localizedMessage}")
                _autoTestState.value = AutoTestUiState.Idle
            } finally {
                startPolling()
            }
        }
    }

    fun applyOptimalSensors(report: com.obdmaster.app.core.protocol.AutoTestReport) {
        setPidSelectionPreset(report.optimalPidHexes)
        _autoTestState.value = AutoTestUiState.Idle
        appendTerminal("Применены оптимальные датчики (${report.optimalPidHexes.size} шт.)")
    }

    fun dismissAutoTest() {
        _autoTestState.value = AutoTestUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
