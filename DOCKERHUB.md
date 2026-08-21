# myHAB

[![Latest version](https://img.shields.io/docker/v/kirpi4ik/myhab?sort=semver&label=latest%20release)](https://hub.docker.com/r/kirpi4ik/myhab/tags)
[![Image size](https://img.shields.io/docker/image-size/kirpi4ik/myhab/latest?label=image)](https://hub.docker.com/r/kirpi4ik/myhab/tags)
[![Licence](https://img.shields.io/badge/licence-LGPLv3-blue)](https://www.gnu.org/licenses/lgpl-3.0.html)

**Self-hosted home automation backend, under the LGPLv3.** MQTT device control, solar and
heat-pump monitoring, floor-plan dashboards, scenario automation, an LLM voice assistant
and tokenised guest links — running on your own hardware, with no vendor cloud in the
control path.

One JVM process, one PostgreSQL database, one MQTT broker. That is the whole system.

📖 [myhab.org](https://myhab.org/) · 📚 [Documentation](https://myhab.org/docs/) ·
💻 [Source](https://github.com/kirpi4ik/myhab) · 🚀 [Live demo](http://demo.myhab.org/)

---

## Try it before you install it

A complete, fully interactive installation runs at **[demo.myhab.org](http://demo.myhab.org/)**
with simulated devices answering on a real MQTT broker.

| Account | Credentials |
| --- | --- |
| User | `demo` / `demo` |
| Admin | `demo-admin` / `demo-admin` |

Data is shared between visitors and resets when idle.

---

## Tags

| Tag | What it is |
| --- | --- |
| `latest` | The most recent successful CI build. Convenient, but it can come from a pre-release branch — pin a version for anything you care about. |
| `2.8.12`, `2.8.11`, … | Release builds from `master`. **Use these in production.** |
| `2.8.12-a1b2c3` | Version plus short commit SHA — pre-release builds from other branches. |

**Platform:** `linux/amd64` only. There is no arm64 image, so a Raspberry Pi or an Apple
Silicon host needs to build its own (`./gradlew buildImage` on the target, or `docker
buildx build --platform linux/arm64`).

---

## What is in the image

| | |
| --- | --- |
| Base | `eclipse-temurin:17-jre` |
| Port | `8181` (HTTP — put a TLS-terminating reverse proxy in front of it) |
| Volume | `/app/config` — an external `application.yml` here is layered over the built-in defaults |
| Workdir | `/app` |
| Entrypoint | `/app/app-entrypoint.sh` → `java -jar /app/myhab.jar` |
| Timezone | The JVM is pinned to UTC; set `TZ` for the container's local time |
| Health | `GET /actuator/healthcheck` |

The web client (Vue 3 + Quasar PWA) is bundled in the same jar and served from the same
port — there is no separate frontend container.

---

## Quick start

myHAB needs three things: a PostgreSQL database, an MQTT broker, and a **git repository**
holding its runtime configuration.

### 1. Create the configuration repository

Configuration lives in git rather than in the image, so you can change broker
credentials, feature flags and dashboard bindings without rebuilding or restarting
anything — and you get a history of every change. Create a private repository (it holds
credentials) with one branch per environment; production reads `prod` by default.

```bash
mkdir myhab-config && cd myhab-config
git init -b prod
cat > config.yaml <<'YAML'
mqtt:
  hostname: mosquitto
  port: 1883
  username: myhab
  password: change-me
  topics: myhab/#

cors:
  allowedOrigin:
    - https://home.example.com

ui:
  meteo:
    locationName: My Town
YAML
git add . && git commit -m "Initial myHAB configuration"
git push origin prod
```

### 2. Start the stack

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: myhab
      POSTGRES_USER: myhab
      POSTGRES_PASSWORD: change-me
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped

  mosquitto:
    image: eclipse-mosquitto:2
    volumes:
      - ./mosquitto:/mosquitto/config
    ports:
      - "1883:1883"
    restart: unless-stopped

  myhab:
    image: kirpi4ik/myhab:2.8.12   # pin a version; the badge above shows the newest
    depends_on: [postgres, mosquitto]
    ports:
      - "8181:8181"
    environment:
      GRAILS_ENV: production
      TZ: Europe/Bucharest
      DB_URL: jdbc:postgresql://postgres:5432/myhab
      DB_USERNAME: myhab
      DB_PASSWORD: change-me
      JWT_SECRET: <a long random string>
      CFG_REPO_URI: https://github.com/you/myhab-config.git
      CFG_USERNAME: <git user>
      CFG_PASSWORD: <git token>
    volumes:
      - ./config:/app/config
    restart: unless-stopped

volumes:
  pgdata:
```

```bash
docker compose up -d
```

The schema is created on first start by Hibernate (`dbCreate = update`): it adds tables
and columns as the model grows, and never drops anything. Give it a minute, then check
`http://localhost:8181/actuator/healthcheck`.

### 3. Create the first account

**There is no default admin user** — nothing is seeded, so nobody can log in until you
insert an account. Generate a BCrypt hash:

```bash
htpasswd -bnBC 10 "" 'your-password' | tr -d ':\n'
```

Then, against the `myhab` database:

```sql
INSERT INTO sec_roles (id, version, authority) VALUES
  (1, 0, 'ROLE_USER'),
  (2, 0, 'ROLE_ADMIN')
ON CONFLICT DO NOTHING;

-- note: `users` has no version column
INSERT INTO users (id, username, password, first_name, last_name, email,
                   enabled, account_locked, account_expired, password_expired,
                   ts_created, ts_updated, en_type, language, timezone)
VALUES (1, 'admin', '<bcrypt hash>', 'Site', 'Admin', 'admin@example.com',
        true, false, false, false, now(), now(), 'USER', 'en', 'UTC');

INSERT INTO sec_user_roles (user_id, role_id) VALUES (1, 1), (1, 2);
```

The join table is `sec_user_roles`, not `users_sec_roles`. Log in at
`http://localhost:8181/` and create the rest of your users in the UI.

---

## Environment variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_URL` | ✅ | JDBC URL, e.g. `jdbc:postgresql://postgres:5432/myhab`. `?TimeZone=UTC` is appended if absent. |
| `DB_USERNAME` | ✅ | Database user. |
| `DB_PASSWORD` | ✅ | Database password. |
| `JWT_SECRET` | ✅ | HS256 signing secret for API tokens. Long and random; changing it invalidates every session. |
| `CFG_REPO_URI` | ✅ | Configuration git repository (`https://…` or `file:///…`). |
| `CFG_USERNAME` | | Git user for the configuration repository. |
| `CFG_PASSWORD` | | Git token/password for the configuration repository. |
| `GRAILS_ENV` | | `production` (the image default). |
| `TZ` | | Container local time. Timestamps are stored in UTC regardless. |

Anything else — job intervals, trusted proxies, vendor integrations — goes either in the
configuration repository or in an `application.yml` inside the `/app/config` volume,
which Spring Boot layers over the image's defaults.

---

## Behind a reverse proxy

Terminate TLS at nginx/Caddy/Traefik and proxy to `8181`. Tell myHAB which proxy it can
believe, or every audited action will be attributed to the proxy's own address — in
`/app/config/application.yml`:

```yaml
myhab:
  security:
    trustedProxies:
      - 172.18.0.1
```

WebSocket upgrades must be passed through: real-time port state, dashboards and the
voice assistant all use STOMP over WebSocket.

---

## Upgrading

```bash
docker compose pull && docker compose up -d
```

Schema changes are applied at startup and are not reversible — back up the database
first. Read the release notes for the versions you are skipping:
[github.com/kirpi4ik/myhab/releases](https://github.com/kirpi4ik/myhab/releases).

---

## Companion image

`kirpi4ik/myhab-demo` carries the assets for the public demo sandbox built from the same
commit: the MQTT device simulator, the seed dataset and the configuration seed. It is
only useful if you are running a demo installation of your own — a normal deployment
does not need it.

---

## Licence

myHAB is developed under the **GNU Lesser General Public License v3 (LGPLv3)** —
[gnu.org/licenses/lgpl-3.0.html](https://www.gnu.org/licenses/lgpl-3.0.html).

Run it, modify it and deploy it freely, commercially included. Distributing a modified
myHAB means releasing those modifications under the LGPLv3 as well; software that merely
links against it keeps its own licence.

---

## Documentation

| | |
| --- | --- |
| Installation & configuration | [myhab.org/docs/install.html](https://myhab.org/docs/install.html) |
| Every configuration key | [myhab.org/docs/configuration.html](https://myhab.org/docs/configuration.html) |
| Supported devices & integrations | [myhab.org/docs/integrations.html](https://myhab.org/docs/integrations.html) |
| Architecture | [myhab.org/docs/architecture.html](https://myhab.org/docs/architecture.html) |
| FAQ | [myhab.org/docs/faq.html](https://myhab.org/docs/faq.html) |

Issues and pull requests: [github.com/kirpi4ik/myhab](https://github.com/kirpi4ik/myhab).
