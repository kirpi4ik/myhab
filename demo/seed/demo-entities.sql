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

-- The application connects with TimeZone=UTC, so every timestamp it writes is
-- UTC. psql inherits the server's zone instead, which would store these rows in
-- local wall time - three hours in the future on a UTC+3 host, which the UI then
-- renders as messages that arrive later today. Match the application.
SET timezone = 'UTC';
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
   'peripheral_lock,water_pump,meteo_station,solar_plant,heat_pump,navimow',
   'ui.dashboard.widgets.hidden', 'Widgets with no backing device in the demo'),
  (6102, 0, 2, 'USER', 'ui.dashboard.widgets.hidden',
   'peripheral_lock,water_pump,meteo_station,solar_plant,heat_pump,navimow',
   'ui.dashboard.widgets.hidden', 'Widgets with no backing device in the demo');

-- --------------------------------------------------------------------------
-- Irrigation and upper-floor zones
--
-- Lawn and Garden exist because the sprinkler dashboard card routes to
-- specialZones.lan.id / specialZones.garden.id; without them the card links to
-- /zones/null and the zone page answers "Zone not found". Upper Floor gives
-- specialZones.etaj a zone of its own instead of aliasing the kitchen.
-- --------------------------------------------------------------------------
INSERT INTO zones (id, version, name, description, parent_id, ts_created, ts_updated, en_type) VALUES
  (1005, 0, 'Lawn',        'Front lawn irrigation',   1000, now(), now(), 'ZONE'),
  (1006, 0, 'Garden',      'Vegetable garden beds',   1000, now(), now(), 'ZONE'),
  (1007, 0, 'Upper Floor', 'Bedrooms, office, bath',  1000, now(), now(), 'ZONE');

INSERT INTO device_ports (id, version, device_id, internal_ref, name, type, state, value,
                          ts_created, ts_updated, en_type)
