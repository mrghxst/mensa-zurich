// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import android.content.Context
import android.content.Intent
import io.github.mrghxst.ethmensa.domain.model.Mensa
import io.github.mrghxst.ethmensa.domain.todayWeekday
import io.github.mrghxst.ethmensa.domain.zurichClock

/** Opens the Sharesheet with the mensa's name, today's hours and dishes as text. */
fun shareMensa(context: Context, mensa: Mensa) =
    shareText(context, mensa.shareText(todayWeekday(zurichClock())), mensa.name)

fun shareText(context: Context, text: String, title: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (title != null) putExtra(Intent.EXTRA_TITLE, title)
    }
    context.startActivity(Intent.createChooser(send, title))
}
