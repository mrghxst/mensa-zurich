// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mrghxst.ethmensa.R
import io.github.mrghxst.ethmensa.domain.model.NotificationSound
import io.github.mrghxst.ethmensa.notifications.Geofences
import io.github.mrghxst.ethmensa.notifications.Reminders
import io.github.mrghxst.ethmensa.ui.components.Icons
import io.github.mrghxst.ethmensa.ui.theme.Eth
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Denied { NOTIFICATIONS, LOCATION }

/** Settings > Notifications (§10.5): reminder switch, sound, time and location-based reminders. */
@Composable
fun NotificationsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf<Denied?>(null) }
    var askBackground by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    // Permissions can be revoked in the system settings while the app is away: revert the switches.
    val activity = LocalActivity.current as? ComponentActivity
    if (activity != null) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME, lifecycleOwner = activity) {
            val current = viewModel.settings.value
            if (current.notifications && !Reminders.canPost(context)) viewModel.update { it.copy(notifications = false) }
            if (current.locationNotifications && !Geofences.hasPermission(context)) viewModel.update { it.copy(locationNotifications = false) }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && Reminders.canPost(context)) viewModel.update { it.copy(notifications = true) } else denied = Denied.NOTIFICATIONS
    }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && Geofences.hasPermission(context)) viewModel.update { it.copy(locationNotifications = true) } else denied = Denied.LOCATION
    }
    // Foreground location first; background is a separate, explained step (Android 11+ sends the user to Settings).
    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        when {
            result[Manifest.permission.ACCESS_FINE_LOCATION] != true -> denied = Denied.LOCATION
            Geofences.hasPermission(context) -> viewModel.update { it.copy(locationNotifications = true) }
            else -> askBackground = true
        }
    }

    fun setNotifications(on: Boolean) {
        when {
            !on -> viewModel.update { it.copy(notifications = false) }
            Reminders.canPost(context) -> viewModel.update { it.copy(notifications = true) }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted(context, Manifest.permission.POST_NOTIFICATIONS) ->
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Granted, but notifications are blocked in the system settings.
            else -> denied = Denied.NOTIFICATIONS
        }
    }

    fun setLocation(on: Boolean) {
        when {
            !on -> viewModel.update { it.copy(locationNotifications = false) }
            Geofences.hasPermission(context) -> viewModel.update { it.copy(locationNotifications = true) }
            granted(context, Manifest.permission.ACCESS_FINE_LOCATION) -> askBackground = true
            else -> foregroundLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    val preview = rememberSoundPreview(context)

    SettingsPage(stringResource(R.string.notifications), onBack) {
        settingsGroup("enable", { null }, footerText = { stringResource(R.string.enable_notifications_footer) }) {
            SettingsSwitch(
                title = stringResource(R.string.enable_notifications),
                checked = settings.notifications,
                onCheckedChange = ::setNotifications,
                icon = Icons.bell,
            )
        }
        if (settings.notifications) {
            settingsGroup("sound", { stringResource(R.string.notification_sound) }) {
                NotificationSound.entries.forEachIndexed { index, sound ->
                    if (index > 0) RowDivider()
                    SoundRow(
                        label = stringResource(soundLabel(sound)),
                        selected = settings.notificationSound == sound,
                        onSelect = {
                            viewModel.update { it.copy(notificationSound = sound) }
                            preview(sound)
                        },
                    )
                }
            }
            settingsGroup(
                "time",
                { null },
                footerText = { stringResource(R.string.time_of_notifications_footer) + " " + stringResource(R.string.notification_time_footer_android) },
            ) {
                SettingsRow(
                    title = stringResource(R.string.time_of_notifications),
                    onClick = { pickTime = true },
                    icon = Icons.clock,
                    value = formatTime(settings.notificationTime),
                )
            }
            settingsGroup("location", { null }, footerText = { stringResource(R.string.locations_based_notifications_footer_android) }) {
                SettingsSwitch(
                    title = stringResource(R.string.locations_based_notifications),
                    checked = settings.locationNotifications,
                    onCheckedChange = ::setLocation,
                    icon = Icons.mapPin,
                )
            }
        }
    }

    if (pickTime) {
        TimeDialog(
            initial = settings.notificationTime,
            onConfirm = { time ->
                viewModel.update { it.copy(notificationTime = time) }
                pickTime = false
            },
            onDismiss = { pickTime = false },
        )
    }
    if (askBackground) {
        AlertDialog(
            onDismissRequest = { askBackground = false },
            title = { Text(stringResource(R.string.background_location_title), style = Eth.type.screenTitle) },
            text = { Text(stringResource(R.string.background_location_message), style = Eth.type.body, color = Eth.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    askBackground = false
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) { Text(stringResource(R.string.continue_label), color = Eth.colors.accent, style = Eth.type.label) }
            },
            dismissButton = {
                TextButton(onClick = { askBackground = false }) {
                    Text(stringResource(R.string.cancel), color = Eth.colors.textPrimary, style = Eth.type.label)
                }
            },
            containerColor = Eth.colors.surface,
        )
    }
    denied?.let { kind ->
        AlertDialog(
            onDismissRequest = { denied = null },
            title = { Text(stringResource(R.string.permission_denied), style = Eth.type.screenTitle) },
            text = {
                Text(
                    stringResource(if (kind == Denied.NOTIFICATIONS) R.string.notifications_denied_message else R.string.location_denied_message),
                    style = Eth.type.body,
                    color = Eth.colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    denied = null
                    openPermissionSettings(context, kind)
                }) { Text(stringResource(R.string.open_settings), color = Eth.colors.accent, style = Eth.type.label) }
            },
            dismissButton = {
                TextButton(onClick = { denied = null }) {
                    Text(stringResource(R.string.close), color = Eth.colors.textPrimary, style = Eth.type.label)
                }
            },
            containerColor = Eth.colors.surface,
        )
    }
}

