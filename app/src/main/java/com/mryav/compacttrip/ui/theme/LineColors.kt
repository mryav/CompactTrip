package com.mryav.compacttrip.ui.theme

import androidx.compose.ui.graphics.Color
import com.mryav.compacttrip.data.model.Mode

/** Official TfNSW line colours (Figma style guide, node 2:98). */
object LineColors {
    private val byLine: Map<String, Color> = mapOf(
        // Sydney Metro
        "M1" to Color(0xFF168388),
        // Sydney Trains
        "T1" to Color(0xFFF99D1C), "T2" to Color(0xFF0098CD), "T3" to Color(0xFFF37021),
        "T4" to Color(0xFF005AA3), "T5" to Color(0xFFC4258F), "T6" to Color(0xFF7C3E21),
        "T7" to Color(0xFF6F818E), "T8" to Color(0xFF00954C), "T9" to Color(0xFFD11F2F),
        // Intercity
        "BMT" to Color(0xFFF99D1C), "CCN" to Color(0xFFD11F2F), "HUN" to Color(0xFF833134),
        "SCO" to Color(0xFF005AA3), "SHL" to Color(0xFF00954C),
        // Ferries
        "F1" to Color(0xFF00774B), "F2" to Color(0xFF144734), "F3" to Color(0xFF648C3C),
        "F4" to Color(0xFFBFD730), "F5" to Color(0xFF286142), "F6" to Color(0xFF00AB51),
        "F7" to Color(0xFF00B189), "F8" to Color(0xFF55622B), "F9" to Color(0xFF65B32E),
        "F10" to Color(0xFF5AB031), "STKN" to Color(0xFF5AB031),
        // Light rail
        "L1" to Color(0xFFBE1622), "L2" to Color(0xFFDD1E25), "L3" to Color(0xFF781140),
        "L4" to Color(0xFFBB2043), "NLR" to Color(0xFFEE343F),
    )

    private val RegionalTrain = Color(0xFFF6891F)
    private val Coach = Color(0xFF732A82)
    private val Bus = Color(0xFF00B8F6)

    fun of(line: String, mode: Mode): Color = byLine[line.uppercase()] ?: when (mode) {
        Mode.Train -> RegionalTrain
        Mode.Metro -> byLine.getValue("M1")
        Mode.LightRail -> byLine.getValue("L1")
        Mode.Bus, Mode.SchoolBus -> Bus
        Mode.Coach -> Coach
        Mode.Ferry -> byLine.getValue("F10")
        Mode.Walk -> Color(0xFF586065)
    }
}
