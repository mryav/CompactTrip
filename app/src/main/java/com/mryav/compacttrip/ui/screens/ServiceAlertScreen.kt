package com.mryav.compacttrip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mryav.compacttrip.ui.components.FocusSurface
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.mryav.compacttrip.R
import com.mryav.compacttrip.data.model.Notice
import com.mryav.compacttrip.ui.components.ActionRow
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.LineBadgeChip
import com.mryav.compacttrip.ui.components.dpadScroll
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One service alert (Figma 11:1634): title, affected lines, "Open transportnsw.info", "Hide alert"
 * (only for a Home network banner: [onHide]), and the alert's HTML body. D-pad scrolls the body
 * when there's nothing else to focus.
 */
@Composable
fun ServiceAlertScreen(
    notice: Notice,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    onHide: (() -> Unit)? = null,
) {
    val body = remember(notice.html) { notice.html?.let(::alertBody) }
    val scroll = rememberScrollState()
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus)
            .dpadScroll(scroll),
    ) {
        AppBar(
            title = "Service Alert",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Card)
                    .padding(16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                BasicText(notice.title, style = Type.title(Palette.BodyText))
                notice.lines.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.du)) {
                        LineBadgeChip(line.badge)
                        line.name?.let { BasicText(it, style = Type.secondary(Color.Black), modifier = Modifier.weight(1f)) }
                    }
                }
            }
            notice.url?.let { url ->
                ActionRow(
                    label = "Open transportnsw.info",
                    icon = R.drawable.ic_arrow_outward,
                    color = Palette.LinkRow,
                    onClick = { onOpenUrl(url) },
                )
            }
            if (onHide != null) {
                FocusSurface(onClick = onHide, color = Palette.NoticeBackground, modifier = Modifier.fillMaxWidth()) { focused ->
                    val content = if (focused) Palette.FocusedContent else Palette.NoticeText
                    Row(
                        modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.du),
                    ) {
                        BasicText("Hide alert", style = Type.row(content), modifier = Modifier.weight(1f))
                        Image(
                            painter = painterResource(R.drawable.ic_visibility_off),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(content),
                            modifier = Modifier.size(40.du),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Card)
                    .padding(16.du),
                verticalArrangement = Arrangement.spacedBy(16.du),
            ) {
                notice.updatedAt?.let { BasicText("Updated ${updatedLabel(it)}", style = Type.body(Palette.BodyText, bold = true)) }
                if (body != null) BasicText(body, style = Type.body(Palette.BodyText))
            }
        }
    }
}

/**
 * TfNSW alert HTML → styled text. Lists are flattened to "• " lines first so bullets survive on
 * every Compose version; bold, line breaks and entities come through fromHtml.
 */
private fun alertBody(html: String): AnnotatedString {
    val flattened = html
        .replace(Regex("(?i)<li[^>]*>\\s*"), "• ")
        .replace(Regex("(?i)\\s*</li>"), "<br>")
        .replace(Regex("(?i)</?(ul|ol)[^>]*>"), "")
    val text = AnnotatedString.fromHtml(flattened)
    // Trim trailing blank lines left by the conversion.
    val end = text.text.trimEnd().length
    return if (end < text.length) text.subSequence(0, end) else text
}

private val SYDNEY: ZoneId = ZoneId.of("Australia/Sydney")
private val TIME = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
private val DATE_TIME = DateTimeFormatter.ofPattern("d MMM, h:mma", Locale.ENGLISH)

/** "11:42am" today, otherwise "9 Nov, 11:42am". */
private fun updatedLabel(epochMillis: Long): String {
    val at = Instant.ofEpochMilli(epochMillis).atZone(SYDNEY)
    val today = LocalDate.now(SYDNEY)
    val formatter = if (at.toLocalDate() == today) TIME else DATE_TIME
    return formatter.format(at).replace("AM", "am").replace("PM", "pm")
}
