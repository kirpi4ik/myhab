--
-- Database schema for the public demo.
--
-- A `pg_dump --schema-only` snapshot, tracked here because the demo has to be able to
-- build its database from a clean checkout, and inside the myhab-demo image.
--
-- It carries the things GORM's dbCreate=update cannot create for itself: the eleven
-- qrtz_* tables Quartz's JobStoreTX requires, the RANGE-partitioned event_log and
-- port_values, and the archive schema.
--
-- Contains DDL only - no rows, no owners, no grants. Regenerate it with
-- `pg_dump --schema-only` against an up-to-date database when the domain model
-- changes; dbCreate=update covers simple additions in the meantime.
--

--
-- PostgreSQL database dump
--

\restrict oDyDUg6ToqQIMsMtw2japAJ2KTrqOaFCl3abMBVJGys2eCkg00FGYgH77C6btUd

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: archive; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA archive;


--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: partition_archive_log; Type: TABLE; Schema: archive; Owner: -
--

CREATE TABLE archive.partition_archive_log (
    id integer NOT NULL,
    table_name text NOT NULL,
    partition_name text NOT NULL,
    archive_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archived_by text DEFAULT CURRENT_USER,
    row_count bigint,
    date_range_start timestamp without time zone,
    date_range_end timestamp without time zone,
    notes text
);


--
-- Name: partition_archive_log_id_seq; Type: SEQUENCE; Schema: archive; Owner: -
--

CREATE SEQUENCE archive.partition_archive_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: partition_archive_log_id_seq; Type: SEQUENCE OWNED BY; Schema: archive; Owner: -
--

ALTER SEQUENCE archive.partition_archive_log_id_seq OWNED BY archive.partition_archive_log.id;


--
-- Name: port_values_2019; Type: TABLE; Schema: archive; Owner: -
--

CREATE TABLE archive.port_values_2019 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2020; Type: TABLE; Schema: archive; Owner: -
--

CREATE TABLE archive.port_values_2020 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2021; Type: TABLE; Schema: archive; Owner: -
--

CREATE TABLE archive.port_values_2021 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2022; Type: TABLE; Schema: archive; Owner: -
--

CREATE TABLE archive.port_values_2022 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2023; Type: TABLE; Schema: archive; Owner: -
--

CREATE TABLE archive.port_values_2023 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
)
WITH (fillfactor='90');


--
-- Name: port_values_archived; Type: VIEW; Schema: archive; Owner: -
--

CREATE VIEW archive.port_values_archived AS
 SELECT port_values_2019.id,
    port_values_2019.ts_updated,
    port_values_2019.value,
    port_values_2019.ts_created,
    port_values_2019.en_type,
    port_values_2019.event_id,
    port_values_2019.port_id
   FROM archive.port_values_2019
UNION ALL
 SELECT port_values_2020.id,
    port_values_2020.ts_updated,
    port_values_2020.value,
    port_values_2020.ts_created,
    port_values_2020.en_type,
    port_values_2020.event_id,
    port_values_2020.port_id
   FROM archive.port_values_2020
UNION ALL
 SELECT port_values_2021.id,
    port_values_2021.ts_updated,
    port_values_2021.value,
    port_values_2021.ts_created,
    port_values_2021.en_type,
    port_values_2021.event_id,
    port_values_2021.port_id
   FROM archive.port_values_2021
UNION ALL
 SELECT port_values_2022.id,
    port_values_2022.ts_updated,
    port_values_2022.value,
    port_values_2022.ts_created,
    port_values_2022.en_type,
    port_values_2022.event_id,
    port_values_2022.port_id
   FROM archive.port_values_2022
UNION ALL
 SELECT port_values_2023.id,
    port_values_2023.ts_updated,
    port_values_2023.value,
    port_values_2023.ts_created,
    port_values_2023.en_type,
    port_values_2023.event_id,
    port_values_2023.port_id
   FROM archive.port_values_2023;


--
-- Name: access_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_token (
    id bigint NOT NULL,
    expiration timestamp without time zone NOT NULL,
    authentication bytea NOT NULL,
    authentication_key character varying(255) NOT NULL,
    username character varying(255),
    refresh_token character varying(255),
    value character varying(255) NOT NULL,
    token_type character varying(255) NOT NULL,
    client_id character varying(255) NOT NULL
);


--
-- Name: access_token_additional_information; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_token_additional_information (
    access_token_id bigint,
    additional_information_object character varying(255),
    additional_information_idx character varying(255),
    additional_information_elt character varying(255) CONSTRAINT access_token_additional_inf_additional_information_elt_not_null NOT NULL
);


--
-- Name: access_token_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_token_scope (
    access_token_id bigint NOT NULL,
    scope_string character varying(255)
);


--
-- Name: authorization_code; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authorization_code (
    id bigint NOT NULL,
    authentication bytea NOT NULL,
    code character varying(255) NOT NULL
);


--
-- Name: cable_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cable_categories (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone,
    name character varying(255) NOT NULL,
    en_type character varying(255),
    ts_created timestamp without time zone
);


--
-- Name: cables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cables (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    nr_wires integer NOT NULL,
    code_old character varying(255),
    code_new character varying(255),
    rack_id bigint,
    category_id bigint,
    max_amp double precision NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255),
    code character varying(255),
    en_type character varying(255),
    patch_panel_port character varying(255),
    patch_panel_id bigint,
    rack_row_nr integer,
    order_in_row integer
);


--
-- Name: cables_peripherals_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cables_peripherals_join (
    cable_id bigint NOT NULL,
    device_peripheral_id bigint,
    peripheral_id bigint NOT NULL
);


--
-- Name: client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client (
    id bigint NOT NULL,
    version bigint NOT NULL,
    client_secret character varying(255),
    access_token_validity_seconds integer,
    refresh_token_validity_seconds integer,
    client_id character varying(255) NOT NULL
);


--
-- Name: client_additional_information; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_additional_information (
    client_id bigint,
    additional_information_object character varying(255),
    additional_information_idx character varying(255),
    additional_information_elt character varying(255) CONSTRAINT client_additional_informati_additional_information_elt_not_null NOT NULL
);


--
-- Name: client_authorities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_authorities (
    client_id bigint NOT NULL,
    authorities_string character varying(255)
);


--
-- Name: client_authorized_grant_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_authorized_grant_types (
    client_id bigint NOT NULL,
    authorized_grant_types_string character varying(255)
);


--
-- Name: client_auto_approve_scopes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_auto_approve_scopes (
    client_id bigint NOT NULL,
    auto_approve_scopes_string character varying(255)
);


--
-- Name: client_redirect_uris; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_redirect_uris (
    client_id bigint NOT NULL,
    redirect_uris_string character varying(255)
);


--
-- Name: client_resource_ids; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_resource_ids (
    client_id bigint NOT NULL,
    resource_ids_string character varying(255)
);


--
-- Name: client_scopes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client_scopes (
    client_id bigint NOT NULL,
    scopes_string character varying(255)
);


--
-- Name: configurations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.configurations (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255),
    value text NOT NULL,
    entity_type character varying(255) NOT NULL,
    key character varying(255) NOT NULL,
    description character varying(255),
    entity_id bigint NOT NULL
);


--
-- Name: dashboard_screen_backgrounds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_screen_backgrounds (
    id bigint NOT NULL,
    version bigint NOT NULL,
    screen_id bigint NOT NULL,
    data bytea NOT NULL
);


--
-- Name: dashboard_screens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dashboard_screens (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone,
    en_type character varying(255),
    layout_json text,
    background_content_type character varying(255),
    background_height integer,
    ordinal integer NOT NULL,
    name character varying(255) NOT NULL,
    background_width integer,
    ts_created timestamp without time zone,
    enabled boolean NOT NULL
);


--
-- Name: device_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_accounts (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    username character varying(255),
    password character varying(255),
    is_default boolean,
    device_id bigint NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255)
);


--
-- Name: device_backup; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_backup (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone,
    device_id bigint NOT NULL,
    configuration character varying(255),
    en_type character varying(255),
    frm_version character varying(255) NOT NULL,
    firmware oid,
    ts_created timestamp without time zone,
    backups_idx integer
);


--
-- Name: device_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_categories (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255) NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255)
);


--
-- Name: device_controllers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_controllers (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    offline_scenario character varying(255),
    code character varying(255) NOT NULL,
    rack_id bigint,
    name character varying(255),
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255),
    model character varying(255),
    network_address_port character varying(255),
    network_address_gateway character varying(255),
    network_address_ip character varying(255),
    en_type character varying(255),
    type_id bigint,
    status character varying(255)
);


--
-- Name: device_peripherals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_peripherals (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255),
    category_id bigint NOT NULL,
    max_amp double precision NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255),
    model character varying(255),
    en_type character varying(255)
);


--
-- Name: device_peripherals_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_peripherals_categories (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255) NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    title character varying(255),
    icon character varying(255)
);


--
-- Name: device_peripherals_categories_cables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_peripherals_categories_cables (
    peripheral_category_cables_id bigint CONSTRAINT device_peripherals_categori_peripheral_category_cables_not_null NOT NULL,
    cable_id bigint
);


--
-- Name: device_ports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_ports (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    run_action boolean,
    internal_ref character varying(255),
    state character varying(255),
    name character varying(255),
    device_id bigint NOT NULL,
    value text,
    action character varying(255),
    must_send_to_server boolean,
    type character varying(255),
    mode character varying(255),
    ts_created timestamp(6) without time zone NOT NULL,
    misc_value character varying(255),
    description character varying(255),
    model character varying(255),
    hyst_deviation_value character varying(255),
    run_scenario boolean,
    en_type character varying(255)
);


--
-- Name: device_ports_cables_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_ports_cables_join (
    port_id bigint NOT NULL,
    cable_id bigint
);


--
-- Name: device_ports_peripherals_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_ports_peripherals_join (
    port_id bigint NOT NULL,
    peripheral_id bigint NOT NULL
);


--
-- Name: device_ports_scenarios_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_ports_scenarios_join (
    scenario_id bigint NOT NULL,
    port_id bigint NOT NULL
);


--
-- Name: device_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_types (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255) NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255)
);


--
-- Name: event_definitions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_definitions (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    has_publisher boolean NOT NULL,
    name character varying(255) NOT NULL,
    has_subscriber boolean NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255)
);


--
-- Name: event_definitions_subscriptions_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_definitions_subscriptions_join (
    subscription_id bigint NOT NULL,
    event_definition_id bigint
);


