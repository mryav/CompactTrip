package com.mryav.compacttrip.widget

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.mryav.compacttrip.R
import com.mryav.compacttrip.ui.components.clockTime
import com.mryav.compacttrip.ui.components.countdown
import com.mryav.compacttrip.ui.components.minutesUntil
import com.mryav.compacttrip.ui.theme.LineColors

/** Narrower than this, list rows stack the countdown under the details (Figma 30:4454, 30:4317). */
const val LIST_WIDE_MIN_DP = 180

/** At least this wide, the Fixed card shows two cards, a refresh button and the footer (Figma 30:4584). */
const val CARD_TWO_MIN_DP = 230

/** Line badges shown before "+N". */
private const val MAX_BADGES = 3
private const val MORE_BADGE_COLOR = 0xFF586065.toInt()

/** Rows drawn directly in the setup preview (the real widget scrolls a list). */
private const val PREVIEW_ROWS = 3

/** Builds widget views from stored data. Shared by the widgets, their list service and the setup preview. */
object WidgetRenderer {

    /**
     * The whole widget. [refreshing] swaps refresh for a spinner. [preview] draws rows directly
     * (inside the app a remote list can't bind).
     */
    fun widget(
        context: Context,
        id: Int,
        config: WidgetConfig,
        snapshot: WidgetSnapshot?,
        now: Long,
        widthDp: Int,
        refreshing: Boolean,
        preview: Boolean = false,
    ): RemoteViews {
        val content = snapshot.content(now)
        val fetchedAt = snapshot?.fetchedAt
        return when (config.style) {
            WidgetStyle.DefaultList -> RemoteViews(context.packageName, R.layout.widget_default_list).apply {
                titles(config)
                refreshButton(context, id, refreshing)
                setOnClickPendingIntent(R.id.header, WidgetIntents.openApp(context, id, config))
                list(context, id, config, content, widthDp >= LIST_WIDE_MIN_DP, now, refreshing, preview)
                footer(fetchedAt, now)
            }
            WidgetStyle.Headless -> RemoteViews(context.packageName, R.layout.widget_headless_list).apply {
                list(context, id, config, content, widthDp >= LIST_WIDE_MIN_DP, now, refreshing, preview)
            }
            WidgetStyle.FixedCard -> RemoteViews(context.packageName, R.layout.widget_fixed_card).apply {
                val two = widthDp >= CARD_TWO_MIN_DP
                titles(config)
                setOnClickPendingIntent(R.id.header, WidgetIntents.openApp(context, id, config))
                if (two) refreshButton(context, id, refreshing) else setViewVisibility(R.id.refresh, View.GONE)
                when (content) {
                    is WidgetContent.Rows -> {
                        setViewVisibility(R.id.cards, View.VISIBLE)
                        setViewVisibility(R.id.message, View.GONE)
                        removeAllViews(R.id.cards)
                        content.rows.take(if (two) 2 else 1).forEach { addView(R.id.cards, card(context, it, now)) }
                        setOnClickPendingIntent(R.id.cards, WidgetIntents.openApp(context, id, config))
                    }
                    is WidgetContent.Message -> {
                        setViewVisibility(R.id.cards, View.GONE)
                        message(context, id, config, content, refreshing)
                    }
                }
                if (two) footer(fetchedAt, now) else setViewVisibility(R.id.footer, View.GONE)
            }
        }
    }

    /** One list row: wide (countdown on the right) or narrow (countdown below). */
    fun row(context: Context, row: WidgetRow, wide: Boolean, now: Long): RemoteViews =
        RemoteViews(context.packageName, if (wide) R.layout.widget_row_wide else R.layout.widget_row_narrow).apply {
            if (row.cancelled) setInt(R.id.row, "setBackgroundResource", R.drawable.widget_row_cancelled_bg)
            badges(context, row)
            setTextViewText(R.id.headsign, row.headsign)
            setTextViewText(R.id.departs, clockTime(row.departsAt))
            val arrives = row.arrivesAt
            if (arrives != null) {
                setTextViewText(R.id.arrives, clockTime(arrives))
            } else {
                setViewVisibility(R.id.dash, View.GONE)
                setViewVisibility(R.id.arrives, View.GONE)
            }
            place(row.place)
            countdown(context, row, now, R.dimen.w_text_countdown)
            // Every row opens the route's services (the list's click template).
            setOnClickFillInIntent(R.id.row, Intent())
        }

