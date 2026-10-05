package com.obdmaster.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
    TerminalCommandInfo("01 0F", "Температура IAT", "Температура воздуха на впуске", "Датчики (Mode 01)"),
    TerminalCommandInfo("01 42", "Напряжение ЭБУ", "Питание на выходе блока управления в Вольтах", "Датчики (Mode 01)")
)

@Composable
fun TerminalScreen(viewModel: ObdViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    val vehicleInfo by viewModel.vehicleInfo.collectAsState()
    var showReferenceDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Авто-прокрутка к последней записи
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
        // Паспортные данные (Mode 09)
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
                        Text("Справка", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "VIN НОМЕР", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (vehicleInfo.vin.isNotBlank()) vehicleInfo.vin else "Не прочитан (нажмите 09 02 ниже)",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (vehicleInfo.vin.isNotBlank()) CyanAccent else TextSecondary
                        )
                    }
                    if (vehicleInfo.calibrationId.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "КАЛИБРОВКА (CAL ID)", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = vehicleInfo.calibrationId,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Горизонтальная лента со ВСЕМИ командами (отправка в 1 клик!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "БЫСТРЫЕ КОМАНДЫ (1 КЛИК)",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "${COMMANDS_REFERENCE.size} команд",
                fontSize = 11.sp,
                color = CyanAccent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(COMMANDS_REFERENCE) { cmd ->
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.clickable {
                        viewModel.sendTerminalCommand(cmd.command)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cmd.command,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "• ${cmd.shortTitleRu}",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cmd.descriptionRu,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Экран логов терминала (максимальная высота)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder),
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
                    val isUserCmd = log.startsWith(">")
                    val isError = log.startsWith("ERR") || log.contains("ERROR", ignoreCase = true)
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = when {
                            isUserCmd -> CyanAccent
                            isError -> RedError
                            else -> TextSecondary
                        },
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Кнопка очистки терминала внизу
        Button(
            onClick = { viewModel.clearTerminalLogs() },
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurface,
                contentColor = RedError
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Очистить", tint = RedError, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Очистить терминал",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RedError
            )
        }
    }

    // Модальное окно со справочником команд
    if (showReferenceDialog) {
        AlertDialog(
            onDismissRequest = { showReferenceDialog = false },
            title = {
                Text(
                    text = "СПРАВОЧНИК КОМАНД OBD-II",
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
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
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
