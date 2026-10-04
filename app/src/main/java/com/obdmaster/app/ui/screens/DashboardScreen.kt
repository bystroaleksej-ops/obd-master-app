package com.obdmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.core.protocol.ObdPid
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ConnectionStatus
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@Composable
fun DashboardScreen(viewModel: ObdViewModel) {
    val pids by viewModel.pids.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Status header
        StatusHeader(status)

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Gauges: RPM and Speed
        val rpmPid = pids.find { it is ObdPid.EngineRpm }
        val speedPid = pids.find { it is ObdPid.VehicleSpeed }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroGaugeCard(
                title = "ОБОРОТЫ (RPM)",
                value = rpmPid?.currentValue?.toInt()?.toString() ?: "--",
                unit = "об/мин",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
            HeroGaugeCard(
                title = "СКОРОСТЬ",
                value = speedPid?.currentValue?.toInt()?.toString() ?: "--",
                unit = "км/ч",
                accentColor = GreenAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ПАРАМЕТРЫ ДАТЧИКОВ",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of remaining PIDs
        val remainingPids = pids.filter { it !is ObdPid.EngineRpm && it !is ObdPid.VehicleSpeed }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(remainingPids) { pid ->
                PidCard(pid = pid)
            }
        }
    }
}

@Composable
fun StatusHeader(status: ConnectionStatus) {
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
                    is ConnectionStatus.Connecting -> status.message
                    is ConnectionStatus.Disconnected -> "Отключено"
                    is ConnectionStatus.Error -> "Ошибка: ${status.error}"
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
fun PidCard(pid: ObdPid) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = pid.titleRu,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pid.formattedString,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Linear gauge indicator bar
            val progress = ((pid.currentValue - pid.minVal) / (pid.maxVal - pid.minVal)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyanAccent,
                trackColor = DarkBorder,
            )
        }
    }
}
