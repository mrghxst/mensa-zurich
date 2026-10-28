// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import android.content.Context
import android.content.Intent
import io.github.mrghxst.ethmensa.domain.model.Mensa

/** Opens the Sharesheet with the mensa link and its name as the title. */
fun shareMensa(context: Context, mensa: Mensa) = shareText(context, mensa.shareUrl, mensa.name)

fun shareText(context: Context, text: String, title: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (title != null) putExtra(Intent.EXTRA_TITLE, title)
    }
    context.startActivity(Intent.createChooser(send, title))
}
