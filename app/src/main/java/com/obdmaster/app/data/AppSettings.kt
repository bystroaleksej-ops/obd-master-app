package com.obdmaster.app.data

import android.content.Context
import android.content.SharedPreferences

data class AppSettings(
    // 1. Connection & Protocol
    val protocolIndex: Int = 0, // 0: Auto (AT SP 0), 6: CAN 11b 500k, 7: CAN 29b 500k, 5: KWP Fast, 3: ISO 9141-2
    val pollingIntervalMs: Long = 100L, // 50ms (Fast), 100ms (Normal), 250ms (Safe)
    val autoReconnect: Boolean = true,
    val elmTimeoutMs: Int = 200,

    // 2. Alerts & Engine Protection
    val coolantAlarmEnabled: Boolean = true,
    val coolantAlarmThresholdC: Int = 102,
    val batteryAlarmEnabled: Boolean = true,
    val batteryAlarmThresholdV: Float = 11.8f,
    val rpmAlarmEnabled: Boolean = false,
    val rpmAlarmThreshold: Int = 5500,

    // 3. Display & UI
    val keepScreenOn: Boolean = true,
    val hudMode: Boolean = false,
    val speedUnit: String = "км/ч", // "км/ч", "mph"
    val tempUnit: String = "°C", // "°C", "°F"
    val pressureUnit: String = "кПа", // "кПа", "бар", "psi"

    // 4. Diagnostics & Terminal
    val deepScanAllModules: Boolean = false, // false: ECU only, true: ECU + TCM + ABS
    val terminalAutoTranslate: Boolean = true
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("obd_master_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        return AppSettings(
            protocolIndex = prefs.getInt("protocolIndex", 0),
            pollingIntervalMs = prefs.getLong("pollingIntervalMs", 100L),
            autoReconnect = prefs.getBoolean("autoReconnect", true),
            elmTimeoutMs = prefs.getInt("elmTimeoutMs", 200),
            coolantAlarmEnabled = prefs.getBoolean("coolantAlarmEnabled", true),
            coolantAlarmThresholdC = prefs.getInt("coolantAlarmThresholdC", 102),
            batteryAlarmEnabled = prefs.getBoolean("batteryAlarmEnabled", true),
            batteryAlarmThresholdV = prefs.getFloat("batteryAlarmThresholdV", 11.8f),
            rpmAlarmEnabled = prefs.getBoolean("rpmAlarmEnabled", false),
            rpmAlarmThreshold = prefs.getInt("rpmAlarmThreshold", 5500),
            keepScreenOn = prefs.getBoolean("keepScreenOn", true),
            hudMode = prefs.getBoolean("hudMode", false),
            speedUnit = prefs.getString("speedUnit", "км/ч") ?: "км/ч",
            tempUnit = prefs.getString("tempUnit", "°C") ?: "°C",
            pressureUnit = prefs.getString("pressureUnit", "кПа") ?: "кПа",
            deepScanAllModules = prefs.getBoolean("deepScanAllModules", false),
            terminalAutoTranslate = prefs.getBoolean("terminalAutoTranslate", true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt("protocolIndex", settings.protocolIndex)
            putLong("pollingIntervalMs", settings.pollingIntervalMs)
            putBoolean("autoReconnect", settings.autoReconnect)
            putInt("elmTimeoutMs", settings.elmTimeoutMs)
            putBoolean("coolantAlarmEnabled", settings.coolantAlarmEnabled)
            putInt("coolantAlarmThresholdC", settings.coolantAlarmThresholdC)
            putBoolean("batteryAlarmEnabled", settings.batteryAlarmEnabled)
            putFloat("batteryAlarmThresholdV", settings.batteryAlarmThresholdV)
            putBoolean("rpmAlarmEnabled", settings.rpmAlarmEnabled)
            putInt("rpmAlarmThreshold", settings.rpmAlarmThreshold)
            putBoolean("keepScreenOn", settings.keepScreenOn)
            putBoolean("hudMode", settings.hudMode)
            putString("speedUnit", settings.speedUnit)
            putString("tempUnit", settings.tempUnit)
            putString("pressureUnit", settings.pressureUnit)
            putBoolean("deepScanAllModules", settings.deepScanAllModules)
            putBoolean("terminalAutoTranslate", settings.terminalAutoTranslate)
            apply()
        }
    }
}
