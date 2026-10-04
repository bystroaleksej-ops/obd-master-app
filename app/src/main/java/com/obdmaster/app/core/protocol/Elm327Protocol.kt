package com.obdmaster.app.core.protocol

import com.obdmaster.app.core.transport.ObdTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class Elm327Protocol(
    private val transport: ObdTransport
) {
    var detectedProtocol: String = "Unknown"
        private set

    var batteryVoltage: String = "--.-V"
        private set

    var isEcuConnected: Boolean = false
        private set

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (!transport.isConnected) return@withContext false

        // Handshake sequence
        delay(300)
        sendCommand("AT Z")
        delay(800)

        sendCommand("AT E0") // Echo off
        sendCommand("AT L0") // Linefeed off
        sendCommand("AT S0") // Spaces off
        sendCommand("AT H0") // Headers off
        sendCommand("AT AT 2") // Aggressive adaptive timing auto 2 (much faster response)
        sendCommand("AT ST 32") // Set timeout to ~200ms (avoids 1-2s freezes on missing PIDs)
        sendCommand("AT SP 0") // Automatic protocol

        val voltResp = sendCommand("AT RV")
        batteryVoltage = voltResp.trim()

        // Check connection to ECU using Mode 01 PID 00
        val ecuTest = sendCommand("01 00")
        isEcuConnected = ecuTest.contains("4100") || ecuTest.contains("41 00")

        val dpResp = sendCommand("AT DP")
        detectedProtocol = dpResp.trim()

        isEcuConnected
    }

    suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        val out: OutputStream = transport.getOutputStream() ?: return@withContext "ERROR: NO OUTPUT STREAM"
        val input: InputStream = transport.getInputStream() ?: return@withContext "ERROR: NO INPUT STREAM"

        try {
            // Write command with carriage return
            val toSend = (cmd.trim() + "\r").toByteArray(Charsets.US_ASCII)
            out.write(toSend)
            out.flush()

            val sb = java.lang.StringBuilder()
            val buffer = ByteArray(256)
            val startTime = System.currentTimeMillis()
            val timeout = 2500L

            while (System.currentTimeMillis() - startTime < timeout) {
                val available = input.available()
                if (available > 0) {
                    val readLen = input.read(buffer, 0, minOf(available, buffer.size))
                    if (readLen > 0) {
                        var promptFound = false
                        for (i in 0 until readLen) {
                            val c = buffer[i].toInt().toChar()
                            if (c == '>') {
                                promptFound = true
                                break
                            }
                            sb.append(c)
                        }
                        if (promptFound) break
                    }
                } else {
                    delay(2) // Short non-blocking yield
                }
            }

            cleanResponse(sb.toString())
        } catch (e: Exception) {
            "ERROR: ${e.localizedMessage ?: "Unknown"}"
        }
    }

    private fun cleanResponse(raw: String): String {
        return raw.replace("\r", "\n")
            .replace("SEARCHING...", "")
            .replace("BUS INIT...", "")
            .trim()
    }

    suspend fun runAutoTest(
        allPids: List<ObdPid>,
        onProgress: (step: String, progress: Float) -> Unit
    ): AutoTestReport = withContext(Dispatchers.IO) {
        onProgress("Опрос версии чипа адаптера...", 0.1f)
        val atiResp = sendCommand("ATI").trim()
        val isV15 = atiResp.contains("1.5", ignoreCase = true)
        val chipName = if (isV15) "ELM327 v1.5 (Оригинал PIC18F25K80)" else "ELM327 ($atiResp)"

        onProgress("Замер скорости отклика адаптера...", 0.25f)
        val t0 = System.currentTimeMillis()
        sendCommand("AT RV")
        val ping = (System.currentTimeMillis() - t0).coerceAtLeast(1L)

        onProgress("Проверка бортового напряжения АКБ...", 0.4f)
        val volt = sendCommand("AT RV").trim().ifBlank { batteryVoltage }

        onProgress("Определение протокола связи...", 0.55f)
        val proto = sendCommand("AT DP").trim().ifBlank { detectedProtocol }

        onProgress("Тестирование датчиков автомобиля...", 0.7f)
        val supportedHexes = mutableSetOf<String>()
        var tested = 0
        for (pid in allPids) {
            val stepProg = 0.7f + (0.28f * (tested.toFloat() / allPids.size.toFloat()))
            onProgress("Тест датчика: ${pid.titleRu} (${pid.pidHex})...", stepProg)
            val resp = sendCommand("01 ${pid.pidHex}")
            if (pid.decode(resp)) {
                supportedHexes.add(pid.pidHex)
            }
            tested++
            delay(15)
        }

        onProgress("Формирование отчета автотеста...", 1.0f)
        val rec = when {
            ping < 60 -> "Скорость отличная! Подходит для Real-Time приборки до 25 FPS."
            ping < 120 -> "Скорость нормальная. Рекомендуется пресет «Спорт (4)» или «База (6)»."
            else -> "Высокая задержка адаптера ($ping мс). Включите только 2-3 критичных датчика."
        }

        AutoTestReport(
            chipVersion = chipName,
            isOriginalChip = isV15,
            pingMs = ping,
            batteryVoltage = volt,
            protocolName = proto,
            supportedPidCount = supportedHexes.size,
            totalTestedCount = allPids.size,
            optimalPidHexes = if (supportedHexes.isNotEmpty()) supportedHexes else setOf("0C", "0D"),
            recommendation = rec
        )
    }
}

data class AutoTestReport(
    val chipVersion: String,
    val isOriginalChip: Boolean,
    val pingMs: Long,
    val batteryVoltage: String,
    val protocolName: String,
    val supportedPidCount: Int,
    val totalTestedCount: Int,
    val optimalPidHexes: Set<String>,
    val recommendation: String
)

