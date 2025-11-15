-- =============================================================================
-- Port Values Table Optimization Script - Partition-Aware Version
-- =============================================================================
-- This script creates indexes on the partitioned port_values table
-- by explicitly creating them on each active partition.
--
-- PostgreSQL Version: 12+
-- Target Table: port_values (partitioned by ts_created)
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'PORT VALUES OPTIMIZATION - Starting...'
\echo '====================================================================='

-- =============================================================================
-- Step 1: Check current state
-- =============================================================================

\echo ''
\echo 'Step 1: Checking existing indexes and partitions...'

SELECT 
    COUNT(*) as partition_count
FROM pg_tables 
WHERE tablename LIKE 'port_values_2%';

\echo ''
\echo 'Existing indexes on parent table:'

SELECT indexname 
FROM pg_indexes 
WHERE tablename = 'port_values' 
  AND indexname LIKE 'idx_port_values%';

-- =============================================================================
-- Step 2: Create indexes on active partitions (2023-2026)
-- =============================================================================
-- We focus on recent partitions where data is actively being written

\echo ''
\echo '====================================================================='
\echo 'Step 2: Creating BTREE index on active partitions (2023-2026)...'
\echo 'This will create: (port_id, ts_created DESC) WHERE port_id IS NOT NULL'
\echo '====================================================================='

-- Partition: 2023
\echo 'Creating index on port_values_2023...'
CREATE INDEX IF NOT EXISTS port_values_2023_port_id_ts_created_idx 
ON public.port_values_2023 (port_id, ts_created DESC)
WHERE port_id IS NOT NULL;

-- Partition: 2024
\echo 'Creating index on port_values_2024...'
CREATE INDEX IF NOT EXISTS port_values_2024_port_id_ts_created_idx 
ON public.port_values_2024 (port_id, ts_created DESC)
WHERE port_id IS NOT NULL;

-- Partition: 2025
\echo 'Creating index on port_values_2025...'
CREATE INDEX IF NOT EXISTS port_values_2025_port_id_ts_created_idx 
ON public.port_values_2025 (port_id, ts_created DESC)
WHERE port_id IS NOT NULL;

-- Partition: 2026
\echo 'Creating index on port_values_2026...'
CREATE INDEX IF NOT EXISTS port_values_2026_port_id_ts_created_idx 
ON public.port_values_2026 (port_id, ts_created DESC)
WHERE port_id IS NOT NULL;

\echo 'BTREE indexes created on active partitions!'

-- =============================================================================
-- Step 3: Create BRIN index on active partitions
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'Step 3: Creating BRIN index on active partitions (2023-2026)...'
\echo 'This will create: BRIN index on ts_created'
\echo '====================================================================='

-- BRIN indexes are tiny and fast for time-series data

\echo 'Creating BRIN index on port_values_2023...'
CREATE INDEX IF NOT EXISTS port_values_2023_ts_created_brin_idx
ON public.port_values_2023 USING BRIN (ts_created)
WITH (pages_per_range = 128);

\echo 'Creating BRIN index on port_values_2024...'
CREATE INDEX IF NOT EXISTS port_values_2024_ts_created_brin_idx
ON public.port_values_2024 USING BRIN (ts_created)
WITH (pages_per_range = 128);

\echo 'Creating BRIN index on port_values_2025...'
CREATE INDEX IF NOT EXISTS port_values_2025_ts_created_brin_idx
ON public.port_values_2025 USING BRIN (ts_created)
WITH (pages_per_range = 128);

\echo 'Creating BRIN index on port_values_2026...'
CREATE INDEX IF NOT EXISTS port_values_2026_ts_created_brin_idx
ON public.port_values_2026 USING BRIN (ts_created)
WITH (pages_per_range = 128);

\echo 'BRIN indexes created on active partitions!'

-- =============================================================================
-- Step 4: Set fillfactor on active partitions
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'Step 4: Setting fillfactor=90 on active partitions...'
\echo '====================================================================='

ALTER TABLE public.port_values_2023 SET (fillfactor = 90);
ALTER TABLE public.port_values_2024 SET (fillfactor = 90);
ALTER TABLE public.port_values_2025 SET (fillfactor = 90);
ALTER TABLE public.port_values_2026 SET (fillfactor = 90);

