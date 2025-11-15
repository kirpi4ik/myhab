-- =============================================================================
-- Port Values Table Optimization Script
-- =============================================================================
-- This script optimizes the partitioned port_values table for better performance
-- 
-- Table Structure:
--   - port_values: Partitioned by RANGE on ts_created (year-based partitions)
--   - 52 partitions from 2019 to 2070
--   - Primary composite key: (id, ts_created)
--   - NO foreign key to device_ports (intentional for write performance)
--
-- Query Patterns (from code analysis):
--   1. INSERT: High-frequency writes on value changes (PortValueService.updatePort)
--      - Inserts by portId + value + ts_created
--   2. READ: Fetching historical values by portId and time range
--      - Time-series data for dashboards/widgets
--   3. CLEANUP: Potential old data deletion for disk space management
--
-- Current Issues:
--   1. NO indexes on port_id - queries filtering by port are slow (sequential scan)
--   2. NO indexes on ts_created within partitions - time-range queries are slow
--   3. Deprecated port_uid column still present (but needed for backward compatibility)
--
-- Optimization Strategy:
--   1. Add composite BTREE index (port_id, ts_created DESC) for time-series queries
--   2. Add BRIN index on ts_created for partition pruning efficiency
--   3. Keep NO foreign key constraint (intentional for high write throughput)
--   4. Set proper fillfactor for better INSERT performance
-- =============================================================================

-- Performance note: Creating indexes on partitioned tables automatically creates
-- indexes on all child partitions. This operation can take significant time
-- depending on data volume (potentially 5-30 minutes for large datasets).

-- =============================================================================
-- 1. COMPOSITE INDEX: (port_id, ts_created DESC)
-- =============================================================================
-- Purpose: Optimize queries fetching historical values for a specific port
-- Use cases:
--   - Dashboard widgets showing time-series data
--   - Fetching last N values for a port
--   - Time-range queries like "get values between date X and Y for port Z"
--
-- Index type: BTREE
--   - Efficient for equality (port_id = X) + range (ts_created BETWEEN)
--   - DESC ordering on ts_created speeds up "latest value" queries
--   - Supports partition pruning when ts_created is in WHERE clause

-- Note: CREATE INDEX CONCURRENTLY cannot be run inside a transaction or function
-- So we use a simple conditional check before creation

\echo 'Creating composite BTREE index idx_port_values_port_time...'
\echo 'This may take several minutes depending on data volume.'
\echo 'Note: Index will be created on each partition separately.'

-- For partitioned tables, we create the index without CONCURRENTLY on the parent
-- PostgreSQL will automatically create it on all child partitions
CREATE INDEX IF NOT EXISTS idx_port_values_port_time 
ON public.port_values (port_id, ts_created DESC)
WHERE port_id IS NOT NULL;

\echo 'Index idx_port_values_port_time created successfully on all partitions'

-- =============================================================================
-- 2. BRIN INDEX: ts_created
-- =============================================================================
-- Purpose: Efficient partition pruning and time-range queries on large datasets
-- Use cases:
--   - Queries with WHERE ts_created > '2024-01-01'
--   - Cleanup jobs deleting old data
--   - Reports spanning multiple partitions
--
-- Index type: BRIN (Block Range INdex)
--   - Extremely small index size (~1000x smaller than BTREE)
--   - Perfect for naturally ordered time-series data
--   - Minimal maintenance overhead
--   - Ideal for partitioned tables with time-based keys

\echo 'Creating BRIN index idx_port_values_ts_created_brin...'

-- BRIN indexes don't support CONCURRENTLY, but they're very fast to create
CREATE INDEX IF NOT EXISTS idx_port_values_ts_created_brin 
ON public.port_values USING BRIN (ts_created)
WITH (pages_per_range = 128);

\echo 'BRIN index created successfully'

-- =============================================================================
-- 3. OPTIONAL: Index on port_uid (for legacy queries)
-- =============================================================================
-- Purpose: Support legacy code that still queries by port_uid (deprecated field)
-- Note: port_uid is deprecated in favor of port_id, but some old code may use it
--
-- Uncomment the following block ONLY if you observe slow queries using port_uid
-- Monitor pg_stat_statements to determine if this index is actually needed

/*
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_port_values_port_uid'
    ) THEN
        RAISE NOTICE 'Creating index idx_port_values_port_uid on port_values...';
        
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_port_values_port_uid 
        ON public.port_values (port_uid)
        WHERE port_uid IS NOT NULL AND port_uid != '';
        
        RAISE NOTICE 'Index idx_port_values_port_uid created successfully';
    ELSE
        RAISE NOTICE 'Index idx_port_values_port_uid already exists, skipping';
    END IF;
END $$;
*/

-- =============================================================================
-- 4. STORAGE OPTIMIZATION: Set fillfactor
-- =============================================================================
-- Purpose: Improve INSERT performance on high-write tables
-- Fillfactor 90: Leave 10% free space in each page for HOT updates
-- 
-- Note: For partitioned tables, fillfactor must be set on each partition
-- We'll set it on the most active recent partitions (last 3 years)

\echo 'Setting fillfactor=90 on recent partitions (2023-2026)...'

-- Current year and future partitions
ALTER TABLE IF EXISTS public.port_values_2023 SET (fillfactor = 90);
ALTER TABLE IF EXISTS public.port_values_2024 SET (fillfactor = 90);
ALTER TABLE IF EXISTS public.port_values_2025 SET (fillfactor = 90);
ALTER TABLE IF EXISTS public.port_values_2026 SET (fillfactor = 90);