--
-- Name: event_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_log (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    p2 character varying(255) NOT NULL,
    p3 character varying(255) NOT NULL,
    p4 character varying(255),
    p5 character varying(255),
    category character varying(255),
    p6 character varying(255),
    p1 character varying(255) NOT NULL,
    p0 character varying(255) NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255) NOT NULL,
    action_id character varying(255)
);


--
-- Name: event_subscriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event_subscriptions (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    scenario_id bigint NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    pub_port_id bigint NOT NULL,
    en_type character varying(255)
);


--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: job_execution_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_execution_history (
    id bigint NOT NULL,
    ts_updated timestamp without time zone,
    scheduled_fire_time timestamp without time zone,
    duration_ms bigint,
    end_time timestamp without time zone,
    trigger_group character varying(255),
    job_id bigint,
    fire_instance_id character varying(255),
    recovering boolean,
    actual_fire_time timestamp without time zone,
    error_message text,
    start_time timestamp without time zone,
    job_name character varying(255) NOT NULL,
    trigger_name character varying(255),
    refire_count integer,
    job_group character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    ts_created timestamp without time zone,
    exception_class character varying(500)
);


--
-- Name: job_triggers_cron; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_triggers_cron (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    expression character varying(255) NOT NULL,
    job_id bigint NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    cron_triggers_idx integer,
    en_type character varying(255),
    description character varying(500)
);


--
-- Name: job_triggers_event_definitions_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_triggers_event_definitions_join (
    trigger_id bigint NOT NULL,
    event_definition_id bigint
);


--
-- Name: job_triggers_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_triggers_events (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    job_id bigint NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    event_triggers_idx integer,
    en_type character varying(255) NOT NULL
);


--
-- Name: jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobs (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255) NOT NULL,
    scenario_id bigint NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255) NOT NULL,
    state character varying(255),
    en_type character varying(255),
    peripheral_id bigint
);


--
-- Name: jobs_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobs_tags (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);


--
-- Name: jobs_tags_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobs_tags_join (
    job_id bigint NOT NULL,
    tag_id bigint NOT NULL
);


--
-- Name: layers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.layers (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255) NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255) NOT NULL,
    en_type character varying(255) NOT NULL
);


--
-- Name: layers_peripherals_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.layers_peripherals_join (
    layer_id bigint NOT NULL,
    device_peripheral_id bigint
);


--
-- Name: mqtt_topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mqtt_topics (
    id bigint NOT NULL,
    version bigint NOT NULL,
    regex character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);


--
-- Name: network_address; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.network_address (
    id bigint NOT NULL,
    version bigint NOT NULL,
    port character varying(255) NOT NULL,
    gateway character varying(255),
    ip character varying(255) NOT NULL
);


--
-- Name: notification_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_rules (
    id bigint NOT NULL,
    version bigint NOT NULL,
    user_id bigint NOT NULL,
    match_type character varying(255) NOT NULL,
    pattern character varying(255) NOT NULL,
    target_state character varying(255) NOT NULL,
    en_type character varying(255),
    ts_created timestamp without time zone,
    ts_updated timestamp without time zone
);


--
-- Name: peripheral_access_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.peripheral_access_tokens (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone,
    peripheral_id bigint NOT NULL,
    user_id bigint,
    token character varying(255) NOT NULL,
    en_type character varying(255),
    ts_created timestamp without time zone,
    ts_expiration timestamp without time zone NOT NULL
);


--
-- Name: port_values; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
)
PARTITION BY RANGE (ts_created);


--
-- Name: port_values_2024; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2024 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
)
WITH (fillfactor='90');


--
-- Name: port_values_2025; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2025 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
)
WITH (fillfactor='90');


--
-- Name: port_values_2026; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2026 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
)
WITH (fillfactor='90');


--
-- Name: port_values_2027; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2027 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2028; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2028 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2029; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2029 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2030; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2030 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2031; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2031 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2032; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2032 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2033; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2033 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2034; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2034 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2035; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2035 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2036; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2036 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2037; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2037 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2038; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2038 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2039; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2039 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2040; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2040 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2041; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2041 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2042; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2042 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2043; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2043 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2044; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2044 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2045; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2045 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2046; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2046 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2047; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2047 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2048; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2048 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2049; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2049 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2050; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2050 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2051; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2051 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2052; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2052 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2053; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2053 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2054; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2054 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2055; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2055 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2056; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2056 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2057; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2057 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2058; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2058 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2059; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2059 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2060; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2060 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2061; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2061 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2062; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2062 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2063; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2063 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2064; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2064 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2065; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2065 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2066; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2066 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2067; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2067 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2068; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2068 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2069; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2069 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_2070; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.port_values_2070 (
    id bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    value text NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255),
    event_id bigint,
    port_id bigint
);


--
-- Name: port_values_all_time; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.port_values_all_time AS
 SELECT port_values_archived.id,
    port_values_archived.ts_updated,
    port_values_archived.value,
    port_values_archived.ts_created,
    port_values_archived.en_type,
    port_values_archived.event_id,
    port_values_archived.port_id
   FROM archive.port_values_archived
UNION ALL
 SELECT port_values.id,
    port_values.ts_updated,
    port_values.value,
    port_values.ts_created,
    port_values.en_type,
    port_values.event_id,
    port_values.port_id
   FROM public.port_values;


--
-- Name: push_subscription; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.push_subscription (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone,
    p256dh_key character varying(255) NOT NULL,
    auth_key character varying(255) NOT NULL,
    user_id bigint NOT NULL,
    endpoint text NOT NULL,
    en_type character varying(255),
    user_agent character varying(512),
    ts_created timestamp without time zone
);


--
-- Name: qrtz_blob_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_blob_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    blob_data bytea
);


--
-- Name: qrtz_calendars; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_calendars (
    sched_name character varying(120) NOT NULL,
    calendar_name character varying(200) NOT NULL,
    calendar bytea NOT NULL
);


--
-- Name: qrtz_cron_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_cron_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    cron_expression character varying(120) NOT NULL,
    time_zone_id character varying(80)
);


--
-- Name: qrtz_fired_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_fired_triggers (
    sched_name character varying(120) NOT NULL,
    entry_id character varying(95) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    instance_name character varying(200) NOT NULL,
    fired_time bigint NOT NULL,
    sched_time bigint NOT NULL,
    priority integer NOT NULL,
    state character varying(16) NOT NULL,
    job_name character varying(200),
    job_group character varying(200),
    is_nonconcurrent boolean,
    requests_recovery boolean
);


--
-- Name: qrtz_job_details; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_job_details (
    sched_name character varying(120) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    job_class_name character varying(250) NOT NULL,
    is_durable boolean NOT NULL,
    is_nonconcurrent boolean NOT NULL,
    is_update_data boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea
);


--
-- Name: qrtz_locks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_locks (
    sched_name character varying(120) NOT NULL,
    lock_name character varying(40) NOT NULL
);


--
-- Name: qrtz_paused_trigger_grps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_paused_trigger_grps (
    sched_name character varying(120) NOT NULL,
    trigger_group character varying(200) NOT NULL
);


--
-- Name: qrtz_scheduler_state; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_scheduler_state (
    sched_name character varying(120) NOT NULL,
    instance_name character varying(200) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL
);


--
-- Name: qrtz_simple_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_simple_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL
);


--
-- Name: qrtz_simprop_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_simprop_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    str_prop_1 character varying(512),
    str_prop_2 character varying(512),
    str_prop_3 character varying(512),
    int_prop_1 integer,
    int_prop_2 integer,
    long_prop_1 bigint,
    long_prop_2 bigint,
    dec_prop_1 numeric(13,4),
    dec_prop_2 numeric(13,4),
    bool_prop_1 boolean,
    bool_prop_2 boolean
);


--
-- Name: qrtz_triggers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qrtz_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state character varying(16) NOT NULL,
    trigger_type character varying(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint,
    calendar_name character varying(200),
    misfire_instr smallint,
    job_data bytea
);


--
-- Name: rack_patch_panels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rack_patch_panels (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone NOT NULL,
    rack_id bigint NOT NULL,
    code character varying(255) NOT NULL,
    uid character varying(255),
    en_type character varying(255),
    ts_created timestamp without time zone NOT NULL,
    description character varying(255) NOT NULL,
    size integer,
    name character varying(255) NOT NULL
);


--
-- Name: racks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.racks (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255),
    zone_id bigint,
    uid character varying(255),
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255),
    en_type character varying(255)
);


--
-- Name: refresh_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_token (
    id bigint NOT NULL,
    expiration timestamp without time zone,
    authentication bytea NOT NULL,
    value character varying(255) NOT NULL
);


--
-- Name: scenarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scenarios (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    name character varying(255),
    body text NOT NULL,
    uid character varying(255),
    ts_created timestamp(6) without time zone NOT NULL,
    en_type character varying(255)
);


--
-- Name: sec_remember_me; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sec_remember_me (
    series character varying(64) NOT NULL,
    username character varying(64) NOT NULL,
    token character varying(64) NOT NULL,
    last_used timestamp(6) without time zone NOT NULL
);


--
-- Name: sec_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sec_roles (
    id bigint NOT NULL,
    version bigint NOT NULL,
    authority character varying(255) NOT NULL
);


--
-- Name: sec_user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sec_user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL
);


--
-- Name: shared_widget_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shared_widget_audit (
    id bigint NOT NULL,
    version bigint NOT NULL,
    shared_widget_id bigint NOT NULL,
    action character varying(32) NOT NULL,
    result character varying(255) NOT NULL,
    result_description character varying(255),
    remote_address character varying(64),
    user_agent character varying(512),
    en_type character varying(255),
    ts_created timestamp without time zone,
    ts_updated timestamp without time zone
);


--
-- Name: shared_widgets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shared_widgets (
    id bigint NOT NULL,
    version bigint NOT NULL,
    actions_allowed integer NOT NULL,
    ts_updated timestamp without time zone,
    pin character varying(64),
    share_expire_date timestamp without time zone NOT NULL,
    widget_type character varying(255) NOT NULL,
    token character varying(64) NOT NULL,
    en_type character varying(255),
    state character varying(255) NOT NULL,
    state_description text,
    peripheral_id character varying(255) NOT NULL,
    created_by_username character varying(255) NOT NULL,
    actions_used integer NOT NULL,
    ts_created timestamp without time zone,
    share_start_date timestamp without time zone NOT NULL,
    description text
);


--
-- Name: time_series_statistic; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.time_series_statistic (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp without time zone,
    delta_diff double precision,
    value double precision NOT NULL,
    uid character varying(255),
    en_type character varying(255),
    key character varying(255) NOT NULL,
    ts_created timestamp without time zone
);


