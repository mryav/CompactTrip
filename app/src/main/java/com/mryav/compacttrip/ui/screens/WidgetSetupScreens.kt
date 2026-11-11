package com.mryav.compacttrip.ui.screens

import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mryav.compacttrip.ui.components.AlertBanner
import com.mryav.compacttrip.ui.components.AppBar
import com.mryav.compacttrip.ui.components.BackButton
import com.mryav.compacttrip.ui.components.DoneAction
import com.mryav.compacttrip.ui.components.dpadScroll
import com.mryav.compacttrip.ui.components.focusEntry
import com.mryav.compacttrip.ui.components.rememberInitialFocus
import com.mryav.compacttrip.ui.theme.LocalDesignScale
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du
import com.mryav.compacttrip.widget.WidgetConfig
import com.mryav.compacttrip.widget.WidgetRenderer
import com.mryav.compacttrip.widget.WidgetSnapshot
import com.mryav.compacttrip.widget.WidgetStyle

/** Preview heights, from the Figma widget frames (design units). */
private fun WidgetStyle.previewHeight(): Int = when (this) {
    WidgetStyle.DefaultList -> 339
    WidgetStyle.Headless -> 329
    WidgetStyle.FixedCard -> 338
}

/**
 * Create widget (Figma 30:4966): the disclaimer, then a preview of the widget as it'll look, drawn
 * by the widget's own renderer. The style comes from which widget was picked, so there's no style
 * row. Done places it.
 */
@Composable
fun CreateWidgetScreen(
    config: WidgetConfig,
    /** Null while the first load runs. */
    snapshot: WidgetSnapshot?,
    loading: Boolean,
    now: Long,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = rememberInitialFocus()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Page)
            .focusEntry(initialFocus),
    ) {
        AppBar(
            title = "Create widget",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        ) {
            DoneAction(enabled = true, onClick = onDone)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 16.du),
            verticalArrangement = Arrangement.spacedBy(2.du),
        ) {
            AlertBanner(text = "Do not rely on widgets for real-time information", onClick = onOpenInfo)
            BasicText(
                "Preview",
                style = Type.label(Palette.BodyText),
                modifier = Modifier.padding(start = 8.du, end = 8.du, top = 8.du, bottom = 4.du),
            )
            WidgetPreview(
                config = config,
                snapshot = snapshot,
                loading = loading,
                now = now,
                modifier = Modifier
                    .padding(horizontal = 16.du)
                    .fillMaxWidth()
                    .then(
                        // Fixed card cards grow with their text; the lists are cut off like the real widget.
                        if (config.style == WidgetStyle.FixedCard) {
                            Modifier.heightIn(min = config.style.previewHeight().du)
                        } else {
                            Modifier.height(config.style.previewHeight().du)
                        },
                    ),
            )
        }
    }
}

/** The real widget views (RemoteViews applied in the app), so the preview can't drift from the widget. */
@Composable
private fun WidgetPreview(
    config: WidgetConfig,
    snapshot: WidgetSnapshot?,
    loading: Boolean,
    now: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val widthDp = (LocalConfiguration.current.screenWidthDp - 32 * LocalDesignScale.current.dpPerUnit).toInt()
    val views = remember(config, snapshot, loading, now, widthDp) {
        WidgetRenderer.widget(
            context = context,
            id = 0,
            config = config,
            snapshot = snapshot,
            now = now,
            widthDp = widthDp,
            refreshing = loading,
            preview = true,
        )
    }
    AndroidView(
        // Just a picture: its taps and focus stay out of the widget's views.
        factory = { ctx ->
            object : FrameLayout(ctx) {
                override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true
            }.apply { descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS }
        },
        update = { frame ->
            frame.removeAllViews()
            frame.addView(views.apply(frame.context, frame))
        },
        modifier = modifier,
    )
}

/** Widget information (Figma 30:5306). */
@Composable
fun WidgetInfoScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
            title = "Widget information",
            navigation = { BackButton(onClick = onBack, modifier = Modifier.focusRequester(initialFocus)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(vertical = 16.du),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Card)
                    .padding(16.du),
                verticalArrangement = Arrangement.spacedBy(10.du),
            ) {
                BasicText("Widgets have limitations", style = Type.title(Palette.BodyText))
                BasicText(
                    "The data inside a widget updates every 15-30 minutes, or when you click refresh.\n\n" +
                        "When your phone is in battery saver, widgets can’t automatically refresh.\n\n" +
                        "Always click on a service to open the app for more accurate and up-to-date information.",
                    style = Type.body(Palette.BodyText),
                )
            }
        }
    }
}
