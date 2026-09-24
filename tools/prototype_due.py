#!/usr/bin/env python3
"""Mirror of DueCalculator.kt, used to verify the algorithm before it can be
compiled (no Android SDK in this environment).

Keep in step with app/src/main/java/app/hullbeat/domain/DueCalculator.kt.
Run: python tools/prototype_due.py
"""
from dataclasses import dataclass, field
from datetime import date, timedelta
from typing import List, Optional

SOON_WINDOW_DAYS = 30
SOON_FRACTION_OF_INTERVAL = 0.10
SOON_MIN_HOURS = 10.0
MIN_TREND_SPAN_DAYS = 14
TREND_WINDOW_DAYS = 365
EPSILON = 0.001

DAY, MONTH, YEAR, SEASON = "DAY", "MONTH", "YEAR", "SEASON"


@dataclass
class Reading:
    date: date
    value: float
    precision: str = DAY


@dataclass
class Input:
    today: date
    intervalDays: Optional[int] = None
    intervalHours: Optional[float] = None
    intervalMiles: Optional[float] = None
    expiresOn: Optional[date] = None
    lastServiceDate: Optional[date] = None
    lastServiceMeter: Optional[float] = None
    readings: List[Reading] = field(default_factory=list)
    deferredUntil: Optional[date] = None
    soonWindowDays: Optional[int] = None
    soonWindowHours: Optional[float] = None
    # Months the boat is afloat, inclusive. Both None means year-round, which
    # is the previous behaviour exactly - nothing changes for those vessels.
    seasonStartMonth: Optional[int] = None
    seasonEndMonth: Optional[int] = None


@dataclass
class Candidate:
    driver: str
    date: Optional[date]
    daysRemaining: Optional[int]
    unitsRemaining: Optional[float]
    predicted: bool
    # Close in its own unit, decided without reference to days. This is what
    # lets three remaining engine hours be urgent on a boat whose wear trend
    # is not yet usable, and which therefore has no predicted date.
    soonInUnits: bool = False


DRIVER_ORDER = {"CALENDAR": 0, "HOURS": 1, "MILES": 2, "EXPIRY": 3, "NONE": 4}


MAX_PROJECTION_DAYS = 365 * 20


def in_season(i, day):
    a, b = i.seasonStartMonth, i.seasonEndMonth
    if a is None or b is None:
        return True
    m = day.month
    return a <= m <= b if a <= b else (m >= a or m <= b)


def season_days_between(i, start, end):
    """Sailing days in [start, end). Divides a wear rate honestly."""
    if i.seasonStartMonth is None or i.seasonEndMonth is None:
        return (end - start).days
    day, count = start, 0
    while day < end:
        if in_season(i, day):
            count += 1
        day += timedelta(days=1)
    return count


def add_season_days(i, start, n):
    """The date n sailing days from start, skipping every lay-up day.

    Laid up in January with two sailing days of oil left, this lands in early
    April on its own - no branch for "ashore" and no branch for "the resource
    crosses winter". Both fall out of counting the right days."""
    if i.seasonStartMonth is None or i.seasonEndMonth is None:
        return start + timedelta(days=n)
    day, left, guard = start, n, 0
    while left > 0 and guard < MAX_PROJECTION_DAYS:
        day += timedelta(days=1)
        guard += 1
        if in_season(i, day):
            left -= 1
    return day if left == 0 else None


def usable_segment(readings):
    precise = sorted([r for r in readings if r.precision in (DAY, MONTH)],
                     key=lambda r: r.date)
    if not precise:
        return None
    start = 0
    for i in range(1, len(precise)):
        if precise[i].value < precise[i - 1].value - EPSILON:
            start = i
    return precise[start:]


def wear_rate(i, readings, today):
    seg = usable_segment(readings)
    if not seg or len(seg) < 2:
        return None
    window_start = today - timedelta(days=TREND_WINDOW_DAYS)
    recent = [r for r in seg if r.date >= window_start]
    series = recent if len(recent) >= 2 else seg
    span = (series[-1].date - series[0].date).days
    if span < MIN_TREND_SPAN_DAYS:
        return None
    # Sailing days, not calendar days. A series spanning a lay-up would
    # otherwise report half the real rate and predict twice as far out.
    sailing = season_days_between(i, series[0].date, series[-1].date)
    rate = (series[-1].value - series[0].value) / max(sailing, 1)
    if rate < 0:
        return None
    if len(series) >= 4 and span >= 90:
        conf = "HIGH"
    elif len(series) >= 3 and span >= 30:
        conf = "MEDIUM"
    else:
        conf = "LOW"
    return (rate, conf)


