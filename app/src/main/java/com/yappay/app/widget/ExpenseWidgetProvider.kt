package com.yappay.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.yappay.app.AppContainer
import com.yappay.app.ExpenseApp
import com.yappay.app.R
import com.yappay.app.data.AppSettings
import com.yappay.app.util.DateRanges
import com.yappay.app.util.MoneyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Home-screen widget.
 *
 * It is intentionally a plain RemoteViews widget: the widget process does no listening, no polling
 * and no long-lived work. A tap launches [QuickCaptureActivity], which owns the microphone for the
 * duration of one press-and-hold gesture.
 *
 * The "Today" total is refreshed:
 *  - when the widget is placed / resized / its 30-minute update fires,
 *  - immediately after a quick-capture save,
 *  - when the main app goes to the background.
 */
class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as? ExpenseApp ?: return
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                update(context, app.container, appWidgetManager, appWidgetIds)
            } catch (_: Exception) {
                // Keep the last good RemoteViews if the DB/DataStore is momentarily unavailable.
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // The launcher also persists the new options; refresh reads them back and swaps layouts.
        refresh(context)
    }

    companion object {

        /** Fire-and-forget refresh on the application scope. Safe to call when no widget is placed. */
        fun refresh(context: Context) {
            val app = context.applicationContext as? ExpenseApp ?: return
            app.appScope.launch {
                try {
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(
                        ComponentName(context, ExpenseWidgetProvider::class.java),
                    )
                    if (ids.isNotEmpty()) {
                        update(context, app.container, manager, ids)
                    }
                } catch (_: Exception) {
                    // Best-effort refresh; the widget keeps its previous content.
                }
            }
        }

        /**
         * Called right after a widget capture is inserted. [settings] is passed in because the
         * capture flow already read it, avoiding a second DataStore read in the save path.
         */
        suspend fun updateAll(
            context: Context,
            container: AppContainer,
            settings: AppSettings? = null,
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ExpenseWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            update(context, container, manager, ids, settings)
        }

        private suspend fun update(
            context: Context,
            container: AppContainer,
            manager: AppWidgetManager,
            ids: IntArray,
            knownSettings: AppSettings? = null,
        ) {
            val settings = knownSettings ?: container.settingsRepository.settings.first()
            val today = container.expenseRepository.total(DateRanges.day(LocalDate.now()))
            val month = YearMonth.now()
            val monthTotal = container.expenseRepository.total(DateRanges.month(month))

            ids.forEach { id ->
                val views = buildViews(
                    context = context,
                    settings = settings,
                    todayMinor = today,
                    monthMinor = monthTotal,
                    month = month,
                    options = manager.getAppWidgetOptions(id),
                )
                manager.updateAppWidget(id, views)
            }
        }

        private fun buildViews(
            context: Context,
            settings: AppSettings,
            todayMinor: Long,
            monthMinor: Long,
            month: YearMonth,
            options: Bundle,
        ): RemoteViews {
            val widthDp = options
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                .takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
            val heightDp = options
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                .takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
            val compact = widthDp < WIDE_MIN_WIDTH_DP
            val tall = heightDp >= TALL_MIN_HEIGHT_DP
            val showMonth = !compact && widthDp >= STATS_MIN_WIDTH_DP

            val layout = when {
                compact && tall -> R.layout.expense_widget_tall
                compact -> R.layout.expense_widget_compact
                else -> R.layout.expense_widget
            }
            val views = RemoteViews(context.packageName, layout)

            val todayText = MoneyFormatter.format(
                todayMinor,
                settings.currencySymbol,
                settings.grouping,
            )
            views.setTextViewText(R.id.widget_today_value, todayText)

            val monthText = MoneyFormatter.format(
                monthMinor,
                settings.currencySymbol,
                settings.grouping,
            )
            if (!compact) {
                views.setTextViewText(R.id.widget_month_value, monthText)
                views.setTextViewText(
                    R.id.widget_month_caption,
                    month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                )
                views.setViewVisibility(
                    R.id.widget_month_block,
                    if (showMonth) View.VISIBLE else View.GONE,
                )
                views.setViewVisibility(
                    R.id.widget_hint_block,
                    if (showMonth) View.GONE else View.VISIBLE,
                )
            }

            views.setContentDescription(
                R.id.widget_root,
                if (showMonth) {
                    context.getString(
                        R.string.widget_content_description_wide,
                        todayText,
                        monthText,
                    )
                } else {
                    context.getString(R.string.widget_content_description, todayText)
                },
            )

            val launch = Intent(context, QuickCaptureActivity::class.java).apply {
                // NO_ANIMATION removes the launcher->activity transition. The transparent theme
                // removes the starting-window flash.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }

        /** Below this width the card uses a narrow layout (compact or tall). */
        private const val WIDE_MIN_WIDTH_DP = 250

        /** Above this width the middle column becomes a real "this month" stat. */
        private const val STATS_MIN_WIDTH_DP = 320

        /** A narrow widget taller than this switches from the two-row compact card to the tall card. */
        private const val TALL_MIN_HEIGHT_DP = 170

        /** Some launchers return an empty options bundle on the first update. */
        private const val DEFAULT_WIDTH_DP = 250
        private const val DEFAULT_HEIGHT_DP = 110
    }
}