@Composable
private fun SoundRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Eth.type.body, color = Eth.colors.textPrimary, modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Eth.colors.accent, unselectedColor = Eth.colors.textTertiary),
        )
    }
}

@Composable
private fun TimeDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.time_of_notifications), style = Eth.type.screenTitle) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Eth.colors.surfaceMuted,
                    selectorColor = Eth.colors.accent,
                    timeSelectorSelectedContainerColor = Eth.colors.accentSoft,
                    timeSelectorSelectedContentColor = Eth.colors.accent,
                    periodSelectorSelectedContainerColor = Eth.colors.accentSoft,
                    periodSelectorSelectedContentColor = Eth.colors.accent,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(android.R.string.ok), color = Eth.colors.accent, style = Eth.type.label)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Eth.colors.textPrimary, style = Eth.type.label) }
        },
        containerColor = Eth.colors.surface,
    )
}

/** Plays a short preview of a reminder sound, stopping the previous one. Off the main thread: play() prepares a MediaPlayer. */
@Composable
private fun rememberSoundPreview(context: Context): (NotificationSound) -> Unit {
    val scope = rememberCoroutineScope()
    val playing = remember { AtomicReference<Ringtone?>(null) }
    DisposableEffect(Unit) { onDispose { playing.getAndSet(null)?.stop() } }
    return remember {
        { sound ->
            scope.launch(Dispatchers.IO) {
                playing.getAndSet(null)?.stop()
                val ringtone = RingtoneManager.getRingtone(context, Reminders.soundUri(context, sound)) ?: return@launch
                playing.set(ringtone)
                ringtone.play()
            }
        }
    }
}

private fun soundLabel(sound: NotificationSound): Int = when (sound) {
    NotificationSound.SYSTEM -> R.string.sound_system
    NotificationSound.DEFAULT -> R.string.default_label
    NotificationSound.CLICK -> R.string.sound_click
}

@Composable
private fun formatTime(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun openPermissionSettings(context: Context, kind: Denied) {
    val intent = when (kind) {
        Denied.NOTIFICATIONS -> Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        Denied.LOCATION -> Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    runCatching { context.startActivity(intent) }
}
