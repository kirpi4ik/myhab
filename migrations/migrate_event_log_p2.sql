-- Migration script to replace UIDs with IDs in event_log.p2 column
-- This script updates existing event_log records by finding DevicePeripheral using p2 (which contains uid)
-- and replacing the UID with the corresponding device_peripherals.id
--
-- Note: p2 is a varchar column, so we store the id as a string
-- The event_log table stores event data where p2 references device_peripherals.uid

-- Step 1: Update p2 where it matches device_peripherals.uid
-- Replace the UID with the corresponding device_peripherals.id (as string)
DO $$
DECLARE
    updated_count INT;
BEGIN
    RAISE NOTICE 'Starting migration: Replacing UIDs with IDs in event_log.p2...';
    
    -- Update event_log: Match by device_peripherals.uid and replace with id
    UPDATE event_log el
    SET p2 = dp.id::varchar
    FROM device_peripherals dp
    WHERE el.p2 IS NOT NULL
      AND el.p2 != ''
      AND el.p2 != '-1'
      -- Only update if p2 currently contains a UID (not already an ID)
      AND el.p2 = dp.uid;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'Updated % records with matching device_peripherals.id', updated_count;
END $$;

-- Step 2: Set p2 to '-1' for records where the UID doesn't match any device_peripherals.uid
-- This handles orphaned records (references to deleted or non-existent peripherals)
DO $$
DECLARE
    orphaned_count INT;
BEGIN
    RAISE NOTICE 'Setting orphaned records to -1...';
    
    -- Update event_log: Set -1 for orphaned records
    -- Only update records that still look like UIDs (not numeric IDs)
    UPDATE event_log el
    SET p2 = '-1'
    WHERE el.p2 IS NOT NULL
      AND el.p2 != ''
      AND el.p2 != '-1'
      -- Check if p2 doesn't match any device_peripherals.uid
      AND NOT EXISTS (
        SELECT 1 FROM device_peripherals dp WHERE dp.uid = el.p2
      )
      -- Also check if p2 doesn't match any device_peripherals.id (to avoid false positives)
      AND NOT EXISTS (
        SELECT 1 FROM device_peripherals dp WHERE dp.id::varchar = el.p2
      );
    
    GET DIAGNOSTICS orphaned_count = ROW_COUNT;
    RAISE NOTICE 'Set % orphaned records to -1', orphaned_count;
END $$;

-- Verification query: Overall statistics
SELECT 
    COUNT(*) as total_records,
    COUNT(CASE WHEN p2 IS NOT NULL AND p2 != '' THEN 1 END) as records_with_p2,
    COUNT(CASE WHEN p2 = '-1' THEN 1 END) as records_with_orphaned_p2,
    COUNT(CASE WHEN p2 ~ '^\d+$' AND p2 != '-1' THEN 1 END) as records_with_numeric_p2
FROM event_log;

-- Verification query: Show sample of orphaned records (p2 = '-1')
SELECT 
    el.id,
    el.p0,
    el.p1,
    el.p2,
    el.p3,
    el.category,
    el.ts_created
FROM event_log el
WHERE el.p2 = '-1'
ORDER BY el.ts_created DESC
LIMIT 100;

-- Verification query: Show sample of successfully migrated records
-- Join with device_peripherals to verify the ID matches
SELECT 
    el.id,
    el.p0,
    el.p1,
    el.p2 as p2_device_id,
    dp.id as device_peripheral_id,
    dp.uid as device_peripheral_uid,
    dp.name as device_peripheral_name,
    el.p3,
    el.category,
    el.ts_created
FROM event_log el
INNER JOIN device_peripherals dp ON dp.id::varchar = el.p2
WHERE el.p2 IS NOT NULL
  AND el.p2 != ''
  AND el.p2 != '-1'
  AND el.p2 ~ '^\d+$'  -- Only numeric values (IDs)
ORDER BY el.ts_created DESC
LIMIT 100;

-- Verification query: Show records that might still have UIDs (not migrated)
-- These should be empty after successful migration
SELECT 
    el.id,
    el.p0,
    el.p1,
    el.p2,
    el.p3,
    el.category,
    el.ts_created
FROM event_log el
WHERE el.p2 IS NOT NULL
  AND el.p2 != ''
  AND el.p2 != '-1'
  AND el.p2 !~ '^\d+$'  -- Non-numeric values (potential UIDs)
ORDER BY el.ts_created DESC
LIMIT 100;

-- Verification query: Count records by migration status
SELECT 
    'Successfully migrated (matched)' as status,
    COUNT(*) as count
FROM event_log el
WHERE el.p2 IS NOT NULL
  AND el.p2 != ''
  AND el.p2 != '-1'
  AND el.p2 ~ '^\d+$'
  AND EXISTS (SELECT 1 FROM device_peripherals dp WHERE dp.id::varchar = el.p2)
UNION ALL
SELECT 
    'Orphaned (set to -1)' as status,
    COUNT(*) as count
FROM event_log el
WHERE el.p2 = '-1'
UNION ALL
SELECT 
    'Potentially unmigrated (still UID)' as status,
    COUNT(*) as count
FROM event_log el
WHERE el.p2 IS NOT NULL
  AND el.p2 != ''
  AND el.p2 != '-1'
  AND el.p2 !~ '^\d+$'
UNION ALL
SELECT 
    'Empty or NULL p2' as status,
    COUNT(*) as count
FROM event_log el
WHERE el.p2 IS NULL OR el.p2 = ''
ORDER BY status;