def current_meter(readings):
    seg = usable_segment(readings)
    return seg[-1].value if seg else None


def calendar_due(i):
    if i.intervalDays is None or i.lastServiceDate is None:
        return None
    due = i.lastServiceDate + timedelta(days=i.intervalDays)
    return Candidate("CALENDAR", due, (due - i.today).days, None, False)


def expiry_due(i):
    if i.expiresOn is None:
        return None
    return Candidate("EXPIRY", i.expiresOn, (i.expiresOn - i.today).days, None, False)


def usage_due(i, current, trend, driver, interval):
    if interval is None or i.lastServiceMeter is None or current is None:
        return None
    # A meter only counts up, so a last-service reading ABOVE the current one
    # means the two are not on the same scale: the gauge was replaced (old one
    # stopped at 1850, new one starts from zero) or the figure was mistyped.
    # Computing base + interval - current then reports 2060 hours remaining and
    # hides the service for twelve years. Refuse instead: with a calendar
    # interval the item falls back to it, and without one it says UNKNOWN,
    # which at least asks the owner a question.
    if i.lastServiceMeter > current:
        return None
    remaining = (i.lastServiceMeter + interval) - current
    if remaining <= 0:
        return Candidate(driver, i.today, -1, remaining, False)
    # Ashore, nothing is being consumed, so "ten hours left" is not a warning
    # - it is a fact about next spring. The predicted date turns amber on its
    # own as launch approaches, which is when the owner can act on it.
    soon = units_soon(driver, remaining, i) and in_season(i, i.today)
    rate = trend[0] if trend and trend[0] > 0 else None
    if rate is None:
        return Candidate(driver, None, None, remaining, False, soon)
    sailing_days = round(remaining / rate)
    due = add_season_days(i, i.today, sailing_days)
    if due is None:
        return Candidate(driver, None, None, remaining, False, soon)
    return Candidate(driver, due, (due - i.today).days, remaining, True, soon)


def units_soon(driver, remaining, i):
    # Is this within its warning window measured in hours or miles?
    if i.soonWindowHours is not None:
        return remaining <= i.soonWindowHours
    interval = i.intervalHours if driver == "HOURS" else (
        i.intervalMiles if driver == "MILES" else None)
    if interval is None:
        return False
    return remaining <= max(SOON_MIN_HOURS, interval * SOON_FRACTION_OF_INTERVAL)


def is_soon(c, i):
    if c.soonInUnits:
        return True
    if c.daysRemaining is None:
        return False
    return c.daysRemaining <= (i.soonWindowDays if i.soonWindowDays is not None
                               else SOON_WINDOW_DAYS)


def urgency(c, i):
    # Order candidates before days are compared, so a dateless-but-close
    # schedule outranks a dated-but-distant one.
    if c.daysRemaining is not None and c.daysRemaining < 0:
        return 0
    return 1 if is_soon(c, i) else 2


def evaluate(i):
    trend = wear_rate(i, i.readings, i.today)
    current = current_meter(i.readings)

    cands = [c for c in (
        calendar_due(i),
        expiry_due(i),
        usage_due(i, current, trend, "HOURS", i.intervalHours),
        usage_due(i, current, trend, "MILES", i.intervalMiles),
    ) if c is not None]

    if not cands:
        dormant = i.intervalHours is not None and trend is not None and trend[0] == 0.0
        return dict(state="DORMANT" if dormant else "UNKNOWN", dueDate=None,
                    predicted=False, days=None, units=None, driver="NONE",
                    rate=trend[0] if trend else None,
                    confidence=trend[1] if trend else "NONE")

    winner = min(cands, key=lambda c: (
        urgency(c, i),
        c.daysRemaining if c.daysRemaining is not None else 10 ** 9,
        DRIVER_ORDER[c.driver]))

    days = winner.daysRemaining
    idle = trend is not None and trend[0] == 0.0
    # A live deferral outranks everything: the owner already knows and decided.
    # The real date keeps being reported underneath.
    deferred = i.deferredUntil is not None and i.deferredUntil >= i.today
    if deferred:
        state = "DEFERRED"
    elif days is None and idle and winner.driver in ("HOURS", "MILES"):
        # A usage-driven item on a boat that is not moving is not "unknown":
        # it genuinely is not coming due, and saying so beats an empty dash.
        state = "DORMANT"
    elif days is None and winner.soonInUnits:
        # No date, but a number we do know: hours left. Amber without a date.
        state = "DUE_SOON"
    elif days is None:
        state = "UNKNOWN"
    elif days < 0:
        state = "OVERDUE"
    elif is_soon(winner, i):
        state = "DUE_SOON"
    else:
        state = "UPCOMING"

    if winner.predicted:
        confidence = trend[1] if trend else "NONE"
    else:
        # An actual date is certain; no date at all is not "high confidence".
        confidence = "HIGH" if winner.date is not None else "NONE"

    return dict(state=state, dueDate=winner.date, predicted=winner.predicted,
                days=days, units=winner.unitsRemaining, driver=winner.driver,
                rate=trend[0] if trend else None, confidence=confidence)


