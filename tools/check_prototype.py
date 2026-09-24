#!/usr/bin/env python3
"""Catch the prototype errors that only a browser would otherwise find.

Why this exists.

Adding the journal introduced `const NODE_OF` a second time. A duplicate
top-level `const` is a SyntaxError, so the whole script failed to parse and
every screen in the prototype went dead - and `check_all.py` reported 8/8
green, because not one of its eight steps executes JavaScript. The same class
has bitten twice before:

  * `renderDetail()` was called before `const plural` was initialised, so the
    script threw in the temporal dead zone and the search box silently stopped
    working;
  * `showMirror()` set an element's id before its text, so the next lookup
    returned null and threw inside a delegated click handler - which prints
    nothing at all for the user.

There is no JS engine in this environment, so this is not a parser. It checks
the two things that are cheap to check from the outside and that have actually
broken the page:

1. a top-level binding declared twice (SyntaxError, kills everything);
2. a `plural(...)`/`$(...)` helper used at top level ABOVE its own `const`,
   which is the temporal-dead-zone trap;
3. a CSS custom property that is used but never defined. This one is worse
   than it sounds: an undefined `var()` is invalid at computed-value time, so
   the WHOLE declaration is dropped. `border-top: 1px solid var(--line)`
   renders as no border at all, silently, in every theme - and the palette
   here calls it `--divider`. Eight separators in the journal and the spares
   screen were invisible for exactly this reason, alongside `--tint-ok` and
   `--paper`, two more names invented instead of read.

Run:  python tools/check_prototype.py
Exit: 1 on any finding.
"""
import io
import os
import re
import sys

PROTO = os.path.join("prototype", "index.html")

# Top-level in this file means column zero: the prototype is written flat, one
# declaration per line, with everything nested indented.
DECL = re.compile(r"^(?:const|let|var)\s+([A-Za-z_$][\w$]*)|^function\s+([A-Za-z_$][\w$]*)",
                  re.M)


def script_body(html):
    """The contents of the last <script> block, with its start line number."""
    blocks = list(re.finditer(r"<script>(.*?)</script>", html, re.S))
    if not blocks:
        return None, 0
    last = blocks[-1]
    return last.group(1), html[: last.start(1)].count("\n") + 1


def main():
    html = io.open(PROTO, encoding="utf-8").read()
    body, base = script_body(html)
    if body is None:
        print("no <script> block found in " + PROTO)
        return 1

    problems = []

    # --- 1. a binding declared twice ---------------------------------------
    seen = {}      # name -> (line, kind)
    for m in DECL.finditer(body):
        name = m.group(1) or m.group(2)
        kind = "binding" if m.group(1) else "function"
        line = base + body[: m.start()].count("\n")
        if name in seen:
            problems.append(
                "%s:%d: '%s' is already declared at line %d - a duplicate "
                "top-level binding is a SyntaxError and kills the whole script"
                % (PROTO, line, name, seen[name][0]))
        else:
            seen[name] = (line, kind)

    # --- 2. top-level use above the declaration ----------------------------
    # Only calls at column zero, i.e. statements that run at load. A call
    # inside a function body runs later and is fine.
    for m in re.finditer(r"^([A-Za-z_$][\w$]*)\(", body, re.M):
        name = m.group(1)
        if name not in seen:
            continue
        decl_line, kind = seen[name]
        if kind == "function":
            continue          # hoisted: calling it earlier is legal
        line = base + body[: m.start()].count("\n")
        if line < decl_line:
            problems.append(
                "%s:%d: '%s' is used before its declaration at line %d - "
                "const/let sit in the temporal dead zone until then, and the "
                "throw happens at load with no message on screen"
                % (PROTO, line, name, decl_line))

    # --- 3. a custom property used but never defined -----------------------
    # Definitions live in any rule as `--name:`; uses are `var(--name)`.
    defined = set(re.findall(r"(--[\w-]+)\s*:", html))
    for m in re.finditer(r"var\((--[\w-]+)\s*(?:,([^)]*))?\)", html):
        name = m.group(1)
        if name in defined or m.group(2) is not None:
            continue        # defined, or has an explicit fallback
        line = html[: m.start()].count("\n") + 1
        near = sorted(d for d in defined
                      if d[2:4] == name[2:4] or name[2:] in d or d[2:] in name[2:])
        problems.append(
            "%s:%d: var(%s) is never defined%s - an undefined custom property "
            "makes the whole declaration invalid, so the property silently "
            "does nothing"
            % (PROTO, line, name,
               " (did you mean %s?)" % ", ".join(near[:3]) if near else ""))

    for p in problems:
        print("  ERROR " + p)
    print("%s   %d top-level bindings, %d css tokens checked"
          % (PROTO, len(seen), len(defined)))
    if problems:
        print("FAILED: %d problem(s)" % len(problems))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