--
-- Name: user_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_messages (
    id bigint NOT NULL,
    version bigint NOT NULL,
    level character varying(255) NOT NULL,
    ts_updated timestamp without time zone,
    en_type character varying(255),
    state character varying(255) NOT NULL,
    from_sender character varying(255) NOT NULL,
    message text NOT NULL,
    user_id bigint NOT NULL,
    ts_created timestamp without time zone,
    subject character varying(255) NOT NULL,
    dedup_key character varying(255)
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    first_name character varying(255),
    password_expired boolean NOT NULL,
    username character varying(255) NOT NULL,
    account_locked boolean NOT NULL,
    password character varying(255) NOT NULL,
    account_expired boolean NOT NULL,
    last_name character varying(255),
    enabled boolean NOT NULL,
    email character varying(255),
    ts_updated timestamp without time zone,
    uid character varying(255),
    en_type character varying(255),
    ts_created timestamp without time zone,
    phone_nr character varying(255),
    telegram_username character varying(255),
    avatar bytea,
    language character varying(255),
    timezone character varying(255)
);


--
-- Name: users_fav_jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users_fav_jobs (
    user_id bigint NOT NULL,
    job_id bigint
);


--
-- Name: users_sec_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users_sec_roles (
    user_authorities_id bigint NOT NULL,
    role_id bigint
);


--
-- Name: zones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.zones (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ts_updated timestamp(6) without time zone NOT NULL,
    uid character varying(255),
    name character varying(255) NOT NULL,
    ts_created timestamp(6) without time zone NOT NULL,
    description character varying(255) NOT NULL,
    parent_id bigint,
    en_type character varying(255)
);


--
-- Name: zones_cables_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.zones_cables_join (
    zone_id bigint NOT NULL,
    cable_id bigint NOT NULL
);


--
-- Name: zones_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.zones_categories (
    zone_id bigint NOT NULL,
    categories_string character varying(255)
);


--
-- Name: zones_devices_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.zones_devices_join (
    zone_id bigint NOT NULL,
    device_id bigint NOT NULL
);


--
-- Name: zones_peripherals_join; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.zones_peripherals_join (
    peripheral_id bigint NOT NULL,
    zone_id bigint NOT NULL
);


--
-- Name: port_values_2024; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2024 FOR VALUES FROM ('2024-01-01 00:00:00') TO ('2024-12-31 23:59:59.59');


--
-- Name: port_values_2025; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2025 FOR VALUES FROM ('2025-01-01 00:00:00') TO ('2025-12-31 23:59:59.59');


--
-- Name: port_values_2026; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2026 FOR VALUES FROM ('2026-01-01 00:00:00') TO ('2026-12-31 23:59:59.59');


--
-- Name: port_values_2027; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2027 FOR VALUES FROM ('2027-01-01 00:00:00') TO ('2027-12-31 23:59:59.59');


--
-- Name: port_values_2028; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2028 FOR VALUES FROM ('2028-01-01 00:00:00') TO ('2028-12-31 23:59:59.59');


--
-- Name: port_values_2029; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2029 FOR VALUES FROM ('2029-01-01 00:00:00') TO ('2029-12-31 23:59:59.59');


--
-- Name: port_values_2030; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2030 FOR VALUES FROM ('2030-01-01 00:00:00') TO ('2030-12-31 23:59:59.59');


--
-- Name: port_values_2031; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2031 FOR VALUES FROM ('2031-01-01 00:00:00') TO ('2031-12-31 23:59:59.59');


--
-- Name: port_values_2032; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2032 FOR VALUES FROM ('2032-01-01 00:00:00') TO ('2032-12-31 23:59:59.59');


--
-- Name: port_values_2033; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2033 FOR VALUES FROM ('2033-01-01 00:00:00') TO ('2033-12-31 23:59:59.59');


--
-- Name: port_values_2034; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2034 FOR VALUES FROM ('2034-01-01 00:00:00') TO ('2034-12-31 23:59:59.59');


--
-- Name: port_values_2035; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2035 FOR VALUES FROM ('2035-01-01 00:00:00') TO ('2035-12-31 23:59:59.59');


--
-- Name: port_values_2036; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2036 FOR VALUES FROM ('2036-01-01 00:00:00') TO ('2036-12-31 23:59:59.59');


--
-- Name: port_values_2037; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2037 FOR VALUES FROM ('2037-01-01 00:00:00') TO ('2037-12-31 23:59:59.59');


--
-- Name: port_values_2038; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2038 FOR VALUES FROM ('2038-01-01 00:00:00') TO ('2038-12-31 23:59:59.59');


--
-- Name: port_values_2039; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2039 FOR VALUES FROM ('2039-01-01 00:00:00') TO ('2039-12-31 23:59:59.59');


--
-- Name: port_values_2040; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2040 FOR VALUES FROM ('2040-01-01 00:00:00') TO ('2040-12-31 23:59:59.59');


--
-- Name: port_values_2041; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2041 FOR VALUES FROM ('2041-01-01 00:00:00') TO ('2041-12-31 23:59:59.59');


--
-- Name: port_values_2042; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2042 FOR VALUES FROM ('2042-01-01 00:00:00') TO ('2042-12-31 23:59:59.59');


--
-- Name: port_values_2043; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2043 FOR VALUES FROM ('2043-01-01 00:00:00') TO ('2043-12-31 23:59:59.59');


--
-- Name: port_values_2044; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2044 FOR VALUES FROM ('2044-01-01 00:00:00') TO ('2044-12-31 23:59:59.59');


--
-- Name: port_values_2045; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2045 FOR VALUES FROM ('2045-01-01 00:00:00') TO ('2045-12-31 23:59:59.59');


--
-- Name: port_values_2046; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2046 FOR VALUES FROM ('2046-01-01 00:00:00') TO ('2046-12-31 23:59:59.59');


--
-- Name: port_values_2047; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2047 FOR VALUES FROM ('2047-01-01 00:00:00') TO ('2047-12-31 23:59:59.59');


--
-- Name: port_values_2048; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2048 FOR VALUES FROM ('2048-01-01 00:00:00') TO ('2048-12-31 23:59:59.59');


--
-- Name: port_values_2049; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2049 FOR VALUES FROM ('2049-01-01 00:00:00') TO ('2049-12-31 23:59:59.59');


--
-- Name: port_values_2050; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2050 FOR VALUES FROM ('2050-01-01 00:00:00') TO ('2050-12-31 23:59:59.59');


--
-- Name: port_values_2051; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2051 FOR VALUES FROM ('2051-01-01 00:00:00') TO ('2051-12-31 23:59:59.59');


--
-- Name: port_values_2052; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2052 FOR VALUES FROM ('2052-01-01 00:00:00') TO ('2052-12-31 23:59:59.59');


--
-- Name: port_values_2053; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2053 FOR VALUES FROM ('2053-01-01 00:00:00') TO ('2053-12-31 23:59:59.59');


--
-- Name: port_values_2054; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2054 FOR VALUES FROM ('2054-01-01 00:00:00') TO ('2054-12-31 23:59:59.59');


--
-- Name: port_values_2055; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2055 FOR VALUES FROM ('2055-01-01 00:00:00') TO ('2055-12-31 23:59:59.59');


--
-- Name: port_values_2056; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2056 FOR VALUES FROM ('2056-01-01 00:00:00') TO ('2056-12-31 23:59:59.59');


--
-- Name: port_values_2057; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2057 FOR VALUES FROM ('2057-01-01 00:00:00') TO ('2057-12-31 23:59:59.59');


--
-- Name: port_values_2058; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2058 FOR VALUES FROM ('2058-01-01 00:00:00') TO ('2058-12-31 23:59:59.59');


--
-- Name: port_values_2059; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2059 FOR VALUES FROM ('2059-01-01 00:00:00') TO ('2059-12-31 23:59:59.59');


--
-- Name: port_values_2060; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2060 FOR VALUES FROM ('2060-01-01 00:00:00') TO ('2060-12-31 23:59:59.59');


--
-- Name: port_values_2061; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2061 FOR VALUES FROM ('2061-01-01 00:00:00') TO ('2061-12-31 23:59:59.59');


--
-- Name: port_values_2062; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2062 FOR VALUES FROM ('2062-01-01 00:00:00') TO ('2062-12-31 23:59:59.59');


--
-- Name: port_values_2063; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2063 FOR VALUES FROM ('2063-01-01 00:00:00') TO ('2063-12-31 23:59:59.59');


--
-- Name: port_values_2064; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2064 FOR VALUES FROM ('2064-01-01 00:00:00') TO ('2064-12-31 23:59:59.59');


--
-- Name: port_values_2065; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2065 FOR VALUES FROM ('2065-01-01 00:00:00') TO ('2065-12-31 23:59:59.59');


--
-- Name: port_values_2066; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2066 FOR VALUES FROM ('2066-01-01 00:00:00') TO ('2066-12-31 23:59:59.59');


--
-- Name: port_values_2067; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2067 FOR VALUES FROM ('2067-01-01 00:00:00') TO ('2067-12-31 23:59:59.59');


--
-- Name: port_values_2068; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2068 FOR VALUES FROM ('2068-01-01 00:00:00') TO ('2068-12-31 23:59:59.59');


--
-- Name: port_values_2069; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2069 FOR VALUES FROM ('2069-01-01 00:00:00') TO ('2069-12-31 23:59:59.59');


--
-- Name: port_values_2070; Type: TABLE ATTACH; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values ATTACH PARTITION public.port_values_2070 FOR VALUES FROM ('2070-01-01 00:00:00') TO ('2070-12-31 23:59:59.59');


--
-- Name: partition_archive_log id; Type: DEFAULT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.partition_archive_log ALTER COLUMN id SET DEFAULT nextval('archive.partition_archive_log_id_seq'::regclass);


--
-- Name: partition_archive_log partition_archive_log_pkey; Type: CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.partition_archive_log
    ADD CONSTRAINT partition_archive_log_pkey PRIMARY KEY (id);


--
-- Name: port_values_2019 port_values_2019_pkey; Type: CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2019
    ADD CONSTRAINT port_values_2019_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2020 port_values_2020_pkey; Type: CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2020
    ADD CONSTRAINT port_values_2020_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2021 port_values_2021_pkey; Type: CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2021
    ADD CONSTRAINT port_values_2021_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2022 port_values_2022_pkey; Type: CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2022
    ADD CONSTRAINT port_values_2022_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2023 port_values_2023_pkey; Type: CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2023
    ADD CONSTRAINT port_values_2023_pkey PRIMARY KEY (id, ts_created);


