// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.mrghxst.ethmensa.MainActivity
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.model.NotificationSound
import io.github.mrghxst.ethmensa.domain.model.Settings
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/** The next Monday–Friday at [time] strictly after [now] (iOS schedules weekdays only). */
fun nextWeekdayAt(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    var candidate = now.with(time).withSecond(0).withNano(0)
    if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
    while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
        candidate = candidate.plusDays(1)
    }
    return candidate
}

/** Whether a reminder should be armed: on, and (with location-based on) only while inside a campus. */
fun shouldArmReminder(settings: Settings, insideCampus: Boolean): Boolean =
    settings.notifications && (!settings.locationNotifications || insideCampus)

/** Daily menu reminder: one channel per sound (channel sounds can't change), AlarmManager window, posting. */
object Reminders {
    private const val NOTIFICATION_ID = 1
    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    fun channelId(sound: NotificationSound) = "reminder_${sound.id}"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        NotificationSound.entries.forEach { sound ->
            val name = context.getString(
                when (sound) {
                    NotificationSound.SYSTEM -> R.string.channel_reminder_system
                    NotificationSound.DEFAULT -> R.string.channel_reminder_default
                    NotificationSound.CLICK -> R.string.channel_reminder_click
                },
            )
            val channel = NotificationChannel(channelId(sound), name, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(soundUri(context, sound), attributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun soundUri(context: Context, sound: NotificationSound): Uri = when (sound) {
        NotificationSound.SYSTEM -> AndroidSettings.System.DEFAULT_NOTIFICATION_URI
        NotificationSound.DEFAULT -> rawUri(context, R.raw.reminder_default)
        NotificationSound.CLICK -> rawUri(context, R.raw.reminder_click)
    }

    private fun rawUri(context: Context, res: Int): Uri =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$res".toUri()

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                )

    fun post(context: Context, sound: NotificationSound) {
        if (!canPost(context)) return
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val settings = PendingIntent.getActivity(
            context,
            1,
            Intent(Intent.ACTION_VIEW, "ethmensa://notifications".toUri(), context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channelId(sound))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentText(context.getString(R.string.check_out_todays_mensa_menu))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.check_out_todays_mensa_menu)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.settings), settings)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    /** Clears our delivered reminders (iOS clears them when the app opens). */
    fun clearDelivered(context: Context) = NotificationManagerCompat.from(context).cancelAll()

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Arms the next weekday reminder, or cancels it. Inexact window: no exact-alarm permission needed. */
    fun schedule(context: Context, settings: Settings, insideCampus: Boolean, now: ZonedDateTime = ZonedDateTime.now()) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        if (!shouldArmReminder(settings, insideCampus)) {
            alarms.cancel(alarmIntent(context))
            return
        }
        val at = nextWeekdayAt(now, settings.notificationTime).toInstant().toEpochMilli()
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, alarmIntent(context))
    }
}
