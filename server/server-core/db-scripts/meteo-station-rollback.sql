-- ============================================================================
-- Meteo Station Feature - Rollback Script
-- ============================================================================
--
-- Description: Complete rollback of the Meteo Station feature
--
-- Version:     1.0.0
-- Date:        2025-11-14
-- Database:    PostgreSQL 12+
--
-- WARNING:     This will permanently delete all Meteo Station data including
--              historical weather records. Create a backup before proceeding.
--
-- Run as:
--   psql -U postgres -d myhab -f meteo-station-rollback.sql
--
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: Remove device ports and their values
-- ============================================================================

-- Delete historical port values
DELETE FROM port_values
WHERE port_id IN (
    SELECT id FROM device_ports WHERE device_id = 2000
);

-- Delete device ports
DELETE FROM device_ports
WHERE device_id = 2000;

-- ============================================================================
-- STEP 2: Remove device configurations
-- ============================================================================

DELETE FROM configurations
WHERE entity_type = 'DEVICE' AND entity_id = 2000;

-- ============================================================================
-- STEP 3: Remove the virtual device
-- ============================================================================

DELETE FROM device_controllers
WHERE id = 2000;

-- ============================================================================
-- STEP 4: Remove device category (optional)
-- ============================================================================
-- Note: Only remove if no other METEO_STATION devices exist

DELETE FROM device_categories
WHERE name = 'METEO_STATION'
  AND NOT EXISTS (
    SELECT 1 FROM device_controllers 
    WHERE category = (SELECT id FROM device_categories WHERE name = 'METEO_STATION')
  );

-- ============================================================================
-- STEP 5: Revert column types (optional, not recommended)
-- ============================================================================
-- WARNING: This will fail if any ports have values exceeding 255 characters
--          Only uncomment if you're certain no large values exist

-- To safely revert, first check for large values:
-- SELECT table_name, id, internal_ref, LENGTH(value) as size
-- FROM (
--     SELECT 'device_ports' as table_name, id, internal_ref, value
--     FROM device_ports
--     WHERE LENGTH(value) > 255
--     UNION ALL
--     SELECT 'port_values' as table_name, id::text, port_id::text, value
--     FROM port_values
--     WHERE LENGTH(value) > 255
-- ) large_values
-- ORDER BY size DESC;

-- If you must revert and have large values, truncate them first:
-- UPDATE device_ports SET value = SUBSTRING(value, 1, 255) WHERE LENGTH(value) > 255;
-- UPDATE port_values SET value = SUBSTRING(value, 1, 255) WHERE LENGTH(value) > 255;

-- Then uncomment these lines to revert:
-- ALTER TABLE device_ports ALTER COLUMN value TYPE VARCHAR(255);
-- ALTER TABLE port_values ALTER COLUMN value TYPE VARCHAR(255);

-- ============================================================================
-- STEP 6: Verify the rollback
-- ============================================================================

DO $$
DECLARE
    device_exists BOOLEAN;
    config_count INTEGER;
    port_count INTEGER;
    category_exists BOOLEAN;
BEGIN
    -- Check if device is gone
    SELECT EXISTS(SELECT 1 FROM device_controllers WHERE id = 2000) INTO device_exists;
    
    -- Check if configurations are gone
    SELECT COUNT(*) INTO config_count
    FROM configurations
    WHERE entity_type = 'DEVICE' AND entity_id = 2000;
    
    -- Check if ports are gone
    SELECT COUNT(*) INTO port_count
    FROM device_ports
    WHERE device_id = 2000;
    
    -- Check category (might still exist if we didn't delete it)
    SELECT EXISTS(SELECT 1 FROM device_categories WHERE name = 'METEO_STATION') INTO category_exists;
    
    -- Report results
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Rollback Verification Results:';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Device exists:         % (expected: f)', device_exists;
    RAISE NOTICE 'Configurations remain: % (expected: 0)', config_count;
    RAISE NOTICE 'Ports remain:          % (expected: 0)', port_count;
    RAISE NOTICE 'Category exists:       % (ok if true)', category_exists;
    RAISE NOTICE '=================================================================';
    
    -- Verify critical checks passed
    IF device_exists OR config_count > 0 OR port_count > 0 THEN
        RAISE EXCEPTION 'Rollback verification failed - some data remains';
    END IF;
    
    RAISE NOTICE 'Rollback completed successfully!';
    RAISE NOTICE '=================================================================';
END $$;

COMMIT;

-- ============================================================================
-- Post-Rollback Actions
-- ============================================================================
--
-- 1. Application Code:
--    The following code can remain (it won't cause issues):
--    - DeviceModel.OPEN_METEO_API enum
--    - MeteoStationSyncJob class
--    - MQTTTopic.METEO_STATION class
--    - MeteoStationCard.vue component
--
--    To fully remove the feature, you should also:
--    - Remove or disable MeteoStationSyncJob
--    - Remove MeteoStationCard from dashboard
--
-- 2. Application Configuration:
--    Disable the job in application.yml:
--      quartz.jobs.meteoStationSync.enabled: false
--
-- 3. No restart required (but recommended)
--
-- ============================================================================
