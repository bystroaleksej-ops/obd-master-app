package com.obdmaster.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ObdViewModel

@Composable
fun LiveChartsScreen(viewModel: ObdViewModel) {
    val pids by viewModel.pids.collectAsState()
    val selectedPid by viewModel.chartPid.collectAsState()
    val history by viewModel.chartHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "ОСЦИЛЛОГРАФ ДАТЧИКОВ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Потоковый график показаний в реальном времени",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal selector for PIDs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(pids) { pid ->
                val isSelected = pid.pidHex == selectedPid.pidHex
                Surface(
                    color = if (isSelected) CyanAccent else DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { viewModel.setChartPid(pid) }
                ) {
                    Text(
                        text = pid.titleRu,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DarkBackground else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current Value Display Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedPid.titleRu.uppercase(),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedPid.formattedString,
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "PID: 01 ${selectedPid.pidHex}", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(text = "Категория: ${selectedPid.category}", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time Canvas Graph
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (history.size < 2) {
                    Text(
                        text = "Ожидание потока данных...",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val minLimit = selectedPid.minVal
                    val maxLimit = selectedPid.maxVal

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Draw Grid lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = height * (i.toFloat() / gridLines)
                            drawLine(
                                color = DarkBorder,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Build Wave Path
                        val path = Path()
                        val stepX = width / (history.size - 1).coerceAtLeast(1)

                        history.forEachIndexed { index, value ->
                            val normalizedY = ((value - minLimit) / (maxLimit - minLimit)).coerceIn(0f, 1f)
                            val x = index * stepX
                            val y = height - (normalizedY * height)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = CyanAccent,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
