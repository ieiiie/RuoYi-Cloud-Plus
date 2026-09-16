-- MySQL 8.0+. The runner replaces ${...} placeholders before execution.
-- Read-only precheck: source databases must exist and target databases must be empty.
SELECT schema_name AS source_schema
FROM information_schema.schemata
WHERE schema_name IN ('${SOURCE_PLATFORM_DB}', '${SOURCE_AGRICULTURE_DB}', '${SOURCE_IOT_DB}')
ORDER BY schema_name;

SELECT '${TARGET_AGRICULTURE_DB}' AS target_schema, COUNT(*) AS existing_tables
FROM information_schema.tables WHERE table_schema = '${TARGET_AGRICULTURE_DB}'
UNION ALL
SELECT '${TARGET_IOT_DB}', COUNT(*)
FROM information_schema.tables WHERE table_schema = '${TARGET_IOT_DB}';

SELECT table_schema, table_name, table_rows
FROM information_schema.tables
WHERE table_schema IN ('${SOURCE_PLATFORM_DB}', '${SOURCE_AGRICULTURE_DB}', '${SOURCE_IOT_DB}')
ORDER BY table_schema, table_name;
