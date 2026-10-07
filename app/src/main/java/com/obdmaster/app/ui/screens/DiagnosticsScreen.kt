﻿package com.obdmaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.obdmaster.app.core.protocol.DtcItem
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@Composable
fun DiagnosticsScreen(viewModel: ObdViewModel) {
    val dtcs by viewModel.dtcList.collectAsState()
    val isScanning by viewModel.isDtcScanning.collectAsState()
    val hasScanned by viewModel.hasPerformedDtcScan.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var showForceClearDialog by remember { mutableStateOf(false) }

    // Выбор режимов для сканирования (Mode 03, Mode 07, Mode 0A)
    var mode03Selected by remember { mutableStateOf(true) }
    var mode07Selected by remember { mutableStateOf(true) }
    var mode0ASelected by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "ДИАГНОСТИКА ЭБУ (DTC)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Поиск неисправностей и управление памятью ЭБУ",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Выбор режимов перед началом сканирования
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "РЕЖИМЫ ДЛЯ СКАНИРОВАНИЯ",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Режим 03
                    Surface(
                        color = if (mode03Selected) CyanAccent.copy(alpha = 0.15f) else DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (mode03Selected) CyanAccent else DarkBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (mode03Selected && !mode07Selected && !mode0ASelected) {
                                    // Оставляем хотя бы один режим включенным
                                } else {
                                    mode03Selected = !mode03Selected
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = mode03Selected,
                                onCheckedChange = {
                                    if (mode03Selected && !mode07Selected && !mode0ASelected) {
                                        // Оставляем хотя бы один
                                    } else {
                                        mode03Selected = it
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CyanAccent, checkmarkColor = DarkBackground),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "03 • Сохраненные",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mode03Selected) CyanAccent else TextMuted
                            )
                        }
                    }

                    // Режим 07
                    Surface(
                        color = if (mode07Selected) CyanAccent.copy(alpha = 0.15f) else DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (mode07Selected) CyanAccent else DarkBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (mode07Selected && !mode03Selected && !mode0ASelected) {
                                    // Оставляем хотя бы один режим
                                } else {
                                    mode07Selected = !mode07Selected
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = mode07Selected,
                                onCheckedChange = {
                                    if (mode07Selected && !mode03Selected && !mode0ASelected) {
                                        // Оставляем хотя бы один
                                    } else {
                                        mode07Selected = it
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CyanAccent, checkmarkColor = DarkBackground),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "07 • Ожидающие",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mode07Selected) CyanAccent else TextMuted
                            )
                        }
                    }

                    // Режим 0A
                    Surface(
                        color = if (mode0ASelected) CyanAccent.copy(alpha = 0.15f) else DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (mode0ASelected) CyanAccent else DarkBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (mode0ASelected && !mode03Selected && !mode07Selected) {
                                    // Оставляем хотя бы один режим
                                } else {
                                    mode0ASelected = !mode0ASelected
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = mode0ASelected,
                                onCheckedChange = {
                                    if (mode0ASelected && !mode03Selected && !mode07Selected) {
                                        // Оставляем хотя бы один
                                    } else {
                                        mode0ASelected = it
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CyanAccent, checkmarkColor = DarkBackground),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "0A • Постоянные",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mode0ASelected) CyanAccent else TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Главные кнопки: Сканировать и Сбросить найденные
        val canClearFound = !isScanning && hasScanned && dtcs.isNotEmpty()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.scanDtcs() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сканирование...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("🔍 Сканировать ошибки", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Кнопка сброса найденных ошибок: недоступна до сканирования или если 0 ошибок
            Button(
                onClick = { showClearDialog = true },
                enabled = canClearFound,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canClearFound) RedError else DarkSurface,
                    contentColor = if (canClearFound) TextPrimary else TextMuted,
                    disabledContainerColor = DarkSurface,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("🗑️ Сбросить найденные", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (!canClearFound) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (!hasScanned) "ℹ️ Кнопка сброса станет доступной после сканирования при наличии ошибок"
                       else "ℹ️ Ошибок не найдено. Память ЭБУ чиста",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Кнопка «Принудительный сброс (Mode 04) без поиска»
        Card(
            colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, RedError.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ПРЯМОЙ СБРОС CHECK ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedError,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = RedError.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Mode 04",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedError,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showForceClearDialog = true },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ Принудительный сброс (Mode 04) без поиска",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Моментально очищает память блоков ЭБУ в 1 клик без предварительного сканирования",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Список найденных ошибок
        if (dtcs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isScanning) "Опрос блоков управления..." 
                               else if (hasScanned) "Ошибок не обнаружено" 
                               else "Готов к диагностике",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isScanning) CyanAccent else if (hasScanned) GreenAccent else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isScanning) "Считываются выбранные категории кодов..." 
                               else if (hasScanned) "Память ЭБУ чиста. Check Engine не горит" 
                               else "Выберите нужные режимы выше и нажмите «Сканировать ошибки»",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dtcs) { dtc ->
                    DtcCard(dtc)
                }
            }
        }
    }

    // Диалог подтверждения сброса найденных ошибок
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Сброс найденных ошибок (Mode 04)") },
            text = {
                Text("Будет выполнена команда Mode 04 на очистку памяти блоков управления и погашение лампы Check Engine.\n\nУбедитесь, что зажигание включено, а двигатель заглушен!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearDtcs { }
                    }
                ) {
                    Text("Стереть ошибки", color = RedError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Диалог подтверждения принудительного сброса без поиска
    if (showForceClearDialog) {
        AlertDialog(
            onDismissRequest = { showForceClearDialog = false },
            title = { Text("⚠️ Принудительный сброс ЭБУ без поиска") },
            text = {
                Text("Будет отправлена прямая команда Mode 04 на полное стирание диагностической памяти всех блоков авто и погашение лампы Check Engine без предварительного сканирования кодов.\n\nВнимание: Убедитесь, что зажигание включено, а двигатель заглушен!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForceClearDialog = false
                        viewModel.forceClearDtcs { }
                    }
                ) {
                    Text("Стереть память ЭБУ", color = RedError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceClearDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun DtcCard(dtc: DtcItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RedError.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dtc.code,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RedError
                )
                Surface(
                    color = RedError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = dtc.type,
                        color = RedError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = dtc.description,
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
