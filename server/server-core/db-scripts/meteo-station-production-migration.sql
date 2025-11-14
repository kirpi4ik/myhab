-- ============================================================================
-- Meteo Station Feature - Production Migration Script
-- ============================================================================
--
-- Description: Complete setup for the Meteo Station virtual device feature
--              that fetches weather data from Open-Meteo API and publishes
--              via MQTT for time-series storage and dashboard display.
--
-- Version:     1.0.0
-- Date:        2025-11-14
-- Database:    PostgreSQL 12+
--
-- Changes:
--   1. Expand value columns to support large JSON arrays (VARCHAR(255) -> TEXT)
--   2. Add METEO_STATION device category
--   3. Create virtual Meteo Station device
--   4. Configure API endpoint and parameters
--
-- Prerequisites:
--   - DeviceModel.OPEN_METEO_API enum must be defined in code (already done)
--   - MeteoStationSyncJob must be deployed (already done)
--
-- Run as:
--   psql -U postgres -d myhab -f meteo-station-production-migration.sql
--
-- Rollback:
--   See meteo-station-rollback.sql
--
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: Expand value columns to support time-series data
-- ============================================================================
-- Reason: Hourly weather data (72 points) creates JSON arrays of 300-1500 bytes
--         which exceeds the default VARCHAR(255) limit

ALTER TABLE device_ports 
ALTER COLUMN value TYPE TEXT;

ALTER TABLE port_values 
ALTER COLUMN value TYPE TEXT;

-- ============================================================================
-- STEP 2: Create METEO_STATION device category
-- ============================================================================

INSERT INTO device_categories (
    id,
    version,
    name,
    ts_created,
    ts_updated
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    'METEO_STATION',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO UPDATE SET
    ts_updated = CURRENT_TIMESTAMP;

-- ============================================================================
-- STEP 3: Create virtual Meteo Station device
-- ============================================================================

-- Insert device controller
INSERT INTO device_controllers (
    id,
    version,
    code,
    name,
    description,
    category,
    model,
    controller_type,
    ts_created,
    ts_updated
)
VALUES (
    2000,
    0,
    'METEO_STATION_VIRTUAL_01',
    'Open-Meteo Weather Station',
    'Virtual device that fetches weather data from Open-Meteo API',
    (SELECT id FROM device_categories WHERE name = 'METEO_STATION'),
    'OPEN_METEO_API',
    'VIRTUAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    model = EXCLUDED.model,
    controller_type = EXCLUDED.controller_type,
    ts_updated = CURRENT_TIMESTAMP;

-- ============================================================================
-- STEP 4: Configure API endpoint and parameters
-- ============================================================================

-- Delete existing configurations to ensure clean state
DELETE FROM configurations WHERE entity_type = 'DEVICE' AND entity_id = 2000;

-- API Base URL
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'API Base URL',
    'https://api.open-meteo.com/v1/forecast',
    'API_URL',
    'Open-Meteo API endpoint'
);

-- Latitude (Brasov, Romania)
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'Latitude',
    '45.7519398',
    'latitude',
    'Location latitude coordinate'
);

-- Longitude (Brasov, Romania)
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'Longitude',
    '25.5674708',
    'longitude',
    'Location longitude coordinate'
);

-- Daily parameters (comma-separated)
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'Daily Parameters',
    'sunrise,sunset,daylight_duration,sunshine_duration,rain_sum,apparent_temperature_min,apparent_temperature_max,wind_speed_10m_max',
    'daily',
    'Daily weather parameters to fetch'
);

-- Hourly parameters (comma-separated)
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'Hourly Parameters',
    'temperature_2m,wind_speed_10m,soil_temperature_0cm,visibility,precipitation',
    'hourly',
    'Hourly weather parameters to fetch'
);

-- Timezone (will be URL-encoded automatically by the job)
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'Timezone',
    'GMT',
    'timezone',
    'Timezone for weather data (e.g., GMT, GMT+2, Europe/Bucharest)'
);

-- Forecast days
INSERT INTO configurations (
    id,
    version,
    entity_id,
    entity_type,
    name,
    value,
    key,
    description
)
VALUES (
    nextval('hibernate_sequence'),
    0,
    2000,
    'DEVICE',
    'Forecast Days',
    '3',
    'forecast_days',
    'Number of forecast days to fetch'
);

-- ============================================================================
-- STEP 5: Verify the migration
-- ============================================================================

-- Check value column types
DO $$
DECLARE
    device_ports_type TEXT;
    port_values_type TEXT;
    category_exists BOOLEAN;
    device_exists BOOLEAN;
    config_count INTEGER;
BEGIN
    -- Check column types
    SELECT data_type INTO device_ports_type
    FROM information_schema.columns 
    WHERE table_name = 'device_ports' AND column_name = 'value';
    
    SELECT data_type INTO port_values_type
    FROM information_schema.columns 
    WHERE table_name = 'port_values' AND column_name = 'value';
    
    -- Check category
    SELECT EXISTS(SELECT 1 FROM device_categories WHERE name = 'METEO_STATION') INTO category_exists;
    
    -- Check device
    SELECT EXISTS(SELECT 1 FROM device_controllers WHERE id = 2000) INTO device_exists;
    
    -- Check configurations
    SELECT COUNT(*) INTO config_count
    FROM configurations
    WHERE entity_type = 'DEVICE' AND entity_id = 2000;
    
    -- Report results
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Migration Verification Results:';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'device_ports.value type: % (expected: text)', device_ports_type;
    RAISE NOTICE 'port_values.value type:  % (expected: text)', port_values_type;
    RAISE NOTICE 'METEO_STATION category:  % (expected: t)', category_exists;
    RAISE NOTICE 'Virtual device created:  % (expected: t)', device_exists;
    RAISE NOTICE 'Configurations created:  % (expected: 8)', config_count;
    RAISE NOTICE '=================================================================';
    
    -- Verify all checks passed
    IF device_ports_type != 'text' OR port_values_type != 'text' THEN
        RAISE EXCEPTION 'Column type migration failed';
    END IF;
    
    IF NOT category_exists OR NOT device_exists OR config_count != 8 THEN
        RAISE EXCEPTION 'Device setup incomplete';
    END IF;
    
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE '=================================================================';
END $$;

COMMIT;

-- ============================================================================
-- Post-Migration Notes
-- ============================================================================
--
-- 1. Job Configuration:
--    The MeteoStationSyncJob is configured in application.yml:
--      quartz.jobs.meteoStationSync.enabled: true
--      quartz.jobs.meteoStationSync.interval: 3600  # seconds (1 hour)
--
-- 2. MQTT Configuration:
--    Ensure mosquitto.conf has adequate message size limit:
--      message_size_limit 0  # Unlimited (or at least 10240 for 10KB)
--
-- 3. Application Restart:
--    Restart the Grails application to:
--    - Load the new DeviceModel.OPEN_METEO_API enum
--    - Initialize the MeteoStationSyncJob
--    - Apply the new TEXT column mappings from domain classes
--
-- 4. Verify Operation:
--    After restart, check logs for:
--      INFO MeteoStationSyncJob - Weather data synced successfully
--
--    Query database to verify data:
--      SELECT internal_ref, 
--             CASE WHEN value IS NULL THEN 'NO DATA'
--                  WHEN LENGTH(value) > 100 THEN 'HAS DATA'
--                  ELSE value END as status
--      FROM device_ports
--      WHERE device_id = 2000
--      ORDER BY internal_ref;
--
-- 5. Frontend:
--    The MeteoStationCard widget will automatically display data once
--    the job has run and populated the device ports.
--
-- ============================================================================

