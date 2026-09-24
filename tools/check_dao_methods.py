"""Every `db.xxxDao().method(...)` must name a method that DAO declares.

WHY THIS EXISTS

`check_kotlin_imports.py` resolves TYPE names. `check_enum_constants.py`
resolves members of enums and of a small table of framework objects. Neither
looks at a method call, so this compiled-in-my-head and failed on a real
build:

    val records = db.serviceRecordDao().forComponent(componentId)

`ServiceRecordDao` had `observeForComponent` returning a Flow, and no
suspending `forComponent` at all. Twenty-four checks were green; the Kotlin
compiler found it in four seconds.

DAO calls are worth singling out from method calls in general: the receiver
type is unambiguous (a getter on AppDatabase whose return type is written
right there), the member set is closed and declared in one file, and they are
the boundary where a typo becomes a query that does not exist.

WHAT IT CHECKS
  * `anything.<getter>().<method>(` where <getter> is an AppDatabase DAO
    getter - the direct form, which is how nearly every call site is written
  * `val dao = anything.<getter>()` followed by `dao.<method>(` - the alias
    form

WHAT IT DOES NOT
  Argument types, arity, nullability, return types. A call can pass this and
  still be rejected by the compiler; it simply cannot be rejected for naming
  a method that is not there.
"""

import glob
import io
import os
import re
import sys

SRC = os.path.join("app", "src")

DAO_RE = re.compile(
    r"@Dao\s*(?:@\w+(?:\([^()]*\))?\s*)*"
    r"(?:public |internal |abstract )*interface\s+(\w+)\s*\{", re.S)
# A getter on the database: `abstract fun serviceRecordDao(): ServiceRecordDao`
GETTER_RE = re.compile(r"abstract\s+fun\s+(\w+)\s*\(\s*\)\s*:\s*(\w+)")
FUN_RE = re.compile(r"\bfun\s+(\w+)\s*\(")
# `…someDao().method(`
CALL_RE = re.compile(r"\.(\w+)\(\s*\)\s*\.\s*(\w+)\s*\(")
# `val dao = …someDao()` with nothing chained after it
ALIAS_RE = re.compile(r"\bval\s+(\w+)\s*=\s*[\w.]*\.(\w+)\(\s*\)\s*(?:$|\n)", re.M)


def interface_body(text, start):
    """Text between the brace that opens an interface and its match."""
    depth, i = 0, start
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i]
        i += 1
    return text[start:]


def main():
    sources = {}
    for path in sorted(glob.glob(os.path.join(SRC, "**", "*.kt"), recursive=True)):
        sources[path] = io.open(path, encoding="utf-8").read()
    if not sources:
        print("    NO KOTLIN SOURCES under %s" % SRC)
        return 1

    # --- what each DAO declares ------------------------------------------
    methods = {}
    for text in sources.values():
        for match in DAO_RE.finditer(text):
            body = interface_body(text, match.end() - 1)
            methods[match.group(1)] = set(FUN_RE.findall(body))

    # --- which getter returns which DAO ----------------------------------
    getters = {}
    for text in sources.values():
        for getter, dao in GETTER_RE.findall(text):
            if dao in methods:
                getters[getter] = dao
    if not getters:
        print("    NO DAO GETTERS FOUND - has AppDatabase moved?")
        return 1

    failures = 0
    checked = 0
    for path, text in sources.items():
        text = re.sub(r"//[^\n]*", "", text)

        aliases = {name: getters[getter]
                   for name, getter in ALIAS_RE.findall(text)
                   if getter in getters}

        for getter, method in CALL_RE.findall(text):
            dao = getters.get(getter)
            if dao is None:
                continue
            checked += 1
            if method not in methods[dao]:
                failures += 1
                near = sorted(m for m in methods[dao]
                              if m.lower().startswith(method.lower()[:4]))
                print("    %s: %s().%s does not exist - %s declares %s"
                      % (path, getter, method, dao,
                         ", ".join(near) if near else
                         ", ".join(sorted(methods[dao])[:6]) + " …"))

        for alias, dao in aliases.items():
            for match in re.finditer(r"\b%s\s*\.\s*(\w+)\s*\(" % re.escape(alias), text):
                method = match.group(1)
                checked += 1
                if method not in methods[dao]:
                    failures += 1
                    print("    %s: %s.%s does not exist - %s declares no such method"
                          % (path, alias, method, dao))

    print("dao calls resolved                   %2d DAOs, %d getters, "
          "%d call(s) checked, %d unknown" % (len(methods), len(getters),
                                              checked, failures))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