\echo 'Fillfactor set to 90 on partitions 2023-2026'

-- =============================================================================
-- Step 5: Update statistics
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'Step 5: Updating table statistics...'
\echo '====================================================================='

ANALYZE public.port_values_2023;
ANALYZE public.port_values_2024;
ANALYZE public.port_values_2025;
ANALYZE public.port_values_2026;

\echo 'Statistics updated on all active partitions'

-- =============================================================================
-- Step 6: Verification
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'VERIFICATION: Indexes created on partitions'
\echo '====================================================================='

SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(schemaname||'.'||indexname)) as index_size
FROM pg_indexes 
WHERE tablename IN ('port_values_2023', 'port_values_2024', 'port_values_2025', 'port_values_2026')
  AND (indexname LIKE '%port_id_ts_created%' OR indexname LIKE '%ts_created_brin%')
ORDER BY tablename, indexname;

\echo ''
\echo '====================================================================='
\echo 'VERIFICATION: Row counts per partition'
\echo '====================================================================='

SELECT 
    'port_values_2023' as partition,
    COUNT(*) as row_count,
    pg_size_pretty(pg_relation_size('port_values_2023')) as table_size
FROM port_values_2023
UNION ALL
SELECT 
    'port_values_2024',
    COUNT(*),
    pg_size_pretty(pg_relation_size('port_values_2024'))
FROM port_values_2024
UNION ALL
SELECT 
    'port_values_2025',
    COUNT(*),
    pg_size_pretty(pg_relation_size('port_values_2025'))
FROM port_values_2025
UNION ALL
SELECT 
    'port_values_2026',
    COUNT(*),
    pg_size_pretty(pg_relation_size('port_values_2026'))
FROM port_values_2026;

-- =============================================================================
-- Step 7: Test query performance
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'PERFORMANCE TEST: Sample query using new index'
\echo '====================================================================='

EXPLAIN (ANALYZE, BUFFERS, TIMING OFF) 
SELECT port_id, ts_created, value 
FROM port_values_2024 
WHERE port_id = 1001 
  AND ts_created > NOW() - INTERVAL '7 days'
ORDER BY ts_created DESC
LIMIT 100;

-- =============================================================================
-- Summary
-- =============================================================================

\echo ''
\echo '====================================================================='
\echo 'OPTIMIZATION COMPLETE!'
\echo '====================================================================='
\echo 'Summary of changes:'
\echo '  - BTREE indexes: (port_id, ts_created DESC) on 2023-2026'
\echo '  - BRIN indexes: ts_created on 2023-2026'
\echo '  - Fillfactor: 90 on 2023-2026'
\echo '  - Statistics: Updated on all active partitions'
\echo ''
\echo 'Index names created:'
\echo '  - port_values_YYYY_port_id_ts_created_idx (BTREE)'
\echo '  - port_values_YYYY_ts_created_brin_idx (BRIN)'
\echo ''
\echo 'Next steps:'
\echo '  1. Monitor index usage in pg_stat_user_indexes'
\echo '  2. For older partitions (2019-2022), run similar CREATE INDEX commands'
\echo '     if you need to query historical data frequently'
\echo '  3. Verify dashboard performance improvement'
\echo '====================================================================='
\echo ''

-- =============================================================================
-- Optional: Script to add indexes to older partitions (2019-2022)
-- =============================================================================
-- Uncomment and run this section if you need to optimize older partitions too

/*
\echo 'Creating indexes on older partitions (2019-2022)...'

CREATE INDEX IF NOT EXISTS port_values_2019_port_id_ts_created_idx 
ON public.port_values_2019 (port_id, ts_created DESC) WHERE port_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS port_values_2020_port_id_ts_created_idx 
ON public.port_values_2020 (port_id, ts_created DESC) WHERE port_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS port_values_2021_port_id_ts_created_idx 
ON public.port_values_2021 (port_id, ts_created DESC) WHERE port_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS port_values_2022_port_id_ts_created_idx 
ON public.port_values_2022 (port_id, ts_created DESC) WHERE port_id IS NOT NULL;

\echo 'Indexes created on older partitions!'
*/

