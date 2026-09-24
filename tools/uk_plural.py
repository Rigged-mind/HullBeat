#!/usr/bin/env python3
"""Ukrainian quantity agreement, for generators that write Ukrainian text.

Why this exists as a module and not as three regexes.

`make_prototype_data.py` used to refresh the prototype's blurb with

    re.subn(r"(321 вузол каталогу, українські назви, )\\d+( синонім)", ...)

which failed three separate ways at once:

1. `321` sat INSIDE the search pattern, so the node count could never change -
   it stayed 321 while the catalog grew to 325.
2. The synonym count WAS updated, but only the digits. `синонім` was inside the
   match and `ів` was outside it, so the suffix froze: the page read
   "993 синонімів" where 993 takes the few-form, "993 синоніми".
3. A second copy of the same number in the screen-reader heading ("пошук по
   321 вузлу") was matched by nothing at all.

The lesson is the one the whole project keeps relearning: a number and the word
that agrees with it are ONE phrase. Substituting the digits and leaving the
ending is the same defect as an Android plural that wraps only the noun - and
the same one that produced "21 позначка створять".

Ukrainian also needs the form chosen per grammatical position, not once per
number. The catalog's node count appears in three:

    nominative  325 вузлів каталогу
    locative    пошук по 325 вузлах
    genitive    у 16 категоріях

so `pick` takes the three forms and the caller decides which set applies.
CLDR categories for uk: one / few / many, plus `other` for fractions, which
these generators never produce.
"""


def form(n):
    """Return the CLDR plural category for an integer count: one/few/many."""
    n = abs(int(n))
    if n % 10 == 1 and n % 100 != 11:
        return "one"
    if n % 10 in (2, 3, 4) and n % 100 not in (12, 13, 14):
        return "few"
    return "many"


def pick(n, one, few, many):
    """The form of a word that agrees with `n`.

    >>> pick(321, "вузол", "вузли", "вузлів")
    'вузол'
    >>> pick(325, "вузол", "вузли", "вузлів")
    'вузлів'
    >>> pick(993, "синонім", "синоніми", "синонімів")
    'синоніми'
    >>> pick(11, "вузол", "вузли", "вузлів")     # 11, not 1
    'вузлів'
    >>> pick(112, "вузол", "вузли", "вузлів")    # 12, not 2
    'вузлів'
    """
    return {"one": one, "few": few, "many": many}[form(n)]


def phrase(n, one, few, many):
    """`n` and its agreeing word together, because they are one phrase.

    >>> phrase(325, "вузол", "вузли", "вузлів")
    '325 вузлів'
    """
    return "%d %s" % (n, pick(n, one, few, many))


if __name__ == "__main__":
    import doctest
    import sys
    fails, _ = doctest.testmod()
    # A few explicit boundaries, since these are the ones that get wrong:
    # teens take `many` even though their last digit says otherwise.
    for n, want in ((1, "one"), (2, "few"), (5, "many"), (11, "many"),
                    (12, "many"), (14, "many"), (21, "one"), (22, "few"),
                    (100, "many"), (101, "one"), (111, "many"), (993, "few")):
        if form(n) != want:
            print("FAIL form(%d) = %s, want %s" % (n, form(n), want))
            fails += 1
    print("OK" if not fails else "FAILED: %d" % fails)
    sys.exit(1 if fails else 0)
