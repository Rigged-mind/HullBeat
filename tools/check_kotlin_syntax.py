"""Lex every Kotlin file far enough to catch what a compiler would refuse.

The second real build stopped here:

    SearchRanking.kt:82  Expecting ')'
    terms.add(""$q"")

The intent was an FTS phrase query - `terms.add("\\"$q\\"")` - and the two
backslashes were eaten when the file was written through a shell heredoc.
Kotlin lexes `""$q""` as an empty string, a bare identifier and another empty
string; parentheses still balance, so counting brackets would never find it.

Twenty-three checks read Kotlin as text - imports, enum constants, @Entity
declarations, hardcoded strings - and not one of them lexed it. `kotlinc` is
not available here and `check_prototype.py` already does the same job for the
JavaScript prototype, so this is the Kotlin half of that idea: not a parser,
but enough of a lexer to know where a string starts and stops.

WHAT IT CATCHES
  * unbalanced {} () [] outside strings, chars and comments
  * a string or block comment still open at end of file
  * two string literals with nothing between them - the bug above

WHAT IT DOES NOT
  Types, resolution, control flow, anything semantic. A file can pass this
  and still be rejected by the compiler; it simply cannot be rejected for
  the three reasons above.
"""

import glob
import io
import os
import sys

SRC = os.path.join("app", "src")  # main AND test: a broken test file fails the build too
IDENT_START = "_$"
PAIRS = {")": "(", "]": "[", "}": "{"}


def scan(text, path):
    """Walk the file once, in code / string / comment states."""
    problems = []
    stack = []          # open brackets: (char, line)
    templates = []      # depth of `${` template braces, to pop back to string
    i, line, n = 0, 1, len(text)
    # `mode` is either None (code) or the delimiter that closes the literal
    mode, mode_line, last_string_end = None, 0, None

    while i < n:
        ch = text[i]
        if ch == "\n":
            line += 1
            i += 1
            continue

        if mode is None:
            # --- comments ---------------------------------------------
            if text.startswith("//", i):
                i = text.find("\n", i)
                if i < 0:
                    break
                continue
            if text.startswith("/*", i):
                end = text.find("*/", i + 2)
                if end < 0:
                    problems.append((line, "block comment is never closed"))
                    break
                line += text.count("\n", i, end)
                i = end + 2
                continue
            # --- character literal -------------------------------------
            if ch == "'":
                j = i + 1
                while j < n and text[j] != "'":
                    j += 2 if text[j] == "\\" else 1
                i = j + 1
                continue
            # --- string literal ----------------------------------------
            if text.startswith('"""', i):
                mode, mode_line, i = '"""', line, i + 3
                continue
            if ch == '"':
                mode, mode_line, i = '"', line, i + 1
                continue
            # --- brackets ----------------------------------------------
            if ch in "([{":
                stack.append((ch, line))
            elif ch in ")]}":
                if templates and ch == "}" and len(stack) == templates[-1]:
                    # closing a `${ … }`, back into the string it lives in
                    templates.pop()
                    mode = stack.pop()[0]
                    i += 1
                    continue
                if not stack:
                    problems.append((line, "stray %r with nothing open" % ch))
                elif stack[-1][0] != PAIRS[ch]:
                    opener, oline = stack[-1]
                    problems.append(
                        (line, "%r closes %r opened on line %d"
                         % (ch, opener, oline)))
                    stack.pop()
                else:
                    stack.pop()
            # --- a backslash in code -------------------------------------
            # Kotlin has no line continuation and no escape outside a
            # literal, so a backslash here is always wreckage. A patch
            # script that wrote "\\n" where it meant a newline left the two
            # characters `\` and `n` sitting on their own line between two
            # test functions, and this scanner walked straight past them.
            elif ch == "\\":
                problems.append(
                    (line, "a backslash outside a string literal - Kotlin has "
                           "no escape here, so one was written into the file "
                           "instead of being applied"))
            # --- two literals with nothing between them ------------------
            elif last_string_end is not None and i == last_string_end:
                if ch.isalnum() or ch in IDENT_START:
                    problems.append(
                        (line, "a string literal is followed straight by %r - "
                               "Kotlin has no adjacent-literal concatenation, "
                               "so an escape was probably lost" % ch))
            i += 1
            if ch not in " \t":
                last_string_end = None
            continue

        # --- inside a string ------------------------------------------
        if mode == '"""':
            if text.startswith('"""', i):
                i += 3
                last_string_end = i
                mode = None
                continue
        else:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                i += 1
                last_string_end = i
                mode = None
                continue
            if ch == "\n":
                problems.append((mode_line, "string literal is never closed"))
                mode = None
                continue
        if text.startswith("${", i):
            # step back into code; remember how deep, to come back out
            stack.append((mode, line))
            templates.append(len(stack))
            mode = None
            i += 2
            continue
        i += 1

    if mode is not None:
        problems.append((mode_line, "string literal is never closed"))
    for opener, oline in stack:
        problems.append((oline, "%r opened here is never closed" % opener))
    return problems


def main():
    files = sorted(glob.glob(os.path.join(SRC, "**", "*.kt"), recursive=True))
    if not files:
        print("    NO KOTLIN SOURCES under %s" % SRC)
        return 1
    failures = 0
    for path in files:
        text = io.open(path, encoding="utf-8").read()
        for line, message in scan(text, path):
            failures += 1
            print("    %s:%d: %s" % (path, line, message))
    print("kotlin sources lexed                 %3d files, "
          "%d syntax problem(s)" % (len(files), failures))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
