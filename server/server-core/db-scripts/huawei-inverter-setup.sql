-- ============================================================================
-- Huawei Solar Inverter Feature - Setup Script
-- ============================================================================
--
-- Description: Setup for the Huawei SUN2000 solar inverter device that
--              fetches data from Huawei FusionSolar API and publishes
--              via MQTT for real-time monitoring and dashboard display.
--
-- Version:     1.0.0
-- Date:        2025-11-14
-- Database:    PostgreSQL 12+
--
-- Changes:
--   1. Add SOLAR_INVERTER device category
--   2. Create Huawei SUN2000 inverter device
--   3. Configure API credentials and endpoints
--
-- Prerequisites:
--   - DeviceModel.HUAWEI_SUN2000_12KTL_M2 enum must be defined in code (already done)
--   - HuaweiInfoSyncJob must be deployed (already done)
--   - device_ports.value and port_values.value columns should be TEXT type
--
-- Run as:
--   psql -U postgres -d myhab -f huawei-inverter-setup.sql
--
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: Delete existing data for idempotency
-- ============================================================================
-- Note: Must delete in correct order to avoid foreign key violations

-- Delete configurations first
DELETE FROM configurations WHERE entity_type = 'DEVICE' AND entity_id = 1000;

-- Delete device ports
DELETE FROM device_ports WHERE device_id = 1000;

-- Delete the device
DELETE FROM device_controllers WHERE id = 1000;

-- Delete category (safe now that device is gone)
DELETE FROM device_categories WHERE name = 'SOLAR_INVERTER';

-- ============================================================================
-- STEP 2: Create SOLAR_INVERTER device category
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
    'SOLAR_INVERTER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- STEP 3: Create Huawei SUN2000 inverter device
-- ============================================================================

-- Insert device controller
INSERT INTO device_controllers (
    id,
    version,
    code,
    name,
    description,
    type_id,
    model,
    status,
    ts_created,
    ts_updated
)
VALUES (
    1000,
    0,
    'HUAWEI_SUN2000_INVERTER_01',
    'Huawei SUN2000-12KTL-M2 Inverter',
    'Solar inverter that fetches data from Huawei FusionSolar API',
    (SELECT id FROM device_categories WHERE name = 'SOLAR_INVERTER'),
    'HUAWEI_SUN2000_12KTL_M2',
    'ONLINE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- STEP 4: Configure API credentials and endpoints
-- ============================================================================

-- OAuth Access User (username for Huawei FusionSolar API)
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
    1000,
    'DEVICE',
    'OAuth Access User',
    'madhouseapi',
    'cfg.key.device.oauth.access_user',
    'Username for Huawei FusionSolar API authentication'
);

-- OAuth Access Password (system code for Huawei FusionSolar API)
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
    1000,
    'DEVICE',
    'OAuth Access Password',
    'MHh4rzAZc9rEUzZ',
    'cfg.key.device.oauth.access_passwd',
    'System code for Huawei FusionSolar API authentication'
);

-- API Login URL
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
    1000,
    'DEVICE',
    'API Login URL',
    'https://eu5.fusionsolar.huawei.com/thirdData/login',
    'API_LOGIN_URL',
    'Huawei FusionSolar API login endpoint'
);

-- Station Code (for station-level queries, if needed)
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
    1000,
    'DEVICE',
    'Station Code',
    'NE=36363788',
    'STATION_CODE',
    'Huawei FusionSolar station identifier'
);

-- Token Header Name
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
    1000,
    'DEVICE',
    'Token Header Name',
    'xsrf-token',
    'TOKEN_HEADER',
    'HTTP header name for authentication token'
);

-- Device Type ID for inverter
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
    1000,
    'DEVICE',
    'Device Type ID',
    '1',
    'DEV_TYPE_ID',
    'Device type ID for inverter in Huawei API (1 = inverter, 47 = meter)'
);

-- Device IDs for inverter
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
    1000,
    'DEVICE',
    'Device IDs',
    '1000000036363790',
    'DEV_IDS',
    'Comma-separated device IDs for this inverter'
);

