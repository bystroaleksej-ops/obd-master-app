package com.obdmaster.app.core.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class WifiTcpTransport(
    private val host: String = "192.168.0.10",
    private val port: Int = 35000,
    private val timeoutMs: Int = 5000
) : ObdTransport {

    private var socket: Socket? = null

    override val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    override val transportName: String
        get() = "Wi-Fi ($host:$port)"

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), timeoutMs)
            sock.soTimeout = 4000
            socket = sock
            sock.isConnected
        } catch (e: Exception) {
            disconnect()
            false
        }
    }

    override fun disconnect() {
        try {
            socket?.close()
        } catch (ignored: Exception) {
        } finally {
            socket = null
        }
    }

    override fun getInputStream(): InputStream? = socket?.getInputStream()
    override fun getOutputStream(): OutputStream? = socket?.getOutputStream()
}
