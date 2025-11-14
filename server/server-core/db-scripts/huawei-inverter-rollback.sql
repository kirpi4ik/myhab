-- ============================================================================
-- Huawei Solar Inverter Feature - Rollback Script
-- ============================================================================
--
-- Description: Completely removes the Huawei SUN2000 inverter device and
--              all related configurations, ports, and historical data.
--
-- Version:     1.0.0
-- Date:        2025-11-14
-- Database:    PostgreSQL 12+
--
-- WARNING:     This script will permanently delete:
--              - All device configurations
--              - All device ports
--              - All historical port values
--              - The device itself
--              - The SOLAR_INVERTER category (if no other devices use it)
--
-- Run as:
--   psql -U postgres -d myhab -f huawei-inverter-rollback.sql
--
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: Delete historical port values
-- ============================================================================

DELETE FROM port_values
WHERE port_id IN (
    SELECT id FROM device_ports WHERE device_id = 1000
);

-- ============================================================================
-- STEP 2: Delete device ports
-- ============================================================================

DELETE FROM device_ports
WHERE device_id = 1000;

-- ============================================================================
-- STEP 3: Delete device configurations
-- ============================================================================

DELETE FROM configurations
WHERE entity_type = 'DEVICE' AND entity_id = 1000;

-- ============================================================================
-- STEP 4: Delete the device controller
-- ============================================================================

DELETE FROM device_controllers
WHERE id = 1000;

-- ============================================================================
-- STEP 5: Optionally delete SOLAR_INVERTER category
-- ============================================================================
-- Note: Only remove if no other SOLAR_INVERTER devices exist

DELETE FROM device_categories
WHERE name = 'SOLAR_INVERTER'
  AND NOT EXISTS (
    SELECT 1 FROM device_controllers 
    WHERE category = (SELECT id FROM device_categories WHERE name = 'SOLAR_INVERTER')
  );

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
    -- Check device
    SELECT EXISTS(SELECT 1 FROM device_controllers WHERE id = 1000) INTO device_exists;
    
    -- Check configurations
    SELECT COUNT(*) INTO config_count
    FROM configurations
    WHERE entity_type = 'DEVICE' AND entity_id = 1000;
    
    -- Check ports
    SELECT COUNT(*) INTO port_count
    FROM device_ports
    WHERE device_id = 1000;
    
    -- Check category (might still exist if we didn't delete it)
    SELECT EXISTS(SELECT 1 FROM device_categories WHERE name = 'SOLAR_INVERTER') INTO category_exists;
    
    -- Report results
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Rollback Verification Results:';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Device exists:         % (expected: f)', device_exists;
    RAISE NOTICE 'Configurations remain: % (expected: 0)', config_count;
    RAISE NOTICE 'Ports remain:          % (expected: 0)', port_count;
    RAISE NOTICE 'Category exists:       % (ok if true)', category_exists;
    RAISE NOTICE '=================================================================';
    
    -- Verify rollback succeeded
    IF device_exists OR config_count > 0 OR port_count > 0 THEN
        RAISE EXCEPTION 'Rollback incomplete - some data remains';
    END IF;
    
    RAISE NOTICE 'Rollback completed successfully!';
    RAISE NOTICE '=================================================================';
END $$;

COMMIT;

-- ============================================================================
-- Post-Rollback Notes
-- ============================================================================
--
-- 1. The SOLAR_INVERTER category may still exist if other devices use it.
--    This is intentional to prevent breaking other devices.
--
-- 2. Application restart is not required, but you may want to:
--    - Disable the HuaweiInfoSyncJob in application.yml
--    - Restart to stop the job from attempting to sync
--
-- 3. To restore the device, simply re-run:
--    huawei-inverter-setup.sql
--
-- ============================================================================

