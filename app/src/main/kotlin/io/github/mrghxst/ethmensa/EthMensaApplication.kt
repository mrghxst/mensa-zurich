// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa

import android.app.Application
import io.github.mrghxst.ethmensa.notifications.ReminderCoordinator
import io.github.mrghxst.ethmensa.widget.WidgetUpdates

class EthMensaApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        ReminderCoordinator.start(this, graph)
        WidgetUpdates.start(this, graph)
    }
}
