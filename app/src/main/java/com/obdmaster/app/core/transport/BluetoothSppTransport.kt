package com.obdmaster.app.core.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothSppTransport(
    private val device: BluetoothDevice
) : ObdTransport {

    companion object {
        // Standard SPP UUID for Serial Port Profile (ELM327)
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null

    override val isConnected: Boolean
        get() = socket?.isConnected == true

    override val transportName: String
        get() = try {
            "${device.name ?: "OBDII"} (${device.address})"
        } catch (e: Exception) {
            device.address
        }

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            socket?.isConnected == true
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

    override fun getInputStream(): InputStream? = socket?.inputStream
    override fun getOutputStream(): OutputStream? = socket?.outputStream
}
