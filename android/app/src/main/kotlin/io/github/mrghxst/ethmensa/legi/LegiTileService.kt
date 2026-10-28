// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.legi

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.mrghxst.ethmensa.MainActivity

/** Quick Settings tile "Legi card" (iOS Control Center button): opens the app on `ethmensa://legi`. */
class LegiTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        // An action tile, not a toggle.
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openLegi) else openLegi()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openLegi() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("ethmensa://legi"), this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
