// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.mrghxst.ethmensa.AppGraph
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MenuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MenuWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WidgetRefreshWorker.NAME)
    }
}

/** Refreshes the widget every 4 hours (menus change daily, opening state during the day). */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        MenuWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val NAME = "menu_widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(4, TimeUnit.HOURS).build()
            // KEEP: a periodic request stays enqueued, so onUpdate doesn't restart it.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/** Refreshes placed widgets when the app finishes a reload or the widget-relevant settings change. */
object WidgetUpdates {
    fun start(context: Context, graph: AppGraph) {
        graph.appScope.launch {
            combine(graph.mensaStore.state, graph.settings.settings) { load, s ->
                listOf(load.allProvidersUpToDate, s.favorites, s.hidden, s.diet, s.priceType)
            }
                .distinctUntilChanged()
                .filter { it.first() == true }
                .collect {
                    if (GlanceAppWidgetManager(context).getGlanceIds(MenuWidget::class.java).isNotEmpty()) {
                        MenuWidget().updateAll(context)
                    }
                }
        }
    }
}
