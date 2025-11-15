-- ============================================================================
-- Nibe F1145-8 EM Heat Pump Setup Script
-- ============================================================================
-- Purpose: Initialize database for Nibe F1145-8 EM heat pump integration
-- API: myUplink API (https://api.myuplink.com/v2)
-- Device: Nibe F1145-8 EM Ground Source Heat Pump
--
-- Usage:
--   psql -U postgres -d myhab_db -f nibe-heatpump-setup.sql
--
-- Notes:
--   - This script is idempotent and can be run multiple times
--   - Deletes existing data before inserting new records
--   - Creates device with ID 1002 (Nibe Heat Pump)
--   - Configures OAuth2 authentication with myUplink API
--   - Sets up device ports for monitoring key parameters
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: Delete existing data for idempotency
-- ============================================================================
-- Note: Must delete in correct order to avoid foreign key violations

-- Delete configurations for device
DELETE FROM configurations WHERE entity_type = 'DEVICE' AND entity_id = 1002;

-- Delete device ports
DELETE FROM device_ports WHERE device_id = 1002;

-- Delete the device
DELETE FROM device_controllers WHERE id = 1002;

-- Delete category (safe now that device is gone)
DELETE FROM device_categories WHERE name = 'HEATING';

-- ============================================================================
-- STEP 2: Create HEATING device category
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
    'HEATING',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- STEP 3: Create Nibe F1145-8 EM device (ID 1002)
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
    1002,
    0,
    'NIBE_F1145_8_EM_01',
    'Nibe F1145-8 EM Heat Pump',
    'Ground source heat pump that fetches data from myUplink API',
    (SELECT id FROM device_categories WHERE name = 'HEATING'),
    'NIBE_F1145_8_EM',
    'ONLINE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- STEP 4: Configure myUplink API OAuth2 Settings
-- ============================================================================

-- OAuth Token Refresh URL
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
    1002,
    'DEVICE',
    'OAuth Token URL',
    'https://api.myuplink.com/oauth/token',
    'OAUTH_TOKEN_URL',
    'myUplink API OAuth2 token endpoint for authentication'
);

-- OAuth Client ID
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
    1002,
    'DEVICE',
    'OAuth Client ID',
    '5cda386f-68a2-425b-a099-8f8f8c9736f1',
    'cfg.key.device.oauth.client_id',
    'Client ID for myUplink API OAuth2 authentication'
);

-- OAuth Client Secret
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
    1002,
    'DEVICE',
    'OAuth Client Secret',
    '720A0D5F0ED111CFA0B4E3C450E88246',
    'cfg.key.device.oauth.client_secret',
    'Client secret for myUplink API OAuth2 authentication'
);

-- OAuth Refresh Token (initial value)
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
    1002,
    'DEVICE',
    'OAuth Refresh Token',
    '2A3FA7A8F90C1CF802C8FC09291C59843D9927081A05FF710A601A9E5BE28B02',
    'cfg.key.device.oauth.refresh_token',
    'Refresh token for myUplink API - updated automatically by the sync job'
);

-- OAuth Access Token (will be populated by the job)
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
    1002,
    'DEVICE',
    'OAuth Access Token',
    '',
    'cfg.key.device.oauth.access_token',
    'Access token for myUplink API - refreshed automatically every hour'
);

-- ============================================================================
-- STEP 5: Configure myUplink API Endpoints
-- ============================================================================

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
    1002,
    'DEVICE',
    'API Base URL',
    'https://api.myuplink.com/v2',
    'API_BASE_URL',
    'myUplink API v2 base endpoint'
);

-- Device ID (retrieved from /v2/systems/me)
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
    1002,
    'DEVICE',
    'Device ID',
    'emmy-r-78047-20241223-06511517184001-54-10-ec-11-5b-91',
    'NIBE_DEVICE_ID',
    'Nibe device identifier from myUplink API'
);