--
-- Name: access_token access_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_token
    ADD CONSTRAINT access_token_pkey PRIMARY KEY (id);


--
-- Name: authorization_code authorization_code_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authorization_code
    ADD CONSTRAINT authorization_code_pkey PRIMARY KEY (id);


--
-- Name: cable_categories cable_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cable_categories
    ADD CONSTRAINT cable_categories_pkey PRIMARY KEY (id);


--
-- Name: cables cables_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables
    ADD CONSTRAINT cables_pkey PRIMARY KEY (id);


--
-- Name: client client_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT client_pkey PRIMARY KEY (id);


--
-- Name: device_controllers code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_controllers
    ADD CONSTRAINT code UNIQUE (code);


--
-- Name: configurations configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configurations
    ADD CONSTRAINT configurations_pkey PRIMARY KEY (id);


--
-- Name: dashboard_screen_backgrounds dashboard_screen_backgrounds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_screen_backgrounds
    ADD CONSTRAINT dashboard_screen_backgrounds_pkey PRIMARY KEY (id);


--
-- Name: dashboard_screens dashboard_screens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_screens
    ADD CONSTRAINT dashboard_screens_pkey PRIMARY KEY (id);


--
-- Name: device_accounts device_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_accounts
    ADD CONSTRAINT device_accounts_pkey PRIMARY KEY (id);


--
-- Name: device_backup device_backup_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_backup
    ADD CONSTRAINT device_backup_pkey PRIMARY KEY (id);


--
-- Name: device_categories device_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_categories
    ADD CONSTRAINT device_categories_pkey PRIMARY KEY (id);


--
-- Name: device_controllers device_controllers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_controllers
    ADD CONSTRAINT device_controllers_pkey PRIMARY KEY (id);


--
-- Name: device_peripherals_categories device_peripherals_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_peripherals_categories
    ADD CONSTRAINT device_peripherals_categories_pkey PRIMARY KEY (id);


--
-- Name: device_peripherals device_peripherals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_peripherals
    ADD CONSTRAINT device_peripherals_pkey PRIMARY KEY (id);


--
-- Name: device_ports_peripherals_join device_ports_peripherals_join_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_peripherals_join
    ADD CONSTRAINT device_ports_peripherals_join_pkey PRIMARY KEY (port_id, peripheral_id);


--
-- Name: device_ports device_ports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports
    ADD CONSTRAINT device_ports_pkey PRIMARY KEY (id);


--
-- Name: device_ports_scenarios_join device_ports_scenarions_join_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_scenarios_join
    ADD CONSTRAINT device_ports_scenarions_join_pkey PRIMARY KEY (port_id, scenario_id);


--
-- Name: device_types device_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_types
    ADD CONSTRAINT device_types_pkey PRIMARY KEY (id);


--
-- Name: event_definitions event_definitions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_definitions
    ADD CONSTRAINT event_definitions_pkey PRIMARY KEY (id);


--
-- Name: event_log event_queue_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_log
    ADD CONSTRAINT event_queue_pkey PRIMARY KEY (id);


--
-- Name: event_subscriptions event_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_subscriptions
    ADD CONSTRAINT event_subscriptions_pkey PRIMARY KEY (id);


--
-- Name: job_execution_history job_execution_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_execution_history
    ADD CONSTRAINT job_execution_history_pkey PRIMARY KEY (id);


--
-- Name: job_triggers_cron job_triggers_cron_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_triggers_cron
    ADD CONSTRAINT job_triggers_cron_pkey PRIMARY KEY (id);


--
-- Name: job_triggers_events job_triggers_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_triggers_events
    ADD CONSTRAINT job_triggers_events_pkey PRIMARY KEY (id);


--
-- Name: jobs jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs
    ADD CONSTRAINT jobs_pkey PRIMARY KEY (id);


--
-- Name: jobs_tags_join jobs_tags_join_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs_tags_join
    ADD CONSTRAINT jobs_tags_join_pkey PRIMARY KEY (job_id, tag_id);


--
-- Name: jobs_tags jobs_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs_tags
    ADD CONSTRAINT jobs_tags_pkey PRIMARY KEY (id);


--
-- Name: layers layers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layers
    ADD CONSTRAINT layers_pkey PRIMARY KEY (id);


--
-- Name: mqtt_topics mqtt_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mqtt_topics
    ADD CONSTRAINT mqtt_topics_pkey PRIMARY KEY (id);


--
-- Name: network_address network_address_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.network_address
    ADD CONSTRAINT network_address_pkey PRIMARY KEY (id);


--
-- Name: notification_rules notification_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_rules
    ADD CONSTRAINT notification_rules_pkey PRIMARY KEY (id);


--
-- Name: peripheral_access_tokens peripheral_access_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.peripheral_access_tokens
    ADD CONSTRAINT peripheral_access_tokens_pkey PRIMARY KEY (id);


--
-- Name: port_values port_values_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values
    ADD CONSTRAINT port_values_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2024 port_values_2024_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2024
    ADD CONSTRAINT port_values_2024_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2025 port_values_2025_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2025
    ADD CONSTRAINT port_values_2025_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2026 port_values_2026_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2026
    ADD CONSTRAINT port_values_2026_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2027 port_values_2027_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2027
    ADD CONSTRAINT port_values_2027_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2028 port_values_2028_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2028
    ADD CONSTRAINT port_values_2028_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2029 port_values_2029_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2029
    ADD CONSTRAINT port_values_2029_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2030 port_values_2030_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2030
    ADD CONSTRAINT port_values_2030_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2031 port_values_2031_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2031
    ADD CONSTRAINT port_values_2031_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2032 port_values_2032_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2032
    ADD CONSTRAINT port_values_2032_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2033 port_values_2033_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2033
    ADD CONSTRAINT port_values_2033_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2034 port_values_2034_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2034
    ADD CONSTRAINT port_values_2034_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2035 port_values_2035_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2035
    ADD CONSTRAINT port_values_2035_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2036 port_values_2036_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2036
    ADD CONSTRAINT port_values_2036_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2037 port_values_2037_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2037
    ADD CONSTRAINT port_values_2037_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2038 port_values_2038_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2038
    ADD CONSTRAINT port_values_2038_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2039 port_values_2039_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2039
    ADD CONSTRAINT port_values_2039_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2040 port_values_2040_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2040
    ADD CONSTRAINT port_values_2040_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2041 port_values_2041_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2041
    ADD CONSTRAINT port_values_2041_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2042 port_values_2042_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2042
    ADD CONSTRAINT port_values_2042_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2043 port_values_2043_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2043
    ADD CONSTRAINT port_values_2043_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2044 port_values_2044_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2044
    ADD CONSTRAINT port_values_2044_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2045 port_values_2045_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2045
    ADD CONSTRAINT port_values_2045_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2046 port_values_2046_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2046
    ADD CONSTRAINT port_values_2046_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2047 port_values_2047_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2047
    ADD CONSTRAINT port_values_2047_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2048 port_values_2048_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2048
    ADD CONSTRAINT port_values_2048_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2049 port_values_2049_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2049
    ADD CONSTRAINT port_values_2049_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2050 port_values_2050_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2050
    ADD CONSTRAINT port_values_2050_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2051 port_values_2051_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2051
    ADD CONSTRAINT port_values_2051_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2052 port_values_2052_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2052
    ADD CONSTRAINT port_values_2052_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2053 port_values_2053_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2053
    ADD CONSTRAINT port_values_2053_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2054 port_values_2054_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2054
    ADD CONSTRAINT port_values_2054_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2055 port_values_2055_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2055
    ADD CONSTRAINT port_values_2055_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2056 port_values_2056_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2056
    ADD CONSTRAINT port_values_2056_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2057 port_values_2057_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2057
    ADD CONSTRAINT port_values_2057_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2058 port_values_2058_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2058
    ADD CONSTRAINT port_values_2058_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2059 port_values_2059_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2059
    ADD CONSTRAINT port_values_2059_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2060 port_values_2060_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2060
    ADD CONSTRAINT port_values_2060_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2061 port_values_2061_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2061
    ADD CONSTRAINT port_values_2061_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2062 port_values_2062_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2062
    ADD CONSTRAINT port_values_2062_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2063 port_values_2063_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2063
    ADD CONSTRAINT port_values_2063_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2064 port_values_2064_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2064
    ADD CONSTRAINT port_values_2064_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2065 port_values_2065_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2065
    ADD CONSTRAINT port_values_2065_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2066 port_values_2066_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2066
    ADD CONSTRAINT port_values_2066_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2067 port_values_2067_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2067
    ADD CONSTRAINT port_values_2067_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2068 port_values_2068_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2068
    ADD CONSTRAINT port_values_2068_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2069 port_values_2069_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2069
    ADD CONSTRAINT port_values_2069_pkey PRIMARY KEY (id, ts_created);


--
-- Name: port_values_2070 port_values_2070_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.port_values_2070
    ADD CONSTRAINT port_values_2070_pkey PRIMARY KEY (id, ts_created);


--
-- Name: push_subscription push_subscription_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_subscription
    ADD CONSTRAINT push_subscription_pkey PRIMARY KEY (id);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_calendars qrtz_calendars_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id);


--
-- Name: qrtz_job_details qrtz_job_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group);


--
-- Name: qrtz_locks qrtz_locks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name);


--
-- Name: qrtz_paused_trigger_grps qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group);


--
-- Name: qrtz_scheduler_state qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: rack_patch_panels rack_patch_panels_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rack_patch_panels
    ADD CONSTRAINT rack_patch_panels_pkey PRIMARY KEY (id);


--
-- Name: racks racks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.racks
    ADD CONSTRAINT racks_pkey PRIMARY KEY (id);


--
-- Name: refresh_token refresh_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (id);


--
-- Name: scenarios scenarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scenarios
    ADD CONSTRAINT scenarios_pkey PRIMARY KEY (id);


--
-- Name: sec_remember_me sec_remember_me_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sec_remember_me
    ADD CONSTRAINT sec_remember_me_pkey PRIMARY KEY (series);


--
-- Name: sec_roles sec_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sec_roles
    ADD CONSTRAINT sec_roles_pkey PRIMARY KEY (id);


--
-- Name: sec_user_roles sec_user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sec_user_roles
    ADD CONSTRAINT sec_user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: shared_widget_audit shared_widget_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_widget_audit
    ADD CONSTRAINT shared_widget_audit_pkey PRIMARY KEY (id);


--
-- Name: shared_widgets shared_widgets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_widgets
    ADD CONSTRAINT shared_widgets_pkey PRIMARY KEY (id);


