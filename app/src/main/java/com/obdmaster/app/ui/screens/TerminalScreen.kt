package com.obdmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ObdViewModel

data class TerminalCommandInfo(
    val command: String,
    val shortTitleRu: String,
    val descriptionRu: String,
    val category: String
)

val COMMANDS_REFERENCE = listOf(
    TerminalCommandInfo("AT Z", "Рестарт", "Сброс и перезагрузка чипа ELM327", "Управление адаптером"),
    TerminalCommandInfo("AT RV", "Вольтаж АКБ", "Точный замер напряжения аккумулятора и бортовой сети", "Управление адаптером"),
    TerminalCommandInfo("AT I", "Версия ELM327", "Запрос заводской версии прошивки чипа адаптера", "Управление адаптером"),
    TerminalCommandInfo("AT DP", "Протокол связи", "Показать текстовое название текущего протокола OBD-II", "Управление адаптером"),
    TerminalCommandInfo("AT SP 0", "Авто-протокол", "Включить автоматический подбор протокола подключения", "Управление адаптером"),
    TerminalCommandInfo("AT @1", "Имя чипа", "Считать сохраненное сервисное имя в чипе", "Управление адаптером"),
    TerminalCommandInfo("09 02", "VIN-код авто", "Считывание 17-значного заводского номера кузова (VIN)", "Данные авто (Mode 09)"),
    TerminalCommandInfo("09 04", "Калибровка ЭБУ", "Считывание версии прошивки и ID калибровок ЭБУ", "Данные авто (Mode 09)"),
    TerminalCommandInfo("09 06", "Контрольная сумма", "Считывание контрольной суммы прошивки (CVN)", "Данные авто (Mode 09)"),
    TerminalCommandInfo("09 0A", "Название ЭБУ", "Считывание сервисного имени блока управления двигателем", "Данные авто (Mode 09)"),
    TerminalCommandInfo("03", "Чтение ошибок DTC", "Считать все сохраненные коды неисправностей (Stored DTCs)", "Диагностика ошибок"),
    TerminalCommandInfo("07", "Ожидающие ошибки", "Считать ошибки под подозрением (Pending DTCs)", "Диагностика ошибок"),
    TerminalCommandInfo("0A", "Постоянные ошибки", "Считать постоянные ошибки из энергонезависимой памяти", "Диагностика ошибок"),
    TerminalCommandInfo("04", "Сброс Check Engine", "Стереть все ошибки из ЭБУ и погасить лампу Check Engine", "Диагностика ошибок"),
    TerminalCommandInfo("01 00", "PIDs 01-20", "Проверить, какие базовые датчики поддерживает ЭБУ", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 20", "PIDs 21-40", "Проверить поддерживаемые датчики второго диапазона", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 0C", "Обороты (RPM)", "Текущие обороты коленчатого вала двигателя", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 0D", "Скорость авто", "Текущая скорость автомобиля в км/ч", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 05", "Температура ОЖ", "Температура охлаждающей жидкости (антифриза)", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 11", "Дроссель", "Положение педали газа / дроссельной заслонки в %", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 04", "Нагрузка мотора", "Расчетная нагрузка на двигатель в %", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 10", "Расход воздуха MAF", "Массовый расход воздуха на впуске (г/с)", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 0B", "Давление MAP", "Абсолютное давление во впускном коллекторе", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 0F", "Температура IAT", "Температура воздуха на впуске", "Датчики (Mode 01)")
)

@Composable
fun TerminalScreen(viewModel: ObdViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    val vehicleInfo by viewModel.vehicleInfo.collectAsState()
    var inputCommand by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Find description for current input command
    val activeHelp = remember(inputCommand) {
        val trimmed = inputCommand.trim().uppercase()
        COMMANDS_REFERENCE.find { it.command.uppercase() == trimmed }
    }

    // Auto scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Vehicle Info Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ПАСПОРТНЫЕ ДАННЫЕ (MODE 09)",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showReferenceDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Справка", tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Справочник команд", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "VIN: ${vehicleInfo.vin}", fontSize = 13.sp, color = CyanAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(text = "АКБ: ${vehicleInfo.batteryVoltage}", fontSize = 13.sp, color = GreenAccent, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Протокол: ${vehicleInfo.protocolName}", fontSize = 11.sp, color = TextSecondary)
                    Text(text = "Адаптер: ${vehicleInfo.adapterVersion}", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal Log Output (Takes maximum screen area now)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    val color = when {
                        log.startsWith(">") -> CyanAccent
                        log.startsWith("ERR") || log.contains("ERROR") -> RedError
                        log.contains("OK") || log.contains("Подключено") -> GreenAccent
                        else -> TextPrimary
                    }
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = color,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic explanation of current command if recognized
        if (activeHelp != null) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ℹ️ ${activeHelp.shortTitleRu}: ",
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontSize = 11.sp
                    )
                    Text(
                        text = activeHelp.descriptionRu,
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Input Bar with Dropdown command picker and Send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    placeholder = { Text("Команда или выберите из списка...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Выбрать команду",
                                tint = CyanAccent,
                                modifier = Modifier.rotate(if (isDropdownExpanded) 180f else 0f)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    )
                )

                // Dropdown menu showing all available commands
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 350.dp)
                        .background(DarkSurface)
                ) {
                    COMMANDS_REFERENCE.forEach { cmd ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cmd.command,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyanAccent,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = cmd.category,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Text(
                                        text = "${cmd.shortTitleRu} — ${cmd.descriptionRu}",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            },
                            onClick = {
                                inputCommand = cmd.command
                                isDropdownExpanded = false
                            }
                        )
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.sendTerminalCommand(inputCommand.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Отправить", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal dialog with Full Commands Reference
    if (showReferenceDialog) {
        AlertDialog(
            onDismissRequest = { showReferenceDialog = false },
            title = {
                Text(
                    text = "СПРАВОЧНИК КОМАНД OBD-II И ELM327",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.7f)
                ) {
                    items(COMMANDS_REFERENCE) { item ->
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inputCommand = item.command
                                    viewModel.sendTerminalCommand(item.command)
                                    showReferenceDialog = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.command,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanAccent,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = item.shortTitleRu,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.descriptionRu,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReferenceDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBackground)
                ) {
                    Text("Закрыть")
                }
            },
            containerColor = DarkSurface
        )
    }
}
