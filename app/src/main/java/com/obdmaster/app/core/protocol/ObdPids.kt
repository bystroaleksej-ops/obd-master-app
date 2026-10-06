package com.obdmaster.app.core.protocol

sealed class ObdPid(
    val pidHex: String,
    val titleRu: String,
    val titleEn: String,
    val unit: String,
    val category: String,
    val minVal: Float,
    val maxVal: Float
) {
    private val _rawHex = androidx.compose.runtime.mutableStateOf("")
    var rawHex: String
        get() = _rawHex.value
        protected set(value) { _rawHex.value = value }

    private val _currentValue = androidx.compose.runtime.mutableStateOf(0f)
    var currentValue: Float
        get() = _currentValue.value
        protected set(value) { _currentValue.value = value }

    private val _formattedString = androidx.compose.runtime.mutableStateOf("-- $unit")
    var formattedString: String
        get() = _formattedString.value
        protected set(value) { _formattedString.value = value }

    var isSupported: Boolean = true
    var failureCount: Int = 0

    abstract fun decode(cleanHex: String): Boolean

    open fun getDisplayValue(settings: com.obdmaster.app.data.AppSettings): Pair<String, String> {
        if (formattedString == "-- $unit" || formattedString.startsWith("--")) return Pair("--", unit)
        return Pair(formattedString.removeSuffix(" $unit").removeSuffix(unit).trim(), unit)
    }

    open fun getDisplayMinMax(settings: com.obdmaster.app.data.AppSettings): Pair<Float, Float> = Pair(minVal, maxVal)

    // Helper: extracts bytes from clean hex (e.g., "410C1AFA" -> [26, 250])
    protected fun extractDataBytes(cleanHex: String, expectedPid: String, count: Int): IntArray? {
        val sanitized = cleanHex.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        val prefix = "41$expectedPid"
        val idx = sanitized.indexOf(prefix)
        if (idx == -1) {
            failureCount++
            if (failureCount >= 3) {
                isSupported = false
            }
            return null
        }

        val dataStart = idx + prefix.length
        if (sanitized.length < dataStart + count * 2) return null

        val result = IntArray(count)
        for (i in 0 until count) {
            val hexByte = sanitized.substring(dataStart + i * 2, dataStart + (i + 1) * 2)
            result[i] = hexByte.toIntOrNull(16) ?: return null
        }
        failureCount = 0
        isSupported = true
        return result
    }

    // --- Mode 01 PID Implementations ---

    class EngineRpm : ObdPid("0C", "Обороты двигателя", "Engine RPM", "об/мин", "Двигатель", 0f, 8000f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0C", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) / 4f
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }
    }

    class VehicleSpeed : ObdPid("0D", "Скорость", "Vehicle Speed", "км/ч", "Движение", 0f, 240f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0D", 1) ?: return false
            currentValue = b[0].toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }

        override fun getDisplayValue(settings: com.obdmaster.app.data.AppSettings): Pair<String, String> {
            if (formattedString.startsWith("--")) return Pair("--", settings.speedUnit)
            return if (settings.speedUnit == "mph") {
                val mph = currentValue * 0.621371f
                Pair("${mph.toInt()}", "mph")
            } else {
                Pair("${currentValue.toInt()}", "км/ч")
            }
        }

        override fun getDisplayMinMax(settings: com.obdmaster.app.data.AppSettings): Pair<Float, Float> {
            return if (settings.speedUnit == "mph") Pair(0f, 150f) else Pair(0f, 240f)
        }
    }

    class CoolantTemperature : ObdPid("05", "Температура ОЖ", "Coolant Temp", "°C", "Температура", -40f, 150f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "05", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }

        override fun getDisplayValue(settings: com.obdmaster.app.data.AppSettings): Pair<String, String> {
            if (formattedString.startsWith("--")) return Pair("--", settings.tempUnit)
            return if (settings.tempUnit == "°F") {
                val f = currentValue * 1.8f + 32f
                Pair("${f.toInt()}", "°F")
            } else {
                Pair("${currentValue.toInt()}", "°C")
            }
        }
    }

    class EngineLoad : ObdPid("04", "Нагрузка на двигатель", "Engine Load", "%", "Двигатель", 0f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "04", 1) ?: return false
            currentValue = (b[0] * 100f) / 255f
            formattedString = "${String.format("%.1f", currentValue)} $unit"
            return true
        }
    }

    class ThrottlePosition : ObdPid("11", "Дроссельная заслонка", "Throttle Position", "%", "Двигатель", 0f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "11", 1) ?: return false
            currentValue = (b[0] * 100f) / 255f
            formattedString = "${String.format("%.1f", currentValue)} $unit"
            return true
        }
    }

    class IntakeAirTemperature : ObdPid("0F", "Температура впуска", "Intake Air Temp", "°C", "Температура", -40f, 120f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0F", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }

        override fun getDisplayValue(settings: com.obdmaster.app.data.AppSettings): Pair<String, String> {
            if (formattedString.startsWith("--")) return Pair("--", settings.tempUnit)
            return if (settings.tempUnit == "°F") {
                val f = currentValue * 1.8f + 32f
                Pair("${f.toInt()}", "°F")
            } else {
                Pair("${currentValue.toInt()}", "°C")
            }
        }
    }

    class MafAirFlow : ObdPid("10", "Массовый расход воздуха (MAF)", "MAF Air Flow", "г/с", "Впуск", 0f, 300f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "10", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) / 100f
            formattedString = "${String.format("%.2f", currentValue)} $unit"
            return true
        }
    }

    class IntakeManifoldPressure : ObdPid("0B", "Давление впуска (MAP)", "Intake MAP", "кПа", "Впуск", 0f, 255f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0B", 1) ?: return false
            currentValue = b[0].toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }

        override fun getDisplayValue(settings: com.obdmaster.app.data.AppSettings): Pair<String, String> {
            if (formattedString.startsWith("--")) return Pair("--", settings.pressureUnit)
            return if (settings.pressureUnit == "бар") {
                val bar = currentValue / 100f
                Pair(String.format(java.util.Locale.US, "%.2f", bar), "бар")
            } else {
                Pair("${currentValue.toInt()}", "кПа")
            }
        }
    }

    class TimingAdvance : ObdPid("0E", "Угол опережения зажигания", "Timing Advance", "°", "Зажигание", -64f, 64f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0E", 1) ?: return false
            currentValue = (b[0] / 2f) - 64f
            formattedString = "${String.format("%.1f", currentValue)} $unit"
            return true
        }
    }

    class ShortTermFuelTrim1 : ObdPid("06", "Краткоср. коррекция (Банк 1)", "STFT Bank 1", "%", "Топливо", -100f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "06", 1) ?: return false
            currentValue = ((b[0] - 128) * 100f) / 128f
            formattedString = "${String.format("%.1f", currentValue)} $unit"
            return true
        }
    }

    class LongTermFuelTrim1 : ObdPid("07", "Долгоср. коррекция (Банк 1)", "LTFT Bank 1", "%", "Топливо", -100f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "07", 1) ?: return false
            currentValue = ((b[0] - 128) * 100f) / 128f
            formattedString = "${String.format("%.1f", currentValue)} $unit"
            return true
        }
    }

    class FuelLevel : ObdPid("2F", "Уровень топлива", "Fuel Level", "%", "Топливо", 0f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "2F", 1) ?: return false
            currentValue = (b[0] * 100f) / 255f
            formattedString = "${String.format("%.1f", currentValue)} $unit"
            return true
        }
    }

    class ControlModuleVoltage : ObdPid("42", "Напряжение ЭБУ", "ECU Voltage", "В", "Электрика", 0f, 18f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "42", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) / 1000f
            formattedString = "${String.format("%.2f", currentValue)} $unit"
            return true
        }
    }

    class AmbientAirTemperature : ObdPid("46", "Температура за бортом", "Ambient Temp", "°C", "Температура", -40f, 60f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "46", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }
    }

    class EngineOilTemperature : ObdPid("5C", "Температура моторного масла", "Oil Temp", "°C", "Температура", -40f, 180f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "5C", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }
    }

    class EngineFuelRate : ObdPid("5E", "Расход топлива", "Fuel Rate", "л/ч", "Топливо", 0f, 50f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "5E", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) / 20f
            formattedString = "${String.format("%.2f", currentValue)} $unit"
            return true
        }
    }

    class EngineRunTime : ObdPid("1F", "Время работы двигателя", "Run Time", "мин", "Двигатель", 0f, 9999f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "1F", 2) ?: return false
            val seconds = (b[0] * 256) + b[1]
            currentValue = seconds / 60f
            formattedString = "${currentValue.toInt()} $unit (${seconds}с)"
            return true
        }
    }

    class BarometricPressure : ObdPid("33", "Атмосферное давление", "Barometric Pressure", "кПа", "Впуск", 50f, 120f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "33", 1) ?: return false
            currentValue = b[0].toFloat()
            formattedString = "${currentValue.toInt()} $unit"
            return true
        }
    }

    companion object {
        fun getAllPids(): List<ObdPid> = listOf(
            EngineRpm(),
            VehicleSpeed(),
            CoolantTemperature(),
            EngineLoad(),
            ThrottlePosition(),
            ControlModuleVoltage(),
            IntakeAirTemperature(),
            MafAirFlow(),
            IntakeManifoldPressure(),
            TimingAdvance(),
            ShortTermFuelTrim1(),
            LongTermFuelTrim1(),
            FuelLevel(),
            AmbientAirTemperature(),
            EngineOilTemperature(),
            EngineFuelRate(),
            EngineRunTime(),
            BarometricPressure()
        )
    }
}
