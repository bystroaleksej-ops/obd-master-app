package com.obdmaster.app.core.protocol

import com.obdmaster.app.data.DtcDictionary

data class DtcItem(
    val code: String,
    val description: String,
    val type: String // "Подтвержденная (Stored)", "Ожидающая (Pending)", "Постоянная (Permanent)"
)

class DtcService(
    private val protocol: Elm327Protocol
) {
    suspend fun readStoredDtcs(): List<DtcItem> {
        val raw = protocol.sendCommand("03")
        return parseDtcResponse(raw, "Подтвержденная (Stored)", "43")
    }

    suspend fun readPendingDtcs(): List<DtcItem> {
        val raw = protocol.sendCommand("07")
        return parseDtcResponse(raw, "Ожидающая (Pending)", "47")
    }

    suspend fun readPermanentDtcs(): List<DtcItem> {
        val raw = protocol.sendCommand("0A")
        return parseDtcResponse(raw, "Постоянная (Permanent)", "4A")
    }

    suspend fun clearDtcs(): Boolean {
        val resp = protocol.sendCommand("04")
        return resp.contains("44") || resp.contains("OK")
    }

    private fun parseDtcResponse(raw: String, typeName: String, expectedServiceResponse: String): List<DtcItem> {
        val sanitized = raw.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        if (sanitized.contains("NODATA") || sanitized.contains("4300") || sanitized.contains("4700") || sanitized.contains("4A00")) {
            return emptyList()
        }

        val items = mutableListOf<DtcItem>()
        // Look for service response prefix e.g. "43"
        val idx = sanitized.indexOf(expectedServiceResponse)
        if (idx == -1) return emptyList()

        val dataPayload = sanitized.substring(idx + expectedServiceResponse.length)
        var i = 0
        while (i + 4 <= dataPayload.length) {
            val byte1Hex = dataPayload.substring(i, i + 2)
            val byte2Hex = dataPayload.substring(i + 2, i + 4)
            i += 4

            val b1 = byte1Hex.toIntOrNull(16) ?: continue
            val b2 = byte2Hex.toIntOrNull(16) ?: continue

            // Code 0000 means no code or padding
            if (b1 == 0 && b2 == 0) continue

            val code = decodeDtcBytes(b1, b2)
            if (code.isNotBlank()) {
                val desc = DtcDictionary.getDescription(code)
                items.add(DtcItem(code = code, description = desc, type = typeName))
            }
        }
        return items.distinctBy { it.code }
    }

    private fun decodeDtcBytes(b1: Int, b2: Int): String {
        val firstChar = when ((b1 and 0xC0) shr 6) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> 'P'
        }

        val secondChar = when ((b1 and 0x30) shr 4) {
            0 -> '0'
            1 -> '1'
            2 -> '2'
            3 -> '3'
            else -> '0'
        }

        val thirdChar = Integer.toHexString(b1 and 0x0F).uppercase()
        val fourthAndFifth = String.format("%02X", b2).uppercase()

        return "$firstChar$secondChar$thirdChar$fourthAndFifth"
    }
}
