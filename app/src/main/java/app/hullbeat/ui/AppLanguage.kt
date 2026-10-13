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

    /**
     * The languages a person can pick, in the order a picker lists them.
     *
     * It must name exactly the locales in res/xml/locales_config.xml, and those
     * must be exactly the translated `values-<tag>` folders plus English;
     * tools/check_locales.py fails the build otherwise. A tag is added here
     * only when its translation has passed a native sailor's review - a
     * `values-de` folder reaches every phone set to German whether or not it
     * is listed, so the real gate is not merging that folder before then.
     */
    val SUPPORTED: List<String> = listOf(UK, EN)

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * The language the app is actually showing: the chosen one, or the first
     * system preference HullBeat has, or English - which is what `values/`
     * gives a phone whose languages HullBeat does not ship.
     *
     * This used to return the raw system tag. On a German phone that was
     * "de", matching no option, and the toggle below - "UK if not UK" - then
     * answered a tap on English by switching to Ukrainian.
     */
    fun current(context: Context): String {
        if (isSupported()) {
            val chosen = context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
            if (chosen != null && !chosen.isEmpty) return resolve(chosen)
        }
        return resolve(LocaleList.getDefault())
    }

    private fun resolve(prefs: LocaleList): String {
        for (i in 0 until prefs.size()) {
            val tag = prefs[i]?.language ?: continue
            if (tag in SUPPORTED) return tag
        }
        return EN
    }

    /** Switch to [tag]; a tag HullBeat does not ship is ignored. */
    fun set(context: Context, tag: String) {
        if (!isSupported() || tag !in SUPPORTED) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        manager.applicationLocales = LocaleList.forLanguageTags(tag)
    }

    /** The next language in [SUPPORTED]; with two, the other one. */
    fun toggle(context: Context) {
        set(context, next(context))
    }

    private fun next(context: Context): String {
        val i = SUPPORTED.indexOf(current(context))
        return SUPPORTED[(i + 1) % SUPPORTED.size]
    }

    /** What the button shows: the language it will switch TO. */
    fun glyph(context: Context): String = label(next(context))

    /** Short label for a tag. Ukraine's code is UA; the language's is uk. */
    fun label(tag: String): String = if (tag == UK) "UA" else tag.uppercase()
}
