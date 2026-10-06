package com.obdmaster.app.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import com.obdmaster.app.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.Locale

sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data class Connecting(val message: String = "Подключение...") : ConnectionStatus()
    data class Connected(val adapterInfo: String, val protocol: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus() {
        val error: String get() = message
    }
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

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application.applicationContext)

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

    private val _telemetryTick = MutableStateFlow(0L)
    val telemetryTick: StateFlow<Long> = _telemetryTick.asStateFlow()

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

    private val _appSettings = MutableStateFlow(settingsManager.loadSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Отметка о том, выполнялось ли сканирование DTC
    private val _hasPerformedDtcScan = MutableStateFlow(false)
    val hasPerformedDtcScan: StateFlow<Boolean> = _hasPerformedDtcScan.asStateFlow()

    // Активное предупреждение безопасности
    private val _activeAlarm = MutableStateFlow<String?>(null)
    val activeAlarm: StateFlow<String?> = _activeAlarm.asStateFlow()

    private var toneGenerator: ToneGenerator? = null
    private var lastAlarmTimestamp: Long = 0L

    var currentScreenRoute: String = _appSettings.value.lastActiveScreenRoute
        private set

    private var activeTransport: ObdTransport? = null
    private var protocol: Elm327Protocol? = null
    private var dtcService: DtcService? = null
    private var vehicleInfoService: VehicleInfoService? = null
    private var pollingJob: Job? = null

    init {
        // Восстановление выбранного датчика графика из настроек
        val savedHex = _appSettings.value.savedChartPidHex
        _pids.value.find { it.pidHex == savedHex }?.let { pid ->
            _chartPid.value = pid
            val savedScheme = _appSettings.value.sensorChartSchemes[pid.pidHex]
            if (savedScheme != null) {
                _appSettings.value = _appSettings.value.copy(chartVisualScheme = savedScheme)
            }
        }

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 85)
        } catch (_: Exception) {}

        refreshPairedDevices()
    }

    fun updateSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        settingsManager.saveSettings(newSettings)
    }

    /**
     * Аппаратный сброс опроса и продувка буфера шины
     */
    private fun stopAndFlushPolling() {
        pollingJob?.cancel()
        pollingJob = null
        protocol?.clearBuffer()
    }

    /**
     * Смена экрана (таба): немедленный сброс старой очереди опроса и выбор стратегии
     */
    fun onScreenChanged(route: String) {
        if (currentScreenRoute == route) return
        stopAndFlushPolling()
        currentScreenRoute = route
        updateSettings(_appSettings.value.copy(lastActiveScreenRoute = route))

        if (activeTransport?.isConnected == true) {
            startPollingForCurrentScreen()
        }
    }

    /**
     * Запуск опроса в зависимости от активного экрана:
     * - "charts": опрос ТОЛЬКО 1 датчика графика на 100% скорости шины.
     * - "dashboard": опрос выбранных датчиков.
     * - "diagnostics", "terminal", "settings": полный стоп опроса (0% загрузки, шина свободна).
     */
    private fun startPollingForCurrentScreen() {
        stopAndFlushPolling()

        when (currentScreenRoute) {
            "charts" -> startTurboChartPolling()
            "dashboard" -> startDashboardPolling()
            else -> {
                // На вкладках Ошибок, Терминала и Настроек опрос полностью заглушен
                _activeAlarm.value = null
            }
        }
    }

    private fun startTurboChartPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            val proto = protocol ?: return@launch
            val targetPid = _chartPid.value

            while (isActive && activeTransport?.isConnected == true) {
                val rawResp = proto.sendCommand("01 ${targetPid.pidHex}")
                val success = targetPid.decode(rawResp)
                if (success) {
                    // Принудительно вызываем обновление StateFlow, т.к. мы меняем внутреннее состояние объекта ObdPid
                    _chartPid.value = targetPid

                    val currentList = _chartHistory.value.toMutableList()
                    if (currentList.size > 60) currentList.removeAt(0)
                    currentList.add(targetPid.currentValue)
                    _chartHistory.value = currentList
                    _telemetryTick.value = System.nanoTime()

                    checkAlarmsForPid(targetPid)
                }

                val interval = _appSettings.value.pollingIntervalMs.coerceAtLeast(0L)
                if (interval > 0L) {
                    delay(interval)
                } else {
                    yield()
                }
            }
        }
    }

    private fun startDashboardPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            val proto = protocol ?: return@launch

            while (isActive && activeTransport?.isConnected == true) {
                val allPids = _pids.value
                val selectedHexes = _appSettings.value.selectedPidHexes

                val cyclePids = allPids.filter { it.isSupported && it.pidHex in selectedHexes }
                if (cyclePids.isEmpty()) {
                    delay(200)
                    continue
                }

                for (pid in cyclePids) {
                    if (!isActive) break
                    val rawResp = proto.sendCommand("01 ${pid.pidHex}")
                    val success = pid.decode(rawResp)
                    if (success) {
                        // Важно: переназначаем value для вызова рекомпозиции UI у всех датчиков на Dashboard
                        // Принудительно создаем новый список с теми же объектами, но так как Compose следит
                        // за изменением списка, нам нужно также, чтобы сам элемент обновился, если его состояние мутирует.
                        // В Compose state flow на список объектов со внутренними var не всегда триггерит рекомпозицию.
                        // Простейший способ: используем telemetryTick для форсирования рекомпозиции в UI.
                        _telemetryTick.value = System.nanoTime()
                        checkAlarmsForPid(pid)
                    }
                }

                val interval = _appSettings.value.pollingIntervalMs.coerceAtLeast(0L)
                if (interval > 0L) {
                    delay(interval)
                } else {
                    yield()
                }
            }
        }
    }

    /**
     * Проверка порогов безопасности (строго только для тех датчиков, которые сейчас реально опрашиваются)
     */
    private fun checkAlarmsForPid(pid: ObdPid) {
        val settings = _appSettings.value
        if (!settings.alarmMasterEnabled) {
            _activeAlarm.value = null
            return
        }

        when (pid.pidHex) {
            "05" -> { // ОЖ
                if (settings.coolantAlarmEnabled && pid.currentValue >= settings.coolantAlarmThresholdC) {
                    triggerAlarm("⚠️ ПЕРЕГРЕВ ДВИГАТЕЛЯ! ${pid.currentValue.toInt()} °C (порог ${settings.coolantAlarmThresholdC} °C)")
                    return
                }
            }
            "0D" -> { // Скорость
                if (settings.speedAlarmEnabled && pid.currentValue >= settings.speedAlarmThresholdKmh) {
                    triggerAlarm("⚠️ ПРЕВЫШЕНИЕ СКОРОСТИ! ${pid.currentValue.toInt()} км/ч (порог ${settings.speedAlarmThresholdKmh} км/ч)")
                    return
                }
            }
            "0C" -> { // Обороты
                if (settings.rpmAlarmEnabled && pid.currentValue >= settings.rpmAlarmThresholdRpm) {
                    triggerAlarm("⚠️ ОТСЕЧКА ОБОРОТОВ! ${pid.currentValue.toInt()} об/мин (порог ${settings.rpmAlarmThresholdRpm})")
                    return
                }
            }
            "42" -> { // АКБ
                if (settings.batteryAlarmEnabled && pid.currentValue > 5f && pid.currentValue <= settings.batteryAlarmThresholdV) {
                    val formatted = String.format(Locale.US, "%.1f", pid.currentValue)
                    triggerAlarm("⚠️ ПРОСАДКА АКБ! $formatted В (порог ${settings.batteryAlarmThresholdV} В)")
                    return
                }
            }
        }

        // Если все показатели в норме
        if (_activeAlarm.value != null && System.currentTimeMillis() - lastAlarmTimestamp > 4000L) {
            _activeAlarm.value = null
        }
    }

    private fun triggerAlarm(alertMessage: String) {
        _activeAlarm.value = alertMessage
        val now = System.currentTimeMillis()
        val settings = _appSettings.value
        if (!settings.alarmMasterEnabled) return

        val repeatIntervalMs = when (settings.alarmRepeatIntervalSec) {
            2 -> 2000L   // Турбо
            5 -> 5000L
            15 -> 15000L // Обычный
            30 -> 30000L
            60 -> 60000L
            -1 -> Long.MAX_VALUE // Только 1 раз
            else -> 15000L
        }

        if (now - lastAlarmTimestamp >= repeatIntervalMs) {
            lastAlarmTimestamp = now
            playAlarmSoundAndVibration()
        }
    }

    private fun playAlarmSoundAndVibration() {
        try {
            // Звуковой зуммер
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (_: Exception) {}

        try {
            // Двойной виброотклик
            val ctx = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(300)
                }
            }
        } catch (_: Exception) {}
    }

    fun dismissActiveAlarm() {
        _activeAlarm.value = null
    }

    fun togglePidSelection(pidHex: String) {
        stopAndFlushPolling()
        val current = _appSettings.value.selectedPidHexes.toMutableSet()
        if (current.contains(pidHex)) {
            if (current.size > 1) {
                current.remove(pidHex)
            }
        } else {
            current.add(pidHex)
        }
        if (_activeAlarm.value?.contains(pidHex) == true && !current.contains(pidHex)) {
            _activeAlarm.value = null
        }
        updateSettings(_appSettings.value.copy(selectedPidHexes = current))
        if (currentScreenRoute == "dashboard" && activeTransport?.isConnected == true) {
            startPollingForCurrentScreen()
        }
    }

    fun setPidSelectionPreset(presetHexes: Set<String>) {
        stopAndFlushPolling()
        updateSettings(_appSettings.value.copy(selectedPidHexes = presetHexes))
        if (currentScreenRoute == "dashboard" && activeTransport?.isConnected == true) {
            startPollingForCurrentScreen()
        }
    }

    fun setChartPid(pid: ObdPid) {
        stopAndFlushPolling()
        _chartPid.value = pid
        _chartHistory.value = emptyList()
        _activeAlarm.value = null

        // Восстановление индивидуальной схемы отображения для выбранного датчика
        val savedScheme = _appSettings.value.sensorChartSchemes[pid.pidHex] ?: _appSettings.value.chartVisualScheme
        updateSettings(
            _appSettings.value.copy(
                savedChartPidHex = pid.pidHex,
                chartVisualScheme = savedScheme
            )
        )
        if (currentScreenRoute == "charts" && activeTransport?.isConnected == true) {
            startPollingForCurrentScreen()
        }
    }

    fun selectChartPid(pid: ObdPid) = setChartPid(pid)

    fun setChartVisualScheme(schemeIndex: Int) {
        val currentHex = _chartPid.value.pidHex
        val currentMap = _appSettings.value.sensorChartSchemes.toMutableMap()
        currentMap[currentHex] = schemeIndex
        updateSettings(
            _appSettings.value.copy(
                chartVisualScheme = schemeIndex,
                sensorChartSchemes = currentMap
            )
        )
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
    }

    fun setConnectionType(type: ConnectionType) {
        _connectionType.value = type
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                _pairedDevices.value = adapter.bondedDevices.toList()
            }
        } catch (_: Exception) {
            _pairedDevices.value = emptyList()
        }
    }

    fun connectBluetooth(device: BluetoothDevice) {
        viewModelScope.launch {
            disconnect()
            _connectionStatus.value = ConnectionStatus.Connecting("Подключение к ${device.name ?: device.address}...")
            updateSettings(_appSettings.value.copy(lastConnectedDeviceMac = device.address))

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
            startPollingForCurrentScreen()
            loadVehicleInfo()
        } else {
            _connectionStatus.value = ConnectionStatus.Error("ELM327 ответил, но связь с ЭБУ не установлена (зажигание включено?)")
        }
    }

    fun disconnect() {
        stopAndFlushPolling()
        activeTransport?.disconnect()
        activeTransport = null
        protocol = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        _activeAlarm.value = null
        _hasPerformedDtcScan.value = false
        appendTerminal("Отключено от адаптера.")
    }

    fun scanDtcs(
        includeStored: Boolean = true,
        includePending: Boolean = true,
        includePermanent: Boolean = true
    ) {
        viewModelScope.launch {
            val service = dtcService ?: return@launch
            // Прерываем опрос датчиков и сбрасываем буфер шины
            stopAndFlushPolling()
            delay(50)
            _isDtcScanning.value = true
            _hasPerformedDtcScan.value = true
            appendTerminal("Запуск сканирования DTC (03:$includeStored, 07:$includePending, 0A:$includePermanent)...")

            try {
                val list = mutableListOf<com.obdmaster.app.core.protocol.DtcItem>()
                if (includeStored) {
                    list.addAll(service.readStoredDtcs())
                }
                if (includePending) {
                    list.addAll(service.readPendingDtcs())
                }
                if (includePermanent) {
                    list.addAll(service.readPermanentDtcs())
                }

                val all = list.distinctBy { it.code }
                _dtcList.value = all
                appendTerminal("Сканирование завершено: найдено кодов: ${all.size}")
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
            stopAndFlushPolling()
            delay(50)
            appendTerminal("Отправка команды сброса кодов ошибок (Mode 04)...")
            val success = service.clearDtcs()
            if (success) {
                _dtcList.value = emptyList()
                appendTerminal("Команда сброса выполнена успешно! Check Engine погашен.")
            } else {
                appendTerminal("Не удалось сбросить ошибки.")
            }
            onCompleted(success)
        }
    }

    fun forceClearDtcs(onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val service = dtcService ?: return@launch
            stopAndFlushPolling()
            delay(50)
            appendTerminal("Принудительный сброс (Mode 04) без предварительного поиска...")
            val success = service.clearDtcs()
            if (success) {
                _dtcList.value = emptyList()
                appendTerminal("Принудительный сброс Mode 04 выполнен! Память ЭБУ очищена.")
            } else {
                appendTerminal("Не удалось выполнить принудительный сброс.")
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
    }

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
                startPollingForCurrentScreen()
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
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        disconnect()
    }
}