--
-- Name: time_series_statistic time_series_statistic_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.time_series_statistic
    ADD CONSTRAINT time_series_statistic_pkey PRIMARY KEY (id);


--
-- Name: dashboard_screens uk_2687txfm0usnlb0y1yuyfj43k; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_screens
    ADD CONSTRAINT uk_2687txfm0usnlb0y1yuyfj43k UNIQUE (name);


--
-- Name: shared_widgets uk_3cmejiw5ektxs48vfb1dma3ft; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_widgets
    ADD CONSTRAINT uk_3cmejiw5ektxs48vfb1dma3ft UNIQUE (token);


--
-- Name: refresh_token uk_8rshch3e41dfdp17ljdetdplb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT uk_8rshch3e41dfdp17ljdetdplb UNIQUE (value);


--
-- Name: client uk_bfjdoy2dpussylq7g1s3s1tn8; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT uk_bfjdoy2dpussylq7g1s3s1tn8 UNIQUE (client_id);


--
-- Name: access_token uk_d1r4lodh584dvb8jyhwy7rhcs; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_token
    ADD CONSTRAINT uk_d1r4lodh584dvb8jyhwy7rhcs UNIQUE (authentication_key);


--
-- Name: authorization_code uk_d5sya0t19jj2fdaik5iv09cmh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authorization_code
    ADD CONSTRAINT uk_d5sya0t19jj2fdaik5iv09cmh UNIQUE (code);


--
-- Name: dashboard_screen_backgrounds uk_f2fxe84xm38fqnbokwqwjqw4f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_screen_backgrounds
    ADD CONSTRAINT uk_f2fxe84xm38fqnbokwqwjqw4f UNIQUE (screen_id);


--
-- Name: access_token uk_nbpjntb29t1ml1uottcf0aeev; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_token
    ADD CONSTRAINT uk_nbpjntb29t1ml1uottcf0aeev UNIQUE (value);


--
-- Name: push_subscription uk_qtek2d9716ho69tovncusqi88; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_subscription
    ADD CONSTRAINT uk_qtek2d9716ho69tovncusqi88 UNIQUE (endpoint);


--
-- Name: users uk_r43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_r43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: sec_roles uk_srjr9q6apue4i2b289f3to2e0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sec_roles
    ADD CONSTRAINT uk_srjr9q6apue4i2b289f3to2e0 UNIQUE (authority);


--
-- Name: user_messages user_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_messages
    ADD CONSTRAINT user_messages_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: zones_cables_join zones_cables_join_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_cables_join
    ADD CONSTRAINT zones_cables_join_pkey PRIMARY KEY (zone_id, cable_id);


--
-- Name: zones_peripherals_join zones_peripherals_join_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_peripherals_join
    ADD CONSTRAINT zones_peripherals_join_pkey PRIMARY KEY (zone_id, peripheral_id);


--
-- Name: zones zones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones
    ADD CONSTRAINT zones_pkey PRIMARY KEY (id);


--
-- Name: port_values_2019_port_id_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2019_port_id_ts_created_idx ON archive.port_values_2019 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2019_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2019_ts_created_idx ON archive.port_values_2019 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2020_port_id_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2020_port_id_ts_created_idx ON archive.port_values_2020 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2020_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2020_ts_created_idx ON archive.port_values_2020 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2021_port_id_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2021_port_id_ts_created_idx ON archive.port_values_2021 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2021_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2021_ts_created_idx ON archive.port_values_2021 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2022_port_id_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2022_port_id_ts_created_idx ON archive.port_values_2022 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2022_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2022_ts_created_idx ON archive.port_values_2022 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2023_port_id_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2023_port_id_ts_created_idx ON archive.port_values_2023 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2023_ts_created_idx; Type: INDEX; Schema: archive; Owner: -
--

CREATE INDEX port_values_2023_ts_created_idx ON archive.port_values_2023 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: idx_port_values_port_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_port_values_port_time ON ONLY public.port_values USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: idx_port_values_ts_created_brin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_port_values_ts_created_brin ON ONLY public.port_values USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: idx_qrtz_ft_inst_job_req_rcvry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON public.qrtz_fired_triggers USING btree (sched_name, instance_name, requests_recovery);


--
-- Name: idx_qrtz_ft_j_g; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_ft_j_g ON public.qrtz_fired_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: idx_qrtz_ft_jg; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_ft_jg ON public.qrtz_fired_triggers USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_ft_t_g; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_ft_t_g ON public.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_ft_tg; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_ft_tg ON public.qrtz_fired_triggers USING btree (sched_name, trigger_group);


--
-- Name: idx_qrtz_ft_trig_inst_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_ft_trig_inst_name ON public.qrtz_fired_triggers USING btree (sched_name, instance_name);


--
-- Name: idx_qrtz_j_grp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_j_grp ON public.qrtz_job_details USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_j_req_recovery; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_j_req_recovery ON public.qrtz_job_details USING btree (sched_name, requests_recovery);


--
-- Name: idx_qrtz_t_c; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_c ON public.qrtz_triggers USING btree (sched_name, calendar_name);


--
-- Name: idx_qrtz_t_g; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_g ON public.qrtz_triggers USING btree (sched_name, trigger_group);


--
-- Name: idx_qrtz_t_j; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_j ON public.qrtz_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: idx_qrtz_t_jg; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_jg ON public.qrtz_triggers USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_t_n_g_state; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_n_g_state ON public.qrtz_triggers USING btree (sched_name, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_n_state; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_n_state ON public.qrtz_triggers USING btree (sched_name, trigger_name, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_next_fire_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_next_fire_time ON public.qrtz_triggers USING btree (sched_name, next_fire_time);


--
-- Name: idx_qrtz_t_nft_misfire; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_misfire ON public.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time);


--
-- Name: idx_qrtz_t_nft_st; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_st ON public.qrtz_triggers USING btree (sched_name, trigger_state, next_fire_time);


--
-- Name: idx_qrtz_t_nft_st_misfire; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_st_misfire ON public.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_state);


--
-- Name: idx_qrtz_t_nft_st_misfire_grp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_st_misfire_grp ON public.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_state; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qrtz_t_state ON public.qrtz_triggers USING btree (sched_name, trigger_state);


--
-- Name: port_values_2024_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2024_port_id_ts_created_idx ON public.port_values_2024 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2024_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2024_ts_created_idx ON public.port_values_2024 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2025_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2025_port_id_ts_created_idx ON public.port_values_2025 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2025_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2025_ts_created_idx ON public.port_values_2025 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2026_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2026_port_id_ts_created_idx ON public.port_values_2026 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2026_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2026_ts_created_idx ON public.port_values_2026 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2027_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2027_port_id_ts_created_idx ON public.port_values_2027 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2027_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2027_ts_created_idx ON public.port_values_2027 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2028_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2028_port_id_ts_created_idx ON public.port_values_2028 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2028_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2028_ts_created_idx ON public.port_values_2028 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2029_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2029_port_id_ts_created_idx ON public.port_values_2029 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2029_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2029_ts_created_idx ON public.port_values_2029 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2030_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2030_port_id_ts_created_idx ON public.port_values_2030 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2030_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2030_ts_created_idx ON public.port_values_2030 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2031_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2031_port_id_ts_created_idx ON public.port_values_2031 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2031_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2031_ts_created_idx ON public.port_values_2031 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2032_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2032_port_id_ts_created_idx ON public.port_values_2032 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2032_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2032_ts_created_idx ON public.port_values_2032 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2033_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2033_port_id_ts_created_idx ON public.port_values_2033 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2033_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2033_ts_created_idx ON public.port_values_2033 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2034_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2034_port_id_ts_created_idx ON public.port_values_2034 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2034_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2034_ts_created_idx ON public.port_values_2034 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2035_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2035_port_id_ts_created_idx ON public.port_values_2035 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2035_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2035_ts_created_idx ON public.port_values_2035 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2036_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2036_port_id_ts_created_idx ON public.port_values_2036 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2036_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2036_ts_created_idx ON public.port_values_2036 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2037_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2037_port_id_ts_created_idx ON public.port_values_2037 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2037_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2037_ts_created_idx ON public.port_values_2037 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2038_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2038_port_id_ts_created_idx ON public.port_values_2038 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2038_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2038_ts_created_idx ON public.port_values_2038 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2039_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2039_port_id_ts_created_idx ON public.port_values_2039 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2039_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2039_ts_created_idx ON public.port_values_2039 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2040_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2040_port_id_ts_created_idx ON public.port_values_2040 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2040_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2040_ts_created_idx ON public.port_values_2040 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2041_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2041_port_id_ts_created_idx ON public.port_values_2041 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2041_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2041_ts_created_idx ON public.port_values_2041 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2042_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2042_port_id_ts_created_idx ON public.port_values_2042 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2042_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2042_ts_created_idx ON public.port_values_2042 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2043_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2043_port_id_ts_created_idx ON public.port_values_2043 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2043_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2043_ts_created_idx ON public.port_values_2043 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2044_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2044_port_id_ts_created_idx ON public.port_values_2044 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2044_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2044_ts_created_idx ON public.port_values_2044 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2045_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2045_port_id_ts_created_idx ON public.port_values_2045 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2045_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2045_ts_created_idx ON public.port_values_2045 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2046_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2046_port_id_ts_created_idx ON public.port_values_2046 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2046_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2046_ts_created_idx ON public.port_values_2046 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2047_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2047_port_id_ts_created_idx ON public.port_values_2047 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2047_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2047_ts_created_idx ON public.port_values_2047 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2048_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2048_port_id_ts_created_idx ON public.port_values_2048 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2048_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2048_ts_created_idx ON public.port_values_2048 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2049_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2049_port_id_ts_created_idx ON public.port_values_2049 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2049_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2049_ts_created_idx ON public.port_values_2049 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2050_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2050_port_id_ts_created_idx ON public.port_values_2050 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2050_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2050_ts_created_idx ON public.port_values_2050 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2051_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2051_port_id_ts_created_idx ON public.port_values_2051 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2051_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2051_ts_created_idx ON public.port_values_2051 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2052_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2052_port_id_ts_created_idx ON public.port_values_2052 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2052_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2052_ts_created_idx ON public.port_values_2052 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2053_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2053_port_id_ts_created_idx ON public.port_values_2053 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2053_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2053_ts_created_idx ON public.port_values_2053 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2054_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2054_port_id_ts_created_idx ON public.port_values_2054 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2054_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2054_ts_created_idx ON public.port_values_2054 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2055_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2055_port_id_ts_created_idx ON public.port_values_2055 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2055_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2055_ts_created_idx ON public.port_values_2055 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2056_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2056_port_id_ts_created_idx ON public.port_values_2056 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2056_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2056_ts_created_idx ON public.port_values_2056 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2057_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2057_port_id_ts_created_idx ON public.port_values_2057 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2057_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2057_ts_created_idx ON public.port_values_2057 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2058_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2058_port_id_ts_created_idx ON public.port_values_2058 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2058_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2058_ts_created_idx ON public.port_values_2058 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2059_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2059_port_id_ts_created_idx ON public.port_values_2059 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2059_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2059_ts_created_idx ON public.port_values_2059 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2060_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2060_port_id_ts_created_idx ON public.port_values_2060 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2060_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2060_ts_created_idx ON public.port_values_2060 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2061_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2061_port_id_ts_created_idx ON public.port_values_2061 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2061_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2061_ts_created_idx ON public.port_values_2061 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2062_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2062_port_id_ts_created_idx ON public.port_values_2062 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2062_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2062_ts_created_idx ON public.port_values_2062 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2063_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2063_port_id_ts_created_idx ON public.port_values_2063 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2063_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2063_ts_created_idx ON public.port_values_2063 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2064_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2064_port_id_ts_created_idx ON public.port_values_2064 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2064_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2064_ts_created_idx ON public.port_values_2064 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2065_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2065_port_id_ts_created_idx ON public.port_values_2065 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2065_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2065_ts_created_idx ON public.port_values_2065 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2066_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2066_port_id_ts_created_idx ON public.port_values_2066 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2066_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2066_ts_created_idx ON public.port_values_2066 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2067_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2067_port_id_ts_created_idx ON public.port_values_2067 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2067_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2067_ts_created_idx ON public.port_values_2067 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2068_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2068_port_id_ts_created_idx ON public.port_values_2068 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2068_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2068_ts_created_idx ON public.port_values_2068 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2069_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2069_port_id_ts_created_idx ON public.port_values_2069 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2069_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2069_ts_created_idx ON public.port_values_2069 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: port_values_2070_port_id_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2070_port_id_ts_created_idx ON public.port_values_2070 USING btree (port_id, ts_created DESC) WHERE (port_id IS NOT NULL);


