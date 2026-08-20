-- ---------------------------------------------------------------------------
-- Hand-authored entity set for the public demo.
--
-- Entirely fictional. A demo must never publish a real installation's layout,
-- addresses or device topology.
--
-- Device codes use underscores, never hyphens. Outbound command topics are built
-- from a template, but INBOUND state topics are matched with the regex
-- myhab/(\w+|_+)/(\w+|_+)/(\w+|_+)/state (MQTTTopic.ESP). \w excludes '-', so a
-- hyphenated code publishes commands fine and then silently drops every state echo
-- the device sends back - the control appears dead with nothing in the logs.
--
-- Every controller is an ESP32. Its MQTT topics are symmetric —
--   command  myhab/<device.code>/<port.type lowercased>/<port.internal_ref>/cmd
--   state    myhab/<device.code>/<port.type lowercased>/<port.internal_ref>/state
-- — which keeps demo/simulator to a single uniform rule. MegaD's "port:value;"
-- payload format would need a second code path for no demo benefit.
--
-- Ids are fixed in the 1000-9999 band so history remapping and devices.json can
-- refer to them stably. hibernate_sequence is bumped past them at the end.
--
-- Peripheral category names must be one of LIGHT / SWITCH / HEAT / SPRINKLER /
-- TEMP: ZoneCombinedView.vue's CATEGORY_CONFIG maps exactly those to cards, and
-- anything else renders nothing.
-- ---------------------------------------------------------------------------

\set ON_ERROR_STOP on

-- --------------------------------------------------------------------------
-- Roles and users
--
-- Both accounts are published on the demo login page, so these bcrypt hashes
-- protect nothing and are intentionally in version control.
--   demo / demo             ROLE_USER
--   demo-admin / demo-admin ROLE_ADMIN
-- --------------------------------------------------------------------------
INSERT INTO sec_roles (id, version, authority) VALUES
  (1, 0, 'ROLE_USER'),
  (2, 0, 'ROLE_ADMIN');

-- users has no `version` column, unlike most BaseEntity tables.
INSERT INTO users (id, username, password, first_name, last_name, email,
                   enabled, account_locked, account_expired, password_expired,
                   ts_created, ts_updated, en_type, language, timezone)
VALUES
  (1, 'demo', '$2a$10$iaw3w8hD4PWXRvNcofFQ4eLqXn9zTp.tyMHJAPK74QkfgDFZKd0Mu',
   'Demo', 'User', 'demo@example.invalid',
   true, false, false, false, now(), now(), 'USER', 'en', 'UTC'),
  (2, 'demo-admin', '$2a$10$GW1QVj5Cw.qCJJPdL17il.WLd7vXxfj3WpIoJcZhPgUTq53jLOVOy',
   'Demo', 'Admin', 'demo-admin@example.invalid',
   true, false, false, false, now(), now(), 'USER', 'en', 'UTC');

-- Spring Security resolves authorities through the UserRole domain, which maps to
-- `sec_user_roles`. The similarly-named `users_sec_roles` is a separate legacy join
-- and writing to it leaves every account with ROLE_NO_ROLES.
INSERT INTO sec_user_roles (user_id, role_id) VALUES
  (1, 1),
  (2, 1),
  (2, 2);

-- --------------------------------------------------------------------------
-- Peripheral categories
-- --------------------------------------------------------------------------
INSERT INTO device_peripherals_categories (id, version, name, title, icon, ts_created, ts_updated, en_type) VALUES
  (2001, 0, 'LIGHT',     'Lights',      'mdi-lightbulb',        now(), now(), 'PERIPHERAL_CATEGORY'),
  (2002, 0, 'SWITCH',    'Switches',    'mdi-power-socket-eu',  now(), now(), 'PERIPHERAL_CATEGORY'),
  (2003, 0, 'HEAT',      'Heating',     'mdi-radiator',         now(), now(), 'PERIPHERAL_CATEGORY'),
  (2004, 0, 'SPRINKLER', 'Sprinklers',  'mdi-sprinkler',        now(), now(), 'PERIPHERAL_CATEGORY'),
  (2005, 0, 'TEMP',      'Temperature', 'mdi-thermometer',      now(), now(), 'PERIPHERAL_CATEGORY');

-- --------------------------------------------------------------------------
-- Zones
-- --------------------------------------------------------------------------
INSERT INTO zones (id, version, name, description, parent_id, ts_created, ts_updated, en_type) VALUES
  (1000, 0, 'Demo Home',   'A sample home used by the myHAB demo', NULL, now(), now(), 'ZONE'),
  (1001, 0, 'Living Room', 'Main living area',                     1000, now(), now(), 'ZONE'),
  (1002, 0, 'Kitchen',     'Kitchen and dining',                   1000, now(), now(), 'ZONE'),
  (1003, 0, 'Terrace',     'Outdoor terrace and garden',           1000, now(), now(), 'ZONE'),
  (1004, 0, 'Garage',      'Garage and garden shed',               1000, now(), now(), 'ZONE');

