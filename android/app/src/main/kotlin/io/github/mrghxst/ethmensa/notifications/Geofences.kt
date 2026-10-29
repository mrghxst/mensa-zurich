// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import io.github.mrghxst.ethmensa.domain.model.Campus

/**
 * Campus geofences for location-based reminders (§8): 500 m around each campus, ENTER and EXIT.
 * Unlike GeofencingManager.makeGeoRegion on iOS, latitude and longitude are passed in the right order.
 */
object Geofences {
    const val RADIUS_METERS = 500f

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    // Play services fills in the geofence event, so this one PendingIntent must be mutable.
    // The explicit component keeps it from being redirected.
    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, GeofenceReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @SuppressLint("MissingPermission") // Checked by hasPermission; SecurityException caught below.
    fun register(context: Context) {
        if (!hasPermission(context)) return
        val fences = Campus.entries.map { campus ->
            Geofence.Builder()
                .setRequestId(campus.id)
                .setCircularRegion(campus.center.lat, campus.center.lng, RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(fences)
            .build()
        try {
            LocationServices.getGeofencingClient(context).addGeofences(request, pendingIntent(context))
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    fun unregister(context: Context) {
        LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context))
    }
}
