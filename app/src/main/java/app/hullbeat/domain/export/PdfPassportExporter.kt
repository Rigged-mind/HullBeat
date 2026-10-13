package app.hullbeat.domain.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.AttachmentKind
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.Vessel
import app.hullbeat.ui.components.decodeSampledBitmapFromUri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale
import kotlin.text.StringBuilder

/**
 * The vessel profile in words a person reads, resolved by the caller.
 *
 * `hullType`, `engine` and `drive` are stored as catalogue codes, and the
 * passport used to print them raw: an owner running the app in Ukrainian
 * got "monohull_sail" and "none" in the document they hand to a buyer.
 *
 * Resolved outside rather than in, so `preparePassportData` stays a pure
 * function a unit test can run with no Context - which is the same reason
 * the component names arrive as maps.
 */
data class VesselProfileLabels(
    val hullType: String? = null,
    val engineType: String? = null,
    val driveType: String? = null,
    val fuelType: String? = null,
    val length: String? = null,
)

data class PassportVesselInfo(
    val name: String,
    val makeModel: String? = null,
    val year: Int? = null,
    val hin: String? = null,
    val lengthMetres: Double? = null,
    val hullType: String? = null,
    val engineType: String? = null,
    val driveType: String? = null,
    val fuelType: String? = null,
    val lengthLabel: String? = null,
    val photoUri: String? = null,
)

data class PassportComponentInfo(
    val id: Long,
    val name: String,
    val categoryName: String,
    val make: String? = null,
    val model: String? = null,
    val serial: String? = null,
    val yearInstalled: Int? = null,
    val photoUris: List<String> = emptyList(),
)

data class PassportServiceRecordInfo(
    val id: Long,
    val date: String,
    val componentName: String,
    val workTypes: String,
    val performedBy: String? = null,
    val meterHours: Double? = null,
    val cost: Double? = null,
    val description: String = "",
    val notes: String? = null,
    val photoUris: List<String> = emptyList(),
)

data class PassportReportData(
    val vessel: PassportVesselInfo,
    val components: List<PassportComponentInfo>,
    val serviceRecords: List<PassportServiceRecordInfo>,
    val totalCost: Double,
    val generatedDate: String,
)

data class PassportLabels(
    val docTitle: String = "Vessel Passport & Service Book",
    val sectionVessel: String = "Vessel Specifications",
    val sectionEquipment: String = "Equipment Registry",
    val sectionEquipmentPhotos: String = "Equipment",
    val sectionService: String = "Service History",
    val summaryTitle: String = "Summary",
    val fieldMakeModel: String = "Make / Model",
    val fieldYear: String = "Year Built",
    val fieldHin: String = "HIN / CIN",
    val fieldLength: String = "Length",
    val fieldHull: String = "Hull Type",
    val fieldEngine: String = "Engine Type",
    val fieldDrive: String = "Drive Type",
    val fieldFuel: String = "Fuel Type",
    val fieldComponentsCount: String = "Registered components: %1\$d",
    val fieldRecordsCount: String = "Service records: %1\$d",
    val fieldTotalCost: String = "Total expenses: %1\$s",
    val colComponent: String = "Component / Category",
    val colMakeModel: String = "Make & Model",
    val colSerial: String = "Serial No.",
    val colInstalled: String = "Installed",
    val colDate: String = "Date",
    val colWorkType: String = "Work Type",
    val colPerformedBy: String = "Performed By",
    val colHours: String = "Hours",
    val colCost: String = "Cost",
    val colNotes: String = "Notes",
    val morePhotos: String = "+%1\$d more",
    val footerPage: String = "Page %1\$d",
    val footerBrand: String = "HullBeat Ship's Log",
    /**
     * Who vouches for the contents: the owner, not HullBeat. A buyer or a
     * surveyor reads anything that looks like a certificate with suspicion,
     * so the document says plainly what it is - on the first page and in
     * the footer of every page, since pages get printed and handed on alone.
     */
    val compiledBy: String = "Compiled by owner via HullBeat",
    val currency: String = "",
    /** Unit after engine hours, from R.string.meter_hours_unit ("год", "Std.", "h"). */
    val hoursUnit: String = "h",
    /**
     * Language the document is written in. Numbers follow it - "12,5" in a
     * Ukrainian, German or Spanish passport, "12.5" in an English one - so
     * the caller passes the locale of the strings it resolved, not the
     * phone's. Numbers in a document that disagree with its words read as a
     * careless translation.
     */
    val locale: Locale = Locale.getDefault(),
)

