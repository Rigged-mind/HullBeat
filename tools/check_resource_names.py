"""One name, one resource, per configuration folder.

The first real Gradle build stopped on this:

    :app:mergeDebugResources  1 error
    strings.xml  Found item String/action_undo more than one time

`action_undo` and `action_save` were each declared twice in `strings.xml`,
in both locales - added once with the snackbars and again with the form
dialogs, months apart, with identical values. Identical values are exactly
why nobody noticed: the file read correctly either way, and `aapt2` is the
only reader that counts.

Twenty-two checks did not catch it, and the reason is worth writing down:
`check_translations.py` compares the SET of names in `values/` against the
set in `values-uk/`. A set cannot hold a duplicate. Every check that reads
resources into a dict has the same blind spot - the second declaration
silently overwrites the first and the count comes out right.

So this one counts occurrences instead, before anything turns them into a
dict, and it covers every value type rather than strings alone: a duplicate
`<color>` or `<dimen>` fails the same build the same way.
"""

import collections
import glob
import io
import os
import re
import sys

RES = os.path.join("app", "src", "main", "res")

# <string name="x">, <color name="x">, <plurals>, <dimen>, <bool>, <integer>,
# <string-array>, <style>, and <item name="x" type="..."> in any of them.
DECL = re.compile(
    r"<(string|string-array|plurals|color|dimen|bool|integer|integer-array"
    r"|style|declare-styleable|attr)\s+[^>]*name=\"([^\"]+)\"")


def main():
    folders = sorted(d for d in glob.glob(os.path.join(RES, "values*"))
                     if os.path.isdir(d))
    if not folders:
        print("    NO values/ FOLDER FOUND under %s" % RES)
        return 1

    failures = 0
    total = 0
    for folder in folders:
        seen = collections.defaultdict(list)
        for path in sorted(glob.glob(os.path.join(folder, "*.xml"))):
            text = io.open(path, encoding="utf-8").read()
            # A declaration inside a comment is not a declaration - but
            # blank it out newline-for-newline, or every line number printed
            # below points at the wrong place.
            text = re.sub(r"<!--.*?-->",
                          lambda m: "\n" * m.group(0).count("\n"),
                          text, flags=re.S)
            for match in DECL.finditer(text):
                seen[(match.group(1), match.group(2))].append(
                    "%s:%d" % (os.path.basename(path),
                               text.count("\n", 0, match.start()) + 1))
        total += len(seen)
        for (kind, name), places in sorted(seen.items()):
            if len(places) > 1:
                failures += 1
                print("    DECLARED %d TIMES  %s/%s in %s"
                      % (len(places), kind, name, os.path.basename(folder)))
                for place in places:
                    print("        %s" % place)

    print("android resource names               %3d folders, %d names, "
          "%d declared more than once" % (len(folders), total, failures))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
