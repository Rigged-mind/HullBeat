package app.hullbeat.domain

import app.hullbeat.data.preferences.LengthUnit
import app.hullbeat.data.preferences.PressureUnit
import app.hullbeat.data.preferences.TemperatureUnit
import app.hullbeat.data.preferences.VolumeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class SpecUnitsTest {

    private val metric = UnitSettings(LengthUnit.METRES, VolumeUnit.LITRES, PressureUnit.BAR, TemperatureUnit.CELSIUS)
    private val imperial = UnitSettings(LengthUnit.FEET, VolumeUnit.GALLONS_US, PressureUnit.PSI, TemperatureUnit.FAHRENHEIT)
    private val identity: (String) -> String? = { it }

    private fun units(json: String?) = SpecUnits.readUnits(json)
    private fun spec(json: String) = Json.parseToJsonElement(json).jsonObject.mapValues { it.value.jsonPrimitive.content }

    // ------------------------------------------------------------ conversion

    @Test
    fun `conversions go both ways and temperature keeps its offset`() {
        assertEquals(10.567, SpecUnit.LITRE.convert(40.0, SpecUnit.US_GALLON), 0.001)
        assertEquals(40.0, SpecUnit.US_GALLON.convert(SpecUnit.LITRE.convert(40.0, SpecUnit.US_GALLON), SpecUnit.LITRE), 1e-9)
        assertEquals(212.0, SpecUnit.CELSIUS.convert(100.0, SpecUnit.FAHRENHEIT), 1e-9)
        assertEquals(0.0, SpecUnit.FAHRENHEIT.convert(32.0, SpecUnit.CELSIUS), 1e-9)
        assertEquals(1.0, SpecUnit.MILLIMETRE.convert(25.4, SpecUnit.INCH), 1e-9)
        assertEquals(29.0076, SpecUnit.BAR.convert(2.0, SpecUnit.PSI), 1e-4)
        assertEquals(1585.03, SpecUnit.LITRE_PER_MINUTE.convert(100.0, SpecUnit.US_GALLON_PER_HOUR), 0.01)
    }

    @Test
    fun `settings pick small lengths and flow from the length and volume rows`() {
        assertEquals(SpecUnit.MILLIMETRE, metric.unitFor(SpecQuantity.SMALL_LENGTH))
        assertEquals(SpecUnit.INCH, imperial.unitFor(SpecQuantity.SMALL_LENGTH))
        assertEquals(SpecUnit.LITRE_PER_MINUTE, metric.unitFor(SpecQuantity.FLOW))
        assertEquals(SpecUnit.US_GALLON_PER_HOUR, imperial.unitFor(SpecQuantity.FLOW))
        assertEquals(SpecUnit.UK_GALLON, metric.copy(volume = VolumeUnit.GALLONS_UK).unitFor(SpecQuantity.VOLUME))
    }

    // ------------------------------------------------------------ edit form

    @Test
    fun `a number is converted for the form in the decimal mark of the language`() {
        assertEquals("10.57", SpecUnits.convertForEdit("40", SpecUnit.LITRE, SpecUnit.US_GALLON, Locale.US))
        assertEquals("10,57", SpecUnits.convertForEdit("40", SpecUnit.LITRE, SpecUnit.US_GALLON, Locale.forLanguageTag("uk")))
        assertEquals("10,57", SpecUnits.convertForEdit("40,0", SpecUnit.LITRE, SpecUnit.US_GALLON, Locale.forLanguageTag("uk")))
        assertEquals("82.22", SpecUnits.convertForEdit("180", SpecUnit.FAHRENHEIT, SpecUnit.CELSIUS, Locale.US))
        assertEquals("1", SpecUnits.convertForEdit("25.4", SpecUnit.MILLIMETRE, SpecUnit.INCH, Locale.US))
        assertEquals("≈40", SpecUnits.convertForEdit("≈40", SpecUnit.LITRE, SpecUnit.LITRE, Locale.US))
        assertNull(SpecUnits.convertForEdit("≈40", SpecUnit.LITRE, SpecUnit.US_GALLON, Locale.US))
    }

    @Test
    fun `a save stores the unit shown unless the owner typed one`() {
        assertEquals("40" to SpecUnit.LITRE, SpecUnits.normalizeInput(" 40 ", SpecQuantity.VOLUME, SpecUnit.LITRE))
        assertEquals("10" to SpecUnit.US_GALLON, SpecUnits.normalizeInput("10 gal", SpecQuantity.VOLUME, SpecUnit.LITRE))
        assertEquals("10" to SpecUnit.UK_GALLON, SpecUnits.normalizeInput("10 gal", SpecQuantity.VOLUME, SpecUnit.UK_GALLON))
        assertEquals("" to null, SpecUnits.normalizeInput("  ", SpecQuantity.VOLUME, SpecUnit.LITRE))
        // A text field has no unit, whatever is typed in it.
        assertEquals("15W-40" to null, SpecUnits.normalizeInput("15W-40", null, null))
        // A unit of another quantity is text, not a unit of this field.
        assertEquals("40 kg" to SpecUnit.LITRE, SpecUnits.normalizeInput("40 kg", SpecQuantity.VOLUME, SpecUnit.LITRE))
    }

    @Test
    fun `the screen does not print a unit the text already names`() {
        assertTrue(SpecUnits.mentionsUnit("2 x 50 m", SpecQuantity.LENGTH))
        assertTrue(SpecUnits.mentionsUnit("12 мм", SpecQuantity.SMALL_LENGTH))
        assertFalse(SpecUnits.mentionsUnit("≈40", SpecQuantity.VOLUME))
        assertFalse(SpecUnits.mentionsUnit("2 x 50", SpecQuantity.LENGTH))
    }

    // ------------------------------------------------------------ backfill

    @Test
    fun `backfill - a unit typed after the number wins and is cut from the text`() {
        val (s, u) = SpecUnits.backfill("""{"Capacity (L)":"10 gal"}""", null, metric, identity)!!
        assertEquals("10", spec(s)["Capacity (L)"])
        assertEquals(SpecUnit.US_GALLON, units(u)["Capacity (L)"])
    }

    @Test
    fun `backfill - the unit in the label comes before Settings`() {
        // The owner read "(L)" while typing, whatever Settings said.
        val (_, u) = SpecUnits.backfill(
            """{"Capacity (L)":"40","Alarm threshold (°C)":"90","Litres used":"3"}""", null, imperial, identity)!!
        assertEquals(SpecUnit.LITRE, units(u)["Capacity (L)"])
        assertEquals(SpecUnit.CELSIUS, units(u)["Alarm threshold (°C)"])
        assertEquals(SpecUnit.LITRE, units(u)["Litres used"])
    }

    @Test
    fun `backfill - a field without a unit in its label takes Settings now`() {
        val (_, u) = SpecUnits.backfill("""{"Height":"14","Diameter":"25","Working pressure":"0.25"}""", null, imperial, identity)!!
        assertEquals(SpecUnit.FOOT, units(u)["Height"])
        assertEquals(SpecUnit.INCH, units(u)["Diameter"])
        assertEquals(SpecUnit.PSI, units(u)["Working pressure"])
    }

    @Test
    fun `backfill - leaves text fields, empty values and units already kept`() {
        assertNull(SpecUnits.backfill("""{"Oil grade":"15W-40","Height":""}""", null, metric, identity))
        assertNull(SpecUnits.backfill("""{"Height":"14"}""", """{"Height":"FOOT"}""", metric, identity))
        assertNull(SpecUnits.backfill("not json", null, metric, identity))
        assertNull(SpecUnits.backfill(null, null, metric, identity))
    }

    @Test
    fun `backfill - is idempotent`() {
        val first = SpecUnits.backfill("""{"Capacity (L)":"10 gal","Height":"14"}""", null, metric, identity)!!
        assertNull(SpecUnits.backfill(first.first, first.second, imperial, identity))
    }

    @Test
    fun `backfill - a value saved under a translated label is found through it`() {
        val hintOf: (String) -> String? = { if (it == "Висота") "Height" else null }
        val (_, u) = SpecUnits.backfill("""{"Висота":"14"}""", null, metric, hintOf)!!
        assertEquals(SpecUnit.METRE, units(u)["Висота"])
    }

    // ------------------------------------------------------------ catalog and labels

    private fun file(vararg candidates: String) = candidates.map { File(it) }.first { it.exists() }

    @Test
    fun `every hint with a unit is a hint the catalog has`() {
        val catalog = Json.parseToJsonElement(
            file("src/main/assets/catalog/catalog.json", "app/src/main/assets/catalog/catalog.json").readText()
        ).jsonObject
        val hints = catalog["categories"]!!.jsonArray.flatMap { cat ->
            cat.jsonObject["components"]!!.jsonArray.flatMap { c ->
                c.jsonObject["specHints"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            }
        }.toSet()
        val missing = SpecUnits.QUANTITY_BY_HINT.keys - hints
        assertTrue("not in catalog.json: $missing", missing.isEmpty())
        assertTrue(SpecUnits.LABEL_UNIT.keys.all { it in SpecUnits.QUANTITY_BY_HINT })
    }

    @Test
    fun `labels lose their unit in every language and keep a name`() {
        val slug = { label: String ->
            label.lowercase().replace("%", " pct ").replace("°", " deg ")
                .replace(Regex("[^a-z0-9]+"), "_").trim('_')
        }
        for (dir in listOf("values", "values-uk", "values-de", "values-es")) {
            val xml = file("src/main/res/$dir/strings_catalog.xml", "app/src/main/res/$dir/strings_catalog.xml").readText()
            for (hint in SpecUnits.LABEL_UNIT.keys - "Litres used") {
                val name = "hint_${slug(hint)}"
                val label = Regex("""<string name="$name">([^<]*)</string>""").find(xml)?.groupValues?.get(1)
                    ?: error("$dir: no $name")
                val stripped = SpecUnits.stripLabelUnit(hint, label)
                assertFalse("$dir $name: '$stripped'", stripped.contains('(') || stripped.isBlank())
            }
        }
        assertEquals("Height (m)", SpecUnits.stripLabelUnit("Height", "Height (m)"))
    }
}
