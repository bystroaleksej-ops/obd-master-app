package com.obdmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.core.protocol.AutoTestReport
import com.obdmaster.app.core.protocol.ObdPid
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.AutoTestUiState
import com.obdmaster.app.ui.viewmodel.ConnectionStatus
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@Composable
fun DashboardScreen(viewModel: ObdViewModel) {
    val pids by viewModel.pids.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val autoTestState by viewModel.autoTestState.collectAsState()
    val tick by viewModel.telemetryTick.collectAsState()
    var showSensorDialog by remember { mutableStateOf(false) }

    if (showSensorDialog) {
        SensorSelectionDialog(
            allPids = pids,
            selectedHexes = settings.selectedPidHexes,
            onTogglePid = { hex -> viewModel.togglePidSelection(hex) },
            onSetPreset = { preset -> viewModel.setPidSelectionPreset(preset) },
            onDismiss = { showSensorDialog = false }
        )
    }

    AutoTestModal(
        state = autoTestState,
        onApplyOptimal = { report -> viewModel.applyOptimalSensors(report) },
        onDismiss = { viewModel.dismissAutoTest() }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Status header
        StatusHeader(
            status = status,
            onRunAutoTest = { viewModel.runAutoTest() }
        )

        // Панель датчиков (Вариант 1: строго выбранные пользователем датчики)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ПРИБОРЫ И ДАТЧИКИ",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Button(
                onClick = { showSensorDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "⚙️ Датчики (${settings.selectedPidHexes.size}/${pids.size})",
                    fontSize = 11.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Строго выбранные датчики (включая обороты и скорость, если они выбраны)
        val activePids = pids.filter { it.pidHex in settings.selectedPidHexes }

        if (activePids.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет выбранных датчиков.\nНажмите «⚙️ Датчики» выше для выбора.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activePids) { pid ->
                    val (dispVal, dispUnit) = pid.getDisplayValue(settings)
                    val (dispMin, dispMax) = pid.getDisplayMinMax(settings)

                    // Проверка тревоги для подсветки карточки
                    val isAlarming = settings.alarmMasterEnabled && when (pid.pidHex) {
                        "05" -> settings.coolantAlarmEnabled && pid.currentValue >= settings.coolantAlarmThresholdC
                        "0D" -> settings.speedAlarmEnabled && pid.currentValue >= settings.speedAlarmThresholdKmh
                        "0C" -> settings.rpmAlarmEnabled && pid.currentValue >= settings.rpmAlarmThresholdRpm
                        "42" -> settings.batteryAlarmEnabled && pid.currentValue > 5f && pid.currentValue <= settings.batteryAlarmThresholdV
                        else -> false
                    }

                    PidCard(
                        title = pid.titleRu,
                        displayValue = dispVal,
                        unit = dispUnit,
                        currentValue = pid.currentValue,
                        minVal = dispMin,
                        maxVal = dispMax,
                        isAlarm = isAlarming
                    )
                }
            }
        }
    }
}

