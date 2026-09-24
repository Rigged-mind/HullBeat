package app.hullbeat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.ui.HullBeatMainScaffold
import app.hullbeat.ui.now.NowViewModel
import app.hullbeat.ui.theme.HullBeatTheme
import app.hullbeat.ui.theme.Scheme

/**
 * The launcher Activity, and the landing point for both notification routes:
 * tapping the body opens the node, and "Продовжити…" opens the sheet with the
 * new-expiry field, because that record cannot be written without it.
 *
 * Hosts [HullBeatMainScaffold] and connects pending notification intents
 * into the Compose state.
 */
class MainActivity : ComponentActivity() {

    /**
     * Asked for on screen, never from the worker: a worker that posts into a
     * revoked permission fails silently, and the owner has no idea why the
     * reminders stopped. Declining is a real answer - the app keeps working,
     * it just stops nagging - so nothing here retries or explains twice.
     */
    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var pendingComponentId by mutableStateOf<Long?>(null)
    private var pendingRenew by mutableStateOf(false)
    private var currentScheme by mutableStateOf(Scheme.SYSTEM)

    private val nowViewModel: NowViewModel by viewModels {
        val app = application as HullBeatApp
        NowViewModel.provideFactory(app.database, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so decor fitting applies to the first frame.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        currentScheme = AppPreferences.getThemeScheme(this)
        requestNotificationPermissionIfNeeded()
        handle(intent)
        setContent {
            HullBeatTheme(scheme = currentScheme) {
                HullBeatMainScaffold(
                    nowViewModel = nowViewModel,
                    currentScheme = currentScheme,
                    onCycleScheme = {
                        currentScheme = when (currentScheme) {
                            Scheme.SYSTEM -> Scheme.SUNLIGHT
                            Scheme.SUNLIGHT -> Scheme.NIGHT
                            Scheme.NIGHT -> Scheme.DARK
                            Scheme.DARK -> Scheme.LIGHT
                            Scheme.LIGHT -> Scheme.SYSTEM
                        }
                        AppPreferences.setThemeScheme(this@MainActivity, currentScheme)
                    },
                    onSchemeChanged = {
                        currentScheme = it
                        AppPreferences.setThemeScheme(this@MainActivity, it)
                    },
                    pendingComponentId = pendingComponentId,
                    pendingRenew = pendingRenew,
                    database = (application as HullBeatApp).database,
                )
            }
        }
    }

    /** SINGLE_TOP means a second reminder arrives here, not in a new instance. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val componentId = intent?.getLongExtra(EXTRA_COMPONENT_ID, -1L) ?: -1L
        if (componentId <= 0L) return
        when (intent?.action) {
            ACTION_OPEN_COMPONENT -> {
                pendingComponentId = componentId
                pendingRenew = false
            }
            ACTION_RENEW_COMPONENT -> {
                pendingComponentId = componentId
                pendingRenew = true
            }
        }
    }

    /**
     * Unlike the notification CHANNEL - which needs no version guard, because
     * minSdk 26 IS the release that introduced channels - this permission
     * genuinely arrived in API 33, so the check here is load-bearing rather
     * than decorative.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val ACTION_OPEN_COMPONENT = "app.hullbeat.action.OPEN_COMPONENT"
        const val ACTION_RENEW_COMPONENT = "app.hullbeat.action.RENEW_COMPONENT"
        const val EXTRA_COMPONENT_ID = "component_id"
    }
}
