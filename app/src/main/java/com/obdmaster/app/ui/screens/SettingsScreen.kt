package com.obdmaster.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.data.AppSettings
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@Composable
fun SettingsScreen(viewModel: ObdViewModel) {
    val settings by viewModel.appSettings.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "НАСТРОЙКИ ПРИЛОЖЕНИЯ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
            Text(
                text = "Параметры связи, безопасности, экрана и терминала",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // ================= 1. СВЯЗЬ И АДАПТЕР =================
        item {
            SettingsCategoryCard(title = "СВЯЗЬ И АДАПТЕР ELM327", icon = Icons.Default.Bluetooth) {
                // Protocol selection
                Text(text = "Протокол подключения OBD-II:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                val protocols = listOf("Авто (AT SP 0)", "CAN 11b 500k", "CAN 29b 500k", "KWP Fast", "ISO 9141-2")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    protocols.take(3).forEachIndexed { idx, name ->
                        FilterChip(
                            selected = settings.protocolIndex == idx,
                            onClick = { viewModel.updateSettings(settings.copy(protocolIndex = idx)) },
                            label = { Text(name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Polling Pause between cycles
                Text(text = "Пауза между кругами опроса:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Микропауза после опроса всех выбранных датчиков перед началом нового круга. 0–20 мс — максимальная скорость и плавность приборов.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Reconnect Switch
                SettingsSwitchRow(
                    title = "Авто-переподключение",
                    subtitle = "Автоматически восстанавливать связь при сбоях",
                    checked = settings.autoReconnect,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoReconnect = it)) }
                )
            }
        }

        // ================= 2. ОПОВЕЩЕНИЯ И ЗАЩИТА МОТОРА =================
        item {
            SettingsCategoryCard(title = "ЗАЩИТА И ОПОВЕЩЕНИЯ", icon = Icons.Default.Warning) {
                // Coolant alarm
                SettingsSwitchRow(
                    title = "Тревога по температуре ОЖ",
                    subtitle = "Сигнал перегрева при ${settings.coolantAlarmThresholdC}°C",
                    checked = settings.coolantAlarmEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(coolantAlarmEnabled = it)) }
                )
                if (settings.coolantAlarmEnabled) {
                    Slider(
                        value = settings.coolantAlarmThresholdC.toFloat(),
                        onValueChange = { viewModel.updateSettings(settings.copy(coolantAlarmThresholdC = it.toInt())) },
                        valueRange = 95f..115f,
                        steps = 20,
                        colors = SliderDefaults.colors(thumbColor = RedError, activeTrackColor = RedError)
                    )
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Battery alarm
                SettingsSwitchRow(
                    title = "Контроль разряда АКБ",
                    subtitle = "Предупреждать при падении ниже ${"%.1f".format(settings.batteryAlarmThresholdV)} В",
                    checked = settings.batteryAlarmEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(batteryAlarmEnabled = it)) }
                )
                if (settings.batteryAlarmEnabled) {
                    Slider(
                        value = settings.batteryAlarmThresholdV,
                        onValueChange = { viewModel.updateSettings(settings.copy(batteryAlarmThresholdV = it)) },
                        valueRange = 11.0f..12.4f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Shift Light
                SettingsSwitchRow(
                    title = "Оповещение по оборотам (Shift Light)",
                    subtitle = "Вспышка панели при достижении ${settings.rpmAlarmThreshold} об/мин",
                    checked = settings.rpmAlarmEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(rpmAlarmEnabled = it)) }
                )
                if (settings.rpmAlarmEnabled) {
                    Slider(
                        value = settings.rpmAlarmThreshold.toFloat(),
                        onValueChange = { viewModel.updateSettings(settings.copy(rpmAlarmThreshold = it.toInt())) },
                        valueRange = 3000f..7000f,
                        steps = 40,
                        colors = SliderDefaults.colors(thumbColor = GreenAccent, activeTrackColor = GreenAccent)
                    )
                }
            }
        }

        // ================= 3. ЭКРАН И ИНТЕРФЕЙС =================
        item {
            SettingsCategoryCard(title = "ЭКРАН И ИНТЕРФЕЙС", icon = Icons.Default.Settings) {
                SettingsSwitchRow(
                    title = "Не выключать экран (Keep Screen On)",
                    subtitle = "Запретить телефону блокироваться в режиме приборов",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(keepScreenOn = it)) }
                )

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Режим проекции HUD",
                    subtitle = "Зеркальное отображение для отражения от лобового стекла ночью",
                    checked = settings.hudMode,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(hudMode = it)) }
                )

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Units selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Единицы измерения:", fontSize = 12.sp, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = settings.speedUnit == "км/ч",
                            onClick = { viewModel.updateSettings(settings.copy(speedUnit = "км/ч")) },
                            label = { Text("км/ч", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = settings.speedUnit == "mph",
                            onClick = { viewModel.updateSettings(settings.copy(speedUnit = "mph")) },
                            label = { Text("mph", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // ================= 4. ДИАГНОСТИКА И ТЕРМИНАЛ =================
        item {
            SettingsCategoryCard(title = "ДИАГНОСТИКА И ТЕРМИНАЛ", icon = Icons.Default.Search) {
                SettingsSwitchRow(
                    title = "Глубокое сканирование всех блоков",
                    subtitle = "Опрашивать не только ДВС (ECU), но и АКПП (TCM)",
                    checked = settings.deepScanAllModules,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(deepScanAllModules = it)) }
                )

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Автоперевод ответов в терминале",
                    subtitle = "Показывать расшифровку параметров на русском языке",
                    checked = settings.terminalAutoTranslate,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(terminalAutoTranslate = it)) }
                )
            }
        }
    }
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