-- System ID (retrieved from /v2/systems/me)
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
    1002,
    'DEVICE',
    'System ID',
    '1aad2bfd-f442-475f-ad5c-312fe37da917',
    'NIBE_SYSTEM_ID',
    'Nibe system identifier from myUplink API'
);

-- ============================================================================
-- STEP 6: Configure API Parameter Selection
-- ============================================================================
-- Note: These parameters are selected based on real data from API response
-- Only parameters with actual values (non-null, non-zero) are included

-- Temperature Parameters (sensors with actual readings)
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
    1002,
    'DEVICE',
    'Temperature Parameters',
    '40004,40008,40012,40014,40015,40016,40017,40018,40019,40022,40033,40067,40071,43009',
    'NIBE_TEMP_PARAMS',
    'Temperature sensors with real values: outdoor(BT1), supply(BT2), return(BT3), hot water(BT6), brine in/out(BT10/BT11), condenser(BT12), discharge(BT14), liquid(BT15), suction gas(BT17), room(BT50), avg outdoor, external supply, calc supply'
);

-- Compressor Parameters (with real operational data)
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
    1002,
    'DEVICE',
    'Compressor Parameters',
    '43416,43420,43424,43427',
    'NIBE_COMPRESSOR_PARAMS',
    'Compressor parameters with real data: total starts(43416=6948), operating time(43420=21793h), hot water time(43424=3181h), status(43427)'
);

-- Energy Parameters (actual consumption and production)
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
    1002,
    'DEVICE',
    'Energy Parameters',
    '44298,44300,44306,44308,40072',
    'NIBE_ENERGY_PARAMS',
    'Energy parameters with real values: hot water incl add heat(44298=23548kWh), heating incl add heat(44300=139789kWh), hot water compressor only(44306=23502kWh), heating compressor only(44308=139393kWh), flow sensor(40072=9.7l/m)'
);

-- Status and Additional Parameters
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
    1002,
    'DEVICE',
    'Status Parameters',
    '40941',
    'NIBE_STATUS_PARAMS',
    'Status parameters with real values: degree minutes(40941)'
);

-- ============================================================================
-- STEP 7: Create device ports for key parameters (OPTIONAL)
-- ============================================================================
-- Note: Ports will be auto-created by NibeInfoSyncJob when data is received
-- This section is commented out to allow dynamic port creation
-- Uncomment if you want to pre-create ports with specific names