--
-- Name: port_values_2070_ts_created_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX port_values_2070_ts_created_idx ON public.port_values_2070 USING brin (ts_created) WITH (pages_per_range='128');


--
-- Name: shared_widget_audit_widget_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX shared_widget_audit_widget_idx ON public.shared_widget_audit USING btree (shared_widget_id);


--
-- Name: port_values_2024_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2024_pkey;


--
-- Name: port_values_2024_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2024_port_id_ts_created_idx;


--
-- Name: port_values_2024_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2024_ts_created_idx;


--
-- Name: port_values_2025_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2025_pkey;


--
-- Name: port_values_2025_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2025_port_id_ts_created_idx;


--
-- Name: port_values_2025_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2025_ts_created_idx;


--
-- Name: port_values_2026_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2026_pkey;


--
-- Name: port_values_2026_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2026_port_id_ts_created_idx;


--
-- Name: port_values_2026_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2026_ts_created_idx;


--
-- Name: port_values_2027_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2027_pkey;


--
-- Name: port_values_2027_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2027_port_id_ts_created_idx;


--
-- Name: port_values_2027_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2027_ts_created_idx;


--
-- Name: port_values_2028_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2028_pkey;


--
-- Name: port_values_2028_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2028_port_id_ts_created_idx;


--
-- Name: port_values_2028_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2028_ts_created_idx;


--
-- Name: port_values_2029_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2029_pkey;


--
-- Name: port_values_2029_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2029_port_id_ts_created_idx;


--
-- Name: port_values_2029_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2029_ts_created_idx;


--
-- Name: port_values_2030_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2030_pkey;


--
-- Name: port_values_2030_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2030_port_id_ts_created_idx;


--
-- Name: port_values_2030_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2030_ts_created_idx;


--
-- Name: port_values_2031_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2031_pkey;


--
-- Name: port_values_2031_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2031_port_id_ts_created_idx;


--
-- Name: port_values_2031_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2031_ts_created_idx;


--
-- Name: port_values_2032_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2032_pkey;


--
-- Name: port_values_2032_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2032_port_id_ts_created_idx;


--
-- Name: port_values_2032_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2032_ts_created_idx;


--
-- Name: port_values_2033_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2033_pkey;


--
-- Name: port_values_2033_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2033_port_id_ts_created_idx;


--
-- Name: port_values_2033_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2033_ts_created_idx;


--
-- Name: port_values_2034_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2034_pkey;


--
-- Name: port_values_2034_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2034_port_id_ts_created_idx;


--
-- Name: port_values_2034_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2034_ts_created_idx;


--
-- Name: port_values_2035_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2035_pkey;


--
-- Name: port_values_2035_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2035_port_id_ts_created_idx;


--
-- Name: port_values_2035_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2035_ts_created_idx;


--
-- Name: port_values_2036_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2036_pkey;


--
-- Name: port_values_2036_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2036_port_id_ts_created_idx;


--
-- Name: port_values_2036_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2036_ts_created_idx;


--
-- Name: port_values_2037_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2037_pkey;


--
-- Name: port_values_2037_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2037_port_id_ts_created_idx;


--
-- Name: port_values_2037_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2037_ts_created_idx;


--
-- Name: port_values_2038_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2038_pkey;


--
-- Name: port_values_2038_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2038_port_id_ts_created_idx;


--
-- Name: port_values_2038_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2038_ts_created_idx;


--
-- Name: port_values_2039_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2039_pkey;


--
-- Name: port_values_2039_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2039_port_id_ts_created_idx;


--
-- Name: port_values_2039_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2039_ts_created_idx;


--
-- Name: port_values_2040_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2040_pkey;


--
-- Name: port_values_2040_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2040_port_id_ts_created_idx;


--
-- Name: port_values_2040_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2040_ts_created_idx;


--
-- Name: port_values_2041_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2041_pkey;


--
-- Name: port_values_2041_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2041_port_id_ts_created_idx;


--
-- Name: port_values_2041_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2041_ts_created_idx;


--
-- Name: port_values_2042_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2042_pkey;


--
-- Name: port_values_2042_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2042_port_id_ts_created_idx;


--
-- Name: port_values_2042_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2042_ts_created_idx;


--
-- Name: port_values_2043_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2043_pkey;


--
-- Name: port_values_2043_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2043_port_id_ts_created_idx;


--
-- Name: port_values_2043_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2043_ts_created_idx;


--
-- Name: port_values_2044_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2044_pkey;


--
-- Name: port_values_2044_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2044_port_id_ts_created_idx;


--
-- Name: port_values_2044_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2044_ts_created_idx;


--
-- Name: port_values_2045_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2045_pkey;


--
-- Name: port_values_2045_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2045_port_id_ts_created_idx;


--
-- Name: port_values_2045_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2045_ts_created_idx;


--
-- Name: port_values_2046_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2046_pkey;


--
-- Name: port_values_2046_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2046_port_id_ts_created_idx;


--
-- Name: port_values_2046_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2046_ts_created_idx;


--
-- Name: port_values_2047_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2047_pkey;


--
-- Name: port_values_2047_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2047_port_id_ts_created_idx;


--
-- Name: port_values_2047_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2047_ts_created_idx;


--
-- Name: port_values_2048_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2048_pkey;


--
-- Name: port_values_2048_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2048_port_id_ts_created_idx;


--
-- Name: port_values_2048_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2048_ts_created_idx;


--
-- Name: port_values_2049_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2049_pkey;


--
-- Name: port_values_2049_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2049_port_id_ts_created_idx;


--
-- Name: port_values_2049_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2049_ts_created_idx;


--
-- Name: port_values_2050_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2050_pkey;


--
-- Name: port_values_2050_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2050_port_id_ts_created_idx;


--
-- Name: port_values_2050_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2050_ts_created_idx;


--
-- Name: port_values_2051_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2051_pkey;


--
-- Name: port_values_2051_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2051_port_id_ts_created_idx;


--
-- Name: port_values_2051_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2051_ts_created_idx;


--
-- Name: port_values_2052_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2052_pkey;


--
-- Name: port_values_2052_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2052_port_id_ts_created_idx;


--
-- Name: port_values_2052_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2052_ts_created_idx;


--
-- Name: port_values_2053_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2053_pkey;


--
-- Name: port_values_2053_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2053_port_id_ts_created_idx;


--
-- Name: port_values_2053_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2053_ts_created_idx;


--
-- Name: port_values_2054_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2054_pkey;


--
-- Name: port_values_2054_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2054_port_id_ts_created_idx;


--
-- Name: port_values_2054_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2054_ts_created_idx;


--
-- Name: port_values_2055_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2055_pkey;


--
-- Name: port_values_2055_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2055_port_id_ts_created_idx;


--
-- Name: port_values_2055_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2055_ts_created_idx;


--
-- Name: port_values_2056_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2056_pkey;


--
-- Name: port_values_2056_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2056_port_id_ts_created_idx;


--
-- Name: port_values_2056_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2056_ts_created_idx;


--
-- Name: port_values_2057_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2057_pkey;


--
-- Name: port_values_2057_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2057_port_id_ts_created_idx;


--
-- Name: port_values_2057_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2057_ts_created_idx;


--
-- Name: port_values_2058_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2058_pkey;


--
-- Name: port_values_2058_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2058_port_id_ts_created_idx;


--
-- Name: port_values_2058_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2058_ts_created_idx;


--
-- Name: port_values_2059_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2059_pkey;


--
-- Name: port_values_2059_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2059_port_id_ts_created_idx;


--
-- Name: port_values_2059_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2059_ts_created_idx;


--
-- Name: port_values_2060_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2060_pkey;


--
-- Name: port_values_2060_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2060_port_id_ts_created_idx;


--
-- Name: port_values_2060_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2060_ts_created_idx;


--
-- Name: port_values_2061_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2061_pkey;


--
-- Name: port_values_2061_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2061_port_id_ts_created_idx;


--
-- Name: port_values_2061_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2061_ts_created_idx;


--
-- Name: port_values_2062_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2062_pkey;


--
-- Name: port_values_2062_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2062_port_id_ts_created_idx;


--
-- Name: port_values_2062_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2062_ts_created_idx;


--
-- Name: port_values_2063_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2063_pkey;


--
-- Name: port_values_2063_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2063_port_id_ts_created_idx;


--
-- Name: port_values_2063_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2063_ts_created_idx;


