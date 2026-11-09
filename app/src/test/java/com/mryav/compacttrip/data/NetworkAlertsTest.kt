package com.mryav.compacttrip.data

import com.mryav.compacttrip.data.dto.AddInfoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NetworkAlertsTest {
    private fun fixture(): AddInfoResponse {
        val text = javaClass.classLoader!!.getResource("fixtures/add_info_sample.json")!!.readText()
        return TfnswJson.decodeFromString(AddInfoResponse.serializer(), text)
    }

    @Test
    fun onlyHighPriority_networkWideOrBroad_inForce_mostImportantFirst() {
        val alerts = fixture().infos.current.toNetworkAlerts(Instant.parse("2026-11-09T03:00:00Z"))
        // veryHigh + 6 lines first, then the high area-wide bus industrial action. Left out: normal
        // priority, high on only 4 lines, a high stop alert, and an expired area alert.
        assertEquals(listOf("test-veryhigh-6-lines", "ems-81146"), alerts.map { it.id })
        val industrial = alerts[1]
        assertTrue(industrial.title.startsWith("BUS INDUSTRIAL ACTION"))
        assertTrue(industrial.urgent)
    }

    @Test
    fun affectedLines_areCountedNotKept() {
        val counts = fixture().infos.current.map { it.affected.lines.size }
        assertEquals(listOf(0, 6, 6, 4, 1, 0), counts)
    }
}