    /** A Quick View card (Fixed card widget). */
    fun card(context: Context, row: WidgetRow, now: Long): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_card).apply {
            if (row.cancelled) setInt(R.id.card, "setBackgroundResource", R.drawable.widget_card_cancelled_bg)
            badges(context, row)
            setTextViewText(R.id.headsign, row.headsign)
            place(row.place)
            countdown(context, row, now, R.dimen.w_text_card_countdown)
            setTextViewText(R.id.clock, clockTime(row.departsAt))
        }

    private fun RemoteViews.titles(config: WidgetConfig) {
        setTextViewText(R.id.title_from, config.origin.shortName)
        setTextViewText(R.id.title_to, "to ${config.destination.shortName}")
    }

    private fun RemoteViews.refreshButton(context: Context, id: Int, refreshing: Boolean) {
        setViewVisibility(R.id.refresh, View.VISIBLE)
        setViewVisibility(R.id.refresh_icon, if (refreshing) View.INVISIBLE else View.VISIBLE)
        setViewVisibility(R.id.refreshing, if (refreshing) View.VISIBLE else View.GONE)
        setOnClickPendingIntent(R.id.refresh, WidgetIntents.refresh(context, id))
    }

    private fun RemoteViews.footer(fetchedAt: Long?, now: Long) {
        if (fetchedAt == null) {
            setViewVisibility(R.id.footer, View.GONE)
        } else {
            setViewVisibility(R.id.footer, View.VISIBLE)
            setTextViewText(R.id.footer, widgetUpdatedAgo(fetchedAt, now))
        }
    }

    private fun RemoteViews.list(
        context: Context,
        id: Int,
        config: WidgetConfig,
        content: WidgetContent,
        wide: Boolean,
        now: Long,
        refreshing: Boolean,
        preview: Boolean,
    ) {
        if (!preview) {
            setRemoteAdapter(R.id.list, WidgetIntents.listService(context, id, wide))
            setPendingIntentTemplate(R.id.list, WidgetIntents.openApp(context, id, config, forList = true))
        }
        when (content) {
            is WidgetContent.Rows -> {
                setViewVisibility(R.id.message, View.GONE)
                if (preview) {
                    setViewVisibility(R.id.list, View.GONE)
                    setViewVisibility(R.id.rows, View.VISIBLE)
                    removeAllViews(R.id.rows)
                    content.rows.take(PREVIEW_ROWS).forEach { addView(R.id.rows, row(context, it, wide, now)) }
                } else {
                    setViewVisibility(R.id.list, View.VISIBLE)
                }
            }
            is WidgetContent.Message -> {
                setViewVisibility(R.id.list, View.GONE)
                setViewVisibility(R.id.rows, View.GONE)
                message(context, id, config, content, refreshing)
            }
        }
    }

    /** The tappable message: it either retries or opens the app. */
    private fun RemoteViews.message(
        context: Context,
        id: Int,
        config: WidgetConfig,
        content: WidgetContent.Message,
        refreshing: Boolean,
    ) {
        setViewVisibility(R.id.message, View.VISIBLE)
        val updating = refreshing && !content.message.opensApp
        setTextViewText(R.id.message, if (updating) "Updating…" else content.text)
        setOnClickPendingIntent(
            R.id.message,
            if (content.message.opensApp) WidgetIntents.openApp(context, id, config) else WidgetIntents.refresh(context, id),
        )
    }

    private fun RemoteViews.place(place: String?) {
        if (place.isNullOrBlank()) {
            setViewVisibility(R.id.place, View.GONE)
        } else {
            setTextViewText(R.id.place, place)
        }
    }

    /** Neutral black countdown in every style: widget data may be minutes old, so no on-time colours. */
    private fun RemoteViews.countdown(context: Context, row: WidgetRow, now: Long, numberSize: Int) {
        if (row.cancelled) {
            setTextViewText(R.id.countdown, "Cancelled")
            setTextViewTextSize(
                R.id.countdown,
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.w_text_cancelled),
            )
            setViewVisibility(R.id.countdown_unit, View.GONE)
            return
        }
        val c = countdown(minutesUntil(row.departsAt, now))
        setTextViewText(R.id.countdown, c.number)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(numberSize))
        if (c.unit == null) {
            setViewVisibility(R.id.countdown_unit, View.GONE)
        } else {
            setViewVisibility(R.id.countdown_unit, View.VISIBLE)
            setTextViewText(R.id.countdown_unit, c.unit)
        }
    }

    private fun RemoteViews.badges(context: Context, row: WidgetRow) {
        removeAllViews(R.id.badges)
        val shown = if (row.badges.size > MAX_BADGES) row.badges.take(MAX_BADGES - 1) else row.badges
        shown.forEach { b ->
            addView(R.id.badges, badge(context, b.label, LineColors.of(b.label, b.mode).toArgb()))
        }
        if (shown.size < row.badges.size) {
            addView(R.id.badges, badge(context, "+${row.badges.size - shown.size}", MORE_BADGE_COLOR))
        }
    }

    private fun badge(context: Context, label: String, color: Int) =
        RemoteViews(context.packageName, R.layout.widget_badge).apply {
            setTextViewText(R.id.badge_label, label)
            setInt(R.id.badge_bg, "setColorFilter", color)
        }
}
