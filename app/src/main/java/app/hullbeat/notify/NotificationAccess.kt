package app.hullbeat.notify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.hullbeat.HullBeatApp

/**
 * Whether Android will actually show a reminder, as opposed to whether the
 * owner wants one.
 *
 * The two used to be one switch that only knew the second half: with the
 * permission denied, or the channel turned off in system settings, Settings
 * still showed reminders "on" while every post was dropped without a trace.
 */
object NotificationAccess {

    /** App-level notifications are allowed and the due channel is not muted. */
    fun systemAllows(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannelCompat(HullBeatApp.CHANNEL_DUE)
        return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    /** The runtime permission exists on this release and has not been granted. */
    fun canAskAtRuntime(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /** The system page where the owner can lift a denial the app may no longer ask about. */
    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
