package com.obdmaster.app.core.protocol

data class VehicleInfo(
    val vin: String = "Не определен",
    val calibrationId: String = "Не определен",
    val cvn: String = "Не определен",
    val ecuName: String = "Не определен",
    val protocolName: String = "Не определен",
    val adapterVersion: String = "Не определен",
    val batteryVoltage: String = "--.-V"
)

class VehicleInfoService(
    private val protocol: Elm327Protocol
) {
    suspend fun readVehicleInfo(): VehicleInfo {
        val adapterVer = protocol.sendCommand("AT I").trim()
        val protocolName = protocol.sendCommand("AT DP").trim()
        val voltage = protocol.sendCommand("AT RV").trim()

        val vin = readVin()
        val calId = readCalibrationId()
        val cvn = readCvn()
        val ecuName = readEcuName()

        return VehicleInfo(
            vin = vin,
            calibrationId = calId,
            cvn = cvn,
            ecuName = ecuName,
            protocolName = protocolName,
            adapterVersion = adapterVer,
            batteryVoltage = voltage
        )
    }

    private suspend fun readVin(): String {
        val raw = protocol.sendCommand("09 02")
        val sanitized = raw.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        val prefix = "4902"
        val idx = sanitized.indexOf(prefix)
        if (idx == -1) return "Не поддерживается ЭБУ"

        val payload = sanitized.substring(idx + prefix.length)
        val sb = java.lang.StringBuilder()

        var i = 0
        while (i + 2 <= payload.length) {
            val byteHex = payload.substring(i, i + 2)
            i += 2
            val charCode = byteHex.toIntOrNull(16) ?: continue
            // Standard ASCII printable characters (0-9, A-Z)
            if (charCode in 32..126) {
                sb.append(charCode.toChar())
            }
        }
        val result = sb.toString().trim()
        return if (result.length >= 10) result else "Не определен"
    }

    private suspend fun readCalibrationId(): String {
        val raw = protocol.sendCommand("09 04")
        val sanitized = raw.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        val prefix = "4904"
        val idx = sanitized.indexOf(prefix)
        if (idx == -1) return "Не поддерживается"
        return parseAscii(sanitized.substring(idx + prefix.length))
    }

    private suspend fun readCvn(): String {
        val raw = protocol.sendCommand("09 06")
        val sanitized = raw.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        val prefix = "4906"
        val idx = sanitized.indexOf(prefix)
        if (idx == -1) return "Не поддерживается"
        return sanitized.substring(idx + prefix.length).take(8)
    }

    private suspend fun readEcuName(): String {
        val raw = protocol.sendCommand("09 0A")
        val sanitized = raw.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        val prefix = "490A"
        val idx = sanitized.indexOf(prefix)
        if (idx == -1) return "Не поддерживается"
        return parseAscii(sanitized.substring(idx + prefix.length))
    }

    private fun parseAscii(hexPayload: String): String {
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i + 2 <= hexPayload.length) {
            val byteHex = hexPayload.substring(i, i + 2)
            i += 2
            val charCode = byteHex.toIntOrNull(16) ?: continue
            if (charCode in 32..126) {
                sb.append(charCode.toChar())
            }
        }
        val res = sb.toString().trim()
        return if (res.isNotBlank()) res else "Не определен"
    }
}
