#!/usr/bin/env python3
"""Run every Room @Query against a real SQLite engine built from the entities.

WHY THIS EXISTS

Room's KSP processor validates SQL at compile time, and it has never run on
this project: there is no Android SDK here, no Gradle, and the local JDK is 25
against an AGP that wants 17-21. So the four-join `ComponentSpare` projection,
the FTS queries and everything else were written blind.

The reviewer pointed out what should have been obvious: `sqlite3` ships with
Python. Room's SQL is SQLite's SQL. The schema is derivable from the entities.
So the engine that will actually run these statements on the phone can prepare
them here, right now, and say which ones are wrong.

WHAT IT CATCHES: syntax errors, unknown tables, unknown columns, ambiguous
column references, wrong argument counts to SQL functions, bad FTS syntax -
which is most of what a `@Query` gets wrong.

WHAT IT DOES NOT: whether the projection's Kotlin data class matches the
selected columns (KSP's job), nullability, or return types. It proves the
statement is valid SQL against this schema, not that Room will bind it.

Run:  python tools/check_room_sql.py
Exit: 1 if any statement fails to prepare.
"""
import io
import os
import re
import sqlite3
import sys

DB_DIR = os.path.join("app", "src", "main", "java", "app", "hullbeat", "data", "db")
ENTITIES = os.path.join(DB_DIR, "Entities.kt")
DATABASE = os.path.join(DB_DIR, "Database.kt")

# Kotlin type -> SQLite affinity. The enums and LocalDate all reach the
# database as TEXT because Converters turns each of them into a String; that
# mapping is read from the file rather than assumed, below.
BASE_TYPES = {
    "Long": "INTEGER", "Int": "INTEGER", "Short": "INTEGER", "Byte": "INTEGER",
    "Boolean": "INTEGER", "Double": "REAL", "Float": "REAL",
    "String": "TEXT", "ByteArray": "BLOB",
}

# The WHOLE annotation block above the class, in either order: Room does not
# care whether @Fts4 precedes or follows @Entity, and neither may this.
ENTITY_RE = re.compile(
    r"(?P<anns>(?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*)+)"
    r"data class (?P<cls>\w+)\((?P<body>(?:[^()]|\([^()]*\))*)\)",
    re.S)
FIELD_RE = re.compile(
    r"(?P<ann>(?:@\w+(?:\([^()]*\))?\s*)*)"
    r"val\s+(?P<name>\w+)\s*:\s*(?P<type>[\w<>, .]+?)(?P<nullable>\?)?\s*(?:=|,|$)")
QUERY_RE = re.compile(r'@Query\(\s*("""(?P<triple>.*?)"""|"(?P<single>(?:[^"\\]|\\.)*)")',
                      re.S)

# @Query(...) … fun name(args): ReturnType - the return type is what says
# which entity Room has to fill in.
SIGNATURE_RE = re.compile(
    r'@Query\(\s*(?:"""(?P<triple>.*?)"""|"(?P<single>(?:[^"\\]|\\.)*)")\s*\)'
    r'(?:\s*@\w+(?:\([^()]*\))?)*'
    r'\s*(?:suspend\s+)?fun\s+(?P<fn>\w+)\s*\((?P<args>(?:[^()]|\([^()]*\))*)\)'
    r'\s*:\s*(?P<ret>[^\n{=]+)',
    re.S)


def converter_targets(src):
    """Kotlin types that a @TypeConverter turns into something storable."""
    out = {}
    for m in re.finditer(r"@TypeConverter fun \w+\(\w+: (\w+)\??\): (\w+)\??", src):
        frm, to = m.group(1), m.group(2)
        if to in BASE_TYPES:
            out[frm] = BASE_TYPES[to]
    return out


def entity_sql(src, converters):
    """CREATE statements for every @Entity, FTS ones as virtual tables."""
    stmts = []
    for m in ENTITY_RE.finditer(src):
        anns = m.group("anns")
        if "@Entity" not in anns:
            continue
        cls = m.group("cls")
        fts = "@Fts4" in anns or "@Fts3" in anns
        ent = re.search(r"@Entity\((?:[^()]|\([^()]*\))*\)", anns)
        args = ent.group(0) if ent else ""
        tbl = re.search(r'tableName\s*=\s*"([^"]+)"', args)
        table = tbl.group(1) if tbl else cls

        cols = []
        for f in FIELD_RE.finditer(m.group("body")):
            ann, name = f.group("ann"), f.group("name")
            if "@Ignore" in ann:
                continue
            ci = re.search(r'@ColumnInfo\([^)]*name\s*=\s*"([^"]+)"', ann)
            col = ci.group(1) if ci else name
            ktype = f.group("type").strip().split("<")[0]
            sqltype = BASE_TYPES.get(ktype) or converters.get(ktype)
            if sqltype is None:
                # A field Room could not store either: worth saying so rather
                # than quietly inventing a type for it.
                stmts.append(("!unmappable", "%s.%s is %s, and no TypeConverter "
                                             "produces a storable type for it"
                              % (table, col, ktype)))
                continue
            pk = "@PrimaryKey" in ann
            # An FTS table's key IS rowid; declaring it again is an error.
            if fts and col == "rowid":
                continue
            cols.append("%s %s%s%s" % (
                col, sqltype,
                " PRIMARY KEY" if pk and not fts else "",
                "" if f.group("nullable") or pk else " NOT NULL"))
        if fts:
            stmts.append((table, "CREATE VIRTUAL TABLE %s USING fts4(%s)"
                          % (table, ", ".join(c.split(" ")[0] for c in cols))))
        else:
            stmts.append((table, "CREATE TABLE %s (%s)" % (table, ", ".join(cols))))
    return stmts


