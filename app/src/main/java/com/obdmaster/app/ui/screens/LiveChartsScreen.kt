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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Slider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.nativeCanvas
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
     // 0: Неон, 1: Зоны (светофор), 2: Столбцы, 3: Прибор

    val (dispVal, dispUnit) = selectedPid.getDisplayValue(settings)
    val (dispMin, dispMax) = selectedPid.getDisplayMinMax(settings)
    var showSettings by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary) }
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
                        
                    val sensorSet = settings.sensorGaugeSettings[selectedPid.pidHex]
                    val drawMin = sensorSet?.minVal ?: dispMin
                    val drawMax = sensorSet?.maxVal ?: (if (dispMax > dispMin) dispMax else dispMin + 1f)
                    val drawStep = sensorSet?.stepVal ?: ((drawMax - drawMin) / 10f).coerceAtLeast(1f)
                    
                    drawGaugeDialScheme(history.last(), drawMin, drawMax, drawStep, dispVal, dispUnit)
                }

                if (showSettings) {
                    val sensorSet = settings.sensorGaugeSettings[selectedPid.pidHex]
                    var minVal by remember(selectedPid.pidHex, showSettings) { mutableStateOf(sensorSet?.minVal ?: dispMin) }
                    var maxVal by remember(selectedPid.pidHex, showSettings) { mutableStateOf(sensorSet?.maxVal ?: (if (dispMax > dispMin) dispMax else dispMin + 100f)) }
                    var stepVal by remember(selectedPid.pidHex, showSettings) { mutableStateOf(sensorSet?.stepVal ?: ((maxVal - minVal) / 10f).coerceAtLeast(1f)) }

                    Column(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(16.dp)) {
                        Text(text = "Диапазон:  - ", color = TextPrimary, fontSize = 14.sp)
                        RangeSlider(
                            value = minVal..maxVal,
                            onValueChange = { range ->
                                minVal = range.start
                                maxVal = range.endInclusive
                                if (maxVal - minVal < stepVal) {
                                    stepVal = ((maxVal - minVal) / 2f).coerceAtLeast(1f)
                                }
                                val newMap = settings.sensorGaugeSettings.toMutableMap()
                                newMap[selectedPid.pidHex] = com.obdmaster.app.data.SensorGaugeSettings(minVal, maxVal, stepVal)
                                viewModel.updateSettings(settings.copy(sensorGaugeSettings = newMap))
                            },
                            valueRange = dispMin..(if (dispMax > dispMin) dispMax * 2f else 10000f),
                            steps = 100,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Шаг делений: ", color = TextPrimary, fontSize = 14.sp)
                        Slider(
                            value = stepVal,
                            onValueChange = {
                                stepVal = it
                                val newMap = settings.sensorGaugeSettings.toMutableMap()
                                newMap[selectedPid.pidHex] = com.obdmaster.app.data.SensorGaugeSettings(minVal, maxVal, stepVal)
                                viewModel.updateSettings(settings.copy(sensorGaugeSettings = newMap))
                            },
                            valueRange = 1f..(maxVal - minVal).coerceAtLeast(10f),
                            steps = 100,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                    }
                }
            }
        }
    }
}

// 1. Схема: 🌊 Неоновый градиент со свечением и заливкой

