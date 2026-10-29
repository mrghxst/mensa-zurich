// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import io.github.mrghxst.ethmensa.AppGraph
import io.github.mrghxst.ethmensa.EthMensaApplication
import kotlinx.coroutines.launch

private val Context.graph: AppGraph get() = (applicationContext as EthMensaApplication).graph

/** Runs [block] on the app scope while keeping the broadcast alive. */
private fun BroadcastReceiver.async(context: Context, block: suspend AppGraph.() -> Unit) {
    val pending = goAsync()
    val graph = context.graph
    graph.appScope.launch {
        try {
            graph.block()
        } finally {
            pending.finish()
        }
    }
}

/** The reminder alarm: posts the notification (if still allowed) and arms the next weekday. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        async(context) {
            val current = settings.current()
            val inside = settings.flag(INSIDE_CAMPUS_FLAG)
            if (shouldArmReminder(current, inside)) Reminders.post(context, current.notificationSound)
            Reminders.schedule(context, current, inside)
        }
    }

    companion object {
        const val ACTION_REMIND = "io.github.mrghxst.ethmensa.action.REMIND"
    }
}

/** Boot, clock, time zone and app update: alarms and geofences don't survive these. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        async(context) {
            val current = settings.current()
            Reminders.schedule(context, current, settings.flag(INSIDE_CAMPUS_FLAG))
            if (current.notifications && current.locationNotifications) Geofences.register(context)
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/** ENTER arms the reminder, EXIT cancels it (§8). */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val inside = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }
        async(context) {
            settings.setFlag(INSIDE_CAMPUS_FLAG, inside)
            Reminders.schedule(context, settings.current(), inside)
        }
    }
}

/** DataStore flag: inside a campus geofence. */
const val INSIDE_CAMPUS_FLAG = "inside_campus"
