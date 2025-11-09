-- Migration script to populate port_id in port_values table
-- This script updates existing port_values records by finding DevicePort using port_uid
-- and setting port_id where it doesn't exist
--
-- Note: port_values is partitioned by year (port_values_2019, port_values_2020, etc.)
-- This script explicitly updates each partition for better performance and clarity

-- Step 1: Update port_id where port_uid matches DevicePort.uid (PRIORITY: UID matching)
-- This handles historical data where port_uid references DevicePort.uid
-- Update all partitions explicitly

DO $$
DECLARE
    partition_name TEXT;
    year_val INT;
BEGIN
    -- Loop through all partitions from 2019 to 2070
    FOR year_val IN 2019..2070 LOOP
        partition_name := 'port_values_' || year_val;
        
        -- Check if partition exists before updating
        IF EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = partition_name
            AND n.nspname = 'public'
        ) THEN
            -- Update partition: Match by UID
            EXECUTE format('
                UPDATE %I pv
                SET port_id = dp.id
                FROM device_ports dp
                WHERE pv.port_id IS NULL
                  AND pv.port_uid IS NOT NULL
                  AND pv.port_uid != ''''
                  AND dp.uid = pv.port_uid
            ', partition_name);
            
            RAISE NOTICE 'Updated partition: %', partition_name;
        END IF;
    END LOOP;
END $$;

-- Step 2: Set port_id to -1 for records where port_uid doesn't match any DevicePort.uid
-- This handles missing matches (orphaned records)
-- Update all partitions explicitly

DO $$
DECLARE
    partition_name TEXT;
    year_val INT;
BEGIN
    -- Loop through all partitions from 2019 to 2070
    FOR year_val IN 2019..2070 LOOP
        partition_name := 'port_values_' || year_val;
        
        -- Check if partition exists before updating
        IF EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = partition_name
            AND n.nspname = 'public'
        ) THEN
            -- Update partition: Set -1 for orphaned records
            EXECUTE format('
                UPDATE %I pv
                SET port_id = -1
                WHERE pv.port_id IS NULL
                  AND pv.port_uid IS NOT NULL
                  AND pv.port_uid != ''''
                  AND NOT EXISTS (
                    SELECT 1 FROM device_ports dp WHERE dp.uid = pv.port_uid
                  )
            ', partition_name);
            
            RAISE NOTICE 'Set orphaned records in partition: %', partition_name;
        END IF;
    END LOOP;
END $$;

-- Verification query: Check how many records were updated (aggregate across all partitions)
SELECT 
    COUNT(*) as total_records,
    COUNT(port_id) as records_with_port_id,
    COUNT(*) - COUNT(port_id) as records_without_port_id,
    COUNT(CASE WHEN port_id = -1 THEN 1 END) as records_with_orphaned_port_id
FROM port_values;

-- Verification query: Per-partition statistics (shows coverage for each partition)
DO $$
DECLARE
    partition_name TEXT;
    year_val INT;
    result_record RECORD;
BEGIN
    RAISE NOTICE '=== Per-Partition Statistics ===';
    
    FOR year_val IN 2019..2070 LOOP
        partition_name := 'port_values_' || year_val;
        
        -- Check if partition exists
        IF EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = partition_name
            AND n.nspname = 'public'
        ) THEN
            -- Get statistics for this partition
            EXECUTE format('
                SELECT 
                    COUNT(*) as total,
                    COUNT(port_id) as with_port_id,
                    COUNT(*) - COUNT(port_id) as without_port_id,
                    COUNT(CASE WHEN port_id = -1 THEN 1 END) as orphaned
                FROM %I
            ', partition_name) INTO result_record;
            
            RAISE NOTICE 'Partition: % | Total: % | With port_id: % | Without port_id: % | Orphaned (-1): %',
                partition_name,
                result_record.total,
                result_record.with_port_id,
                result_record.without_port_id,
                result_record.orphaned;
        END IF;
    END LOOP;
    
    RAISE NOTICE '=== End Per-Partition Statistics ===';
END $$;

-- Verification query: Show records that still don't have port_id (should be empty after migration)
SELECT 
    pv.id,
    pv.port_uid,
    pv.port_id,
    pv.value,
    pv.ts_created
FROM port_values pv
WHERE pv.port_id IS NULL
  AND pv.port_uid IS NOT NULL
  AND pv.port_uid != ''
ORDER BY pv.ts_created DESC
LIMIT 100;

-- Verification query: Show sample of records with port_id = -1 (orphaned records)
SELECT 
    pv.id,
    pv.port_uid,
    pv.port_id,
    pv.value,
    pv.ts_created
FROM port_values pv
WHERE pv.port_id = -1
ORDER BY pv.ts_created DESC
LIMIT 100;

-- Verification query: Show sample of successfully matched records
SELECT 
    pv.id,
    pv.port_uid,
    pv.port_id,
    dp.uid as device_port_uid,
    pv.value,
    pv.ts_created
FROM port_values pv
LEFT JOIN device_ports dp ON dp.id = pv.port_id
WHERE pv.port_id IS NOT NULL
  AND pv.port_id != -1
ORDER BY pv.ts_created DESC
LIMIT 100;