private fun DrawScope.drawGaugeDialScheme(
    currentVal: Float,
    minVal: Float,
    maxVal: Float,
    stepVal: Float,
    dispVal: String,
    dispUnit: String
) {
    val center = Offset(size.width / 2f, size.height * 0.52f)
    val radius = (size.minDimension / 2f) * 0.85f

    val startAngle = 135f
    val sweepAngle = 270f

    drawArc(
        color = DarkBorder,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )

    val totalRange = if (maxVal > minVal) maxVal - minVal else 1f
    var safeStep = if (stepVal > 0) stepVal else 1f
    if (totalRange / safeStep > 100) safeStep = totalRange / 100f
    val numTicks = (totalRange / safeStep).toInt()

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            textSize = 14.sp.toPx()
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }

        if (numTicks <= 20) {
            val subTicksPerSegment = 5
            for (i in 0..numTicks * subTicksPerSegment) {
                if (i % subTicksPerSegment == 0) continue
                val tickVal = minVal + (i * (safeStep / subTicksPerSegment))
                val norm = (tickVal - minVal) / totalRange
                if (norm > 1f) continue

                val angleDeg = startAngle + (sweepAngle * norm)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                
                var tColor = android.graphics.Color.DKGRAY
                if (norm >= 0.8f) tColor = android.graphics.Color.argb(128, 255, 76, 76)
                else if (norm >= 0.6f) tColor = android.graphics.Color.argb(128, 255, 152, 0)
                
                val p1 = Offset(
                    (float)(center.x + Math.cos(angleRad) * (radius + 8.dp.toPx())),
                    (float)(center.y + Math.sin(angleRad) * (radius + 8.dp.toPx()))
                )
                val p2 = Offset(
                    (float)(center.x + Math.cos(angleRad) * (radius - 2.dp.toPx())),
                    (float)(center.y + Math.sin(angleRad) * (radius - 2.dp.toPx()))
                )
                drawLine(
                    color = androidx.compose.ui.graphics.Color(tColor),
                    start = p1,
                    end = p2,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        for (i in 0..numTicks) {
            val tickVal = minVal + (i * safeStep)
            val norm = (tickVal - minVal) / totalRange
            if (norm > 1f) continue

            val angleDeg = startAngle + (sweepAngle * norm)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            var tColor = android.graphics.Color.GRAY
            if (norm >= 0.8f) tColor = android.graphics.Color.parseColor("#FF4C4C")
            else if (norm >= 0.6f) tColor = android.graphics.Color.parseColor("#FF9800")

            val p1 = Offset(
                (float)(center.x + Math.cos(angleRad) * (radius + 12.dp.toPx())),
                (float)(center.y + Math.sin(angleRad) * (radius + 12.dp.toPx()))
            )
            val p2 = Offset(
                (float)(center.x + Math.cos(angleRad) * (radius - 2.dp.toPx())),
                (float)(center.y + Math.sin(angleRad) * (radius - 2.dp.toPx()))
            )
            drawLine(
                color = androidx.compose.ui.graphics.Color(tColor),
                start = p1,
                end = p2,
                strokeWidth = 4.dp.toPx()
            )

            val textRadius = radius + 28.dp.toPx()
            val tx = (float)(center.x + Math.cos(angleRad) * textRadius)
            val ty = (float)(center.y + Math.sin(angleRad) * textRadius)

            paint.color = tColor
            var textStr = Math.round(tickVal).toString()
            if (dispUnit.contains("RPM") && tickVal >= 1000) {
                textStr = (tickVal / 1000).ToString()
            } else if (dispUnit.contains("RPM") && tickVal == 0f) {
                textStr = "0"
            }
            drawText(textStr, tx, ty + (paint.textSize / 3f), paint)
        }
    }

    val normVal = ((currentVal - minVal) / totalRange).coerceIn(0f, 1f)
    if (normVal > 0f) {
        val sweep = sweepAngle * normVal
        val brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(CyanAccent, androidx.compose.ui.graphics.Color(0xFFFF9800), androidx.compose.ui.graphics.Color(0xFFFF4C4C)),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f)
        )
        drawArc(
            brush = brush,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }

    val needleAngle = startAngle + (sweepAngle * normVal)
    val needleRad = Math.toRadians(needleAngle.toDouble())
    val needleEnd = Offset(
        (float)(center.x + Math.cos(needleRad) * (radius * 0.95f)),
        (float)(center.y + Math.sin(needleRad) * (radius * 0.95f))
    )
    drawLine(
        color = androidx.compose.ui.graphics.Color.White,
        start = center,
        end = needleEnd,
        strokeWidth = 3.dp.toPx(),
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )

    drawCircle(
        color = CyanAccent,
        radius = 8.dp.toPx(),
        center = center
    )
    drawCircle(
        color = androidx.compose.ui.graphics.Color.Black,
        radius = 8.dp.toPx(),
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )

    drawContext.canvas.nativeCanvas.apply {
        val paintVal = android.graphics.Paint().apply {
            textSize = 48.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, android.graphics.Color.argb(100, 255, 255, 255))
        }
        val paintUnit = android.graphics.Paint().apply {
            textSize = 16.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.LTGRAY
            isAntiAlias = true
        }

        var unitText = dispUnit
        if (dispUnit.contains("RPM")) {
            unitText = "RPM (x1000)"
        }

        drawText(dispVal, center.x, center.y + radius * 0.6f, paintVal)
        drawText(unitText, center.x, center.y + radius * 0.85f, paintUnit)
    }
}

}
