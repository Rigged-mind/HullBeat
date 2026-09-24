package app.hullbeat.ui

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * Per-app language, without dragging in AppCompat.
 *
 * The app shipped 622 translated strings and no way to ask for them. Android
 * only offers a per-app language picker when the manifest declares
 * `android:localeConfig`, which it did not, so the only way to read HullBeat
 * in Ukrainian was to put the whole phone into Ukrainian.
 *
 * `LocaleManager` is API 33. Below that a per-app locale needs AppCompat,
 * and adding a library to this project for one button is the wrong trade:
 * older devices follow the system language, which is what they did before.
 */
object AppLanguage {

    const val UK = "uk"
    const val EN = "en"

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** The tag in force, or the first system preference when none is set. */
    fun current(context: Context): String {
        if (!isSupported()) return systemTag()
        val manager = context.getSystemService(LocaleManager::class.java)
            ?: return systemTag()
        val chosen = manager.applicationLocales
        if (chosen.isEmpty) return systemTag()
        return chosen[0]?.language ?: systemTag()
    }

    private fun systemTag(): String =
        LocaleList.getDefault()[0]?.language ?: EN

    /**
     * Two languages, so one button is enough. A list would be the right
     * control at three, and this file is where that change belongs.
     */
    fun toggle(context: Context) {
        if (!isSupported()) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        val next = if (current(context) == UK) EN else UK
        manager.applicationLocales = LocaleList.forLanguageTags(next)
    }

    /** What the button shows: the language it will switch TO. */
    fun glyph(context: Context): String =
        if (current(context) == UK) "EN" else "UA"
}
