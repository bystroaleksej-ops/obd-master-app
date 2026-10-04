package com.obdmaster.app.core.transport

import java.io.InputStream
import java.io.OutputStream

interface ObdTransport {
    val isConnected: Boolean
    val transportName: String

    suspend fun connect(): Boolean
    fun disconnect()
    fun getInputStream(): InputStream?
    fun getOutputStream(): OutputStream?
}
