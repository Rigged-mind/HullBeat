"""Nothing you tap is smaller than 56 dp.

`docs/ui-spec.md` §1 states the rule and the reason:

    | Руки мокрі, брудні або в рукавичках | Ціль дотику **≥ 56 dp**, великі
      проміжки, жодних дрібних іконок |

Material's own minimum is 48 dp, and that is the number a developer reaches
for without thinking - it is what the framework enforces by default. This
project rejected it on purpose: 48 dp is sized for a dry index finger on a
still surface. So the rule needs a guard, or it decays back to 48 one
component at a time. It already had: a sweep "to 56 dp everywhere" landed
twenty-odd controls at 48 and two at 36.

WHAT COUNTS AS A TOUCH TARGET
  The size given to something you can press: IconButton, Button and its
  variants, FilterChip, a Surface or Box with onClick or .clickable.

WHAT DOES NOT
  The glyph inside it. A 20 dp icon centred in a 56 dp button is correct -
  the rule is about where the finger lands, not how big the drawing is.
  `Icon`, `Image`, `Spacer` and anything with `painter =` are skipped.

LIMITS
  This reads modifier chains as text. A size passed through a variable, or
  set by a wrapping layout, is invisible to it. It can prove a number is
  too small; it cannot prove one is big enough.
"""

import glob
import io
import os
import re
import sys

SRC = os.path.join("app", "src", "main", "java")
MINIMUM = 56

SIZE_RE = re.compile(r"\.(size|heightIn|defaultMinSize)\(\s*(?:min\w*\s*=\s*)?(\d+)\.dp")

# Pressable things. `Surface` and `Box` only count with an onClick/clickable,
# which is checked separately.
INTERACTIVE = (
    "IconButton", "FilledIconButton", "OutlinedIconButton", "FilledTonalIconButton",
    "Button", "FilledTonalButton", "OutlinedButton", "TextButton", "ElevatedButton",
    "FloatingActionButton", "SmallFloatingActionButton", "ExtendedFloatingActionButton",
    "FilterChip", "AssistChip", "InputChip", "SuggestionChip",
    "Switch", "Checkbox", "RadioButton", "Slider",
    "DropdownMenuItem", "ListItem", "NavigationBarItem", "Tab",
)
# Drawings, not targets.
DECORATIVE = ("Icon(", "Image(", "Spacer(", "painter =", "imageVector =",
              "CircularProgressIndicator", "LinearProgressIndicator", "Divider")


def enclosing_call(lines, index):
    """The nearest composable call opening above this line."""
    for i in range(index, max(-1, index - 12), -1):
        line = lines[i]
        for name in DECORATIVE:
            if name in line:
                return "decorative"
        match = re.search(r"\b([A-Z]\w*)\s*\(", line)
        if match:
            return match.group(1)
    return None


def main():
    files = sorted(glob.glob(os.path.join(SRC, "**", "*.kt"), recursive=True))
    if not files:
        print("    NO KOTLIN SOURCES under %s" % SRC)
        return 1

    failures = 0
    checked = 0
    for path in files:
        text = io.open(path, encoding="utf-8").read()
        text = re.sub(r"//[^\n]*", "", text)
        lines = text.split("\n")
        for i, line in enumerate(lines):
            for match in SIZE_RE.finditer(line):
                size = int(match.group(2))
                owner = enclosing_call(lines, i)
                if owner == "decorative" or owner is None:
                    continue
                clickable = owner in INTERACTIVE or "clickable" in "\n".join(
                    lines[max(0, i - 6):i + 6]) or "onClick" in "\n".join(
                    lines[max(0, i - 6):i + 6])
                if not clickable:
                    continue
                checked += 1
                if size < MINIMUM:
                    failures += 1
                    print("    %s:%d: %s on %s is %d dp, below the %d dp "
                          "a gloved hand needs"
                          % (path, i + 1, match.group(1), owner, size, MINIMUM))

    print("touch targets                        %3d sized interactive "
          "element(s), %d below %d dp" % (checked, failures, MINIMUM))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