--
-- Name: port_values_2064_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2064_pkey;


--
-- Name: port_values_2064_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2064_port_id_ts_created_idx;


--
-- Name: port_values_2064_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2064_ts_created_idx;


--
-- Name: port_values_2065_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2065_pkey;


--
-- Name: port_values_2065_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2065_port_id_ts_created_idx;


--
-- Name: port_values_2065_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2065_ts_created_idx;


--
-- Name: port_values_2066_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2066_pkey;


--
-- Name: port_values_2066_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2066_port_id_ts_created_idx;


--
-- Name: port_values_2066_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2066_ts_created_idx;


--
-- Name: port_values_2067_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2067_pkey;


--
-- Name: port_values_2067_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2067_port_id_ts_created_idx;


--
-- Name: port_values_2067_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2067_ts_created_idx;


--
-- Name: port_values_2068_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2068_pkey;


--
-- Name: port_values_2068_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2068_port_id_ts_created_idx;


--
-- Name: port_values_2068_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2068_ts_created_idx;


--
-- Name: port_values_2069_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2069_pkey;


--
-- Name: port_values_2069_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2069_port_id_ts_created_idx;


--
-- Name: port_values_2069_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2069_ts_created_idx;


--
-- Name: port_values_2070_pkey; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.port_values_pkey ATTACH PARTITION public.port_values_2070_pkey;


--
-- Name: port_values_2070_port_id_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_port_time ATTACH PARTITION public.port_values_2070_port_id_ts_created_idx;


--
-- Name: port_values_2070_ts_created_idx; Type: INDEX ATTACH; Schema: public; Owner: -
--

ALTER INDEX public.idx_port_values_ts_created_brin ATTACH PARTITION public.port_values_2070_ts_created_idx;


--
-- Name: port_values_2019 fkcq71a63afih8ear7j2hkcb7bb; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2019
    ADD CONSTRAINT fkcq71a63afih8ear7j2hkcb7bb FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2020 fkcq71a63afih8ear7j2hkcb7bb; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2020
    ADD CONSTRAINT fkcq71a63afih8ear7j2hkcb7bb FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2021 fkcq71a63afih8ear7j2hkcb7bb; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2021
    ADD CONSTRAINT fkcq71a63afih8ear7j2hkcb7bb FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2022 fkcq71a63afih8ear7j2hkcb7bb; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2022
    ADD CONSTRAINT fkcq71a63afih8ear7j2hkcb7bb FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2023 fkcq71a63afih8ear7j2hkcb7bb; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2023
    ADD CONSTRAINT fkcq71a63afih8ear7j2hkcb7bb FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2019 fkhyga7gmggfsi2ky8f4rd3eqkv; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2019
    ADD CONSTRAINT fkhyga7gmggfsi2ky8f4rd3eqkv FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2020 fkhyga7gmggfsi2ky8f4rd3eqkv; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2020
    ADD CONSTRAINT fkhyga7gmggfsi2ky8f4rd3eqkv FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2021 fkhyga7gmggfsi2ky8f4rd3eqkv; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2021
    ADD CONSTRAINT fkhyga7gmggfsi2ky8f4rd3eqkv FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2022 fkhyga7gmggfsi2ky8f4rd3eqkv; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2022
    ADD CONSTRAINT fkhyga7gmggfsi2ky8f4rd3eqkv FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: port_values_2023 fkhyga7gmggfsi2ky8f4rd3eqkv; Type: FK CONSTRAINT; Schema: archive; Owner: -
--

ALTER TABLE ONLY archive.port_values_2023
    ADD CONSTRAINT fkhyga7gmggfsi2ky8f4rd3eqkv FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: racks fk142q8kov02jev7iq6wyp7gmqt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.racks
    ADD CONSTRAINT fk142q8kov02jev7iq6wyp7gmqt FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: client_resource_ids fk1go6m7su1h8n0ddp7d3qibphf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_resource_ids
    ADD CONSTRAINT fk1go6m7su1h8n0ddp7d3qibphf FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: dashboard_screen_backgrounds fk1lo8tcc0pfbcfe5ofcb6rclvl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dashboard_screen_backgrounds
    ADD CONSTRAINT fk1lo8tcc0pfbcfe5ofcb6rclvl FOREIGN KEY (screen_id) REFERENCES public.dashboard_screens(id);


--
-- Name: device_peripherals fk33qx0r9snc9eccyalyjaj07p0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_peripherals
    ADD CONSTRAINT fk33qx0r9snc9eccyalyjaj07p0 FOREIGN KEY (category_id) REFERENCES public.device_peripherals_categories(id);


--
-- Name: device_controllers fk381q4tps38n7a5ovphjxsclxl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_controllers
    ADD CONSTRAINT fk381q4tps38n7a5ovphjxsclxl FOREIGN KEY (rack_id) REFERENCES public.racks(id);