/**
 * An amount as people in [locale] write it, thousands split by a plain space:
 * "1 234,56" (uk, de, es), "1 234.56" (en); whole sums without decimals.
 *
 * The first version formatted with the locale and then replaced every ','
 * with a space, meant for the grouping comma of English. In every locale
 * whose DECIMAL mark is a comma that ate the decimal: 1234.56 came out as
 * "1 234 56" in Ukrainian and "1.234 56" in German and Spanish.
 *
 * A plain space, not the locale's own grouping character: Ukrainian's is a
 * no-break space, and this text is drawn glyph by glyph onto a PDF canvas.
 */
internal fun formatAmount(value: Double, locale: Locale): String {
    val symbols = DecimalFormatSymbols.getInstance(locale).apply { groupingSeparator = ' ' }
    val pattern = if (value % 1.0 == 0.0) "#,##0" else "#,##0.00"
    return DecimalFormat(pattern, symbols).format(value)
}

/** Engine hours: "1234" or "1234,5" / "1234.5" by [locale], no grouping. */
internal fun formatHours(value: Double, locale: Locale): String {
    val pattern = if (value % 1.0 == 0.0) "0" else "0.0"
    return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(value)
}

object PdfPassportExporter {

    private const val TAG = "PdfPassport"

    private const val COLOR_BRAND_TEAL = -0xebbfca // 0xFF14403A
    private const val COLOR_BRAND_LIGHT = -0x190f13 // 0xFFE6F0ED
    private const val COLOR_TEXT_DARK = -0xe0dbdf // 0xFF1F2421
    private const val COLOR_TEXT_MUTED = -0xa5999e // 0xFF5A6662
    private const val COLOR_BORDER_LIGHT = -0x2f232a // 0xFFD0DCD6
    private const val COLOR_WHITE = -0x1 // 0xFFFFFFFF
    private const val COLOR_CARD_BG = -0x80507 // 0xFFF7FAF9

    // Four across the content width to the pixel: 4 x 124 + 3 x 9 = 523.
    private const val THUMB_W = 124f
    private const val THUMB_H = 93f
    private const val THUMB_GAP = 9f
    private const val PHOTOS_PER_ROW = 4
    private const val MAX_PHOTOS_PER_COMPONENT = 8

    // Line pitch. Raised a little from 16 / 15 / 13 after the first passport
    // was printed: at 8.5 pt the rows were legible but crowded, and this is
    // a document people read on paper and hand to someone else.
    //
    // Each of these is used BOTH to reserve the space and to advance down
    // the page. They were literals in two places before, which is the shape
    // that eventually draws a card shorter than the text inside it.
    // Line pitch. Raised for improved readability and comfortable line spacing
    // across title, specification grids, equipment table, and service history cards.
    // Each of these is used BOTH to reserve space and to advance down the page.
    private const val ROW_PITCH = 24f        // a row of the equipment table (was 22f)
    private const val SPEC_PITCH = 23f       // the vessel specification grid (was 20f)
    private const val RECORD_LINE = 20f      // lines inside a service card (was 17f)
    private const val WRAP_LINE = 15f        // wrapped description and notes (was 13f)

    // Air inside service cards
    private const val CARD_TOP = 17f         // first baseline inside a card (was 15f)
    private const val CARD_BOTTOM = 8f       // air under the last line (was 6f)

    /**
     * The kinds that are actually an image.
     *
     * Every attachment used to be treated as a photograph. It worked only
     * because all three writers happen to store PHOTO today - and the enum
     * already carries MANUAL_PDF, VIDEO and AUDIO_NOTE. An audio note would
     * have reserved its strip of the page and drawn nothing into it.
     */
    private val IMAGE_KINDS = setOf(
        AttachmentKind.PHOTO,
        AttachmentKind.RECEIPT,
        AttachmentKind.DIAGRAM,
        AttachmentKind.METER_PHOTO,
    )