-- --------------------------------------------------------------------------
-- Controllers
--
-- esp_shed is ONLINE-by-omission nowhere: it is seeded OFFLINE on purpose so the
-- demo shows what an unreachable device looks like (red badge, disabled toggle)
-- without blocking anything a visitor would want to interact with. The simulator
-- likewise never answers for it. Every other device must stay ONLINE, or its
-- peripherals' controls render disabled.
-- --------------------------------------------------------------------------
INSERT INTO device_controllers (id, version, code, name, description, model, status,
                                ts_created, ts_updated, en_type)
VALUES
  (3001, 0, 'esp_indoor',  'Indoor controller',  'ESP32 covering the living room and kitchen', 'ESP32', 'ONLINE',  now(), now(), 'DEVICE'),
  (3002, 0, 'esp_outdoor', 'Outdoor controller', 'ESP32 covering the terrace and garden',      'ESP32', 'ONLINE',  now(), now(), 'DEVICE'),
  (3003, 0, 'esp_garage',  'Garage controller',  'ESP32 in the garage',                        'ESP32', 'ONLINE',  now(), now(), 'DEVICE'),
  (3004, 0, 'esp_shed',    'Shed controller',    'ESP32 in the garden shed - intentionally offline in the demo', 'ESP32', 'OFFLINE', now(), now(), 'DEVICE');

INSERT INTO zones_devices_join (zone_id, device_id) VALUES
  (1001, 3001), (1002, 3001), (1003, 3002), (1004, 3003), (1004, 3004);

-- --------------------------------------------------------------------------
-- Ports
--
-- internal_ref is the MQTT port code; type lowercased is the topic segment.
--
-- Two different columns, easily confused:
--   value  the live reading. For switchable ports 'ON'/'OFF' verbatim, which is
--          what the peripheral cards compare against
--          (PeripheralSwitchCard.vue: portValue === 'ON').
--   state  a PortState enum (UNKNOW/CONFIGURED/ACTIVE/INACTIVE) describing how the
--          port is provisioned - NOT whether it is currently on.
-- --------------------------------------------------------------------------
INSERT INTO device_ports (id, version, device_id, internal_ref, name, type, state, value,
                          ts_created, ts_updated, en_type)