--
-- Name: peripheral_access_tokens fk4en9g9s62rm2w5c2j3syc42wt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.peripheral_access_tokens
    ADD CONSTRAINT fk4en9g9s62rm2w5c2j3syc42wt FOREIGN KEY (peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: layers_peripherals_join fk4l4qrqt0qx7d3erhwq9ao6xvx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layers_peripherals_join
    ADD CONSTRAINT fk4l4qrqt0qx7d3erhwq9ao6xvx FOREIGN KEY (device_peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: layers_peripherals_join fk5w9ms4nv9qibsv2x4pt7ypaqs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.layers_peripherals_join
    ADD CONSTRAINT fk5w9ms4nv9qibsv2x4pt7ypaqs FOREIGN KEY (layer_id) REFERENCES public.layers(id);


--
-- Name: users_fav_jobs fk6290ymbenuhayeuitmf0c5j0o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users_fav_jobs
    ADD CONSTRAINT fk6290ymbenuhayeuitmf0c5j0o FOREIGN KEY (job_id) REFERENCES public.jobs(id);


--
-- Name: user_messages fk75hjwni9dvpwdw3gwb1a0fwc8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_messages
    ADD CONSTRAINT fk75hjwni9dvpwdw3gwb1a0fwc8 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: zones_peripherals_join fk79hsoi73imw8ylbjxw2ybxmr8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_peripherals_join
    ADD CONSTRAINT fk79hsoi73imw8ylbjxw2ybxmr8 FOREIGN KEY (peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: cables fk7ttry0l2chymwyad8h6ry3sn5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables
    ADD CONSTRAINT fk7ttry0l2chymwyad8h6ry3sn5 FOREIGN KEY (patch_panel_id) REFERENCES public.rack_patch_panels(id);


--
-- Name: users_fav_jobs fk88thkfntgtd0019lo13i4vwi2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users_fav_jobs
    ADD CONSTRAINT fk88thkfntgtd0019lo13i4vwi2 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: cables_peripherals_join fk8an3vkpi6h380srljmst9hr5i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables_peripherals_join
    ADD CONSTRAINT fk8an3vkpi6h380srljmst9hr5i FOREIGN KEY (peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: zones_cables_join fk8ii05424xgkd3frtdi6jcwk7l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_cables_join
    ADD CONSTRAINT fk8ii05424xgkd3frtdi6jcwk7l FOREIGN KEY (cable_id) REFERENCES public.cables(id);


--
-- Name: cables fk8mjq6ygghowdeyt32sdws09mj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables
    ADD CONSTRAINT fk8mjq6ygghowdeyt32sdws09mj FOREIGN KEY (category_id) REFERENCES public.cable_categories(id);


--
-- Name: cables_peripherals_join fk8ugdvd4h2da44maku6ml4d7kl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables_peripherals_join
    ADD CONSTRAINT fk8ugdvd4h2da44maku6ml4d7kl FOREIGN KEY (cable_id) REFERENCES public.cables(id);


--
-- Name: users_sec_roles fk8uodma1951qcqq07eji1oni2m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users_sec_roles
    ADD CONSTRAINT fk8uodma1951qcqq07eji1oni2m FOREIGN KEY (role_id) REFERENCES public.sec_roles(id);


--
-- Name: notification_rules fk8xwu8u9m20diqv0l761m8fgp2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_rules
    ADD CONSTRAINT fk8xwu8u9m20diqv0l761m8fgp2 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: cables_peripherals_join fk90p7mwjcdps77eefjbptiemdv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables_peripherals_join
    ADD CONSTRAINT fk90p7mwjcdps77eefjbptiemdv FOREIGN KEY (device_peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: sec_user_roles fkaht7rcji1mu16c5hros6bmlev; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sec_user_roles
    ADD CONSTRAINT fkaht7rcji1mu16c5hros6bmlev FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: client_redirect_uris fkai301ylblo02p5381fgie7npr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_redirect_uris
    ADD CONSTRAINT fkai301ylblo02p5381fgie7npr FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: users_sec_roles fkb3dbi4ph19bjx9av4vym2w336; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users_sec_roles
    ADD CONSTRAINT fkb3dbi4ph19bjx9av4vym2w336 FOREIGN KEY (user_authorities_id) REFERENCES public.users(id);


--
-- Name: jobs_tags_join fkb6p1oepftvimh9h4ocldaf3yj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs_tags_join
    ADD CONSTRAINT fkb6p1oepftvimh9h4ocldaf3yj FOREIGN KEY (tag_id) REFERENCES public.jobs_tags(id);


--
-- Name: device_peripherals_categories_cables fkbvideqwaeo82u1u22db09qbd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_peripherals_categories_cables
    ADD CONSTRAINT fkbvideqwaeo82u1u22db09qbd FOREIGN KEY (peripheral_category_cables_id) REFERENCES public.device_peripherals_categories(id);


--
-- Name: device_ports_peripherals_join fkc3sla994x9xwg8jpf725vwahu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_peripherals_join
    ADD CONSTRAINT fkc3sla994x9xwg8jpf725vwahu FOREIGN KEY (peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: job_triggers_cron fkc7y7gmwdx4ngcg8a61onirt9r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_triggers_cron
    ADD CONSTRAINT fkc7y7gmwdx4ngcg8a61onirt9r FOREIGN KEY (job_id) REFERENCES public.jobs(id);


--
-- Name: client_auto_approve_scopes fkc83g7pq7djm5rrdtpmimosf3i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_auto_approve_scopes
    ADD CONSTRAINT fkc83g7pq7djm5rrdtpmimosf3i FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: client_authorities fkcbp3gdu39soenw19wicxiijeo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_authorities
    ADD CONSTRAINT fkcbp3gdu39soenw19wicxiijeo FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: port_values fkcq71a63afih8ear7j2hkcb7bb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.port_values
    ADD CONSTRAINT fkcq71a63afih8ear7j2hkcb7bb FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: push_subscription fkcqj36j93g0f1rliyokmxv1nxk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_subscription
    ADD CONSTRAINT fkcqj36j93g0f1rliyokmxv1nxk FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: peripheral_access_tokens fkcqm221v726trmsds2xpt1ai9i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.peripheral_access_tokens
    ADD CONSTRAINT fkcqm221v726trmsds2xpt1ai9i FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: event_subscriptions fkd7cqysk216dxuhfekrubccctr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_subscriptions
    ADD CONSTRAINT fkd7cqysk216dxuhfekrubccctr FOREIGN KEY (pub_port_id) REFERENCES public.device_ports(id);


--
-- Name: device_ports_cables_join fke347a2a9t82o6rkdj012njmts; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_cables_join
    ADD CONSTRAINT fke347a2a9t82o6rkdj012njmts FOREIGN KEY (cable_id) REFERENCES public.cables(id);


--
-- Name: device_ports_scenarios_join fke4r8s7jony37cu4wid9e4tdh6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_scenarios_join
    ADD CONSTRAINT fke4r8s7jony37cu4wid9e4tdh6 FOREIGN KEY (port_id) REFERENCES public.device_ports(id);


--
-- Name: device_ports fke8qcm1c5vfq2jgvu1yruhqbh7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports
    ADD CONSTRAINT fke8qcm1c5vfq2jgvu1yruhqbh7 FOREIGN KEY (device_id) REFERENCES public.device_controllers(id);


--
-- Name: device_accounts fkeumv8tl5ba7uqvn38jx6q0skb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_accounts
    ADD CONSTRAINT fkeumv8tl5ba7uqvn38jx6q0skb FOREIGN KEY (device_id) REFERENCES public.device_controllers(id);


--
-- Name: job_triggers_event_definitions_join fkfim7y25jb2go34j37ydond8mg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_triggers_event_definitions_join
    ADD CONSTRAINT fkfim7y25jb2go34j37ydond8mg FOREIGN KEY (event_definition_id) REFERENCES public.event_definitions(id);


--
-- Name: event_subscriptions fkfjqu6pe4rplfk7bgh4nrframa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_subscriptions
    ADD CONSTRAINT fkfjqu6pe4rplfk7bgh4nrframa FOREIGN KEY (scenario_id) REFERENCES public.scenarios(id);


--
-- Name: event_definitions_subscriptions_join fkfkbqo7fbb96iagw5uj907nba5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_definitions_subscriptions_join
    ADD CONSTRAINT fkfkbqo7fbb96iagw5uj907nba5 FOREIGN KEY (subscription_id) REFERENCES public.event_subscriptions(id);


--
-- Name: rack_patch_panels fkfxiox7fvh06q6kg37nnbfgsab; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rack_patch_panels
    ADD CONSTRAINT fkfxiox7fvh06q6kg37nnbfgsab FOREIGN KEY (rack_id) REFERENCES public.racks(id);


--
-- Name: device_ports_peripherals_join fkg94tfjvyuh24cvm3aa5mtg2t0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_peripherals_join
    ADD CONSTRAINT fkg94tfjvyuh24cvm3aa5mtg2t0 FOREIGN KEY (port_id) REFERENCES public.device_ports(id);


--
-- Name: job_triggers_event_definitions_join fkghjnm9okyohs6tsg22kxliypm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_triggers_event_definitions_join
    ADD CONSTRAINT fkghjnm9okyohs6tsg22kxliypm FOREIGN KEY (trigger_id) REFERENCES public.job_triggers_events(id);


--
-- Name: client_scopes fkh2i5jh2otc2cui0gyr6g6nc1x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_scopes
    ADD CONSTRAINT fkh2i5jh2otc2cui0gyr6g6nc1x FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: event_definitions_subscriptions_join fkh4v14f42n7tkoww5i946r1ofl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_definitions_subscriptions_join
    ADD CONSTRAINT fkh4v14f42n7tkoww5i946r1ofl FOREIGN KEY (event_definition_id) REFERENCES public.event_definitions(id);


--
-- Name: zones_peripherals_join fkh96qo0b4wvqik2bb5fv9ehsc3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_peripherals_join
    ADD CONSTRAINT fkh96qo0b4wvqik2bb5fv9ehsc3 FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: device_backup fkh9a9awlj7es6qa979dt3eno3b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_backup
    ADD CONSTRAINT fkh9a9awlj7es6qa979dt3eno3b FOREIGN KEY (device_id) REFERENCES public.device_controllers(id);


--
-- Name: jobs_tags_join fkhd2et6ttp9hwumwmdl1n4ah2n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs_tags_join
    ADD CONSTRAINT fkhd2et6ttp9hwumwmdl1n4ah2n FOREIGN KEY (job_id) REFERENCES public.jobs(id);


--
-- Name: port_values fkhyga7gmggfsi2ky8f4rd3eqkv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.port_values
    ADD CONSTRAINT fkhyga7gmggfsi2ky8f4rd3eqkv FOREIGN KEY (event_id) REFERENCES public.event_log(id);


--
-- Name: job_triggers_events fkiicrbteqlfb6qfs25gqioi130; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_triggers_events
    ADD CONSTRAINT fkiicrbteqlfb6qfs25gqioi130 FOREIGN KEY (job_id) REFERENCES public.jobs(id);


--
-- Name: cables fkiig7qe84upyjs0dqe2a0yok1e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cables
    ADD CONSTRAINT fkiig7qe84upyjs0dqe2a0yok1e FOREIGN KEY (rack_id) REFERENCES public.racks(id);


--
-- Name: device_ports_cables_join fkiqt93emcdv5nk80su0ojghvb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_cables_join
    ADD CONSTRAINT fkiqt93emcdv5nk80su0ojghvb FOREIGN KEY (port_id) REFERENCES public.device_ports(id);


--
-- Name: device_ports_scenarios_join fkir1xktp2rssjs05ux4wjo8lrs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_scenarios_join
    ADD CONSTRAINT fkir1xktp2rssjs05ux4wjo8lrs FOREIGN KEY (scenario_id) REFERENCES public.scenarios(id);


--
-- Name: zones_devices_join fkjuoxw215x4bv6p98mnr7fps50; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_devices_join
    ADD CONSTRAINT fkjuoxw215x4bv6p98mnr7fps50 FOREIGN KEY (device_id) REFERENCES public.device_controllers(id);


--
-- Name: shared_widget_audit fkjxxfccl0x22jsiibtw8wo80x8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_widget_audit
    ADD CONSTRAINT fkjxxfccl0x22jsiibtw8wo80x8 FOREIGN KEY (shared_widget_id) REFERENCES public.shared_widgets(id);


--
-- Name: sec_user_roles fkk055j973dbr211p28c0uf5i2h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sec_user_roles
    ADD CONSTRAINT fkk055j973dbr211p28c0uf5i2h FOREIGN KEY (role_id) REFERENCES public.sec_roles(id);


--
-- Name: job_execution_history fkkxs91vlq2r43d7q7lkp2dig6f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_execution_history
    ADD CONSTRAINT fkkxs91vlq2r43d7q7lkp2dig6f FOREIGN KEY (job_id) REFERENCES public.jobs(id);


--
-- Name: client_authorized_grant_types fkl32hlober4h1mers03qn5t5rq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client_authorized_grant_types
    ADD CONSTRAINT fkl32hlober4h1mers03qn5t5rq FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: zones_categories fklv88j5iluc9c35xmupj5lpjaq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_categories
    ADD CONSTRAINT fklv88j5iluc9c35xmupj5lpjaq FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: device_controllers fknxvlpcqrpr1nwtsxm8lp0ig5y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_controllers
    ADD CONSTRAINT fknxvlpcqrpr1nwtsxm8lp0ig5y FOREIGN KEY (type_id) REFERENCES public.device_categories(id);


--
-- Name: zones_cables_join fko8nvao35juubw6dd3bastaddo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_cables_join
    ADD CONSTRAINT fko8nvao35juubw6dd3bastaddo FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: jobs fkp7ucf5dilyl4boojcpxmubmlu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs
    ADD CONSTRAINT fkp7ucf5dilyl4boojcpxmubmlu FOREIGN KEY (scenario_id) REFERENCES public.scenarios(id);


--
-- Name: device_ports_scenarios_join fkpnv94scvh7xgoid5o5kfhdl4v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_scenarios_join
    ADD CONSTRAINT fkpnv94scvh7xgoid5o5kfhdl4v FOREIGN KEY (scenario_id) REFERENCES public.scenarios(id);


--
-- Name: zones_devices_join fkq24w9x1apo0shlufeyptab7s2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones_devices_join
    ADD CONSTRAINT fkq24w9x1apo0shlufeyptab7s2 FOREIGN KEY (zone_id) REFERENCES public.zones(id);


--
-- Name: device_peripherals_categories_cables fkqoo0q5aqqynjkci6m8os4jwlq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_peripherals_categories_cables
    ADD CONSTRAINT fkqoo0q5aqqynjkci6m8os4jwlq FOREIGN KEY (cable_id) REFERENCES public.cables(id);


--
-- Name: access_token_scope fkrgeltco2v6wi4q9lkmtesxvd1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_token_scope
    ADD CONSTRAINT fkrgeltco2v6wi4q9lkmtesxvd1 FOREIGN KEY (access_token_id) REFERENCES public.access_token(id);


--
-- Name: zones fkry6fspsbsl40x3x8ge6b1s4ov; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.zones
    ADD CONSTRAINT fkry6fspsbsl40x3x8ge6b1s4ov FOREIGN KEY (parent_id) REFERENCES public.zones(id);


--
-- Name: device_ports_scenarios_join fksocqyutnuw2ex8av3wynva766; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_ports_scenarios_join
    ADD CONSTRAINT fksocqyutnuw2ex8av3wynva766 FOREIGN KEY (port_id) REFERENCES public.device_ports(id);


--
-- Name: jobs fkspkv1pok7n0ye7ht5vq2fwccd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs
    ADD CONSTRAINT fkspkv1pok7n0ye7ht5vq2fwccd FOREIGN KEY (peripheral_id) REFERENCES public.device_peripherals(id);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES public.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES public.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES public.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES public.qrtz_triggers(sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_sched_name_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES public.qrtz_job_details(sched_name, job_name, job_group);


--
-- PostgreSQL database dump complete
--

\unrestrict oDyDUg6ToqQIMsMtw2japAJ2KTrqOaFCl3abMBVJGys2eCkg00FGYgH77C6btUd