    /**
     * Every paint in the document. Android hints text for a screen: each
     * glyph's advance is rounded to a whole unit. On this canvas a unit is
     * a typographic point, and at 8-10 pt that rounding is a quarter of a
     * letter - spaces shrank to almost nothing and words ran together on
     * paper. Linear, unhinted metrics keep the font's own spacing.
     */
    private fun pdfPaint() = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG
    ).apply { hinting = Paint.HINTING_OFF }

    private fun imageUris(attachments: List<Attachment>?): List<String> =
        attachments.orEmpty().filter { it.kind in IMAGE_KINDS }.map { it.fileUri }

    fun preparePassportData(
        vessel: Vessel,
        components: List<Component>,
        componentCategoryNames: Map<Long, String>,
        componentDisplayNames: Map<Long, String>,
        records: List<ServiceRecord>,
        meters: Map<Long, Double>,
        vesselProfile: VesselProfileLabels,
        /** Raw `workTypes` column value -> the words for it. */
        workTypeNames: Map<String, String>,
        recordAttachments: Map<Long, List<Attachment>>,
        componentAttachments: Map<Long, List<Attachment>>,
        generatedDate: String = LocalDate.now().toString(),
    ): PassportReportData {
        val makeModelStr = listOfNotNull(vessel.make, vessel.model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { null }

        val vesselInfo = PassportVesselInfo(
            name = vessel.name,
            makeModel = makeModelStr,
            year = vessel.year,
            hin = vessel.hin?.ifBlank { null },
            lengthMetres = vessel.lengthMetres,
            hullType = vesselProfile.hullType,
            engineType = vesselProfile.engineType,
            driveType = vesselProfile.driveType,
            fuelType = vesselProfile.fuelType,
            lengthLabel = vesselProfile.length,
            photoUri = vessel.photoUri,
        )

        val componentInfos = components.map { c ->
            PassportComponentInfo(
                id = c.id,
                name = componentDisplayNames[c.id] ?: c.name,
                categoryName = componentCategoryNames[c.id] ?: c.categoryCode,
                make = c.make?.ifBlank { null },
                model = c.model?.ifBlank { null },
                serial = c.serial?.ifBlank { null },
                yearInstalled = c.yearInstalled,
                photoUris = imageUris(componentAttachments[c.id]),
            )
        }.sortedWith(compareBy({ it.categoryName }, { it.name }))

        var sumCost = 0.0
        val recordInfos = records.map { r ->
            val costVal = r.cost
            if (costVal != null) {
                sumCost += costVal
            }
            val compName = componentDisplayNames[r.componentId] ?: ""
            val reading = r.meterReadingId?.let { meters[it] }
            val attUris = imageUris(recordAttachments[r.id])
            PassportServiceRecordInfo(
                id = r.id,
                date = r.date.toString(),
                componentName = compName,
                // Falls back to the raw value: a work type nobody has
                // translated yet should still be legible, not blank.
                workTypes = workTypeNames[r.workTypes] ?: r.workTypes,
                performedBy = r.performedBy?.ifBlank { null },
                meterHours = reading,
                cost = costVal,
                description = r.description,
                notes = r.notes?.ifBlank { null },
                photoUris = attUris,
            )
        }.sortedByDescending { it.date }

        return PassportReportData(
            vessel = vesselInfo,
            components = componentInfos,
            serviceRecords = recordInfos,
            totalCost = sumCost,
            generatedDate = generatedDate,
        )
    }

    /**
     * Render the passport. Returns null on success, or the reason it failed.
     *
     * ⚠️ Two things here were wrong, and together they produced a 0-byte file
     * with no explanation.
     *
     * First, `catch (_: Exception) { return false }` destroyed the only
     * evidence there was. The owner saw a file that would not open and the
     * app knew exactly why and said nothing.
     *
     * Second, it drew straight into the caller's stream. Opening a SAF
     * document TRUNCATES it, so the moment anything threw, the file the
     * owner had chosen was already empty - and if they were overwriting an
     * earlier passport, that one was gone too. The pages are built in memory
     * now and the stream is touched only when there are bytes to put in it.
     */
    fun generatePdf(
        context: Context,
        data: PassportReportData,
        outputStream: OutputStream,
        labels: PassportLabels = PassportLabels(),
    ): String? {
        val document = PdfDocument()
        val buffer = ByteArrayOutputStream()
        try {
            val pdf = PdfContext(document, labels, data.vessel.name, data.generatedDate)
            pdf.newPage()

            // 1. Title Banner
            drawTitleBanner(pdf, data.vessel, labels)

            // 2. Specifications & Vessel Photo
            drawVesselSpecs(context, pdf, data.vessel, labels)

            // 3. Summary Statistics Card
            drawSummaryCard(pdf, data, labels)

            // 4. Equipment Registry Section
            if (data.components.isNotEmpty()) {
                drawEquipmentSection(pdf, data.components, labels)
            }

            // 5. Equipment Photographs
            drawComponentGallery(context, pdf, data.components, labels)

            // 6. Service History Section
            if (data.serviceRecords.isNotEmpty()) {
                drawServiceHistorySection(context, pdf, data.serviceRecords, labels)
            }

            pdf.finish()
            document.writeTo(buffer)
        } catch (e: Exception) {
            // Logged in full, because a stack trace in logcat is the only
            // thing that turns "it did not work" into a fix.
            Log.e(TAG, "Passport PDF failed while rendering", e)
            return e.javaClass.simpleName + ": " + (e.message ?: "")
        } finally {
            document.close()
        }

        return try {
            outputStream.write(buffer.toByteArray())
            outputStream.flush()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Passport PDF rendered but could not be written", e)
            e.javaClass.simpleName + ": " + (e.message ?: "")
        }
    }

    private class PdfContext(
        val document: PdfDocument,
        val labels: PassportLabels,
        val vesselName: String,
        val generatedDate: String,
    ) {
        var pageNumber = 0
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y: Float = 0f

        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 36f
        val rightMargin = 559f
        val contentWidth = 523f
        val topMargin = 36f
        val bottomMargin = 800f

        fun newPage() {
            currentPage?.let { document.finishPage(it) }
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            currentPage = page
            canvas = page.canvas
            drawPageDecorations()
            y = topMargin + 26f
        }

        private fun drawPageDecorations() {
            // Called from newPage, one line after startPage. It draws through
            // the same accessor as everything else: no function in this file
            // keeps a canvas in a local, so there is no exception for the
            // checker to carry and none for a reader to remember.
            val paint = pdfPaint()

            paint.color = COLOR_BRAND_TEAL
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("HULLBEAT", leftMargin, topMargin + 10f, paint)

            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            val vesselW = paint.measureText(vesselName)
            c.drawText(vesselName, rightMargin - vesselW, topMargin + 10f, paint)

            paint.color = COLOR_BORDER_LIGHT
            paint.strokeWidth = 0.5f
            c.drawLine(leftMargin, topMargin + 15f, rightMargin, topMargin + 15f, paint)

            c.drawLine(leftMargin, bottomMargin + 5f, rightMargin, bottomMargin + 5f, paint)

            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8f
            val footerRight = String.format(labels.footerPage, pageNumber)
            val rightW = paint.measureText(footerRight)

            // The owner's line where it fits beside the page number; the
            // shorter brand line where a long translation would run into it.
            val footerLeft = "${labels.compiledBy} • $generatedDate"
                .takeIf { paint.measureText(it) <= contentWidth - rightW - 12f }
                ?: "${labels.footerBrand} • $generatedDate"
            c.drawText(footerLeft, leftMargin, bottomMargin + 18f, paint)

            c.drawText(footerRight, rightMargin - rightW, bottomMargin + 18f, paint)
        }

        /**
         * The canvas of the page being drawn RIGHT NOW.
         *
         * Every section used to open with `val c = pdf.canvas ?: return` and
         * then draw through that `c` for the rest of its body - across
         * `ensureSpace`, which calls `newPage`, which calls
         * `document.finishPage`. finishPage releases the page's native
         * canvas, so the next drawText on the captured reference read a null
         * pointer: SIGSEGV, fault addr 0x0, inside libhwui. A native crash is
         * not an exception - no try/catch in Kotlin can see it, which is why
         * the export "just closed the app".
         *
         * The equipment table is where it always landed: a vessel seeded from
         * the full catalogue has far more rows than one page holds.
         *
         * Read it fresh at every use. Never store it in a local.
         */
        val c: Canvas
            get() = canvas ?: error("drawing with no page started")

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > bottomMargin) {
                newPage()
            }
        }

        fun finish() {
            currentPage?.let { document.finishPage(it) }
            currentPage = null
            canvas = null
        }
    }

    private fun drawTitleBanner(pdf: PdfContext, vessel: PassportVesselInfo, labels: PassportLabels) {
        val paint = pdfPaint()

        val bannerHeight = 78f
        val rect = RectF(pdf.leftMargin, pdf.y, pdf.rightMargin, pdf.y + bannerHeight)

        paint.color = COLOR_BRAND_LIGHT
        pdf.c.drawRoundRect(rect, 6f, 6f, paint)

        paint.color = COLOR_BRAND_TEAL
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pdf.c.drawText(labels.docTitle.uppercase(), pdf.leftMargin + 14f, pdf.y + 20f, paint)

        paint.color = COLOR_TEXT_DARK
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pdf.c.drawText(vessel.name, pdf.leftMargin + 14f, pdf.y + 45f, paint)

        if (vessel.makeModel != null || vessel.year != null) {
            val sub = listOfNotNull(vessel.makeModel, vessel.year?.toString()).joinToString(" • ")
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            val subW = paint.measureText(sub)
            pdf.c.drawText(sub, pdf.rightMargin - subW - 14f, pdf.y + 44f, paint)
        }

        // A line of its own under the name with comfortable vertical separation
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        pdf.c.drawText("${labels.compiledBy} • ${pdf.generatedDate}", pdf.leftMargin + 14f, pdf.y + 65f, paint)

        pdf.y += bannerHeight + 16f
    }

    private fun drawVesselSpecs(context: Context, pdf: PdfContext, vessel: PassportVesselInfo, labels: PassportLabels) {
        val paint = pdfPaint()

        pdf.ensureSpace(130f)

        // Section header
        paint.color = COLOR_BRAND_TEAL
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pdf.c.drawText(labels.sectionVessel, pdf.leftMargin, pdf.y, paint)
        pdf.y += 16f

        val photoBitmap = vessel.photoUri?.let { uriStr ->
            runCatching {
                decodeSampledBitmapFromUri(context, Uri.parse(uriStr), 240)
            }.getOrNull()
        }

        val photoWidth = if (photoBitmap != null) 140f else 0f
        val specsWidth = if (photoBitmap != null) pdf.contentWidth - photoWidth - 14f else pdf.contentWidth

        val specs = listOf(
            labels.fieldHin to (vessel.hin ?: "—"),
            labels.fieldMakeModel to (vessel.makeModel ?: "—"),
            labels.fieldYear to (vessel.year?.toString() ?: "—"),
            labels.fieldLength to (vessel.lengthLabel ?: "—"),
            labels.fieldHull to (vessel.hullType ?: "—"),
            labels.fieldEngine to (vessel.engineType ?: "—"),
            labels.fieldDrive to (vessel.driveType ?: "—"),
            labels.fieldFuel to (vessel.fuelType ?: "—"),
        )

        val startY = pdf.y
        val colW = specsWidth / 2f
        specs.forEachIndexed { idx, (key, value) ->
            val col = idx % 2
            val row = idx / 2
            val cellX = pdf.leftMargin + (col * colW)
            val cellY = startY + (row * SPEC_PITCH)

            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            pdf.c.drawText("$key:", cellX, cellY, paint)

            paint.color = COLOR_TEXT_DARK
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val keyWidth = paint.measureText("$key: ")
            pdf.c.drawText(value, cellX + keyWidth, cellY, paint)
        }

        val specsHeight = (specs.size / 2) * SPEC_PITCH

        // Draw photo if available
        if (photoBitmap != null) {
            val photoX = pdf.rightMargin - photoWidth
            val photoHeight = (photoBitmap.height.toFloat() / photoBitmap.width.toFloat()) * photoWidth
            val finalHeight = minOf(photoHeight, 90f)

            val dstRect = RectF(photoX, startY - 4f, photoX + photoWidth, startY - 4f + finalHeight)
            val srcRect = Rect(0, 0, photoBitmap.width, photoBitmap.height)
            pdf.c.drawBitmap(photoBitmap, srcRect, dstRect, paint)

            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER_LIGHT
            paint.strokeWidth = 0.5f
            pdf.c.drawRect(dstRect, paint)
            paint.style = Paint.Style.FILL

            pdf.y = maxOf(startY + specsHeight, startY - 4f + finalHeight) + 16f
        } else {
            pdf.y = startY + specsHeight + 16f
        }
    }

    private fun drawSummaryCard(pdf: PdfContext, data: PassportReportData, labels: PassportLabels) {
        val paint = pdfPaint()

        pdf.ensureSpace(44f)

        val cardRect = RectF(pdf.leftMargin, pdf.y, pdf.rightMargin, pdf.y + 36f)
        paint.color = COLOR_CARD_BG
        pdf.c.drawRoundRect(cardRect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER_LIGHT
        paint.strokeWidth = 0.5f
        pdf.c.drawRoundRect(cardRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        val colW = pdf.contentWidth / 3f
        val yText = pdf.y + 22.5f

        // Metric 1: Components
        paint.color = COLOR_TEXT_DARK
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val m1 = String.format(labels.fieldComponentsCount, data.components.size)
        pdf.c.drawText(m1, pdf.leftMargin + 12f, yText, paint)

        // Metric 2: Records
        val m2 = String.format(labels.fieldRecordsCount, data.serviceRecords.size)
        pdf.c.drawText(m2, pdf.leftMargin + colW + 12f, yText, paint)

        // Metric 3: Total cost
        val costFormatted = formatAmount(data.totalCost, labels.locale)
        val costWithCurrency = if (labels.currency.isNotBlank()) "$costFormatted ${labels.currency}" else costFormatted
        val m3 = String.format(labels.fieldTotalCost, costWithCurrency)
        pdf.c.drawText(m3, pdf.leftMargin + (colW * 2) + 12f, yText, paint)

        pdf.y += 48f
    }

    private fun drawEquipmentSection(pdf: PdfContext, components: List<PassportComponentInfo>, labels: PassportLabels) {
        val paint = pdfPaint()

        pdf.ensureSpace(55f)

        paint.color = COLOR_BRAND_TEAL
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pdf.c.drawText(labels.sectionEquipment, pdf.leftMargin, pdf.y, paint)
        pdf.y += 15f

        val col1X = pdf.leftMargin + 6f
        val col2X = pdf.leftMargin + 180f
        val col3X = pdf.leftMargin + 360f
        val col4X = pdf.leftMargin + 460f

        fun drawTableHeader() {
            val headerRect = RectF(pdf.leftMargin, pdf.y, pdf.rightMargin, pdf.y + 18f)
            paint.color = COLOR_BRAND_LIGHT
            pdf.c.drawRect(headerRect, paint)

            paint.color = COLOR_BRAND_TEAL
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerY = pdf.y + 12.5f

            pdf.c.drawText(labels.colComponent, col1X, headerY, paint)
            pdf.c.drawText(labels.colMakeModel, col2X, headerY, paint)
            pdf.c.drawText(labels.colSerial, col3X, headerY, paint)
            pdf.c.drawText(labels.colInstalled, col4X, headerY, paint)

            pdf.y += 21f
        }

        // Initial Table Header
        drawTableHeader()

        val boldFace = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val cellGap = 8f
        components.forEachIndexed { index, comp ->
            paint.textSize = 8.5f
            paint.typeface = boldFace
            val nameLines = wrapLines(comp.name, col2X - col1X - cellGap, paint)

            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            val makeModel = listOfNotNull(comp.make, comp.model).joinToString(" ").ifBlank { "—" }
            val makeLines = wrapLines(makeModel, col3X - col2X - cellGap, paint)
            val serialLines = wrapLines(comp.serial ?: "—", col4X - col3X - cellGap, paint)

            val lineCount = maxOf(nameLines.size, makeLines.size, serialLines.size)
            val rowHeight = ROW_PITCH + (lineCount - 1) * WRAP_LINE

            if (pdf.y + rowHeight > pdf.bottomMargin) {
                pdf.newPage()
                drawTableHeader()
            }

            // Zebra striping: alternate rows get subtle background
            if (index % 2 == 1) {
                val bgRect = RectF(pdf.leftMargin, pdf.y, pdf.rightMargin, pdf.y + rowHeight)
                paint.color = COLOR_CARD_BG
                pdf.c.drawRect(bgRect, paint)
            }

            val rowY = pdf.y + (ROW_PITCH / 2f) + 3.5f

            paint.color = COLOR_TEXT_DARK
            paint.textSize = 8.5f
            paint.typeface = boldFace
            nameLines.forEachIndexed { i, line -> pdf.c.drawText(line, col1X, rowY + i * WRAP_LINE, paint) }

            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            makeLines.forEachIndexed { i, line -> pdf.c.drawText(line, col2X, rowY + i * WRAP_LINE, paint) }
            serialLines.forEachIndexed { i, line -> pdf.c.drawText(line, col3X, rowY + i * WRAP_LINE, paint) }
            pdf.c.drawText(comp.yearInstalled?.toString() ?: "—", col4X, rowY, paint)

            paint.color = COLOR_BORDER_LIGHT
            paint.strokeWidth = 0.5f
            val ruleY = pdf.y + rowHeight - 1.5f
            pdf.c.drawLine(pdf.leftMargin, ruleY, pdf.rightMargin, ruleY, paint)

            pdf.y += rowHeight
        }

        pdf.y += 18f
    }

    /**
     * The photographs of the equipment, one block per component that has any.
     *
     * Bitmaps are decoded one row at a time rather than all at once: a
     * vessel seeded from the full catalogue has 171 components to hang
     * photographs on. They stay in memory until the page they were drawn on
     * is finished, because that is when PdfDocument replays the page - so
     * the real bound is one page's worth, not one boat's worth.
     *
     * The height of a block follows what actually DECODED, never what was
     * promised. The service-history section still reserves its photo strip
     * from the uri count before trying to read a single one, so an
     * unreadable file leaves a hole there; here an unreadable file simply
     * takes up no room.
     *
     * Thumbnails keep their aspect ratio inside the cell instead of being
     * stretched to fill it - a distorted photograph of a seacock is worse
     * than no photograph.
     */
    private fun drawComponentGallery(
        context: Context,
        pdf: PdfContext,
        components: List<PassportComponentInfo>,
        labels: PassportLabels,
    ) {
        val withPhotos = components.filter { it.photoUris.isNotEmpty() }
        if (withPhotos.isEmpty()) return

        val paint = pdfPaint()
        pdf.ensureSpace(40f + THUMB_H)

        paint.color = COLOR_BRAND_TEAL
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pdf.c.drawText(labels.sectionEquipmentPhotos, pdf.leftMargin, pdf.y, paint)
        pdf.y += 16f

        for (comp in withPhotos) {
            val shown = comp.photoUris.take(MAX_PHOTOS_PER_COMPONENT)
            var headerDrawn = false

            for (row in shown.chunked(PHOTOS_PER_ROW)) {
                val bitmaps = row.mapNotNull { uriStr ->
                    runCatching {
                        decodeSampledBitmapFromUri(context, Uri.parse(uriStr), 320)
                    }.getOrNull()
                }
                if (bitmaps.isEmpty()) continue

                val headerHeight = if (headerDrawn) 0f else 24f
                pdf.ensureSpace(headerHeight + THUMB_H + 8f)

                if (!headerDrawn) {
                    paint.color = COLOR_TEXT_DARK
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    pdf.c.drawText(comp.name, pdf.leftMargin, pdf.y + 8f, paint)

                    val details = listOfNotNull(
                        comp.categoryName.ifBlank { null },
                        listOfNotNull(comp.make, comp.model)
                            .joinToString(" ").ifBlank { null },
                        comp.serial?.let { labels.colSerial + ": " + it },
                    ).joinToString(" · ")
                    if (details.isNotBlank()) {
                        paint.color = COLOR_TEXT_MUTED
                        paint.textSize = 8f
                        paint.typeface = Typeface.DEFAULT
                        pdf.c.drawText(details, pdf.leftMargin, pdf.y + 18f, paint)
                    }
                    headerDrawn = true
                }

                val top = pdf.y + headerHeight
                var x = pdf.leftMargin
                for (bmp in bitmaps) {
                    val scale = minOf(THUMB_W / bmp.width, THUMB_H / bmp.height)
                    val w = bmp.width * scale
                    val h = bmp.height * scale
                    val dst = RectF(
                        x + ((THUMB_W - w) / 2f),
                        top + ((THUMB_H - h) / 2f),
                        x + ((THUMB_W - w) / 2f) + w,
                        top + ((THUMB_H - h) / 2f) + h,
                    )
                    paint.style = Paint.Style.FILL
                    pdf.c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, paint)

                    paint.style = Paint.Style.STROKE
                    paint.color = COLOR_BORDER_LIGHT
                    paint.strokeWidth = 0.5f
                    pdf.c.drawRect(dst, paint)
                    paint.style = Paint.Style.FILL

                    // No recycle() here, however tempting. A PdfDocument page
                    // RECORDS its drawing and replays it at finishPage, so the
                    // bitmap must outlive the draw call. Freeing it early gets
                    // "trying to use a recycled bitmap" at the page break - or
                    // nothing at all, which is worse.
                    x += THUMB_W + THUMB_GAP
                }
                pdf.y = top + THUMB_H + 6f
            }

            // Said out loud rather than dropped in silence: a passport that
            // quietly shows four of a component's twelve photographs is
            // lying about the boat.
            val hidden = comp.photoUris.size - shown.size
            if (headerDrawn && hidden > 0) {
                pdf.ensureSpace(12f)
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 8f
                paint.typeface = Typeface.DEFAULT
                pdf.c.drawText(
                    String.format(labels.morePhotos, hidden),
                    pdf.leftMargin,
                    pdf.y + 6f,
                    paint,
                )
                pdf.y += 12f
            }

            if (headerDrawn) {
                pdf.y += 6f
            }
        }

        pdf.y += 10f
    }

    private fun drawServiceHistorySection(
        context: Context,
        pdf: PdfContext,
        records: List<PassportServiceRecordInfo>,
        labels: PassportLabels,
    ) {
        val paint = pdfPaint()

        pdf.ensureSpace(50f)

        paint.color = COLOR_BRAND_TEAL
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pdf.c.drawText(labels.sectionService, pdf.leftMargin, pdf.y, paint)
        pdf.y += 16f

        val cardContentWidth = pdf.contentWidth - 16f

        for (rec in records) {
            val hasPhotos = rec.photoUris.isNotEmpty()

            // Details line (Hours, Cost, Performed By)
            val detailsList = mutableListOf<String>()
            if (rec.meterHours != null) {
                val formattedHours = "${formatHours(rec.meterHours, labels.locale)} ${labels.hoursUnit}"
                detailsList.add("${labels.colHours}: $formattedHours")
            }
            if (rec.cost != null) {
                val formattedCost = formatAmount(rec.cost, labels.locale)
                val costStr = if (labels.currency.isNotBlank()) "$formattedCost ${labels.currency}" else formattedCost
                detailsList.add("${labels.colCost}: $costStr")
            }
            if (!rec.performedBy.isNullOrBlank()) {
                detailsList.add("${labels.colPerformedBy}: ${rec.performedBy}")
            }
            val detailsStr = detailsList.joinToString(" • ")
            val hasDetails = detailsStr.isNotBlank()

            // Measure description lines
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            val descLines = if (rec.description.isNotBlank()) {
                wrapLines(rec.description, cardContentWidth, paint)
            } else emptyList()

            // Measure note lines
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            val noteLine = if (!rec.notes.isNullOrBlank()) "${labels.colNotes}: ${rec.notes}" else null
            val noteLines = if (noteLine != null) {
                wrapLines(noteLine, cardContentWidth, paint)
            } else emptyList()

            val detailsHeight = if (hasDetails) RECORD_LINE else 0f
            val descHeight = descLines.size * WRAP_LINE
            val noteHeight = noteLines.size * WRAP_LINE
            val photoHeight = if (hasPhotos) 54f else 0f

            val totalCardHeight = CARD_TOP + RECORD_LINE + detailsHeight + descHeight + noteHeight + photoHeight + CARD_BOTTOM

            pdf.ensureSpace(totalCardHeight + 10f)

            val cardRect = RectF(pdf.leftMargin, pdf.y, pdf.rightMargin, pdf.y + totalCardHeight)
            paint.color = COLOR_CARD_BG
            pdf.c.drawRoundRect(cardRect, 4f, 4f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER_LIGHT
            paint.strokeWidth = 0.5f
            pdf.c.drawRoundRect(cardRect, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            var innerY = pdf.y + CARD_TOP

            // Line 1: Date, WorkType Badge, Component
            paint.color = COLOR_TEXT_DARK
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            pdf.c.drawText(rec.date, pdf.leftMargin + 8f, innerY, paint)

            val dateW = paint.measureText(rec.date)
            val badgeX = pdf.leftMargin + 14f + dateW

            // Work type pill badge
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val badgeTextW = paint.measureText(rec.workTypes)
            val badgePadH = 5f
            val badgeRect = RectF(badgeX, innerY - 9.5f, badgeX + badgeTextW + (badgePadH * 2), innerY + 4f)

            paint.color = COLOR_BRAND_LIGHT
            pdf.c.drawRoundRect(badgeRect, 3f, 3f, paint)

            paint.color = COLOR_BRAND_TEAL
            pdf.c.drawText(rec.workTypes, badgeX + badgePadH, innerY, paint)

            // Component name
            paint.color = COLOR_TEXT_DARK
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val compX = badgeRect.right + 8f
            val maxCompW = pdf.rightMargin - 8f - compX
            if (maxCompW > 20f) {
                val compName = if (paint.measureText(rec.componentName) <= maxCompW) {
                    rec.componentName
                } else {
                    var trimmed = rec.componentName
                    while (trimmed.isNotEmpty() && paint.measureText("$trimmed…") > maxCompW) {
                        trimmed = trimmed.dropLast(1)
                    }
                    "$trimmed…"
                }
                pdf.c.drawText(compName, compX, innerY, paint)
            }

            innerY += RECORD_LINE

            // Line 2: Details (Hours, Cost, Performed By)
            if (hasDetails) {
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 8f
                paint.typeface = Typeface.DEFAULT
                pdf.c.drawText(detailsStr, pdf.leftMargin + 8f, innerY, paint)
                innerY += RECORD_LINE
            }

            // Description lines
            if (descLines.isNotEmpty()) {
                paint.color = COLOR_TEXT_DARK
                paint.textSize = 8.5f
                paint.typeface = Typeface.DEFAULT
                for (line in descLines) {
                    pdf.c.drawText(line, pdf.leftMargin + 8f, innerY, paint)
                    innerY += WRAP_LINE
                }
            }

            // Note lines
            if (noteLines.isNotEmpty()) {
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                for (line in noteLines) {
                    pdf.c.drawText(line, pdf.leftMargin + 8f, innerY, paint)
                    innerY += WRAP_LINE
                }
            }

            // Inline photo thumbnails (up to 3)
            if (hasPhotos) {
                var photoX = pdf.leftMargin + 8f
                val thumbWidth = 60f
                val thumbHeight = 45f

                val takePhotos = rec.photoUris.take(3)
                for (uriStr in takePhotos) {
                    val bmp = runCatching {
                        decodeSampledBitmapFromUri(context, Uri.parse(uriStr), 120)
                    }.getOrNull()

                    if (bmp != null) {
                        val dstRect = RectF(photoX, innerY + 3f, photoX + thumbWidth, innerY + 3f + thumbHeight)
                        val srcRect = Rect(0, 0, bmp.width, bmp.height)
                        pdf.c.drawBitmap(bmp, srcRect, dstRect, paint)

                        paint.style = Paint.Style.STROKE
                        paint.color = COLOR_BORDER_LIGHT
                        paint.strokeWidth = 0.5f
                        pdf.c.drawRect(dstRect, paint)
                        paint.style = Paint.Style.FILL

                        photoX += thumbWidth + 8f
                    }
                }
            }

            pdf.y += totalCardHeight + 10f
        }
    }

    /**
     * [text] broken at spaces into lines no wider than [maxWidth]. A single
     * word wider than that keeps a line of its own rather than being cut.
     * Newlines ('\n') start a new line.
     */
    internal fun wrapLines(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            var current = ""
            for (word in paragraph.split(" ").filter { it.isNotEmpty() }) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (current.isEmpty() || measure(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    lines += current
                    current = word
                }
            }
            if (current.isNotEmpty()) lines += current
        }
        return lines.ifEmpty { listOf("") }
    }

    private fun wrapLines(text: String, maxWidth: Float, paint: Paint): List<String> =
        wrapLines(text, maxWidth) { paint.measureText(it) }
}
