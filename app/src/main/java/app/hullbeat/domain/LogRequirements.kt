package app.hullbeat.domain

import app.hullbeat.data.db.ServiceSchedule

/**
 * What a service record for a schedule cannot be saved without. One answer
 * for the three places that ask: the reminder deciding which button to show,
 * the receiver behind its one-tap "Done", and the log sheet's Save.
 */
object LogRequirements {

    /**
     * A printed expiry: the new date, which nothing in the app can derive.
     *
     * Asked of a schedule that has a date, and of one with no interval at
     * all - the shape the catalog seeds for a policy or a licence before the
     * owner has entered its first date. Testing `expiresOn != null` alone
     * left those with no way to get one: the sheet never asked, so the
     * document stayed "date unknown" and was never reminded about.
     *
     * Only the catalog makes interval-less schedules, and IntervalDialog can
     * neither save an empty interval nor open on one, so here "no interval"
     * means "runs on a date". A future path that makes such a schedule for
     * anything else must be squared with this rule first.
     */
    fun needsNewExpiry(schedule: ServiceSchedule): Boolean =
        schedule.expiresOn != null || hasNoInterval(schedule)

    /** Runs on a date and has none yet: the first one is still to come. */
    fun awaitsFirstExpiry(schedule: ServiceSchedule): Boolean =
        schedule.expiresOn == null && hasNoInterval(schedule)

    private fun hasNoInterval(schedule: ServiceSchedule): Boolean =
        schedule.intervalDays == null && schedule.intervalHours == null && schedule.intervalMiles == null

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
