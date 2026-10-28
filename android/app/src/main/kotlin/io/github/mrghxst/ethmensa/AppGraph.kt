// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa

import android.content.Context

/** Manual dependency container, created once in [EthMensaApplication]. */
class AppGraph(context: Context) {
    val appContext: Context = context.applicationContext
}
