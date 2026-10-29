// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.mrghxst.ethmensa.EthMensaApplication
import io.github.mrghxst.ethmensa.MainActivity
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.OpeningState
import io.github.mrghxst.ethmensa.ui.theme.DarkEthColors
import io.github.mrghxst.ethmensa.ui.theme.LightEthColors

private val Background = ColorProvider(day = LightEthColors.surface, night = DarkEthColors.surface)
private val TextPrimary = ColorProvider(day = LightEthColors.textPrimary, night = DarkEthColors.textPrimary)
private val TextSecondary = ColorProvider(day = LightEthColors.textSecondary, night = DarkEthColors.textSecondary)
private val TextTertiary = ColorProvider(day = LightEthColors.textTertiary, night = DarkEthColors.textTertiary)
private val OpenColor = ColorProvider(day = LightEthColors.open, night = DarkEthColors.open)
private val ClosedColor = ColorProvider(day = LightEthColors.closed, night = DarkEthColors.closed)
private val UnknownColor = ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFF525252))

/**
 * "Today's menu" home-screen widget (Phase 9): the favorite or most opened mensa, its state and
 * hours, and today's meals. Text only, so it stays far below the RemoteViews bitmap limit.
 */
class MenuWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = (context.applicationContext as EthMensaApplication).graph
        // Started from a widget update with no app data in memory: load it (cache first, then fresh).
        if (graph.mensaStore.state.value.mensas == null && !graph.mensaStore.state.value.loading) graph.mensaStore.refresh()
        val updates = widgetData(graph)
        provideContent {
            // Collected inside the session, so a refresh or a settings change recomposes the widget.
            val data by updates.collectAsState(initial = null)
            WidgetContent(data)
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        val fallback = RemoteViews(context.packageName, R.layout.widget_error)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }
}

@Composable
private fun WidgetContent(data: WidgetData?) {
    val context = LocalContext.current
    val link = data?.mensaId?.let { "ethmensa://s/$it" }
    val intent = Intent(context, MainActivity::class.java).apply {
        if (link != null) {
            action = Intent.ACTION_VIEW
            this.data = link.toUri()
        }
    }
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(20.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(actionStartActivity(intent)),
    ) {
        if (data == null) {
            Text(context.getString(R.string.widget_loading), style = TextStyle(color = TextSecondary, fontSize = 13.sp))
            return@Column
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                data.name,
                maxLines = 1,
                style = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            val (dot, label) = when (data.state) {
                OpeningState.OPEN -> OpenColor to context.getString(R.string.widget_open)
                OpeningState.CLOSED -> ClosedColor to context.getString(R.string.widget_closed)
                else -> UnknownColor to null
            }
            Box(GlanceModifier.size(8.dp).background(dot).cornerRadius(4.dp)) {}
            if (label != null) {
                Spacer(GlanceModifier.width(6.dp))
                Text(label, style = TextStyle(color = TextSecondary, fontSize = 12.sp))
            }
        }
        if (data.hours.isNotEmpty()) {
            Text(data.hours, maxLines = 1, style = TextStyle(color = TextTertiary, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
        }
        Spacer(GlanceModifier.height(8.dp))
        if (data.meals.isEmpty()) {
            Text(context.getString(R.string.no_menu), style = TextStyle(color = TextSecondary, fontSize = 13.sp))
            return@Column
        }
        // As many meal lines as fit: about 22 dp each below a 64 dp header.
        val fit = ((LocalSize.current.height.value - 64f) / 22f).toInt().coerceAtLeast(1)
        val shown = if (data.meals.size > fit) data.meals.take((fit - 1).coerceAtLeast(1)) else data.meals
        shown.forEach { line ->
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(line.name, maxLines = 1, style = TextStyle(color = TextPrimary, fontSize = 13.sp), modifier = GlanceModifier.defaultWeight())
                if (line.price != null) {
                    Spacer(GlanceModifier.width(8.dp))
                    Text(line.price, maxLines = 1, style = TextStyle(color = TextTertiary, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                }
            }
        }
        val more = data.meals.size - shown.size
        if (more > 0) {
            Text(context.resources.getQuantityString(R.plurals.widget_more, more, more), style = TextStyle(color = TextTertiary, fontSize = 12.sp))
        }
    }
}