def entity_fields(src, converters):
    """{EntityClass: (columns, non-null columns)} straight from Entities.kt."""
    out = {}
    for m in ENTITY_RE.finditer(src):
        if "@Entity" not in m.group("anns"):
            continue
        cols, required = [], []
        for f in FIELD_RE.finditer(m.group("body")):
            ann, name = f.group("ann"), f.group("name")
            if "@Ignore" in ann:
                continue
            ci = re.search(r'@ColumnInfo\([^)]*name\s*=\s*"([^"]+)"', ann)
            col = ci.group(1) if ci else name
            cols.append(col)
            if not f.group("nullable"):
                required.append(col)
        out[m.group("cls")] = (cols, required)
    return out


def returned_entity(ret, known):
    """The entity a DAO method promises, through Flow<>, List<> and `?`."""
    for name in re.findall(r"\w+", ret):
        if name in known:
            return name
    return None


def main():
    ent_src = io.open(ENTITIES, encoding="utf-8").read()
    db_src = io.open(DATABASE, encoding="utf-8").read()
    converters = converter_targets(db_src)

    con = sqlite3.connect(":memory:")
    problems = []
    tables = 0
    for table, stmt in entity_sql(ent_src, converters):
        if table == "!unmappable":
            problems.append("schema: " + stmt)
            continue
        try:
            con.execute(stmt)
            tables += 1
        except sqlite3.Error as e:
            problems.append("schema: %s: %s\n        %s" % (table, e, stmt))

    checked = 0
    for m in QUERY_RE.finditer(db_src):
        sql = m.group("triple") or m.group("single") or ""
        sql = sql.replace('\\"', '"').strip()
        if not sql:
            continue
        line = db_src[: m.start()].count("\n") + 1
        checked += 1
        try:
            # Preparing is enough: SQLite resolves every table, column and
            # function name at prepare time, which is exactly the class of
            # mistake a hand-written @Query makes.
            con.execute("EXPLAIN " + sql, {
                n: None for n in re.findall(r":(\w+)", sql)})
        except sqlite3.Error as e:
            one = " ".join(sql.split())
            problems.append("%s:%d: %s\n        %s"
                            % (DATABASE, line, e,
                               one[:150] + ("…" if len(one) > 150 else "")))

    # --- can Room actually fill the entity from what comes back? ---------
    fields = entity_fields(ent_src, converters)
    mapped = 0
    for m in SIGNATURE_RE.finditer(db_src):
        sql = (m.group("triple") or m.group("single") or "")
        sql = sql.replace('\\"', '"').strip()
        cls = returned_entity(m.group("ret"), fields)
        if not sql or not cls or not sql.lstrip().upper().startswith("SELECT"):
            continue
        line = db_src[: m.start()].count("\n") + 1
        # NULL is fine for a WHERE parameter and fatal for LIMIT, which
        # SQLite rejects with "datatype mismatch". Binding it blindly made
        # this very check skip the one query that broke the first real
        # build - the same green-on-nothing it exists to prevent. And a
        # query that cannot be run is now REPORTED, never swallowed: a
        # silent `continue` is how a checker stops checking.
        params = {}
        for n in re.findall(r":(\w+)", sql):
            low = n.lower()
            params[n] = 1 if "limit" in low or "offset" in low else None
        try:
            cur = con.execute(sql, params)
            got = [d[0] for d in cur.description or []]
        except sqlite3.Error as e:
            problems.append(
                "%s:%d: could not run this query to see its columns: %s"
                "\n        %s"
                % (DATABASE, line, e, " ".join(sql.split())[:120]))
            continue
        mapped += 1
        _, required = fields[cls]
        missing = [c for c in required if c not in got]
        if missing:
            problems.append(
                "%s:%d: %s cannot be built from this query - it returns "
                "%s, and %s is non-null in the entity\n        %s"
                % (DATABASE, line, cls, got, ", ".join(missing),
                   " ".join(sql.split())[:120]))

    for p in problems:
        print("  ERROR " + p)
    print("%d tables built, %d @Query statements prepared, "
          "%d result sets mapped onto their entity" % (tables, checked, mapped))
    if problems:
        print("FAILED: %d problem(s)" % len(problems))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