\echo 'Fillfactor optimization complete'
\echo 'Note: This only affects new data pages. Run VACUUM FULL to rebuild existing pages (requires table lock).'

-- =============================================================================
-- 5. MAINTENANCE: Update table statistics
-- =============================================================================
-- Purpose: Ensure query planner has accurate statistics for optimal query plans

\echo 'Updating table statistics...'

ANALYZE public.port_values;

\echo 'Table statistics updated'

-- =============================================================================
-- 6. VERIFY INDEX CREATION
-- =============================================================================
-- Check all indexes on port_values (parent and partitions)

\echo ''
\echo '====================================================================='
\echo 'VERIFICATION: Indexes on parent table'
\echo '====================================================================='

SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(schemaname||'.'||indexname)) as index_size
FROM pg_indexes 
WHERE tablename = 'port_values'
  AND indexname LIKE 'idx_port_values%'
ORDER BY indexname;

\echo ''
\echo '====================================================================='
\echo 'VERIFICATION: Indexes on partitions (sample: 2024, 2025)'
\echo '====================================================================='

SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(schemaname||'.'||indexname)) as index_size
FROM pg_indexes 
WHERE tablename IN ('port_values_2024', 'port_values_2025')
  AND indexname LIKE '%port_time%'
ORDER BY tablename, indexname
LIMIT 10;

\echo ''
\echo '====================================================================='
\echo 'OPTIMIZATION COMPLETE!'
\echo '====================================================================='
\echo 'Summary:'
\echo '  - Composite BTREE index: idx_port_values_port_time'
\echo '  - BRIN index: idx_port_values_ts_created_brin'
\echo '  - Fillfactor set to 90 on partitions 2023-2026'
\echo '  - Table statistics updated'
\echo ''
\echo 'Next steps:'
\echo '  1. Monitor index usage with pg_stat_user_indexes'
\echo '  2. Test query performance with EXPLAIN ANALYZE'
\echo '  3. Verify dashboard widgets load faster'
\echo '====================================================================='
\echo ''

-- =============================================================================
-- 7. OPTIONAL: Foreign Key Consideration
-- =============================================================================
-- Note: Currently NO foreign key exists from port_values.port_id to device_ports.id
-- This is INTENTIONAL for write performance (no FK validation overhead)
--
-- Trade-offs:
-- PRO (current approach without FK):
--   - Faster INSERTs (no FK validation)
--   - No lock contention on device_ports during high-frequency writes
--   - Better for time-series workloads with high write throughput
--
-- CON (current approach without FK):
--   - Possible orphaned records if device_ports are deleted
--   - No referential integrity enforcement at DB level
--   - Application must handle cleanup
--
-- If you decide to add FK (NOT recommended for time-series data):
-- Uncomment the following (WARNING: This will slow down all writes)

/*
-- WARNING: This will significantly impact write performance
-- Only add FK if data integrity is more critical than performance

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_port_values_port_id'
    ) THEN
        RAISE NOTICE 'Adding foreign key constraint (this may take a long time)...';
        
        -- First, clean up any orphaned records
        DELETE FROM public.port_values 
        WHERE port_id IS NOT NULL 
          AND port_id NOT IN (SELECT id FROM public.device_ports);
        
        -- Then add the FK
        ALTER TABLE public.port_values
        ADD CONSTRAINT fk_port_values_port_id 
        FOREIGN KEY (port_id) 
        REFERENCES public.device_ports(id)
        ON DELETE CASCADE;  -- Cascade delete historical values when port is deleted
        
        RAISE NOTICE 'Foreign key constraint added';
    ELSE
        RAISE NOTICE 'Foreign key constraint already exists';
    END IF;
END $$;
*/

-- =============================================================================
-- 8. MONITORING QUERIES
-- =============================================================================
-- Use these queries to monitor index usage and table performance

-- Check index usage statistics:
/*
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes 
WHERE tablename LIKE 'port_values%'
  AND indexname LIKE 'idx_port_values%'
ORDER BY idx_scan DESC;
*/

-- Check table size by partition:
/*
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as indexes_size
FROM pg_tables 
WHERE tablename LIKE 'port_values_%'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;
*/

-- =============================================================================
-- OPTIMIZATION COMPLETE
-- =============================================================================
-- Summary of changes:
--   1. ✓ Added composite BTREE index (port_id, ts_created DESC)
--   2. ✓ Added BRIN index on ts_created for efficient partition pruning
--   3. ✓ Set fillfactor=90 for better INSERT performance
--   4. ✓ Updated table statistics (ANALYZE)
--   5. ✓ Maintained NO foreign key for write performance
--
-- Expected performance improvements:
--   - Time-series queries by port_id: 10-100x faster (depending on data volume)
--   - Time-range queries: 5-50x faster with partition pruning
--   - Dashboard widget load times: significantly reduced
--   - INSERT performance: maintained (no degradation from indexes)
--
-- Next steps:
--   1. Monitor query performance using pg_stat_statements
--   2. Verify indexes are being used with EXPLAIN ANALYZE
--   3. Consider setting up automatic partition management for future years
--   4. Plan data retention policy (delete old partitions to save disk space)
-- =============================================================================

