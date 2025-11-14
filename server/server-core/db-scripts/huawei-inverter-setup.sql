-- ============================================================================
-- Huawei Solar Inverter Feature - Setup Script
-- ============================================================================
--
-- Description: Setup for the Huawei SUN2000 solar inverter and DTSU666-H
--              smart meter devices that fetch data from Huawei FusionSolar
--              API and publish via MQTT for real-time monitoring.
--
-- Version:     2.0.0
-- Date:        2025-11-14
-- Database:    PostgreSQL 12+
--
-- Changes:
--   1. Add SOLAR_INVERTER and ELECTRIC_METER device categories
--   2. Create Huawei SUN2000 inverter device (ID 1000)
--   3. Create Huawei DTSU666-H meter device (ID 1001)
--   4. Configure API credentials and endpoints
--   5. Configure parameter tracking for both devices
--
-- Prerequisites:
--   - DeviceModel.HUAWEI_SUN2000_12KTL_M2 enum defined in code (already done)
--   - DeviceModel.ELECTRIC_METER_DTS enum defined in code (already done)
--   - HuaweiInfoSyncJob must be deployed (already done)
--   - device_ports.value and port_values.value columns should be TEXT type
--
-- Run as:
--   psql -U postgres -d myhab -f huawei-inverter-setup.sql
--
-- If you get "current transaction is aborted" error:
--   1. Run: ROLLBACK;
--   2. Then run this script again
--
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: Delete existing data for idempotency
-- ============================================================================
-- Note: Must delete in correct order to avoid foreign key violations

-- Delete configurations (both devices)
DELETE FROM configurations WHERE entity_type = 'DEVICE' AND entity_id IN (1000, 1001);

-- Delete device ports (both devices)
DELETE FROM device_ports WHERE device_id IN (1000, 1001);

-- Delete the devices
DELETE FROM device_controllers WHERE id IN (1000, 1001);

-- Delete categories (safe now that devices are gone)
DELETE FROM device_categories WHERE name IN ('SOLAR_INVERTER', 'ELECTRIC_METER');

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
-- STEP 3: Create ELECTRIC_METER device category
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
    'ELECTRIC_METER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- STEP 4: Create Huawei SUN2000 inverter device (ID 1000)
-- ============================================================================

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
-- STEP 5: Create Huawei DTSU666-H meter device (ID 1001)
-- ============================================================================

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
    1001,
    0,
    'HUAWEI_DTSU666_METER_01',
    'Huawei DTSU666-H Smart Meter',
    'Smart power meter connected to Huawei FusionSolar system',
    (SELECT id FROM device_categories WHERE name = 'ELECTRIC_METER'),
    'ELECTRIC_METER_DTS',
    'ONLINE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- STEP 6: Configure API credentials and endpoints (Inverter Device)
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

-- ============================================================================
-- STEP 7: Configure Meter Device Settings
-- ============================================================================

-- Device IDs for meter
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
    1001,
    'DEVICE',
    'Meter Device IDs',
    '1000000036406276',
    'DEV_IDS',
    'Device ID for the smart meter in Huawei FusionSolar API'
);

-- ============================================================================
-- STEP 8: Configure API Parameter Selection (Inverter Device)
-- ============================================================================

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

-- ============================================================================
-- STEP 9: Configure API Parameter Selection (Meter Device)
-- ============================================================================

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
    1001,
    'DEVICE',
    'Meter API Parameters',
    'active_power,active_power_a,active_power_b,active_power_c,meter_i,c_i,b_i,active_cap,reverse_active_cap,c_u,b_u,meter_u,grid_frequency,power_factor,meter_status,run_state',
    'METER_API_PARAMS',
    'Meter parameters to track: grid power (total and per-phase), currents, voltages, energy totals'
);

-- ============================================================================
-- STEP 10: Verify the migration
-- ============================================================================

DO $$
DECLARE
    inverter_category_exists BOOLEAN;
    meter_category_exists BOOLEAN;
    inverter_device_exists BOOLEAN;
    meter_device_exists BOOLEAN;
    inverter_config_count INTEGER;
    meter_config_count INTEGER;
    total_config_count INTEGER;
BEGIN
    -- Check categories
    SELECT EXISTS(SELECT 1 FROM device_categories WHERE name = 'SOLAR_INVERTER') INTO inverter_category_exists;
    SELECT EXISTS(SELECT 1 FROM device_categories WHERE name = 'ELECTRIC_METER') INTO meter_category_exists;
    
    -- Check devices
    SELECT EXISTS(SELECT 1 FROM device_controllers WHERE id = 1000 AND model = 'HUAWEI_SUN2000_12KTL_M2') INTO inverter_device_exists;
    SELECT EXISTS(SELECT 1 FROM device_controllers WHERE id = 1001 AND model = 'ELECTRIC_METER_DTS') INTO meter_device_exists;
    
    -- Count configurations
    SELECT COUNT(*) INTO inverter_config_count FROM configurations WHERE entity_id = 1000 AND entity_type = 'DEVICE';
    SELECT COUNT(*) INTO meter_config_count FROM configurations WHERE entity_id = 1001 AND entity_type = 'DEVICE';
    total_config_count := inverter_config_count + meter_config_count;
    
    -- Report results
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Migration Verification Results:';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Categories:';
    RAISE NOTICE '  SOLAR_INVERTER:         % (expected: t)', inverter_category_exists;
    RAISE NOTICE '  ELECTRIC_METER:         % (expected: t)', meter_category_exists;
    RAISE NOTICE 'Devices:';
    RAISE NOTICE '  Inverter (ID 1000):     % (expected: t)', inverter_device_exists;
    RAISE NOTICE '  Meter (ID 1001):        % (expected: t)', meter_device_exists;
    RAISE NOTICE 'Configurations:';
    RAISE NOTICE '  Inverter configs:       % (expected: 10)', inverter_config_count;
    RAISE NOTICE '  Meter configs:          % (expected: 2)', meter_config_count;
    RAISE NOTICE '  Total:                  % (expected: 12)', total_config_count;
    RAISE NOTICE '=================================================================';
    
    -- Verify all checks passed
    IF NOT inverter_category_exists OR NOT meter_category_exists OR 
       NOT inverter_device_exists OR NOT meter_device_exists OR 
       inverter_config_count != 10 OR meter_config_count != 2 THEN
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

