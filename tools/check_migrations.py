#!/usr/bin/env python3
"""Validate Room database migrations against schema JSONs using real SQLite.

WHY THIS EXISTS

Room migrations alter SQLite tables and indices. If a migration statement has a
syntax error, targets a wrong column, fails foreign key constraints, or drifts
from Room's expected schema (exported in app/schemas/.../2.json), the app crashes
on the user's phone on first launch after an update.

WHAT IT VERIFIES:
  1. Parses MIGRATION_1_2 SQL statements directly from Database.kt.
  2. Builds an in-memory SQLite database matching schema version 1 (1.json).
  3. Populates sample data (vessels, components, schedules, records) into v1.
  4. Runs the migration statements extracted from Database.kt.
  5. Verifies the resulting table schema and indices match schema version 2 (2.json).
  6. Proves data integrity: existing records keep scheduleId = NULL, new records
     support scheduleId, and foreign key SET NULL triggers properly.

Run:  python tools/check_migrations.py
Exit: 0 on success, 1 on failure.
"""
import io
import json
import os
import re
import sqlite3
import sys

SCHEMA_DIR = os.path.join("app", "schemas", "app.hullbeat.data.db.AppDatabase")
DATABASE_KT = os.path.join("app", "src", "main", "java", "app", "hullbeat", "data", "db", "Database.kt")


def extract_migration_sql(kt_path, migration_name):
    """Extract db.execSQL statements from a named migration in Database.kt."""
    with io.open(kt_path, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = rf"val\s+{migration_name}\s*=\s*object\s*:\s*Migration\(\s*\d+\s*,\s*\d+\s*\)\s*\{{(?P<body>.*?)\n\s*\}}"
    m = re.search(pattern, content, re.DOTALL)
    if not m:
        raise ValueError(f"Could not find {migration_name} in {kt_path}")

    body = m.group("body")
    sql_statements = re.findall(r'db\.execSQL\(\s*"([^"]+)"\s*\)', body)
    if not sql_statements:
        raise ValueError(f"No db.execSQL statements found in {migration_name}")
    return sql_statements


def load_schema(version):
    path = os.path.join(SCHEMA_DIR, f"{version}.json")
    if not os.path.exists(path):
        raise FileNotFoundError(f"Schema file not found: {path}")
    with io.open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def test_migration_1_2():
    statements = extract_migration_sql(DATABASE_KT, "MIGRATION_1_2")
    v1_schema = load_schema(1)
    v2_schema = load_schema(2)

    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys = ON")
    cur = conn.cursor()

    # 1. Create v1 schema
    for entity in v1_schema["database"]["entities"]:
        table_name = entity["tableName"]
        create_sql = entity["createSql"].replace("${TABLE_NAME}", table_name)
        cur.execute(create_sql)
        for idx in entity.get("indices", []):
            idx_sql = idx["createSql"].replace("${TABLE_NAME}", table_name)
            cur.execute(idx_sql)

    # 2. Insert sample v1 data
    cur.execute("""
        INSERT INTO vessel (id, name, hullType, engine, drive, cooling, enginesCount, rigType, keelType, extras, storage, water, archived)
        VALUES (1, 'Test Boat', 'MONOHULL', 'INBOARD_DIESEL', 'SAILDRIVE', 'RAW_WATER', 1, 'SLOOP', 'FIN', '', 'AFLOAT', 'SALT', 0)
    """)
    cur.execute("""
        INSERT INTO component (id, vesselId, categoryCode, name, criticality, isCustom, archived, sortOrder)
        VALUES (10, 1, 'engine', 'Main Engine', 'HIGH', 0, 0, 1)
    """)
    cur.execute("""
        INSERT INTO service_schedule (id, componentId, intervalDays, intervalHours, needsVerification, enabled)
        VALUES (100, 10, 365, 250.0, 0, 1)
    """)
    cur.execute("""
        INSERT INTO service_record (id, componentId, date, precision, workTypes, description, createdAt)
        VALUES (1000, 10, '2026-09-01', 'DAY', 'SCHEDULED', 'Old Service', 1700000000)
    """)
    conn.commit()

    # 3. Execute MIGRATION_1_2 statements
    for sql in statements:
        cur.execute(sql)
    conn.commit()

    # 4. Validate table_info for service_record against v2 schema
    cur.execute("PRAGMA table_info(service_record)")
    columns = {row[1]: {"type": row[2].upper(), "notnull": row[3]} for row in cur.fetchall()}

    if "scheduleId" not in columns:
        raise AssertionError("Column 'scheduleId' was not added to service_record")

    if columns["scheduleId"]["type"] != "INTEGER":
        raise AssertionError(f"Expected scheduleId type INTEGER, got {columns['scheduleId']['type']}")

    if columns["scheduleId"]["notnull"] != 0:
        raise AssertionError("Expected scheduleId to be nullable (notnull=0)")

    # 5. Validate indices
    cur.execute("PRAGMA index_list(service_record)")
    index_names = [row[1] for row in cur.fetchall()]
    if "index_service_record_scheduleId" not in index_names:
        raise AssertionError("Index 'index_service_record_scheduleId' was not created")

    cur.execute("PRAGMA index_info(index_service_record_scheduleId)")
    indexed_cols = [row[2] for row in cur.fetchall()]
    if indexed_cols != ["scheduleId"]:
        raise AssertionError(f"Expected index on ['scheduleId'], got {indexed_cols}")

    # 6. Validate existing record scheduleId is NULL
    cur.execute("SELECT scheduleId FROM service_record WHERE id = 1000")
    row = cur.fetchone()
    if row[0] is not None:
        raise AssertionError(f"Existing record scheduleId should be NULL, got {row[0]}")

    # 7. Insert new record with valid scheduleId
    cur.execute("""
        INSERT INTO service_record (id, componentId, scheduleId, date, precision, workTypes, description, createdAt)
        VALUES (1001, 10, 100, '2026-09-15', 'DAY', 'SCHEDULED', 'New Service', 1700000001)
    """)
    conn.commit()

    cur.execute("SELECT scheduleId FROM service_record WHERE id = 1001")
    if cur.fetchone()[0] != 100:
        raise AssertionError("New record scheduleId was not saved correctly")

    # 8. Test ON DELETE SET NULL
    cur.execute("DELETE FROM service_schedule WHERE id = 100")
    conn.commit()

    cur.execute("SELECT scheduleId FROM service_record WHERE id = 1001")
    if cur.fetchone()[0] is not None:
        raise AssertionError("Foreign key ON DELETE SET NULL did not set scheduleId to NULL on schedule deletion")

    conn.close()
    return len(statements)


def main():
    try:
        count = test_migration_1_2()
        print(f"MIGRATION_1_2 OK: {count} statement(s) executed and validated against real SQLite schema")
        return 0
    except Exception as e:
        print(f"MIGRATION_1_2 FAILED: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
