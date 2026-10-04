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
        sendCommand("AT AT 1") // Adaptive timing auto 1
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
            var b: Int
            val startTime = System.currentTimeMillis()
            val timeout = 4000L

            while (System.currentTimeMillis() - startTime < timeout) {
                if (input.available() > 0) {
                    b = input.read()
                    if (b == -1) break
                    val c = b.toChar()
                    if (c == '>') {
                        break // End of ELM327 response
                    }
                    sb.append(c)
                } else {
                    delay(10)
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
}
