package com.obdmaster.app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ConnectionStatus
import com.obdmaster.app.ui.viewmodel.ConnectionType
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(viewModel: ObdViewModel) {
    val connectionType by viewModel.connectionType.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()

    var wifiHost by remember { mutableStateOf("192.168.0.10") }
    var wifiPort by remember { mutableStateOf("35000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "НАСТРОЙКА ПОДКЛЮЧЕНИЯ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Выбор типа адаптера ELM327 (Bluetooth или Wi-Fi)",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Type Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = connectionType == ConnectionType.BLUETOOTH,
                onClick = { viewModel.setConnectionType(ConnectionType.BLUETOOTH) },
                label = { Text("Bluetooth SPP") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent,
                    selectedLabelColor = DarkBackground,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = connectionType == ConnectionType.WIFI,
                onClick = { viewModel.setConnectionType(ConnectionType.WIFI) },
                label = { Text("Wi-Fi (TCP Socket)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent,
                    selectedLabelColor = DarkBackground,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current status card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "ТЕКУЩЕЕ СОСТОЯНИЕ",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val desc = when (status) {
                    is ConnectionStatus.Connected -> "Подключено (${(status as ConnectionStatus.Connected).protocol})"
                    is ConnectionStatus.Connecting -> (status as ConnectionStatus.Connecting).message
                    is ConnectionStatus.Disconnected -> "Отключено"
                    is ConnectionStatus.Error -> "Ошибка: ${(status as ConnectionStatus.Error).error}"
                }
                Text(
                    text = desc,
                    fontSize = 14.sp,
                    color = when (status) {
                        is ConnectionStatus.Connected -> GreenAccent
                        is ConnectionStatus.Connecting -> OrangeWarning
                        is ConnectionStatus.Disconnected -> TextSecondary
                        is ConnectionStatus.Error -> RedError
                    },
                    fontWeight = FontWeight.SemiBold
                )

                if (status is ConnectionStatus.Connected || status is ConnectionStatus.Connecting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Разорвать соединение", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode content
        if (connectionType == ConnectionType.BLUETOOTH) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "СОПРЯЖЕННЫЕ BLUETOOTH УСТРОЙСТВА",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.refreshPairedDevices() }) {
                    Text("Обновить", color = CyanAccent, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (pairedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет сопряженных устройств.\nПодключите ELM327 в настройках Bluetooth Android (PIN: 1234 или 0000).",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pairedDevices) { device ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.connectBluetooth(device) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = device.name ?: "OBDII Адаптер",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = device.address,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = "Подключить",
                                    fontSize = 13.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Wi-Fi Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ПАРАМЕТРЫ WI-FI ПОДКЛЮЧЕНИЯ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = wifiHost,
                        onValueChange = { wifiHost = it },
                        label = { Text("IP адрес адаптера (по умолч. 192.168.0.10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = wifiPort,
                        onValueChange = { wifiPort = it },
                        label = { Text("Порт (по умолч. 35000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val portInt = wifiPort.toIntOrNull() ?: 35000
                            viewModel.connectWifi(wifiHost.trim(), portInt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Подключиться по Wi-Fi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