# ------------------------------------------------------------------ tests

T = date(2026, 9, 5)


def d(n):
    return T + timedelta(days=n)


CASES = []


def case(name, inp, **expect):
    CASES.append((name, inp, expect))


# --- meter replaced: the two readings are not on one scale --------------
# Oil changed at 1850 h on the old tachometer; the gauge was then replaced and
# the new one now reads 40. This used to report "2060 hours remaining, 4414
# days" - the service quietly invisible for twelve years.
RESET = [Reading(d(-400), 1700), Reading(d(-300), 1850),
         Reading(d(-60), 12), Reading(T, 40)]

case("meter replaced: hours alone cannot be computed",
     Input(T, intervalHours=250, lastServiceMeter=1850, readings=RESET),
     state="UNKNOWN", driver="NONE")

case("meter replaced: the calendar still carries the item",
     Input(T, intervalDays=365, lastServiceDate=d(-300),
           intervalHours=250, lastServiceMeter=1850, readings=RESET),
     state="UPCOMING", driver="CALENDAR", days=65)

# --- seasonal haul-out freeze -------------------------------------------
# Rate is measured over sailing days: 150 hours across 92 summer days.
SEASON = dict(seasonStartMonth=4, seasonEndMonth=10)
SUMMER = [Reading(date(2026, 6, 1), 100), Reading(date(2026, 9, 1), 250)]
# Monotonic, so the series is one segment: 145 hours over 141 sailing days.
NEARLY = [Reading(date(2026, 6, 1), 100), Reading(date(2026, 8, 1), 200),
          Reading(date(2026, 10, 20), 245)]

# Mid-January, ashore. The old code extrapolated straight through the winter
# and put the oil change in the middle of the lay-up.
case("ashore: the prediction lands after launch",
     Input(date(2027, 1, 15), intervalHours=500, lastServiceMeter=0,
           readings=SUMMER, **SEASON),
     state="UPCOMING", driver="HOURS", predicted=True)

# Five hours left, but nothing is consuming them until spring, so amber here
# would be a warning the owner cannot act on.
case("ashore: nearly out of hours is not amber",
     Input(date(2027, 1, 15), intervalHours=250, lastServiceMeter=0,
           readings=NEARLY, **SEASON),
     state="UPCOMING", driver="HOURS")

# The same component, six weeks later. Nothing changed except the calendar:
# the predicted date is now inside the warning window, which is exactly when
# the owner can do something about it.
case("just before launch: the same item turns amber",
     Input(date(2027, 3, 20), intervalHours=250, lastServiceMeter=0,
           readings=NEARLY, **SEASON),
     state="DUE_SOON", driver="HOURS")

# 400 hours over a calendar year, of which only 214 days were sailing days.
# Dividing by 365 would report 1.10 h/day and predict nearly twice as far out.
case("wear rate divides by sailing days, not calendar days",
     Input(date(2027, 5, 1), intervalHours=1000, lastServiceMeter=0,
           readings=[Reading(date(2026, 5, 1), 0),
                     Reading(date(2027, 5, 1), 400)], **SEASON),
     state="UPCOMING", driver="HOURS", rate=400 / 214)

# --- reported by review, 2026-09-08 -------------------------------------
# A single hour-driven schedule with three hours left and no usable trend.
# There is no honest date, but "three hours left" is not "unknown".
case("hours nearly gone, no trend yet",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(0), 247)]),
     state="DUE_SOON", driver="HOURS", days=None, units=3)