-- Core Temperature Sensors (Primary Dashboard Metrics)
-- INSERT INTO device_ports (id, version, device_id, internal_ref, name, description, type, ts_created, ts_updated)
-- VALUES 
--     (nextval('hibernate_sequence'), 0, 1002, '40004', 'Outdoor Temperature (BT1)', 'Current outdoor temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40008', 'Supply Line (BT2)', 'Supply line temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40012', 'Return Line (BT3)', 'Return line temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40014', 'Hot Water (BT6)', 'Hot water charging temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40015', 'Brine In (BT10)', 'Brine input temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40016', 'Brine Out (BT11)', 'Brine output temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40033', 'Room Temperature (BT50)', 'Average room temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40017', 'Condenser (BT12)', 'Condenser temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40018', 'Discharge (BT14)', 'Discharge pipe temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40019', 'Liquid (BT15)', 'Liquid line temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40022', 'Suction Gas (BT17)', 'Suction gas temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40067', 'Avg Outdoor Temp', 'Average outdoor temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40071', 'External Supply (BT25)', 'External supply line temperature (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '43009', 'Calculated Supply Temp', 'Calculated supply temperature for climate system (°C)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Compressor Parameters
-- INSERT INTO device_ports (id, version, device_id, internal_ref, name, description, type, ts_created, ts_updated)
-- VALUES 
--     (nextval('hibernate_sequence'), 0, 1002, '43416', 'Compressor Starts', 'Total number of compressor starts', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '43420', 'Compressor Operating Time', 'Total compressor operating hours (h)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '43424', 'Compressor HW Time', 'Hot water compressor operating hours (h)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '43427', 'Compressor State', 'Current compressor state/status', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Energy Parameters
-- INSERT INTO device_ports (id, version, device_id, internal_ref, name, description, type, ts_created, ts_updated)
-- VALUES 
--     (nextval('hibernate_sequence'), 0, 1002, '44298', 'Hot Water Energy Total', 'Hot water energy including additional heating (kWh)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '44300', 'Heating Energy Total', 'Heating energy including additional heating (kWh)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '44306', 'Hot Water Energy Comp', 'Hot water energy from compressor only (kWh)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '44308', 'Heating Energy Comp', 'Heating energy from compressor only (kWh)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, '40072', 'Flow Sensor', 'Heat transfer fluid flow rate (l/m)', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Status Parameters
-- INSERT INTO device_ports (id, version, device_id, internal_ref, name, description, type, ts_created, ts_updated)
-- VALUES 
--     (nextval('hibernate_sequence'), 0, 1002, '40941', 'Degree Minutes', 'Degree minutes value for heating control', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- System Info
-- INSERT INTO device_ports (id, version, device_id, internal_ref, name, description, type, ts_created, ts_updated)
-- VALUES 
--     (nextval('hibernate_sequence'), 0, 1002, 'system.connection_state', 'Connection State', 'Device connection status', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, 'system.firmware_version', 'Firmware Version', 'Current firmware version', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, 'system.desired_firmware', 'Desired Firmware', 'Target firmware version', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     (nextval('hibernate_sequence'), 0, 1002, 'system.has_alarm', 'Has Alarm', 'System alarm indicator', 'SENSOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;

-- ============================================================================
-- Verification Queries (Optional)
-- ============================================================================

-- SELECT * FROM device_categories WHERE name = 'HEATING';
-- SELECT * FROM device_controllers WHERE id = 1002;
-- SELECT * FROM configurations WHERE entity_id = 1002 ORDER BY id;
-- SELECT * FROM device_ports WHERE device_id = 1002 ORDER BY internal_ref;

-- ============================================================================
-- Summary
-- ============================================================================
-- Created:
--   - 1 device category (HEATING)
--   - 1 device (ID 1002: Nibe F1145-8 EM)
--   - 9 configurations (OAuth2, API endpoints, parameter lists)
--   - 0 device ports (auto-created by NibeInfoSyncJob on first sync)
--
-- Key Parameters (with real values from API):
--   Temperatures (14): BT1(5°C), BT2(32.5°C), BT3(32.6°C), BT6(53.3°C), 
--                      BT10(22.8°C), BT11(15.6°C), BT12(32.6°C), BT14(38.3°C),
--                      BT15(29.7°C), BT17(33.5°C), BT25(32.5°C), BT50(30°C), 
--                      Avg(5.9°C), Calc(34.7°C)
--   Compressor (4): Starts(6948), OperTime(21793h), HWTime(3181h), Status(Off)
--   Energy (5): HW Total(23548kWh), Heating Total(139789kWh), 
--               HW Comp(23502kWh), Heating Comp(139393kWh), Flow(9.7l/m)
--   Status (1): Degree Minutes(0)
--
-- Port Creation:
--   Device ports are automatically created by NibeInfoSyncJob when:
--   1. Job runs for the first time and receives data from API
--   2. Parameter is listed in NIBE_*_PARAMS configurations
--   3. Parameter has a valid value in API response
--   4. Port doesn't already exist in database
--
-- Next Steps:
--   1. Run this SQL script to create device and configurations
--   2. Ensure NibeInfoSyncJob is enabled in application.yml:
--        quartz:
--          jobs:
--            nibeTokenRefresh:
--              enabled: false  # Deprecated
--            nibeInfoSync:
--              enabled: true
--              interval: 120  # seconds
--   3. Restart application
--   4. Monitor logs for successful API sync and automatic port creation
--   5. Verify ports created: SELECT * FROM device_ports WHERE device_id = 1002;
--   6. Create/update HeatPump.vue widget to display data
-- ============================================================================

