"""PostToolUse hook: after design/palette.json changes, regenerate and check.

Color.kt, colors.xml and the prototype CSS are derived from the palette, and
check_contrast only knows the pairs listed in it. Running both here means a
palette edit can never sit in the tree un-generated or un-checked; a contrast
failure is fed straight back so it is fixed in the same turn.
"""
import json
import os
import subprocess
import sys


def run(root, script):
    return subprocess.run(
        [sys.executable, "-I", os.path.join(root, "tools", script)],
        cwd=root, capture_output=True, text=True, encoding="utf-8",
        errors="replace")


def main():
    payload = json.load(sys.stdin)
    path = ((payload.get("tool_input") or {}).get("file_path") or "").replace("\\", "/")
    if not path.endswith("design/palette.json"):
        return
    root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()

    gen = run(root, "make_palette.py")
    if gen.returncode != 0:
        json.dump({"decision": "block",
                   "reason": "make_palette.py failed:\n" + gen.stdout + gen.stderr},
                  sys.stdout)
        return
    check = run(root, "check_contrast.py")
    if check.returncode != 0:
        failures = check.stdout[check.stdout.find("FAILED"):]
        json.dump({"decision": "block",
                   "reason": "Palette regenerated, but contrast fails:\n" + failures},
                  sys.stdout)
        return
    json.dump({"hookSpecificOutput": {
        "hookEventName": "PostToolUse",
        "additionalContext": "Palette regenerated (Color.kt, colors.xml, prototype CSS); "
                             "check_contrast: all pairs pass.",
    }}, sys.stdout)


main()