# The same schedule alongside a calendar one that is nine months away. The
# calendar candidate has a number of days, the hour candidate has none, so a
# comparison on days alone hands the card to the calendar and drops the three
# remaining hours out of the result entirely.
case("hours nearly gone beats a distant calendar",
     Input(T, intervalDays=365, lastServiceDate=d(-85),
           intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(0), 247)]),
     state="DUE_SOON", driver="HOURS", units=3)

case("hours already past threshold",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-100), 160), Reading(d(0), 260)]),
     state="OVERDUE", driver="HOURS")

case("hours due soon, predicted",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-100), 147), Reading(d(0), 247)]),
     state="DUE_SOON", driver="HOURS", predicted=True, days=3)

case("hours far off, predicted date",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-200), 0), Reading(d(0), 100)]),
     state="UPCOMING", driver="HOURS", predicted=True, days=300)

case("calendar beats hours",
     Input(T, intervalDays=365, lastServiceDate=d(-360),
           intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-200), 0), Reading(d(0), 100)]),
     state="DUE_SOON", driver="CALENDAR", days=5, predicted=False)

case("printed expiry wins",
     Input(T, expiresOn=d(10), intervalDays=365, lastServiceDate=d(-100)),
     state="DUE_SOON", driver="EXPIRY", days=10)

case("meter reset splits the series",
     Input(T, intervalHours=100, lastServiceMeter=0,
           readings=[Reading(d(-400), 100), Reading(d(-300), 200), Reading(d(-200), 300),
                     Reading(d(-100), 5), Reading(d(-50), 15), Reading(d(0), 25)]),
     state="UPCOMING", driver="HOURS", rate=0.2, predicted=True)

case("fuzzy dates are ignored for the trend",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-700), 10, YEAR), Reading(d(-500), 20, SEASON),
                     Reading(d(-100), 147), Reading(d(0), 247)]),
     state="DUE_SOON", driver="HOURS", rate=1.0)

case("boat not used: no honest date",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-100), 50), Reading(d(0), 50)]),
     state="DORMANT", driver="HOURS", confidence="NONE")

case("nothing known at all",
     Input(T),
     state="UNKNOWN", driver="NONE")

case("two readings a week apart give no trend",
     Input(T, intervalHours=250, lastServiceMeter=0,
           readings=[Reading(d(-7), 100), Reading(d(0), 110)]),
     state="UNKNOWN", driver="HOURS", units=140, confidence="NONE")

case("deferred stays deferred, not red",
     Input(T, intervalDays=365, lastServiceDate=d(-1500), deferredUntil=d(60)),
     state="DEFERRED", driver="CALENDAR", days=-1135)

case("a lapsed deferral goes back to overdue",
     Input(T, intervalDays=365, lastServiceDate=d(-1500), deferredUntil=d(-1)),
     state="OVERDUE", driver="CALENDAR")

case("narrower hour window: not soon after all",
     Input(T, intervalHours=1000, lastServiceMeter=0, soonWindowHours=15,
           readings=[Reading(d(-200), 840), Reading(d(0), 940)]),
     state="UPCOMING", driver="HOURS", units=60)

case("wider day window: soon after all",
     Input(T, intervalDays=365, lastServiceDate=d(-305), soonWindowDays=90),
     state="DUE_SOON", driver="CALENDAR", days=60)

case("confidence rises with evidence",
     Input(T, intervalHours=500, lastServiceMeter=0,
           readings=[Reading(d(-300), 0), Reading(d(-200), 50),
                     Reading(d(-100), 100), Reading(d(0), 150)]),
     state="UPCOMING", confidence="HIGH")


def run():
    failed = 0
    print(f"{'case':44} {'state':9} {'driver':9} {'days':>6}  {'rate':>6} {'conf':7}")
    print("-" * 92)
    for name, inp, expect in CASES:
        got = evaluate(inp)
        bad = []
        for key, want in expect.items():
            actual = got.get(key)
            if key == "rate" and actual is not None:
                ok = abs(actual - want) < 1e-9
            else:
                ok = actual == want
            if not ok:
                bad.append(f"{key}: want {want!r}, got {actual!r}")
        rate = f"{got['rate']:.3f}" if got["rate"] is not None else "-"
        days = got["days"] if got["days"] is not None else "-"
        mark = "" if not bad else "  <-- FAIL"
        print(f"{name:44} {got['state']:9} {got['driver']:9} {days:>6}  "
              f"{rate:>6} {got['confidence']:7}{mark}")
        for b in bad:
            print(f"    {b}")
            failed += 1
    print()
    print("FAILED" if failed else "all cases pass", f"({failed} mismatches)")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(run())
