package app.hullbeat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.notify.MaintenanceCheckWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * The class `AndroidManifest.xml` has named in `android:name` since before it
 * existed. Until now the manifest declared it and no file defined it, so the
 * process would have died with ClassNotFoundException before a single pixel
 * appeared. `tools/check_manifest.py` now fails the build on that.
 *
 * Holds the two things that must be created once per process: the Room
 * database, and the notification channel that every reminder is posted to.
 */
class HullBeatApp : Application() {

    @Volatile
    private var _database: AppDatabase? = null

    /**
     * One database per process, recreated if restored from backup.
     * The first real query happens off the main thread and opens it then.
     */
    val database: AppDatabase
        get() = synchronized(this) {
            _database ?: buildDatabase().also { _database = it }
        }

    private fun buildDatabase(): AppDatabase {
        return Room.databaseBuilder(applicationContext, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Normalize legacy category codes from custom component creation
                    db.execSQL("UPDATE component SET categoryCode = 'engine_main' WHERE categoryCode = 'engine'")
                    db.execSQL("UPDATE component SET categoryCode = 'elec_dc' WHERE categoryCode = 'electrical'")
                    db.execSQL("UPDATE component SET categoryCode = 'hull' WHERE categoryCode = 'hull_deck'")

                    // Re-attach misplaced 'Engine' meters from non-engine components to the engine component if present
                    db.execSQL("""
                        UPDATE meter SET componentId = (
                            SELECT c2.id FROM component c2 
                            WHERE c2.vesselId = (SELECT c1.vesselId FROM component c1 WHERE c1.id = meter.componentId)
                              AND c2.categoryCode IN ('engine_main', 'drivetrain')
                              AND c2.archived = 0
                            ORDER BY c2.id ASC LIMIT 1
                        )
                        WHERE meter.label = 'Engine'
                          AND EXISTS (
                            SELECT 1 FROM component c1 
                            WHERE c1.id = meter.componentId 
                              AND c1.categoryCode NOT IN ('engine_main', 'drivetrain')
                          )
                          AND EXISTS (
                            SELECT 1 FROM component c2 
                            WHERE c2.vesselId = (SELECT c1.vesselId FROM component c1 WHERE c1.id = meter.componentId)
                              AND c2.categoryCode IN ('engine_main', 'drivetrain')
                              AND c2.archived = 0
                          )
                    """.trimIndent())
                }
            })
            .build()
    }

    /**
     * Closes the active database connection pool and clears the cached instance.
     * Must be called before copying or replacing database files on disk.
     */
    fun closeAndResetDatabase() {
        synchronized(this) {
            try {
                _database?.close()
            } catch (_: Exception) {
            }
            _database = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleDailyCheck()
    }

    /**
     * The daily pass that decides whether anything is due.
     *
     * KEEP, not UPDATE: re-enqueueing on every process start with UPDATE would
     * reset the period each time the owner opens the app, so a boat used every
     * morning would never reach the 24-hour mark and never get a reminder.
     *
     * The initial delay aims at 09:00 rather than "whenever the app was first
     * launched", because a reminder that arrives at 03:00 gets swiped in the
     * dark. WorkManager batches and defers, so the real delivery drifts by up
     * to a few hours - it is a morning, not an alarm clock. An exact time
     * would need AlarmManager and the exact-alarm permission, which is not
     * worth the Play review for a maintenance nudge.
     */
    private fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<MaintenanceCheckWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(millisUntilNextMorning(), TimeUnit.MILLISECONDS)
            .setConstraints(
                // No network constraint on purpose: the app has no INTERNET
                // permission at all. Battery-not-low is the only condition
                // that makes sense for something this deferrable.
                Constraints.Builder().setRequiresBatteryNotLow(true).build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MaintenanceCheckWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun millisUntilNextMorning(hour: Int = 9): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    /**
     * No `Build.VERSION.SDK_INT >= O` guard here on purpose: `minSdk` is 26,
     * which IS Oreo, so the check can only ever be true. A version guard that
     * cannot fail reads as caution and is dead code - and it invites the next
     * reader to believe the project supports something it does not.
     *
     * The name and description come from resources, not from literals in this
     * file. They are user-visible text, and `values/strings.xml` already had
     * them - written and, until now, used by nothing.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_DUE,
            getString(R.string.notification_channel_due),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_due_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        /** Reminders that a job is due or overdue. */
        const val CHANNEL_DUE = "maintenance_due"
    }
}