@Composable
fun SensorSelectionDialog(
    allPids: List<ObdPid>,
    selectedHexes: Set<String>,
    onTogglePid: (String) -> Unit,
    onSetPreset: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "ВЫБОР ДАТЧИКОВ",
                    color = CyanAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Чем меньше датчиков — тем быстрее обновление онлайн!",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Быстрые пресеты:",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val sportHexes = setOf("0C", "0D", "04", "11") // RPM, Speed, Load, Throttle
                val standardHexes = setOf("0C", "0D", "05", "04", "11", "42") // + Coolant, Voltage
                val allHexes = allPids.map { it.pidHex }.toSet()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onSetPreset(sportHexes) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedHexes == sportHexes) CyanAccent else DarkBackground
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "⚡ Спорт (4)",
                            fontSize = 10.sp,
                            color = if (selectedHexes == sportHexes) DarkBackground else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onSetPreset(standardHexes) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedHexes == standardHexes) CyanAccent else DarkBackground
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🚗 База (6)",
                            fontSize = 10.sp,
                            color = if (selectedHexes == standardHexes) DarkBackground else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onSetPreset(allHexes) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedHexes == allHexes) CyanAccent else DarkBackground
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🌐 Все (${allPids.size})",
                            fontSize = 10.sp,
                            color = if (selectedHexes == allHexes) DarkBackground else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allPids) { pid ->
                        val isChecked = pid.pidHex in selectedHexes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) DarkBackground else DarkCard)
                                .clickable { onTogglePid(pid.pidHex) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pid.titleRu,
                                    color = if (isChecked) TextPrimary else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = "[PID ${pid.pidHex}] • ${pid.category}",
                                    color = if (isChecked) CyanAccent else TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { onTogglePid(pid.pidHex) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyanAccent,
                                    checkedTrackColor = CyanAccent.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "ГОТОВО", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun StatusHeader(
    status: ConnectionStatus,
    onRunAutoTest: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "СТАТУС ПОДКЛЮЧЕНИЯ",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                val statusText = when (status) {
                    is ConnectionStatus.Connected -> "АКТИВНО: ${status.protocol}"
                    is ConnectionStatus.Connecting -> "Подключение..."
                    is ConnectionStatus.Disconnected -> "Отключено"
                    is ConnectionStatus.Error -> "Ошибка: ${status.message}"
                }
                val statusColor = when (status) {
                    is ConnectionStatus.Connected -> GreenAccent
                    is ConnectionStatus.Connecting -> OrangeWarning
                    is ConnectionStatus.Disconnected -> TextMuted
                    is ConnectionStatus.Error -> RedError
                }
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            if (status is ConnectionStatus.Connected) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRunAutoTest,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "⚡ Автотест",
                            fontSize = 11.sp,
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "АКБ: ${status.adapterInfo}",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroGaugeCard(
    title: String,
    value: String,
    unit: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 38.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun PidCard(
    title: String,
    displayValue: String,
    unit: String,
    currentValue: Float,
    minVal: Float,
    maxVal: Float,
    isAlarm: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAlarm) RedError.copy(alpha = 0.2f) else DarkSurface
        ),
        border = if (isAlarm) androidx.compose.foundation.BorderStroke(1.5.dp, RedError) else null,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = if (isAlarm) RedError else TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayValue,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlarm) RedError else TextPrimary
                )
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = if (isAlarm) RedError else TextMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Linear gauge indicator bar
            val progress = if (maxVal > minVal) ((currentValue - minVal) / (maxVal - minVal)).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isAlarm) RedError else CyanAccent,
                trackColor = DarkBorder,
            )
        }
    }
}

@Composable
fun AutoTestModal(
    state: AutoTestUiState,
    onApplyOptimal: (AutoTestReport) -> Unit,
    onDismiss: () -> Unit
) {
    if (state is AutoTestUiState.Idle) return

    AlertDialog(
        onDismissRequest = {
            if (state is AutoTestUiState.Completed) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚡ ЭКСПРЕСС-АВТОТЕСТ",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (state) {
                    is AutoTestUiState.Running -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.step,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanAccent,
                            trackColor = DarkBorder
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(state.progress * 100).toInt()}% завершено",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    is AutoTestUiState.Completed -> {
                        val r = state.report
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoTestInfoRow(label = "Адаптер:", value = r.chipVersion, isGood = r.isOriginalChip)
                            AutoTestInfoRow(label = "Задержка (пинг):", value = "${r.pingMs} мс", isGood = r.pingMs < 100)
                            AutoTestInfoRow(label = "Напряжение АКБ:", value = r.batteryVoltage, isGood = true)
                            AutoTestInfoRow(label = "Протокол шины:", value = r.protocolName, isGood = true)
                            AutoTestInfoRow(label = "Датчики авто:", value = "${r.supportedPidCount} из ${r.totalTestedCount} активны", isGood = r.supportedPidCount > 0)

                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = DarkCard,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 ${r.recommendation}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (state is AutoTestUiState.Completed) {
                Button(
                    onClick = { onApplyOptimal(state.report) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ПРИМЕНИТЬ ДАТЧИКИ",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = {
            if (state is AutoTestUiState.Completed) {
                TextButton(onClick = onDismiss) {
                    Text(text = "ЗАКРЫТЬ", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun AutoTestInfoRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(
            text = value,
            color = if (isGood) GreenAccent else OrangeWarning,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

