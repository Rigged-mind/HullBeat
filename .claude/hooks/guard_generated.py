"""PreToolUse hook: refuse hand edits to generated files.

A generated file edited by hand is silently reverted by the next generator
run, or worse, survives until check_all reports drift. The source is named
in each file's header; this sends the edit there instead.
"""
import json
import sys

GENERATED = {
    "app/src/main/java/app/hullbeat/ui/theme/Color.kt":
        "design/palette.json, then python tools/make_palette.py",
    "app/src/main/res/values/colors.xml":
        "design/palette.json, then python tools/make_palette.py",
    "app/src/main/res/values-night/colors.xml":
        "design/palette.json, then python tools/make_palette.py",
    "app/src/main/java/app/hullbeat/domain/search/SearchConfig.kt":
        "design/search-ranking.json, then python tools/make_search_config.py",
    "strings_catalog.xml":
        "catalog/ (catalog.json, i18n/*.json), then python tools/make_strings.py",
}


def main():
    payload = json.load(sys.stdin)
    path = (payload.get("tool_input") or {}).get("file_path") or ""
    path = path.replace("\\", "/")
    for suffix, source in GENERATED.items():
        if path.endswith(suffix):
            json.dump({
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason":
                        "%s is generated - do not edit it by hand. Edit %s."
                        % (suffix, source),
                },
            }, sys.stdout)
            return


main()
