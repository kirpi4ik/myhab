-- ============================================================================
-- PostgreSQL Database Structure Dump Script
-- ============================================================================
-- This script extracts the complete database structure including:
-- - Table definitions with columns and data types
-- - Primary keys, unique constraints, and indexes
-- - Foreign key relationships
-- - Check constraints
-- - Default values
-- ============================================================================

-- Usage:
--   psql -U postgres -d myhab -f dump-database-structure.sql > schema_output.txt
-- Or:
--   psql -U postgres -d myhab -f dump-database-structure.sql

\echo '============================================================================'
\echo 'DATABASE STRUCTURE DUMP'
\echo '============================================================================'
\echo ''

-- Set output format for better readability
\pset border 2
\pset format wrapped
\pset columns 120

-- ============================================================================
-- 1. DATABASE INFORMATION
-- ============================================================================

\echo '1. DATABASE INFORMATION'
\echo '============================================================================'

SELECT 
    current_database() as database_name,
    current_user as current_user,
    version() as postgresql_version;

\echo ''

-- ============================================================================
-- 2. ALL TABLES IN PUBLIC SCHEMA
-- ============================================================================

\echo '2. ALL TABLES (with row counts)'
\echo '============================================================================'

SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    (SELECT COUNT(*) FROM information_schema.columns 
     WHERE table_schema = schemaname AND table_name = tablename) AS column_count
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

\echo ''

-- ============================================================================
-- 3. DETAILED TABLE STRUCTURES
-- ============================================================================

\echo '3. DETAILED TABLE STRUCTURES'
\echo '============================================================================'

SELECT 
    t.table_name,
    c.column_name,
    c.ordinal_position AS pos,
    c.data_type,
    c.character_maximum_length AS max_length,
    c.numeric_precision,
    c.numeric_scale,
    c.is_nullable,
    c.column_default,
    CASE 
        WHEN pk.column_name IS NOT NULL THEN 'PK'
        ELSE ''
    END AS is_pk,
    CASE 
        WHEN fk.column_name IS NOT NULL THEN 'FK → ' || fk.foreign_table_name
        ELSE ''
    END AS foreign_key
FROM information_schema.tables t
LEFT JOIN information_schema.columns c 
    ON t.table_name = c.table_name AND t.table_schema = c.table_schema
LEFT JOIN (
    -- Primary Keys
    SELECT ku.table_name, ku.column_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage ku
        ON tc.constraint_name = ku.constraint_name
        AND tc.table_schema = ku.table_schema
    WHERE tc.constraint_type = 'PRIMARY KEY'
        AND tc.table_schema = 'public'
) pk ON c.table_name = pk.table_name AND c.column_name = pk.column_name
LEFT JOIN (
    -- Foreign Keys
    SELECT 
        ku.table_name,
        ku.column_name,
        ccu.table_name AS foreign_table_name
    FROM information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS ku
        ON tc.constraint_name = ku.constraint_name
        AND tc.table_schema = ku.table_schema
    JOIN information_schema.constraint_column_usage AS ccu
        ON ccu.constraint_name = tc.constraint_name
        AND ccu.table_schema = tc.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
        AND tc.table_schema = 'public'
) fk ON c.table_name = fk.table_name AND c.column_name = fk.column_name
WHERE t.table_schema = 'public'
    AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name, c.ordinal_position;

\echo ''

-- ============================================================================
-- 4. PRIMARY KEYS
-- ============================================================================

\echo '4. PRIMARY KEYS'
\echo '============================================================================'

SELECT 
    tc.table_name,
    kcu.column_name,
    tc.constraint_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.constraint_type = 'PRIMARY KEY'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.ordinal_position;

\echo ''

-- ============================================================================
-- 5. FOREIGN KEYS
-- ============================================================================

\echo '5. FOREIGN KEYS'
\echo '============================================================================'

SELECT 
    tc.table_name AS from_table,
    kcu.column_name AS from_column,
    ccu.table_name AS to_table,
    ccu.column_name AS to_column,
    tc.constraint_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.ordinal_position;

\echo ''

-- ============================================================================
-- 6. UNIQUE CONSTRAINTS
-- ============================================================================

\echo '6. UNIQUE CONSTRAINTS'
\echo '============================================================================'

SELECT 
    tc.table_name,
    tc.constraint_name,
    STRING_AGG(kcu.column_name, ', ' ORDER BY kcu.ordinal_position) AS columns
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.constraint_type = 'UNIQUE'
    AND tc.table_schema = 'public'
GROUP BY tc.table_name, tc.constraint_name
ORDER BY tc.table_name;

\echo ''

-- ============================================================================
-- 7. INDEXES
-- ============================================================================

\echo '7. INDEXES'
\echo '============================================================================'

SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

\echo ''

