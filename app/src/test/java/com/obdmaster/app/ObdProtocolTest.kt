package com.obdmaster.app

import com.obdmaster.app.core.protocol.DtcService
import com.obdmaster.app.core.protocol.ObdPid
import com.obdmaster.app.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdProtocolTest {

    @Test
    fun testRpmDecoding() {
        val rpm = ObdPid.EngineRpm()
        // Raw ELM response: 41 0C 1A FA -> ((26 * 256) + 250) / 4 = 1726.5 RPM
        val ok = rpm.decode("410C1AFA")
        assertTrue("RPM decode should succeed", ok)
        assertEquals(1726.5f, rpm.currentValue, 0.1f)
        assertEquals("1726 об/мин", rpm.formattedString)
    }

    @Test
    fun testSpeedDecoding() {
        val speed = ObdPid.VehicleSpeed()
        // Raw ELM response: 41 0D 3C -> 0x3C = 60 km/h
        val ok = speed.decode("410D3C")
        assertTrue("Speed decode should succeed", ok)
        assertEquals(60f, speed.currentValue, 0.01f)
        assertEquals("60 км/ч", speed.formattedString)

        // Custom unit test: mph
        val mphSettings = AppSettings(speedUnit = "mph")
        val (valStr, unitStr) = speed.getDisplayValue(mphSettings)
        assertEquals("37", valStr)
        assertEquals("mph", unitStr)
    }

    @Test
    fun testCoolantTempDecoding() {
        val temp = ObdPid.CoolantTemp()
        // Raw ELM response: 41 05 5A -> 0x5A = 90 -> 90 - 40 = 50 °C
        val ok = temp.decode("41055A")
        assertTrue("Coolant temp decode should succeed", ok)
        assertEquals(50f, temp.currentValue, 0.01f)
        assertEquals("50 °C", temp.formattedString)

        // Custom unit test: °F
        val fahrSettings = AppSettings(tempUnit = "°F")
        val (fValStr, fUnitStr) = temp.getDisplayValue(fahrSettings)
        assertEquals("122", fValStr)
        assertEquals("°F", fUnitStr)
    }

    @Test
    fun testModuleVoltageDecoding() {
        val volt = ObdPid.ControlModuleVoltage()
        // Raw ELM response: 41 42 34 56 -> ((52 * 256) + 86) / 1000 = 13.398 V
        val ok = volt.decode("41423456")
        assertTrue("Voltage decode should succeed", ok)
        assertEquals(13.398f, volt.currentValue, 0.01f)
    }

    @Test
    fun testDtcParsing() {
        // Mode 03 response with P0171 and P0300
        val raw = "43 02 01 71 03 00"
        val dtcs = DtcService.parseDtcResponse(raw, "Подтвержденная (Stored)", "43")
        assertEquals(2, dtcs.size)
        assertEquals("P0171", dtcs[0].code)
        assertEquals("P0300", dtcs[1].code)
    }

    @Test
    fun testDefaultSettings() {
        val settings = AppSettings()
        assertTrue("Selected PIDs should contain RPM (0C)", settings.selectedPidHexes.contains("0C"))
        assertTrue("Selected PIDs should contain Speed (0D)", settings.selectedPidHexes.contains("0D"))
        assertEquals(100L, settings.pollingIntervalMs)
        assertEquals(2, settings.alarmRepeatIntervalSec) // Default 2s turbo repeat
    }
}