VALUES
  -- Living room (esp_indoor)
  (4001, 0, 3001, 'lr_ceiling',   'Living room ceiling',  'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4002, 0, 3001, 'lr_lamp',      'Living room lamp',     'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4003, 0, 3001, 'lr_tv',        'TV socket',            'SWITCH', 'ACTIVE', 'ON',   now(), now(), 'PORT'),
  (4004, 0, 3001, 'lr_heat',      'Living room radiator', 'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4005, 0, 3001, 'lr_temp',      'Living room temp',     'SENSOR', 'ACTIVE', '21.4', now(), now(), 'PORT'),
  -- Kitchen (esp_indoor)
  (4006, 0, 3001, 'kt_light',     'Kitchen light',        'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4007, 0, 3001, 'kt_coffee',    'Coffee machine',       'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4008, 0, 3001, 'kt_temp',      'Kitchen temp',         'SENSOR', 'ACTIVE', '22.1', now(), now(), 'PORT'),
  -- Terrace (esp_outdoor)
  (4009, 0, 3002, 'tr_light',     'Terrace light',        'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4010, 0, 3002, 'tr_garden',    'Garden lights',        'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4011, 0, 3002, 'tr_sprinkler', 'Lawn sprinkler',       'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4012, 0, 3002, 'tr_pump',      'Water pump',           'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4013, 0, 3002, 'tr_temp',      'Outdoor temp',         'SENSOR', 'ACTIVE', '14.8', now(), now(), 'PORT'),
  -- Garage (esp_garage)
  (4014, 0, 3003, 'gr_light',     'Garage light',         'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4015, 0, 3003, 'gr_temp',      'Garage temp',          'SENSOR', 'ACTIVE', '16.2', now(), now(), 'PORT'),
  -- Shed (esp_shed, offline showcase)
  (4016, 0, 3004, 'sh_light',     'Shed light',           'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT');

-- --------------------------------------------------------------------------
-- Peripherals
-- --------------------------------------------------------------------------
INSERT INTO device_peripherals (id, version, name, description, category_id, max_amp,
                                ts_created, ts_updated, en_type)
VALUES
  (5001, 0, 'Ceiling Light',    'Living room ceiling light',  2001, 0.5,  now(), now(), 'PERIPHERAL'),
  (5002, 0, 'Floor Lamp',       'Living room floor lamp',     2001, 0.3,  now(), now(), 'PERIPHERAL'),
  (5003, 0, 'TV Socket',        'Smart socket behind the TV', 2002, 2.0,  now(), now(), 'PERIPHERAL'),
  (5004, 0, 'Radiator',         'Living room radiator valve', 2003, 0.2,  now(), now(), 'PERIPHERAL'),
  (5005, 0, 'Living Room Temp', 'Living room temperature',    2005, 0.01, now(), now(), 'PERIPHERAL'),
  (5006, 0, 'Kitchen Light',    'Kitchen ceiling light',      2001, 0.6,  now(), now(), 'PERIPHERAL'),
  (5007, 0, 'Coffee Machine',   'Kitchen coffee machine',     2002, 6.0,  now(), now(), 'PERIPHERAL'),
  (5008, 0, 'Kitchen Temp',     'Kitchen temperature',        2005, 0.01, now(), now(), 'PERIPHERAL'),
  (5009, 0, 'Terrace Light',    'Terrace wall light',         2001, 0.4,  now(), now(), 'PERIPHERAL'),
  (5010, 0, 'Garden Lights',    'Garden path lights',         2001, 0.8,  now(), now(), 'PERIPHERAL'),
  (5011, 0, 'Lawn Sprinkler',   'Lawn sprinkler valve',       2004, 1.0,  now(), now(), 'PERIPHERAL'),
  (5012, 0, 'Water Pump',       'Garden water pump',          2002, 4.5,  now(), now(), 'PERIPHERAL'),
  (5013, 0, 'Outdoor Temp',     'Outdoor temperature',        2005, 0.01, now(), now(), 'PERIPHERAL'),
  (5014, 0, 'Garage Light',     'Garage ceiling light',       2001, 0.5,  now(), now(), 'PERIPHERAL'),
  (5015, 0, 'Garage Temp',      'Garage temperature',         2005, 0.01, now(), now(), 'PERIPHERAL'),
  (5016, 0, 'Shed Light',       'Garden shed light',          2001, 0.3,  now(), now(), 'PERIPHERAL');

INSERT INTO device_ports_peripherals_join (port_id, peripheral_id) VALUES
  (4001, 5001), (4002, 5002), (4003, 5003), (4004, 5004), (4005, 5005),
  (4006, 5006), (4007, 5007), (4008, 5008),
  (4009, 5009), (4010, 5010), (4011, 5011), (4012, 5012), (4013, 5013),
  (4014, 5014), (4015, 5015),
  (4016, 5016);

INSERT INTO zones_peripherals_join (zone_id, peripheral_id) VALUES
  (1001, 5001), (1001, 5002), (1001, 5003), (1001, 5004), (1001, 5005),
  (1002, 5006), (1002, 5007), (1002, 5008),
  (1003, 5009), (1003, 5010), (1003, 5011), (1003, 5012), (1003, 5013),
  (1004, 5014), (1004, 5015), (1004, 5016);

-- --------------------------------------------------------------------------
-- Zone categories
--
-- Drives which card groups a zone renders in ZoneCombinedView.
-- --------------------------------------------------------------------------
INSERT INTO zones_categories (zone_id, categories_string) VALUES
  (1001, 'LIGHT'), (1001, 'SWITCH'), (1001, 'HEAT'), (1001, 'TEMP'),
  (1002, 'LIGHT'), (1002, 'SWITCH'), (1002, 'TEMP'),
  (1003, 'LIGHT'), (1003, 'SWITCH'), (1003, 'SPRINKLER'), (1003, 'TEMP'),
  (1004, 'LIGHT'), (1004, 'TEMP');

-- --------------------------------------------------------------------------
-- Auto-off timeouts
--
-- Gives the demo something to show for SwitchOFFOnTimeoutJob: the sprinkler and
-- the pump switch themselves off again, which is real behaviour rather than a
-- scripted animation.
-- --------------------------------------------------------------------------
INSERT INTO configurations (id, version, entity_id, entity_type, key, value, name, description) VALUES
  (6001, 0, 5011, 'PERIPHERAL', 'key.on.timeout', '120', 'key.on.timeout', 'Auto-off after 2 minutes'),
  (6002, 0, 5012, 'PERIPHERAL', 'key.on.timeout', '300', 'key.on.timeout', 'Auto-off after 5 minutes');

-- --------------------------------------------------------------------------
-- Dashboard widget visibility, per user.
--
-- Per-user UI preferences piggyback on the Configuration table
-- (entity_type='USER', entity_id=<user id>) - see user-prefs.store.js. The demo
-- has no solar inverter, heat pump, weather station, mower or gate, and those
-- widgets would otherwise render as permanently "Offline", which reads as a
-- broken product rather than an absent device. Hide them; the remaining
-- quick-access tiles all resolve to real demo zones via specialZones.* in
-- demo/config/config.yaml.
-- --------------------------------------------------------------------------
INSERT INTO configurations (id, version, entity_id, entity_type, key, value, name, description) VALUES
  (6101, 0, 1, 'USER', 'ui.dashboard.widgets.hidden',
   'peripheral_lock,sprinklers,water_pump,meteo_station,solar_plant,heat_pump,navimow',
   'ui.dashboard.widgets.hidden', 'Widgets with no backing device in the demo'),
  (6102, 0, 2, 'USER', 'ui.dashboard.widgets.hidden',
   'peripheral_lock,sprinklers,water_pump,meteo_station,solar_plant,heat_pump,navimow',
   'ui.dashboard.widgets.hidden', 'Widgets with no backing device in the demo');

-- --------------------------------------------------------------------------
-- Keep generated ids clear of the fixed band above.
-- --------------------------------------------------------------------------
SELECT setval('hibernate_sequence', 10000, false);
