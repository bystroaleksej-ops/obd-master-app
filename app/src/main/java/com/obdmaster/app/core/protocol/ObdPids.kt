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
    var rawHex: String = ""
        protected set

    var currentValue: Float = 0f
        protected set

    var formattedString: String = "-- $unit"
        protected set

    var isSupported: Boolean = true
    var failureCount: Int = 0

    abstract fun decode(cleanHex: String): Boolean

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
            formattedString = "${currentValue.toInt()} об/мин"
            return true
        }
    }

    class VehicleSpeed : ObdPid("0D", "Скорость автомобиля", "Vehicle Speed", "км/ч", "Движение", 0f, 260f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0D", 1) ?: return false
            currentValue = b[0].toFloat()
            formattedString = "${b[0]} км/ч"
            return true
        }
    }

    class CoolantTemp : ObdPid("05", "Температура ОЖ", "Engine Coolant Temp", "°C", "Двигатель", -40f, 150f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "05", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} °C"
            return true
        }
    }

    class EngineLoad : ObdPid("04", "Нагрузка на двигатель", "Calculated Engine Load", "%", "Двигатель", 0f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "04", 1) ?: return false
            currentValue = (b[0] * 100f) / 255f
            formattedString = String.format("%.1f %%", currentValue)
            return true
        }
    }

    class ThrottlePosition : ObdPid("11", "Дроссельная заслонка", "Throttle Position", "%", "Впуск", 0f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "11", 1) ?: return false
            currentValue = (b[0] * 100f) / 255f
            formattedString = String.format("%.1f %%", currentValue)
            return true
        }
    }

    class IntakeAirTemp : ObdPid("0F", "Температура на впуске", "Intake Air Temp", "°C", "Впуск", -40f, 120f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0F", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} °C"
            return true
        }
    }

    class MafAirFlow : ObdPid("10", "Расход воздуха (MAF)", "MAF Air Flow Rate", "г/с", "Впуск", 0f, 300f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "10", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) / 100f
            formattedString = String.format("%.2f г/с", currentValue)
            return true
        }
    }

    class FuelLevel : ObdPid("2F", "Уровень топлива", "Fuel Tank Level", "%", "Топливо", 0f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "2F", 1) ?: return false
            currentValue = (b[0] * 100f) / 255f
            formattedString = String.format("%.1f %%", currentValue)
            return true
        }
    }

    class ControlModuleVoltage : ObdPid("42", "Напряжение ЭБУ", "Control Module Voltage", "В", "Электрика", 0f, 18f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "42", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) / 1000f
            formattedString = String.format("%.2f В", currentValue)
            return true
        }
    }

    class ShortTermFuelTrimBank1 : ObdPid("06", "Краткосрочная коррекция Б1", "STFT Bank 1", "%", "Топливо", -100f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "06", 1) ?: return false
            currentValue = (b[0] - 128) * 100f / 128f
            formattedString = String.format("%+.1f %%", currentValue)
            return true
        }
    }

    class LongTermFuelTrimBank1 : ObdPid("07", "Долгосрочная коррекция Б1", "LTFT Bank 1", "%", "Топливо", -100f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "07", 1) ?: return false
            currentValue = (b[0] - 128) * 100f / 128f
            formattedString = String.format("%+.1f %%", currentValue)
            return true
        }
    }

    class TimingAdvance : ObdPid("0E", "Угол опережения зажигания", "Timing Advance", "°", "Двигатель", -64f, 64f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0E", 1) ?: return false
            currentValue = (b[0] / 2f) - 64f
            formattedString = String.format("%.1f °", currentValue)
            return true
        }
    }

    class IntakeManifoldPressure : ObdPid("0B", "Давление во впускном (MAP)", "Intake Manifold Pressure", "кПа", "Впуск", 0f, 255f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "0B", 1) ?: return false
            currentValue = b[0].toFloat()
            formattedString = "${b[0]} кПа"
            return true
        }
    }

    class BarometricPressure : ObdPid("33", "Атмосферное давление", "Barometric Pressure", "кПа", "Впуск", 0f, 255f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "33", 1) ?: return false
            currentValue = b[0].toFloat()
            formattedString = "${b[0]} кПа"
            return true
        }
    }

    class AmbientAirTemp : ObdPid("46", "Температура за бортом", "Ambient Air Temp", "°C", "Кузов", -40f, 100f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "46", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} °C"
            return true
        }
    }

    class EngineRunTime : ObdPid("1F", "Время работы мотора", "Run Time Since Start", "сек", "Двигатель", 0f, 65535f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "1F", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]).toFloat()
            val min = currentValue.toInt() / 60
            val sec = currentValue.toInt() % 60
            formattedString = "${min}м ${sec}с"
            return true
        }
    }

    class EngineOilTemperature : ObdPid("5C", "Температура масла", "Engine Oil Temp", "°C", "Двигатель", -40f, 210f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "5C", 1) ?: return false
            currentValue = (b[0] - 40).toFloat()
            formattedString = "${currentValue.toInt()} °C"
            return true
        }
    }

    class EngineFuelRate : ObdPid("5E", "Мгновенный расход", "Engine Fuel Rate", "л/ч", "Топливо", 0f, 50f) {
        override fun decode(cleanHex: String): Boolean {
            val b = extractDataBytes(cleanHex, "5E", 2) ?: return false
            currentValue = ((b[0] * 256) + b[1]) * 0.05f
            formattedString = String.format("%.2f л/ч", currentValue)
            return true
        }
    }

    companion object {
        fun getAllPids(): List<ObdPid> = listOf(
            EngineRpm(),
            VehicleSpeed(),
            CoolantTemp(),
            EngineLoad(),
            ThrottlePosition(),
            IntakeAirTemp(),
            MafAirFlow(),
            FuelLevel(),
            ControlModuleVoltage(),
            ShortTermFuelTrimBank1(),
            LongTermFuelTrimBank1(),
            TimingAdvance(),
            IntakeManifoldPressure(),
            BarometricPressure(),
            AmbientAirTemp(),
            EngineRunTime(),
            EngineOilTemperature(),
            EngineFuelRate()
        )
    }
}
