#!/bin/sh
#
# Entrypoint for the kirpi4ik/myhab-demo image. Two roles, selected by the first
# argument — see demo/Dockerfile and demo/README.md.
#
set -e

BROKER="${MQTT_BROKER:-tcp://demo-mqtt:1883}"
MQTT_USERNAME="${MQTT_USERNAME:-demo}"
MQTT_PASSWORD="${MQTT_PASSWORD:-demo}"

# Where the app expects to find them (both are shared volumes in the compose file).
DB_INIT_DIR="${DB_INIT_DIR:-/db-init}"
CONFIG_REPO_DIR="${CONFIG_REPO_DIR:-/config-repo/demo-config.git}"

case "$1" in
  init)
    echo "[demo-init] staging seed SQL into ${DB_INIT_DIR}"
    mkdir -p "${DB_INIT_DIR}"
    # postgres runs /docker-entrypoint-initdb.d/* in filename order, and only on an
    # empty data directory — so this is a first-boot operation, not every start.
    cp /demo/seed/schema.sql        "${DB_INIT_DIR}/01-schema.sql"
    cp /demo/seed/demo-entities.sql "${DB_INIT_DIR}/02-entities.sql"
    cp /demo/seed/demo-screens.sql  "${DB_INIT_DIR}/02b-screens.sql"
    cp /demo/seed/demo-seed-schema.sql "${DB_INIT_DIR}/03-seed-schema.sql"

    echo "[demo-init] building the config repository at ${CONFIG_REPO_DIR}"
    # ConfigProvider clones over file://, so a bare repo on a shared volume is enough:
    # no git server, and nothing that could reach the production config repo.
    WORK=$(mktemp -d)
    cp /demo/config/* "${WORK}/"
    cd "${WORK}"
    git init --quiet --initial-branch=demo
    git -c user.email=demo@example.invalid -c user.name="myHAB demo" add .
    git -c user.email=demo@example.invalid -c user.name="myHAB demo" \
        commit --quiet -m "Demo configuration seed"
    rm -rf "${CONFIG_REPO_DIR}"
    mkdir -p "$(dirname "${CONFIG_REPO_DIR}")"
    git clone --quiet --bare --branch demo "${WORK}" "${CONFIG_REPO_DIR}"
    rm -rf "${WORK}"

    echo "[demo-init] done"
    ;;

  sim)
    echo "[demo-sim] connecting to ${BROKER}"
    exec java -jar /demo/simulator.jar \
      --broker "${BROKER}" \
      --username "${MQTT_USERNAME}" \
      --password "${MQTT_PASSWORD}" \
      --devices /demo/seed/devices.json
    ;;

  *)
    echo "usage: $0 {init|sim}" >&2
    exit 1
    ;;
esac
