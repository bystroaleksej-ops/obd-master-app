package com.obdmaster.app.data

import android.content.Context
import android.content.SharedPreferences

data class SensorGaugeSettings(val minVal: Float, val maxVal: Float, val stepVal: Float)

data class AppSettings(
    // 1. Connection & Timing
    val pollingIntervalMs: Long = 100L, // 0: Turbo, 20: Fast, 50, 100: Normal, 250: Eco
    val lastConnectedDeviceMac: String = "",
    val protocolIndex: Int = 0,
    val autoReconnect: Boolean = true,
    val elmTimeoutMs: Int = 200,

    // 2. Active Sensors & Charts
    val selectedPidHexes: Set<String> = setOf("0C", "0D", "05", "04", "11", "42"),
    val savedChartPidHex: String = "0C", // Запомненный датчик на вкладке графиков
    val chartVisualScheme: Int = 0, // Дефолтная схема
    val sensorGaugeSettings: Map<String, SensorGaugeSettings> = emptyMap(),
    val lastActiveScreenRoute: String = "dashboard", // Запоминание последней открытой вкладки

    // 3. Alerts & Engine Protection
    val alarmMasterEnabled: Boolean = true, // Главный переключатель звука/вибрации тревог
    val alarmRepeatIntervalSec: Int = 15, // 2: Турбо, 5, 15, 30, 60, -1: Только 1 раз

    val coolantAlarmEnabled: Boolean = true,
    val coolantAlarmThresholdC: Int = 102,

    val speedAlarmEnabled: Boolean = true,
    val speedAlarmThresholdKmh: Int = 110,

    val rpmAlarmEnabled: Boolean = true,
    val rpmAlarmThresholdRpm: Int = 5500,

    val batteryAlarmEnabled: Boolean = true,
    val batteryAlarmThresholdV: Float = 11.8f,

    // 4. Custom Units for Sensors
    val speedUnit: String = "км/ч", // "км/ч" или "mph"
    val tempUnit: String = "°C", // "°C" или "°F"
    val pressureUnit: String = "кПа", // "кПа" или "бар"

    // 5. Display & Extras
    val keepScreenOn: Boolean = true,
    val hudMode: Boolean = false,
    val deepScanAllModules: Boolean = false,
    val terminalAutoTranslate: Boolean = true
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("obd_master_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        return AppSettings(
            pollingIntervalMs = prefs.getLong("pollingIntervalMs", 100L),
            lastConnectedDeviceMac = prefs.getString("lastConnectedDeviceMac", "") ?: "",
            protocolIndex = prefs.getInt("protocolIndex", 0),
            autoReconnect = prefs.getBoolean("autoReconnect", true),
            elmTimeoutMs = prefs.getInt("elmTimeoutMs", 200),

            selectedPidHexes = prefs.getStringSet("selectedPidHexes", setOf("0C", "0D", "05", "04", "11", "42")) ?: setOf("0C", "0D", "05", "04", "11", "42"),
            savedChartPidHex = prefs.getString("savedChartPidHex", "0C") ?: "0C",
            chartVisualScheme = prefs.getInt("chartVisualScheme", 0),
            sensorGaugeSettings = parseGaugeSettings(prefs.getString("sensorGaugeSettings", "")),
            lastActiveScreenRoute = prefs.getString("lastActiveScreenRoute", "dashboard") ?: "dashboard",

            alarmMasterEnabled = prefs.getBoolean("alarmMasterEnabled", true),
            alarmRepeatIntervalSec = prefs.getInt("alarmRepeatIntervalSec", 15),

            coolantAlarmEnabled = prefs.getBoolean("coolantAlarmEnabled", true),
            coolantAlarmThresholdC = prefs.getInt("coolantAlarmThresholdC", 102),

            speedAlarmEnabled = prefs.getBoolean("speedAlarmEnabled", true),
            speedAlarmThresholdKmh = prefs.getInt("speedAlarmThresholdKmh", 110),

            rpmAlarmEnabled = prefs.getBoolean("rpmAlarmEnabled", true),
            rpmAlarmThresholdRpm = prefs.getInt("rpmAlarmThresholdRpm", 5500),

            batteryAlarmEnabled = prefs.getBoolean("batteryAlarmEnabled", true),
            batteryAlarmThresholdV = prefs.getFloat("batteryAlarmThresholdV", 11.8f),

            speedUnit = prefs.getString("speedUnit", "км/ч") ?: "км/ч",
            tempUnit = prefs.getString("tempUnit", "°C") ?: "°C",
            pressureUnit = prefs.getString("pressureUnit", "кПа") ?: "кПа",

            keepScreenOn = prefs.getBoolean("keepScreenOn", true),
            hudMode = prefs.getBoolean("hudMode", false),
            deepScanAllModules = prefs.getBoolean("deepScanAllModules", false),
            terminalAutoTranslate = prefs.getBoolean("terminalAutoTranslate", true)
        )
    }

    /**
     * Мгновенное сквозное сохранение на диск через commit() для 100% защиты от потери данных
     */
    fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putLong("pollingIntervalMs", settings.pollingIntervalMs)
            putString("lastConnectedDeviceMac", settings.lastConnectedDeviceMac)
            putInt("protocolIndex", settings.protocolIndex)
            putBoolean("autoReconnect", settings.autoReconnect)
            putInt("elmTimeoutMs", settings.elmTimeoutMs)

            putStringSet("selectedPidHexes", settings.selectedPidHexes)
            putString("savedChartPidHex", settings.savedChartPidHex)
            putInt("chartVisualScheme", settings.chartVisualScheme)
            putString("sensorGaugeSettings", formatGaugeSettings(settings.sensorGaugeSettings))
            putString("lastActiveScreenRoute", settings.lastActiveScreenRoute)

            putBoolean("alarmMasterEnabled", settings.alarmMasterEnabled)
            putInt("alarmRepeatIntervalSec", settings.alarmRepeatIntervalSec)

            putBoolean("coolantAlarmEnabled", settings.coolantAlarmEnabled)
            putInt("coolantAlarmThresholdC", settings.coolantAlarmThresholdC)

            putBoolean("speedAlarmEnabled", settings.speedAlarmEnabled)
            putInt("speedAlarmThresholdKmh", settings.speedAlarmThresholdKmh)

            putBoolean("rpmAlarmEnabled", settings.rpmAlarmEnabled)
            putInt("rpmAlarmThresholdRpm", settings.rpmAlarmThresholdRpm)

            putBoolean("batteryAlarmEnabled", settings.batteryAlarmEnabled)
            putFloat("batteryAlarmThresholdV", settings.batteryAlarmThresholdV)

            putString("speedUnit", settings.speedUnit)
            putString("tempUnit", settings.tempUnit)
            putString("pressureUnit", settings.pressureUnit)

            putBoolean("keepScreenOn", settings.keepScreenOn)
            putBoolean("hudMode", settings.hudMode)
            putBoolean("deepScanAllModules", settings.deepScanAllModules)
            putBoolean("terminalAutoTranslate", settings.terminalAutoTranslate)
            commit() // Атомарная немедленная запись
        }
    }

        private fun parseGaugeSettings(raw: String?): Map<String, SensorGaugeSettings> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val hex = parts[0].trim()
                val vals = parts[1].trim().split(",")
                if (hex.isNotEmpty() && vals.size == 3) {
                    val min = vals[0].toFloatOrNull() ?: 0f
                    val max = vals[1].toFloatOrNull() ?: 100f
                    val step = vals[2].toFloatOrNull() ?: 10f
                    hex to SensorGaugeSettings(min, max, step)
                } else null
            } else null
        }.toMap()
    }

    private fun formatGaugeSettings(map: Map<String, SensorGaugeSettings>): String {
        return map.entries.joinToString(";") { "${it.key}:,," }
    }:${it.value}" }
    }
}

