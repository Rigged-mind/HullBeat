#!/usr/bin/env python3
"""Every @Entity must be registered in the @Database entities list.

An entity Room does not know about is not an error anywhere: the class
compiles, the data class is usable, and nothing creates the table. Adding
`ComponentPart` and forgetting this list would have produced a spares screen
that reads a table that does not exist - the same shape as every other defect
in this project, a rule written and not carried to its last point of use:
`water` in the vocabulary but not in `Vessel`, `emergency_tiller` scheduled
but in no checklist, `checklist_not_applicable` translated but wired to no
screen.

Also checks the reverse: a class listed in @Database that no longer exists as
an @Entity, which is a compile error but a confusing one.

Run:  python tools/check_entities.py
Exit: 1 on any mismatch.
"""
import io
import os
import re
import sys

DB = os.path.join("app", "src", "main", "java", "app", "hullbeat", "data", "db")
ENTITIES = os.path.join(DB, "Entities.kt")
DATABASE = os.path.join(DB, "Database.kt")


def declared():
    """Class names carrying an @Entity annotation, in declaration order."""
    src = io.open(ENTITIES, encoding="utf-8").read()
    found = []
    for m in re.finditer(r"@Entity\b", src):
        # the next `data class X` after the annotation is the annotated one
        nxt = re.search(r"data class ([A-Z]\w*)", src[m.end():])
        if nxt:
            found.append(nxt.group(1))
    return found


def registered():
    """Class names inside the @Database(entities = [...]) list."""
    src = io.open(DATABASE, encoding="utf-8").read()
    m = re.search(r"entities\s*=\s*\[(.*?)\]", src, re.S)
    if not m:
        return None
    return re.findall(r"([A-Z]\w*)::class", m.group(1))


def main():
    have = declared()
    listed = registered()
    if listed is None:
        print("could not find the entities list in " + DATABASE)
        return 1

    missing = [c for c in have if c not in listed]
    extra = [c for c in listed if c not in have]

    for c in missing:
        print("  ERROR %s is an @Entity but is not in the @Database entities "
              "list - Room will never create its table" % c)
    for c in extra:
        print("  ERROR %s is listed in @Database but is not an @Entity in "
              "Entities.kt" % c)

    print("%s   %d entities, all registered" % (ENTITIES, len(have))
          if not (missing or extra) else
          "%s   %d entities declared, %d registered" % (ENTITIES, len(have), len(listed)))
    if missing or extra:
        print("FAILED: %d mismatch(es)" % (len(missing) + len(extra)))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
