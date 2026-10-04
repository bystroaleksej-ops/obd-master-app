package com.obdmaster.app.ui.screens

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
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "ДИАГНОСТИКА ЭБУ (DTC)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Считывание и сброс диагностических кодов неисправностей (Mode 03, 04, 07)",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text("Сканирование...")
                } else {
                    Text("Сканировать ошибки", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { showClearDialog = true },
                enabled = !isScanning && dtcs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Сбросить ошибки", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dtcs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isScanning) "Опрос блоков управления..." else "Ошибок не обнаружено",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isScanning) CyanAccent else GreenAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isScanning) "Считываются Stored, Pending и Permanent коды..." else "Нажмите «Сканировать ошибки» для проверки систем авто",
                        fontSize = 13.sp,
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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Сброс кодов ошибок (Mode 04)") },
            text = {
                Text("Это действие сотрет все сохраненные ошибки в памяти ЭБУ, удалит стоп-кадры (Freeze Frame) и погасит индикатор Check Engine.\n\nУбедитесь, что зажигание включено, но двигатель заглушен!")
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
            containerColor = DarkSurface,
            textContentColor = TextSecondary,
            titleContentColor = TextPrimary
        )
    }
}

@Composable
fun DtcCard(dtc: DtcItem) {
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
                    text = dtc.code,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RedError
                )

                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = dtc.type,
                        fontSize = 11.sp,
                        color = OrangeWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = dtc.description,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}
