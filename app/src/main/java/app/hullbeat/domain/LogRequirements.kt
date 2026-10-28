package app.hullbeat.domain

import app.hullbeat.data.db.ServiceSchedule

/**
 * What a service record for a schedule cannot be saved without. One answer
 * for the three places that ask: the reminder deciding which button to show,
 * the receiver behind its one-tap "Done", and the log sheet's Save.
 */
object LogRequirements {

    /** A printed expiry: the new date, which nothing in the app can derive. */
    fun needsNewExpiry(schedule: ServiceSchedule): Boolean = schedule.expiresOn != null

    /**
     * An interval in hours: the meter reading at the service. Without it the
     * record has a date and no hours, DueCalculator has nothing to count the
     * next interval from, and the item quietly falls back to its calendar
     * interval - an oil change due in 200 hours reminds in a year.
     */
    fun needsMeterReading(schedule: ServiceSchedule): Boolean = schedule.intervalHours != null

    /** Either - so the job cannot be logged in one tap from the shade. */
    fun needsForm(schedule: ServiceSchedule): Boolean =
        needsNewExpiry(schedule) || needsMeterReading(schedule)

    /**
     * Whether the hours field lets Save through. Something that is not a
     * number never does; blank does only when the schedule does not run on
     * hours - a calendar job on an engine shows the field without needing it.
     */
    fun meterInputOk(required: Boolean, text: String): Boolean =
        if (required) DecimalInput.parse(text) != null else DecimalInput.isAcceptable(text)
}
