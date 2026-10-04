package com.obdmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
                text = "РќРђРЎРўР РћР™РљР РџР РР›РћР–Р•РќРРЇ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
            Text(
                text = "РџР°СЂР°РјРµС‚СЂС‹ СЃРІСЏР·Рё, Р±РµР·РѕРїР°СЃРЅРѕСЃС‚Рё, СЌРєСЂР°РЅР° Рё С‚РµСЂРјРёРЅР°Р»Р°",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // ================= 1. РЎР’РЇР—Р¬ Р РђР”РђРџРўР•Р  =================
        item {
            SettingsCategoryCard(title = "РЎР’РЇР—Р¬ Р РђР”РђРџРўР•Р  ELM327", icon = Icons.Default.Bluetooth) {
                // Protocol selection
                Text(text = "РџСЂРѕС‚РѕРєРѕР» РїРѕРґРєР»СЋС‡РµРЅРёСЏ OBD-II:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                val protocols = listOf("РђРІС‚Рѕ (AT SP 0)", "CAN 11b 500k", "CAN 29b 500k", "KWP Fast", "ISO 9141-2")
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

                // Polling Rate
                Text(text = "Р§Р°СЃС‚РѕС‚Р° РѕРїСЂРѕСЃР° РґР°С‚С‡РёРєРѕРІ:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(50L to "Р‘С‹СЃС‚СЂР°СЏ (50 РјСЃ)", 100L to "РћР±С‹С‡РЅР°СЏ (100 РјСЃ)", 250L to "Р­РєРѕРЅРѕРј (250 РјСЃ)").forEach { (ms, label) ->
                        FilterChip(
                            selected = settings.pollingIntervalMs == ms,
                            onClick = { viewModel.updateSettings(settings.copy(pollingIntervalMs = ms)) },
                            label = { Text(label, fontSize = 10.sp) },
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
                    title = "РђРІС‚Рѕ-РїРµСЂРµРїРѕРґРєР»СЋС‡РµРЅРёРµ",
                    subtitle = "РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РІРѕСЃСЃС‚Р°РЅР°РІР»РёРІР°С‚СЊ СЃРІСЏР·СЊ РїСЂРё СЃР±РѕСЏС…",
                    checked = settings.autoReconnect,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoReconnect = it)) }
                )
            }
        }

        // ================= 2. РћРџРћР’Р•Р©Р•РќРРЇ Р Р—РђР©РРўРђ РњРћРўРћР Рђ =================
        item {
            SettingsCategoryCard(title = "Р—РђР©РРўРђ Р РћРџРћР’Р•Р©Р•РќРРЇ", icon = Icons.Default.Warning) {
                // Coolant alarm
                SettingsSwitchRow(
                    title = "РўСЂРµРІРѕРіР° РїРѕ С‚РµРјРїРµСЂР°С‚СѓСЂРµ РћР–",
                    subtitle = "РЎРёРіРЅР°Р» РїРµСЂРµРіСЂРµРІР° РїСЂРё ${settings.coolantAlarmThresholdC}В°C",
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
                    title = "РљРѕРЅС‚СЂРѕР»СЊ СЂР°Р·СЂСЏРґР° РђРљР‘",
                    subtitle = "РџСЂРµРґСѓРїСЂРµР¶РґР°С‚СЊ РїСЂРё РїР°РґРµРЅРёРё РЅРёР¶Рµ ${"%.1f".format(settings.batteryAlarmThresholdV)} Р’",
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
                    title = "РћРїРѕРІРµС‰РµРЅРёРµ РїРѕ РѕР±РѕСЂРѕС‚Р°Рј (Shift Light)",
                    subtitle = "Р’СЃРїС‹С€РєР° РїР°РЅРµР»Рё РїСЂРё РґРѕСЃС‚РёР¶РµРЅРёРё ${settings.rpmAlarmThreshold} РѕР±/РјРёРЅ",
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

        // ================= 3. Р­РљР РђРќ Р РРќРўР•Р Р¤Р•Р™РЎ =================
        item {
            SettingsCategoryCard(title = "Р­РљР РђРќ Р РРќРўР•Р Р¤Р•Р™РЎ", icon = Icons.Default.Settings) {
                SettingsSwitchRow(
                    title = "РќРµ РІС‹РєР»СЋС‡Р°С‚СЊ СЌРєСЂР°РЅ (Keep Screen On)",
                    subtitle = "Р—Р°РїСЂРµС‚РёС‚СЊ С‚РµР»РµС„РѕРЅСѓ Р±Р»РѕРєРёСЂРѕРІР°С‚СЊСЃСЏ РІ СЂРµР¶РёРјРµ РїСЂРёР±РѕСЂРѕРІ",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(keepScreenOn = it)) }
                )

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Р РµР¶РёРј РїСЂРѕРµРєС†РёРё HUD",
                    subtitle = "Р—РµСЂРєР°Р»СЊРЅРѕРµ РѕС‚РѕР±СЂР°Р¶РµРЅРёРµ РґР»СЏ РѕС‚СЂР°Р¶РµРЅРёСЏ РѕС‚ Р»РѕР±РѕРІРѕРіРѕ СЃС‚РµРєР»Р° РЅРѕС‡СЊСЋ",
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
                    Text(text = "Р•РґРёРЅРёС†С‹ РёР·РјРµСЂРµРЅРёСЏ:", fontSize = 12.sp, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = settings.speedUnit == "РєРј/С‡",
                            onClick = { viewModel.updateSettings(settings.copy(speedUnit = "РєРј/С‡")) },
                            label = { Text("РєРј/С‡", fontSize = 10.sp) }
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

        // ================= 4. Р”РРђР“РќРћРЎРўРРљРђ Р РўР•Р РњРРќРђР› =================
        item {
            SettingsCategoryCard(title = "Р”РРђР“РќРћРЎРўРРљРђ Р РўР•Р РњРРќРђР›", icon = Icons.Default.Search) {
                SettingsSwitchRow(
                    title = "Р“Р»СѓР±РѕРєРѕРµ СЃРєР°РЅРёСЂРѕРІР°РЅРёРµ РІСЃРµС… Р±Р»РѕРєРѕРІ",
                    subtitle = "РћРїСЂР°С€РёРІР°С‚СЊ РЅРµ С‚РѕР»СЊРєРѕ Р”Р’РЎ (ECU), РЅРѕ Рё РђРљРџРџ (TCM)",
                    checked = settings.deepScanAllModules,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(deepScanAllModules = it)) }
                )

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "РђРІС‚РѕРїРµСЂРµРІРѕРґ РѕС‚РІРµС‚РѕРІ РІ С‚РµСЂРјРёРЅР°Р»Рµ",
                    subtitle = "РџРѕРєР°Р·С‹РІР°С‚СЊ СЂР°СЃС€РёС„СЂРѕРІРєСѓ РїР°СЂР°РјРµС‚СЂРѕРІ РЅР° СЂСѓСЃСЃРєРѕРј СЏР·С‹РєРµ",
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
