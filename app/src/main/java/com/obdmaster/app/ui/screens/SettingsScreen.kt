package com.obdmaster.app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.data.AppSettings
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ConnectionStatus
import com.obdmaster.app.ui.viewmodel.ConnectionType
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen(viewModel: ObdViewModel) {
    val settings by viewModel.appSettings.collectAsState()
    val connectionType by viewModel.connectionType.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()

    var wifiHost by remember { mutableStateOf("192.168.0.10") }
    var wifiPort by remember { mutableStateOf("35000") }
    var showSensorThresholdsDialog by remember { mutableStateOf(false) }

    // Автоматическое обновление списка сопряженных устройств при открытии экрана
    LaunchedEffect(connectionType) {
        if (connectionType == ConnectionType.BLUETOOTH) {
            viewModel.refreshPairedDevices()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "НАСТРОЙКИ И СВЯЗЬ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Управление адаптером ELM327, тревогами и датчиками",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // ================= 1. СВЯЗЬ И АДАПТЕР (СВЕРХУ) =================
        item {
            SettingsCategoryCard(title = "СВЯЗЬ И АДАПТЕР ELM327", icon = Icons.Default.Bluetooth) {
                // Статус подключения
                val statusText = when (status) {
                    is ConnectionStatus.Connected -> "Подключено (${(status as ConnectionStatus.Connected).protocol})"
                    is ConnectionStatus.Connecting -> (status as ConnectionStatus.Connecting).message
                    is ConnectionStatus.Disconnected -> "Отключено"
                    is ConnectionStatus.Error -> "Ошибка: ${(status as ConnectionStatus.Error).error}"
                }
                val statusColor = when (status) {
                    is ConnectionStatus.Connected -> GreenAccent
                    is ConnectionStatus.Connecting -> OrangeWarning
                    is ConnectionStatus.Disconnected -> TextSecondary
                    is ConnectionStatus.Error -> RedError
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ТЕКУЩИЙ СТАТУС", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text(text = statusText, fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }

                    if (status is ConnectionStatus.Connected || status is ConnectionStatus.Connecting) {
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedError),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Отключить", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Выбор типа адаптера: Bluetooth / Wi-Fi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = connectionType == ConnectionType.BLUETOOTH,
                        onClick = { viewModel.setConnectionType(ConnectionType.BLUETOOTH) },
                        label = { Text("Bluetooth SPP", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = connectionType == ConnectionType.WIFI,
                        onClick = { viewModel.setConnectionType(ConnectionType.WIFI) },
                        label = { Text("Wi-Fi (TCP)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (connectionType == ConnectionType.BLUETOOTH) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "СОПРЯЖЕННЫЕ BLUETOOTH УСТРОЙСТВА",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.refreshPairedDevices() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Обновить", color = CyanAccent, fontSize = 11.sp)
                        }
                    }

                    if (pairedDevices.isEmpty()) {
                        Text(
                            text = "Нет сопряженных устройств. Сопрягите OBD2 адаптер в настройках Bluetooth телефона.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            pairedDevices.forEach { device ->
                                val isConnected = status is ConnectionStatus.Connected &&
                                        settings.lastConnectedDeviceMac == device.address

                                Surface(
                                    color = if (isConnected) CyanAccent.copy(alpha = 0.15f) else DarkCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isConnected) CyanAccent else DarkBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (status !is ConnectionStatus.Connected) {
                                                viewModel.connectBluetooth(device)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = device.name ?: "OBDII Adapter",
                                                color = if (isConnected) CyanAccent else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = device.address,
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        if (isConnected) {
                                            Text("Подключено", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Button(
                                                onClick = { viewModel.connectBluetooth(device) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Подключить", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Wi-Fi inputs
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wifiHost,
                            onValueChange = { wifiHost = it },
                            label = { Text("IP адрес адаптера") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = wifiPort,
                            onValueChange = { wifiPort = it },
                            label = { Text("Порт TCP") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Button(
                            onClick = {
                                viewModel.connectWifi(wifiHost, wifiPort.toIntOrNull() ?: 35000)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Подключиться по Wi-Fi", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ================= 2. СКОРОСТЬ ОПРОСА ШИНЫ =================
        item {
            SettingsCategoryCard(title = "СКОРОСТЬ И ПАУЗА ОПРОСА ШИНЫ", icon = Icons.Default.Speed) {
                Text(
                    text = "Микропауза между кругами опроса датчиков:",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "0–20 мс обеспечивает максимальную плавность стрелок и приборов. 100–250 мс снижает нагрузку на шину.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val intervals = listOf(
                        0L to "0 мс (Турбо)",
                        20L to "20 мс (Быстро)",
                        50L to "50 мс",
                        100L to "100 мс (Обычная)",
                        250L to "250 мс (Эконом)"
                    )
                    items(intervals) { (ms, label) ->
                        FilterChip(
                            selected = settings.pollingIntervalMs == ms,
                            onClick = { viewModel.updateSettings(settings.copy(pollingIntervalMs = ms)) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // ================= 3. СИСТЕМА ТРЕВОГ И ОПОВЕЩЕНИЙ =================
        item {
            SettingsCategoryCard(title = "СИСТЕМА ТРЕВОГ И ОПОВЕЩЕНИЙ", icon = Icons.Default.Warning) {
                SettingsSwitchRow(
                    title = "Звуковые и вибро-тревоги",
                    subtitle = "Зуммер и вибрация при перегреве, отсечке или превышении скорости",
                    checked = settings.alarmMasterEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(alarmMasterEnabled = it)) }
                )

                if (settings.alarmMasterEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Интервал повтора сигнала при тревоге:",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val repeatOptions = listOf(
                            2 to "2 сек (Турбо)",
                            5 to "5 сек",
                            15 to "15 сек",
                            30 to "30 сек",
                            60 to "60 сек",
                            -1 to "1 раз"
                        )
                        items(repeatOptions) { (sec, label) ->
                            FilterChip(
                                selected = settings.alarmRepeatIntervalSec == sec,
                                onClick = { viewModel.updateSettings(settings.copy(alarmRepeatIntervalSec = sec)) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeWarning,
                                    selectedLabelColor = DarkBackground,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Кнопка настройки датчиков и индивидуальных порогов
                Button(
                    onClick = { showSensorThresholdsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, CyanAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚙️ Настройка датчиков, единиц и порогов",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ================= 4. ЭКРАН =================
        item {
            SettingsCategoryCard(title = "ЭКРАН И ПОВЕДЕНИЕ", icon = Icons.Default.Settings) {
                SettingsSwitchRow(
                    title = "Не выключать экран (Keep Screen On)",
                    subtitle = "Запретить телефону блокироваться в режиме приборов",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(keepScreenOn = it)) }
                )
            }
        }
    }

    // Модальное окно индивидуальной настройки каждого датчика и порогов
    if (showSensorThresholdsDialog) {
        SensorThresholdsCustomizationDialog(
            settings = settings,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onDismiss = { showSensorThresholdsDialog = false }
        )
    }
}

@Composable
fun SensorThresholdsCustomizationDialog(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "НАСТРОЙКА ДАТЧИКОВ И ПОРОГОВ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxHeight(0.75f)
            ) {
                // ЕДИНИЦЫ ИЗМЕРЕНИЯ
                item {
                    Text(text = "ЕДИНИЦЫ ИЗМЕРЕНИЯ", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Скорость: км/ч / mph
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Скорость:", fontSize = 12.sp, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = settings.speedUnit == "км/ч",
                                onClick = { onUpdateSettings(settings.copy(speedUnit = "км/ч")) },
                                label = { Text("км/ч", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = settings.speedUnit == "mph",
                                onClick = { onUpdateSettings(settings.copy(speedUnit = "mph")) },
                                label = { Text("mph", fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Температура: °C / °F
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Температура:", fontSize = 12.sp, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = settings.tempUnit == "°C",
                                onClick = { onUpdateSettings(settings.copy(tempUnit = "°C")) },
                                label = { Text("°C", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = settings.tempUnit == "°F",
                                onClick = { onUpdateSettings(settings.copy(tempUnit = "°F")) },
                                label = { Text("°F", fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Давление наддува: кПа / бар
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Давление наддува:", fontSize = 12.sp, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = settings.pressureUnit == "кПа",
                                onClick = { onUpdateSettings(settings.copy(pressureUnit = "кПа")) },
                                label = { Text("кПа", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = settings.pressureUnit == "бар",
                                onClick = { onUpdateSettings(settings.copy(pressureUnit = "бар")) },
                                label = { Text("бар", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                }

                // ПОРОГИ ТРЕВОГ ПО ДАТЧИКАМ
                item {
                    Text(text = "ИНДИВИДУАЛЬНЫЕ ПОРОГИ ТРЕВОГ", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                }

                // Температура ОЖ
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        SettingsSwitchRow(
                            title = "Перегрев двигателя (ОЖ)",
                            subtitle = "Тревога при >= ${settings.coolantAlarmThresholdC} °C",
                            checked = settings.coolantAlarmEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(coolantAlarmEnabled = it)) }
                        )
                        if (settings.coolantAlarmEnabled) {
                            Slider(
                                value = settings.coolantAlarmThresholdC.toFloat(),
                                onValueChange = { onUpdateSettings(settings.copy(coolantAlarmThresholdC = it.toInt())) },
                                valueRange = 90f..120f,
                                steps = 30,
                                colors = SliderDefaults.colors(thumbColor = RedError, activeTrackColor = RedError)
                            )
                        }
                    }
                }

                // Превышение скорости
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        SettingsSwitchRow(
                            title = "Превышение скорости",
                            subtitle = "Тревога при >= ${settings.speedAlarmThresholdKmh} км/ч",
                            checked = settings.speedAlarmEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(speedAlarmEnabled = it)) }
                        )
                        if (settings.speedAlarmEnabled) {
                            Slider(
                                value = settings.speedAlarmThresholdKmh.toFloat(),
                                onValueChange = { onUpdateSettings(settings.copy(speedAlarmThresholdKmh = it.toInt())) },
                                valueRange = 60f..200f,
                                steps = 28,
                                colors = SliderDefaults.colors(thumbColor = RedError, activeTrackColor = RedError)
                            )
                        }
                    }
                }

                // Обороты двигателя (отсечка)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        SettingsSwitchRow(
                            title = "Отсечка по оборотам (RPM)",
                            subtitle = "Тревога при >= ${settings.rpmAlarmThresholdRpm} об/мин",
                            checked = settings.rpmAlarmEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(rpmAlarmEnabled = it)) }
                        )
                        if (settings.rpmAlarmEnabled) {
                            Slider(
                                value = settings.rpmAlarmThresholdRpm.toFloat(),
                                onValueChange = { onUpdateSettings(settings.copy(rpmAlarmThresholdRpm = it.toInt())) },
                                valueRange = 3000f..7500f,
                                steps = 45,
                                colors = SliderDefaults.colors(thumbColor = OrangeWarning, activeTrackColor = OrangeWarning)
                            )
                        }
                    }
                }

                // Разряд аккумулятора
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        SettingsSwitchRow(
                            title = "Низкое напряжение АКБ",
                            subtitle = "Тревога при <= ${String.format(java.util.Locale.US, "%.1f", settings.batteryAlarmThresholdV)} В",
                            checked = settings.batteryAlarmEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(batteryAlarmEnabled = it)) }
                        )
                        if (settings.batteryAlarmEnabled) {
                            Slider(
                                value = settings.batteryAlarmThresholdV,
                                onValueChange = { onUpdateSettings(settings.copy(batteryAlarmThresholdV = it)) },
                                valueRange = 10.5f..12.5f,
                                steps = 20,
                                colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ГОТОВО", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SettingsCategoryCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBackground,
                checkedTrackColor = CyanAccent,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkCard
            )
        )
    }
}
