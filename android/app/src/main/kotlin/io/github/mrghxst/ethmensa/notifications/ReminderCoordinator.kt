// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.notifications

import android.content.Context
import io.github.mrghxst.ethmensa.AppGraph
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Re-arms the reminder and (un)registers geofences whenever the relevant settings change, resets included. */
object ReminderCoordinator {
    fun start(context: Context, graph: AppGraph) {
        Reminders.ensureChannels(context)
        val settings = graph.settings
        graph.appScope.launch {
            combine(settings.settings, settings.flagFlow(INSIDE_CAMPUS_FLAG)) { s, inside -> s to inside }
                .distinctUntilChanged { (a, ai), (b, bi) ->
                    ai == bi && a.notifications == b.notifications && a.notificationTime == b.notificationTime &&
                        a.locationNotifications == b.locationNotifications
                }
                .collect { (s, inside) -> Reminders.schedule(context, s, inside) }
        }
        graph.appScope.launch {
            settings.settings.map { it.notifications && it.locationNotifications }
                .distinctUntilChanged()
                .collect { on ->
                    if (on) {
                        Geofences.register(context)
                    } else {
                        Geofences.unregister(context)
                        settings.setFlag(INSIDE_CAMPUS_FLAG, false)
                    }
                }
        }
    }
}
