package app.hullbeat.data.db

import app.hullbeat.R
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordDescriptionTest {

    private val enResolver: (Int) -> String = { resId ->
        when (resId) {
            R.string.action_mark_done -> "Done"
            R.string.record_from_notification -> "Logged from the notification"
            R.string.action_renew -> "Renew…"
            R.string.setup_default_name -> "My boat"
            R.string.store_default_loc_engine -> "Engine room"
            R.string.store_default_loc_cockpit -> "Cockpit locker"
            R.string.store_default_loc_chart -> "Chart table"
            R.string.store_default_loc_forepeak -> "Forepeak"
            R.string.store_default_loc_galley -> "Galley"
            else -> ""
        }
    }

    private val ukResolver: (Int) -> String = { resId ->
        when (resId) {
            R.string.action_mark_done -> "Зроблено"
            R.string.record_from_notification -> "Виконано зі сповіщення"
            R.string.action_renew -> "Продовжити…"
            R.string.setup_default_name -> "Моє судно"
            R.string.store_default_loc_engine -> "Моторний відсік"
            R.string.store_default_loc_cockpit -> "Рундук кокпіта"
            R.string.store_default_loc_chart -> "Штурманський стіл"
            R.string.store_default_loc_forepeak -> "Форпік"
            R.string.store_default_loc_galley -> "Камбуз"
            else -> ""
        }
    }

    @Test
    fun ukrainianDoneTranslatesToEnglishInEnglishContext() {
        val result = formatRecordDescription("Зроблено", enResolver)
        assertEquals("Done", result)
    }

    @Test
    fun englishDoneTranslatesToUkrainianInUkrainianContext() {
        val result = formatRecordDescription("Done", ukResolver)
        assertEquals("Зроблено", result)
    }

    @Test
    fun ukrainianNotificationRecordTranslatesToEnglish() {
        val result = formatRecordDescription("Виконано зі сповіщення", enResolver)
        assertEquals("Logged from the notification", result)
    }

    @Test
    fun englishNotificationRecordTranslatesToUkrainian() {
        val result = formatRecordDescription("Logged from the notification", ukResolver)
        assertEquals("Виконано зі сповіщення", result)
    }

    @Test
    fun canonicalTokensTranslateCorrectly() {
        assertEquals("Done", formatRecordDescription("action_mark_done", enResolver))
        assertEquals("Зроблено", formatRecordDescription("@action_mark_done", ukResolver))
        assertEquals("Logged from the notification", formatRecordDescription("record_from_notification", enResolver))
        assertEquals("Виконано зі сповіщення", formatRecordDescription("@record_from_notification", ukResolver))
        assertEquals("Renew…", formatRecordDescription("action_renew", enResolver))
        assertEquals("Продовжити…", formatRecordDescription("@action_renew", ukResolver))
    }

    @Test
    fun customUserDescriptionIsNotModified() {
        val custom1 = "Replaced raw water impeller and gasket"
        val custom2 = "Замінено масло та фільтр"
        assertEquals(custom1, formatRecordDescription(custom1, enResolver))
        assertEquals(custom1, formatRecordDescription(custom1, ukResolver))
        assertEquals(custom2, formatRecordDescription(custom2, enResolver))
        assertEquals(custom2, formatRecordDescription(custom2, ukResolver))
    }

    @Test
    fun blankDescriptionReturnsEmpty() {
        assertEquals("", formatRecordDescription("", enResolver))
        assertEquals("", formatRecordDescription("   ", enResolver))
    }

    @Test
    fun defaultVesselNameTranslatesDynamically() {
        val ukBoat = Vessel(name = "Моє судно", hullType = "monohull_sail", engine = "diesel", drive = "shaft")
        val enBoat = Vessel(name = "My boat", hullType = "monohull_sail", engine = "diesel", drive = "shaft")
        val customBoat = Vessel(name = "Sea Breeze", hullType = "monohull_sail", engine = "diesel", drive = "shaft")

        assertEquals("My boat", ukBoat.displayName(enResolver))
        assertEquals("Моє судно", enBoat.displayName(ukResolver))
        assertEquals("Sea Breeze", customBoat.displayName(enResolver))
        assertEquals("Sea Breeze", customBoat.displayName(ukResolver))
    }

    @Test
    fun starterLockersTranslateDynamically() {
        assertEquals("Engine room", storageLocationDisplayName("Моторний відсік", enResolver))
        assertEquals("Моторний відсік", storageLocationDisplayName("Engine room", ukResolver))
        assertEquals("Cockpit locker", storageLocationDisplayName("Рундук кокпіта", enResolver))
        assertEquals("Forepeak", storageLocationDisplayName("Форпік", enResolver))
        assertEquals("Custom Locker Aft", storageLocationDisplayName("Custom Locker Aft", enResolver))
    }
}
