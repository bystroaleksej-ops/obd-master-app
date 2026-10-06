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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdmaster.app.core.protocol.ObdPid
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ObdViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveChartsScreen(viewModel: ObdViewModel) {
    val pids by viewModel.pids.collectAsState()
    val selectedPid by viewModel.chartPid.collectAsState()
    val history by viewModel.chartHistory.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val currentScheme = settings.chartVisualScheme // 0: Неон, 1: Зоны (светофор), 2: Столбцы, 3: Прибор

    val (dispVal, dispUnit) = selectedPid.getDisplayValue(settings)
    val (dispMin, dispMax) = selectedPid.getDisplayMinMax(settings)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Заголовок
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ОСЦИЛЛОГРАФ И ГРАФИКИ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Турбо-опрос 100% шины • 4 графических стиля",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Горизонтальный выбор датчика (опрос строго выбранного датчика)
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Переключатель 4 графических стилей
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val schemes = listOf(
                "🌊 Неон",
                "🚥 Светофор",
                "📊 Столбцы",
                "⏱️ Прибор"
            )

            schemes.forEachIndexed { index, label ->
                val isSelected = currentScheme == index
                Button(
                    onClick = { viewModel.setChartVisualScheme(index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) CyanAccent else DarkSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DarkBackground else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Карточка текущих показаний
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedPid.titleRu.uppercase(),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = dispVal,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Text(
                            text = dispUnit,
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PID: 01 ${selectedPid.pidHex}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    if (history.isNotEmpty()) {
                        val min = history.minOrNull() ?: 0f
                        val max = history.maxOrNull() ?: 0f
                        Text(
                            text = "Мин: ${min.toInt()} / Макс: ${max.toInt()}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Холст отрисовки выбранной схемы
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
                    .padding(14.dp)
            ) {
                if (history.size < 2) {
                    Text(
                        text = "Ожидание потока данных...\n(Подключитесь к авто для быстрого графика)",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val minLimit = dispMin
                    val maxLimit = if (dispMax > dispMin) dispMax else dispMin + 1f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        when (currentScheme) {
                            0 -> drawNeonGradientScheme(history, minLimit, maxLimit)
                            1 -> drawTrafficLightZonesScheme(history, minLimit, maxLimit)
                            2 -> drawBarSpectrumScheme(history, minLimit, maxLimit)
                            3 -> drawGaugeDialScheme(history.last(), minLimit, maxLimit, dispVal, dispUnit)
                        }
                    }
                }
            }
        }
    }
}

// 1. Схема: 🌊 Неоновый градиент со свечением и заливкой
private fun DrawScope.drawNeonGradientScheme(history: List<Float>, minVal: Float, maxVal: Float) {
    val width = size.width
    val height = size.height

    // Сетка
    for (i in 0..4) {
        val y = height * (i / 4f)
        drawLine(color = DarkBorder, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
    }

    val stepX = width / (history.size - 1).coerceAtLeast(1)
    val wavePath = Path()
    val fillPath = Path()

    var lastX = 0f
    var lastY = height

    history.forEachIndexed { i, v ->
        val norm = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
        val x = i * stepX
        val y = height - (norm * height)

        if (i == 0) {
            wavePath.moveTo(x, y)
            fillPath.moveTo(x, height)
            fillPath.lineTo(x, y)
        } else {
            wavePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        lastX = x
        lastY = y
    }

    fillPath.lineTo(lastX, height)
    fillPath.close()

    // Неоновая полупрозрачная заливка под волной
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.40f), Color.Transparent),
            startY = 0f,
            endY = height
        )
    )

    // Основная неоновая линия волны
    drawPath(
        path = wavePath,
        color = CyanAccent,
        style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Светящаяся точка на переднем крае
    drawCircle(color = CyanAccent.copy(alpha = 0.35f), radius = 14f, center = Offset(lastX, lastY))
    drawCircle(color = Color.White, radius = 5f, center = Offset(lastX, lastY))
}

// 2. Схема: 🚥 Зоны нагрузки (Светофор: зеленый / желтый / красный)
private fun DrawScope.drawTrafficLightZonesScheme(history: List<Float>, minVal: Float, maxVal: Float) {
    val width = size.width
    val height = size.height

    // 3 горизонтальные зоны фона (Красная >80%, Желтая 60-80%, Зеленая <60%)
    drawRect(color = RedError.copy(alpha = 0.12f), topLeft = Offset(0f, 0f), size = Size(width, height * 0.20f))
    drawRect(color = OrangeWarning.copy(alpha = 0.10f), topLeft = Offset(0f, height * 0.20f), size = Size(width, height * 0.20f))
    drawRect(color = GreenAccent.copy(alpha = 0.08f), topLeft = Offset(0f, height * 0.40f), size = Size(width, height * 0.60f))

    // Разделительные линии зон
    drawLine(color = RedError.copy(alpha = 0.5f), start = Offset(0f, height * 0.20f), end = Offset(width, height * 0.20f), strokeWidth = 1.5f)
    drawLine(color = OrangeWarning.copy(alpha = 0.5f), start = Offset(0f, height * 0.40f), end = Offset(width, height * 0.40f), strokeWidth = 1.5f)

    val stepX = width / (history.size - 1).coerceAtLeast(1)

    for (i in 0 until history.size - 1) {
        val norm1 = ((history[i] - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
        val norm2 = ((history[i + 1] - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)

        val x1 = i * stepX
        val y1 = height - (norm1 * height)
        val x2 = (i + 1) * stepX
        val y2 = height - (norm2 * height)

        val avgNorm = (norm1 + norm2) / 2f
        val segColor = when {
            avgNorm >= 0.80f -> RedError
            avgNorm >= 0.60f -> OrangeWarning
            else -> GreenAccent
        }

        drawLine(color = segColor, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 4.5f, cap = StrokeCap.Round)
    }
}

// 3. Схема: 📊 Столбчатый спектр (Гистограмма / Эквалайзер)
private fun DrawScope.drawBarSpectrumScheme(history: List<Float>, minVal: Float, maxVal: Float) {
    val width = size.width
    val height = size.height

    val count = history.size
    val totalSlotWidth = width / count.coerceAtLeast(1)
    val barWidth = (totalSlotWidth * 0.70f).coerceAtLeast(2f)

    history.forEachIndexed { i, v ->
        val norm = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
        val barHeight = (norm * height).coerceAtLeast(4f)
        val x = i * totalSlotWidth + (totalSlotWidth - barWidth) / 2f
        val y = height - barHeight

        val barColor = when {
            norm >= 0.80f -> RedError
            norm >= 0.55f -> OrangeWarning
            else -> CyanAccent
        }

        // Столбец
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Пиковая точка над столбцом
        val peakY = (y - 4f).coerceAtLeast(0f)
        drawRect(
            color = Color.White,
            topLeft = Offset(x, peakY),
            size = Size(barWidth, 2f)
        )
    }
}

// 4. Схема: ⏱️ Стрелочный спортивный прибор (Круговой Gauge)
private fun DrawScope.drawGaugeDialScheme(
    currentVal: Float,
    minVal: Float,
    maxVal: Float,
    dispVal: String,
    dispUnit: String
) {
    val center = Offset(size.width / 2f, size.height * 0.52f)
    val radius = (size.minDimension / 2f) * 0.85f

    val startAngle = 135f
    val sweepAngle = 270f

    // Фоновая серая дуга шкалы
    drawArc(
        color = DarkBorder,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 16f, cap = StrokeCap.Round)
    )

    val norm = ((currentVal - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
    val activeSweep = sweepAngle * norm

    // Цветная активная дуга шкалы
    val arcColor = when {
        norm >= 0.80f -> RedError
        norm >= 0.55f -> OrangeWarning
        else -> CyanAccent
    }

    if (activeSweep > 0f) {
        drawArc(
            color = arcColor,
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )
    }

    // Стрелка прибора
    val currentAngleDeg = startAngle + activeSweep
    val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())

    val needleLength = radius * 0.78f
    val needleEndX = center.x + (needleLength * cos(currentAngleRad)).toFloat()
    val needleEndY = center.y + (needleLength * sin(currentAngleRad)).toFloat()

    drawLine(
        color = Color.White,
        start = center,
        end = Offset(needleEndX, needleEndY),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Центральный кругляк стрелки
    drawCircle(color = arcColor, radius = 12f, center = center)
    drawCircle(color = DarkBackground, radius = 5f, center = center)
}