-- Meter Device IDs (for power meter)
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
    1000,
    'DEVICE',
    'Meter Device IDs',
    '1000000036406276',
    'METER_DEV_IDS',
    'Comma-separated device IDs for power meter'
);

-- ============================================================================
-- STEP 5: Configure API Parameter Selection
-- ============================================================================
-- These configurations define which parameters to track from each API
-- Format: Comma-separated list of parameter names

-- Station API Parameters (aggregated totals)
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
    1000,
    'DEVICE',
    'Station API Parameters',
    'day_power,month_power,total_power,day_on_grid_energy,day_use_energy,day_income,total_income,real_health_state',
    'STATION_API_PARAMS',
    'Station-level parameters to track: daily/monthly/total yields, grid energy, income, health'
);

-- Inverter API Parameters (real-time solar production)
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
    1000,
    'DEVICE',
    'Inverter API Parameters',
    'active_power,day_cap,total_cap,mppt_power,efficiency,temperature,run_state,inverter_state,a_u,b_u,c_u,elec_freq,power_factor',
    'INVERTER_API_PARAMS',
    'Inverter parameters to track: power, energy, efficiency, temperature, state, grid parameters'
);

-- Meter API Parameters (grid import/export and per-phase consumption)
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
    1000,
    'DEVICE',
    'Meter API Parameters',
    'active_power,active_power_a,active_power_b,active_power_c,meter_i,c_i,b_i,active_cap,reverse_active_cap,c_u,b_u,meter_u,grid_frequency,power_factor,meter_status,run_state',
    'METER_API_PARAMS',
    'Meter parameters to track: grid power (total and per-phase), currents, voltages, energy totals'
);

-- ============================================================================
-- STEP 6: Verify the migration
-- ============================================================================

DO $$
DECLARE
    category_exists BOOLEAN;
    device_exists BOOLEAN;
    config_count INTEGER;
BEGIN
    -- Check category
    SELECT EXISTS(SELECT 1 FROM device_categories WHERE name = 'SOLAR_INVERTER') INTO category_exists;
    
    -- Check device
    SELECT EXISTS(SELECT 1 FROM device_controllers WHERE id = 1000) INTO device_exists;
    
    -- Check configurations
    SELECT COUNT(*) INTO config_count
    FROM configurations
    WHERE entity_type = 'DEVICE' AND entity_id = 1000;
    
    -- Report results
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Migration Verification Results:';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'SOLAR_INVERTER category:  % (expected: t)', category_exists;
    RAISE NOTICE 'Virtual device created:   % (expected: t)', device_exists;
    RAISE NOTICE 'Configurations created:   % (expected: 11)', config_count;
    RAISE NOTICE '=================================================================';
    
    -- Verify all checks passed
    IF NOT category_exists OR NOT device_exists OR config_count != 11 THEN
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
--    The HuaweiInfoSyncJob is configured in application.yml:
--      quartz.jobs.huaweiInfoSync.enabled: true
--      quartz.jobs.huaweiInfoSync.interval: 120  # seconds
--
-- 2. Code Changes Required:
--    Update HuaweiInfoSyncJob.groovy to read constants from configurations:
--    - Remove hardcoded constants (API_LOGIN_URL, API_STATION_LIST_URL, etc.)
--    - Use device.getConfigurationByKey() to fetch values
--    - Example:
--        def apiLoginUrl = device.getConfigurationByKey('API_LOGIN_URL')?.value
--        def stationCode = device.getConfigurationByKey('STATION_CODE')?.value
--
-- 3. Application Restart:
--    Restart the Grails application to pick up the new configuration.
--
-- 4. Verify Operation:
--    After restart, check logs for:
--      INFO HuaweiInfoSyncJob - Huawei inverter data synced successfully
--
--    Query database to verify data:
--      SELECT internal_ref, value
--      FROM device_ports
--      WHERE device_id = 1000
--      ORDER BY internal_ref;
--
-- ============================================================================