VALUES
  -- Garden irrigation line (esp_outdoor); the lawn already has tr_sprinkler.
  (4017, 0, 3002, 'tr_sprinkler_gd', 'Garden sprinkler', 'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  -- Upper floor (esp_indoor)
  (4018, 0, 3001, 'uf_office',       'Office light',     'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4019, 0, 3001, 'uf_bed',          'Bedroom light',    'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4020, 0, 3001, 'uf_bath',         'Bathroom light',   'SWITCH', 'ACTIVE', 'OFF',  now(), now(), 'PORT'),
  (4021, 0, 3001, 'uf_temp',         'Upper floor temp', 'SENSOR', 'ACTIVE', '20.8', now(), now(), 'PORT');

INSERT INTO device_peripherals (id, version, name, description, category_id, max_amp,
                                ts_created, ts_updated, en_type)
VALUES
  (5017, 0, 'Garden Sprinkler', 'Vegetable garden valve',  2004, 1.0,  now(), now(), 'PERIPHERAL'),
  (5018, 0, 'Office Light',     'Upper floor office',      2001, 0.4,  now(), now(), 'PERIPHERAL'),
  (5019, 0, 'Bedroom Light',    'Master bedroom',          2001, 0.4,  now(), now(), 'PERIPHERAL'),
  (5020, 0, 'Bathroom Light',   'Upper floor bathroom',    2001, 0.3,  now(), now(), 'PERIPHERAL'),
  (5021, 0, 'Upper Floor Temp', 'Upper floor temperature', 2005, 0.01, now(), now(), 'PERIPHERAL');

INSERT INTO device_ports_peripherals_join (port_id, peripheral_id) VALUES
  (4017, 5017), (4018, 5018), (4019, 5019), (4020, 5020), (4021, 5021);

INSERT INTO zones_devices_join (zone_id, device_id) VALUES
  (1005, 3002), (1006, 3002), (1007, 3001);

-- The garden valve stays visible on the Terrace as well: a peripheral belongs to
-- as many zones as it serves, and the terrace page is where a visitor meets it.
INSERT INTO zones_peripherals_join (zone_id, peripheral_id) VALUES
  (1003, 5017),
  (1005, 5011),
  (1006, 5017),
  (1007, 5018), (1007, 5019), (1007, 5020), (1007, 5021);

INSERT INTO zones_categories (zone_id, categories_string) VALUES
  (1005, 'SPRINKLER'),
  (1006, 'SPRINKLER'),
  (1007, 'LIGHT'), (1007, 'TEMP');

-- Same auto-off as the lawn valve, so the garden line also shuts itself again.
INSERT INTO configurations (id, version, entity_id, entity_type, key, value, name, description) VALUES
  (6003, 0, 5017, 'PERIPHERAL', 'key.on.timeout', '120', 'key.on.timeout', 'Auto-off after 2 minutes');

-- --------------------------------------------------------------------------
-- Rack, patch panel and cabling
--
-- Enough structure for the Cables pages to show something other than empty
-- columns: every cable has a category and a patch-panel port, and carries the
-- ports, peripherals and zones it serves.
-- --------------------------------------------------------------------------
INSERT INTO racks (id, version, name, description, zone_id, ts_created, ts_updated, en_type) VALUES
  (6201, 0, 'Main rack', '10U wall rack in the garage', 1004, now(), now(), 'RACK');

-- `code` is a legacy NOT NULL column with no matching property on PatchPanel.
INSERT INTO rack_patch_panels (id, version, rack_id, code, name, description, size,
                               ts_created, ts_updated, en_type) VALUES
  (6211, 0, 6201, 'PP1', 'Patch panel 1', 'Ground floor distribution', 24, now(), now(), 'PATCH_PANEL');

INSERT INTO cable_categories (id, version, name, ts_created, ts_updated, en_type) VALUES
  (6221, 0, 'Power', now(), now(), 'CABLE_CATEGORY'),
  (6222, 0, 'Data',  now(), now(), 'CABLE_CATEGORY');

INSERT INTO cables (id, version, code, code_new, code_old, description, nr_wires, max_amp,
                    category_id, rack_id, patch_panel_id, patch_panel_port,
                    rack_row_nr, order_in_row, ts_created, ts_updated, en_type)
VALUES
  (6231, 0, 'C-01', NULL, NULL, 'Living room ceiling light drop',   3, 10.0, 6221, 6201, 6211, '01', 1, 1, now(), now(), 'CABLE'),
  (6232, 0, 'C-02', NULL, NULL, 'Kitchen light and coffee spur',    3, 16.0, 6221, 6201, 6211, '02', 1, 2, now(), now(), 'CABLE'),
  (6233, 0, 'C-03', NULL, NULL, 'Terrace run to the outdoor valve', 8,  1.0, 6222, 6201, 6211, '03', 2, 1, now(), now(), 'CABLE');

INSERT INTO device_ports_cables_join (cable_id, port_id) VALUES
  (6231, 4001),
  (6232, 4006), (6232, 4007),
  (6233, 4011);

INSERT INTO cables_peripherals_join (cable_id, peripheral_id) VALUES
  (6231, 5001),
  (6232, 5006), (6232, 5007),
  (6233, 5011);

INSERT INTO zones_cables_join (cable_id, zone_id) VALUES
  (6231, 1001),
  (6232, 1002),
  (6233, 1003), (6233, 1005);

-- --------------------------------------------------------------------------
-- Scenarios
--
-- Bodies are the scenario DSL (see DslService): bare method names resolve through
-- knowledgeService (predicates) first, then scenarioService (actions). Both are
-- written against peripheral ids, so they survive a port being rewired.
--
-- isRaining() returns false when there is neither a rain sensor nor meteo data,
-- which is the demo's situation - the guard is there to be read, not to block.
-- --------------------------------------------------------------------------
INSERT INTO scenarios (id, version, name, body, ts_created, ts_updated, en_type) VALUES
  (7001, 0, 'Evening lights', $scn$
// Living room ceiling and terrace wall light follow dusk.
if (isEvening()) {
  switchOn([peripheralIds: [5001, 5009]])
} else {
  switchOff([peripheralIds: [5001, 5009]])
}
$scn$, now(), now(), 'SCENARIO'),
  (7002, 0, 'Irrigation cycle', $scn$
// Lawn first, then the garden beds. `timeout` is a one-shot auto-off in seconds,
// so a lost OFF command cannot leave a valve open.
if (!isRaining()) {
  switchOn([peripheralIds: [5011], timeout: 120])
  pause(2000)
  switchOn([peripheralIds: [5017], timeout: 120])
}
$scn$, now(), now(), 'SCENARIO');

-- --------------------------------------------------------------------------
-- Scheduled job
--
-- ACTIVE means the scheduler picks it up, so these two triggers really do fire in
-- the demo. `peripheral_id` is what puts the job on the sprinkler schedule page.
-- Quartz cron expressions carry a leading seconds field.
-- --------------------------------------------------------------------------
INSERT INTO jobs (id, version, name, description, scenario_id, state, peripheral_id,
                  ts_created, ts_updated, en_type) VALUES
  (7101, 0, 'Irrigation schedule', 'Runs the irrigation cycle morning and evening',
   7002, 'ACTIVE', 5011, now(), now(), 'JOB');

INSERT INTO job_triggers_cron (id, version, job_id, expression, description,
                               cron_triggers_idx, ts_created, ts_updated, en_type) VALUES
  (7111, 0, 7101, '0 30 6 * * ?', 'Morning cycle at 06:30', 0, now(), now(), NULL),
  (7112, 0, 7101, '0 0 20 * * ?', 'Evening cycle at 20:00', 1, now(), now(), NULL);

INSERT INTO jobs_tags (id, version, name) VALUES
  (7121, 0, 'irrigation');

INSERT INTO jobs_tags_join (job_id, tag_id) VALUES
  (7101, 7121);

-- --------------------------------------------------------------------------
-- Inbox messages
--
-- Both accounts get the same set, so whichever one a visitor logs in as has an
-- inbox holding unread, read and archived mail. Timestamps are relative because
-- DemoService shifts every restored timestamp by (now - seed built_at): they stay
-- minutes-to-days old however long the seed has been sitting in the image.
-- --------------------------------------------------------------------------
INSERT INTO user_messages (id, version, user_id, subject, from_sender, message, level, state,
                           dedup_key, ts_created, ts_updated)
VALUES
  -- demo / NEW
  (7201, 0, 1, 'Welcome to the myHAB demo', 'myHAB',
   'This is a public sandbox. Switch anything on or off - the dataset resets itself periodically.',
   'INFO', 'NEW', NULL, now() - interval '12 minutes', now() - interval '12 minutes'),
  (7202, 0, 1, 'Shed controller is offline', 'Device monitor',
   'esp_shed has not reported for over an hour. Its light control stays disabled until it reconnects.',
   'WARN', 'NEW', 'device.esp_shed.offline', now() - interval '48 minutes', now() - interval '48 minutes'),
  (7203, 0, 1, 'Irrigation cycle finished', 'Scheduler',
   'The lawn sprinkler ran for 2 minutes and switched off on its auto-off timeout.',
   'INFO', 'NEW', 'job.7101.finished', now() - interval '3 hours', now() - interval '3 hours'),
  -- demo / READ
  (7204, 0, 1, 'Kitchen temperature back to normal', 'Rules engine',
   'Kitchen temperature returned to 22.1 C after peaking at 25.0 C.',
   'INFO', 'READ', NULL, now() - interval '1 day', now() - interval '22 hours'),
  (7205, 0, 1, 'Outdoor temperature below 5 C', 'Rules engine',
   'Outdoor temperature dropped to 4.2 C. Irrigation is skipped while there is a frost risk.',
   'WARN', 'READ', 'meteo.outdoor.frost', now() - interval '2 days', now() - interval '2 days'),
  -- demo / ARCHIVE
  (7206, 0, 1, 'Water pump did not confirm', 'Device monitor',
   'The water pump was switched on but sent no state echo within 10 seconds. It was switched off again.',
   'ERROR', 'ARCHIVE', 'peripheral.5012.no_confirm', now() - interval '6 days', now() - interval '5 days'),
  (7207, 0, 1, 'Nightly maintenance completed', 'Maintenance',
   'Statistics were aggregated and old port values pruned.',
   'INFO', 'ARCHIVE', NULL, now() - interval '8 days', now() - interval '8 days'),
  -- demo-admin / NEW
  (7211, 0, 2, 'Welcome to the myHAB demo', 'myHAB',
   'You are signed in as an administrator: devices, zones, scenarios and jobs are all editable.',
   'INFO', 'NEW', NULL, now() - interval '12 minutes', now() - interval '12 minutes'),
  (7212, 0, 2, 'Shed controller is offline', 'Device monitor',
   'esp_shed has not reported for over an hour. Its light control stays disabled until it reconnects.',
   'WARN', 'NEW', 'device.esp_shed.offline', now() - interval '48 minutes', now() - interval '48 minutes'),
  (7213, 0, 2, 'Irrigation cycle finished', 'Scheduler',
   'The lawn sprinkler ran for 2 minutes and switched off on its auto-off timeout.',
   'INFO', 'NEW', 'job.7101.finished', now() - interval '3 hours', now() - interval '3 hours'),
  -- demo-admin / READ
  (7214, 0, 2, 'Kitchen temperature back to normal', 'Rules engine',
   'Kitchen temperature returned to 22.1 C after peaking at 25.0 C.',
   'INFO', 'READ', NULL, now() - interval '1 day', now() - interval '22 hours'),
  (7215, 0, 2, 'Outdoor temperature below 5 C', 'Rules engine',
   'Outdoor temperature dropped to 4.2 C. Irrigation is skipped while there is a frost risk.',
   'WARN', 'READ', 'meteo.outdoor.frost', now() - interval '2 days', now() - interval '2 days'),
  -- demo-admin / ARCHIVE
  (7216, 0, 2, 'Water pump did not confirm', 'Device monitor',
   'The water pump was switched on but sent no state echo within 10 seconds. It was switched off again.',
   'ERROR', 'ARCHIVE', 'peripheral.5012.no_confirm', now() - interval '6 days', now() - interval '5 days'),
  (7217, 0, 2, 'Nightly maintenance completed', 'Maintenance',
   'Statistics were aggregated and old port values pruned.',
   'INFO', 'ARCHIVE', NULL, now() - interval '8 days', now() - interval '8 days');

-- --------------------------------------------------------------------------
-- Keep generated ids clear of the fixed band above.
-- --------------------------------------------------------------------------
SELECT setval('hibernate_sequence', 10000, false);