-- ============================================================================
-- 8. CHECK CONSTRAINTS
-- ============================================================================

\echo '8. CHECK CONSTRAINTS'
\echo '============================================================================'

SELECT 
    tc.table_name,
    tc.constraint_name,
    cc.check_clause
FROM information_schema.table_constraints tc
JOIN information_schema.check_constraints cc
    ON tc.constraint_name = cc.constraint_name
WHERE tc.constraint_type = 'CHECK'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name;

\echo ''

-- ============================================================================
-- 9. SEQUENCES
-- ============================================================================

\echo '9. SEQUENCES'
\echo '============================================================================'

SELECT 
    sequence_schema,
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name;

\echo ''

-- ============================================================================
-- 10. ENUMS (Custom Types)
-- ============================================================================

\echo '10. ENUM TYPES'
\echo '============================================================================'

SELECT 
    t.typname AS enum_name,
    STRING_AGG(e.enumlabel, ', ' ORDER BY e.enumsortorder) AS values
FROM pg_type t
JOIN pg_enum e ON t.oid = e.enumtypid
JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname = 'public'
GROUP BY t.typname
ORDER BY t.typname;

\echo ''

-- ============================================================================
-- 11. TABLE SIZES
-- ============================================================================

\echo '11. TABLE SIZES (sorted by size)'
\echo '============================================================================'

SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

\echo ''

-- ============================================================================
-- 12. SPECIFIC TABLES FOR METEO STATION VALIDATION
-- ============================================================================

\echo '12. METEO STATION RELATED TABLES'
\echo '============================================================================'

\echo 'Table: device_categories'
\d device_categories

\echo ''
\echo 'Table: device_controllers'
\d device_controllers

\echo ''
\echo 'Table: device_ports'
\d device_ports

\echo ''
\echo 'Table: configurations'
\d configurations

\echo ''

-- ============================================================================
-- 13. GENERATE CREATE TABLE STATEMENTS
-- ============================================================================

\echo '13. GENERATE CREATE TABLE STATEMENTS'
\echo '============================================================================'
\echo 'Note: Use pg_dump for production-quality CREATE statements'
\echo 'Command: pg_dump -U postgres -d myhab --schema-only --no-owner --no-acl'
\echo ''

-- Sample for key tables
\echo 'Sample CREATE TABLE syntax (use \\d tablename for details):'
\echo ''
\echo '-- For device_categories:'
\d+ device_categories
\echo ''
\echo '-- For device_controllers:'
\d+ device_controllers
\echo ''
\echo '-- For device_ports:'
\d+ device_ports
\echo ''
\echo '-- For configurations:'
\d+ configurations

\echo ''

-- ============================================================================
-- 14. COLUMN DETAILS FOR KEY TABLES
-- ============================================================================

\echo '14. DETAILED COLUMN INFO FOR KEY TABLES'
\echo '============================================================================'

-- Device Categories
\echo 'DEVICE_CATEGORIES columns:'
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'device_categories'
ORDER BY ordinal_position;

\echo ''

-- Device Controllers
\echo 'DEVICE_CONTROLLERS columns:'
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'device_controllers'
ORDER BY ordinal_position;

\echo ''

-- Device Ports
\echo 'DEVICE_PORTS columns:'
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'device_ports'
ORDER BY ordinal_position;

\echo ''

-- Configurations
\echo 'CONFIGURATIONS columns:'
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'configurations'
ORDER BY ordinal_position;

\echo ''

-- ============================================================================
-- 15. CONSTRAINTS SUMMARY FOR KEY TABLES
-- ============================================================================

\echo '15. CONSTRAINTS FOR KEY TABLES'
\echo '============================================================================'

SELECT 
    tc.table_name,
    tc.constraint_type,
    tc.constraint_name,
    STRING_AGG(kcu.column_name, ', ' ORDER BY kcu.ordinal_position) AS columns
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.table_schema = 'public'
    AND tc.table_name IN ('device_categories', 'device_controllers', 'device_ports', 'configurations')
GROUP BY tc.table_name, tc.constraint_type, tc.constraint_name
ORDER BY tc.table_name, tc.constraint_type;

\echo ''

-- ============================================================================
-- EXPORT COMPLETE SCHEMA TO FILE
-- ============================================================================

\echo '============================================================================'
\echo 'STRUCTURE DUMP COMPLETE'
\echo '============================================================================'
\echo ''
\echo 'For complete CREATE TABLE statements, run:'
\echo '  pg_dump -U postgres -d myhab --schema-only -f myhab_schema.sql'
\echo ''
\echo 'For specific table:'
\echo '  pg_dump -U postgres -d myhab --schema-only -t table_name'
\echo ''
\echo 'For data + structure:'
\echo '  pg_dump -U postgres -d myhab -t table_name'
\echo ''

