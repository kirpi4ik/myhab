# myHAB public demo

A public sandbox of myHAB — a fictional home of seven zones and twenty-one peripherals,
with simulated devices — so someone can try the product without an account, hardware, or
an install.

Everything here is disposable by design.

---

## How it behaves

**Shared, not per-visitor.** Every visitor works on the same dataset. There is no
per-session isolation: the app has a single datasource, Quartz has its own, and the
MQTT and WebSocket background threads carry no tenant context, so real isolation would
mean either a database per session with a routing datasource, or a container per
visitor. Neither is worth it here.

Instead the sandbox **resets**, which gets most of the way there for a fraction of the
cost:

| Trigger | When |
|---|---|
| Idle | Someone changed something, and nobody has changed anything for `myhab.demo.idleMinutes` (default 20) |
| Manual | The **Reset demo** button in the banner |
| Nightly | Once a day, unconditionally, at `myhab.demo.nightlyResetHour` |

In practice the next visitor almost always finds a clean house. The banner says plainly
that the data is shared, so nobody is misled.

A reset needs no restart. Hibernate's second-level cache is off, so restoring at the
database level is immediately visible to the running app.

**Devices are simulated.** `demo/simulator` answers MQTT commands as if it were the
ESP32 controllers. This is not decoration: `PowerService` publishes a command and then
waits for the device to echo its new state back, and that echo is what updates the
database and the UI. With nothing answering the broker, every control in the demo would
appear dead.

One device (`esp_shed`, the Shed Light) is offline on purpose, so a visitor can see what
an unreachable device looks like without it blocking anything they would want to use.

**Accounts** are published on the login page — `demo` / `demo` (user) and
`demo-admin` / `demo-admin` (admin). They protect nothing.

---

## Running it locally

No Docker required. Prerequisites are the same as ordinary development: JDK 17, a local
PostgreSQL, Node/Yarn.

```bash
./gradlew demoSeedLocal   # (re)build the myhab_demo database — destructive, idempotent
./gradlew demoSim         # simulator + embedded MQTT broker on :1883
./gradlew demoRun         # the backend, in the `demo` environment
./gradlew serve           # Quasar dev server on :10002
```

Then open <http://localhost:10002> and sign in with `demo` / `demo`.

Start `demoSim` **before** `demoRun`: the app connects to the broker at boot, and the
simulator hosts it.

`demoSeedLocal` drops and recreates the database, disconnecting anything attached —
including a running `demoRun`. That is deliberate; restart the app afterwards.

| Task | Does |
|---|---|
| `demoSeedLocal` | Drop and rebuild `myhab_demo` from `demo/seed` |
| `demoConfigRepo` | Materialise `build/demo-config.git` from `demo/config` (a `demoRun` dependency) |
| `demoSim` | Simulator with `--embedded-broker`, so no mosquitto install is needed |
| `demoRun` | `bootRun` with `grails.env=demo` and the demo's env vars |

Override the database with `DEMO_DB`, `DEMO_DB_USER`, `DEMO_DB_PASSWORD`,
`DEMO_DB_HOST`, `DEMO_DB_PORT`.

---

## What lives here

```
demo/
  config/            config.yaml seeding the git-backed ConfigProvider — no secrets
  seed/              schema.sql, demo-entities.sql, demo-screens.sql,
                     demo-seed-schema.sql, devices.json
  simulator/         the :demo-simulator Gradle module
  demo.gradle        the tasks above (applied from the root build.gradle)
  Dockerfile         builds the myhab-demo image (simulator + seed + config)
```

Code that ships inside the application stays with the application:
`DemoService`, `DemoController`, `DemoResetJob`, `DemoActivityInterceptor` under
`server/server-core/`, `DemoBanner.vue` under `client/web-vue3/`, and the
`environments: demo:` block in `application.yml`.

### The demo image

CI publishes `kirpi4ik/myhab-demo:<version>` alongside `kirpi4ik/myhab:<version>` from the
same commit. It carries the simulator jar, the seed SQL and the ConfigProvider seed, and
runs in one of two roles:

| Command | Does |
|---|---|
| `init` | Stage the seed SQL into the database's init directory and build the bare config repo, then exit |
| `sim` | Run the device simulator against the broker |

Deployment — compose, networking, TLS and the public hostname — is not defined in this
repository. A deployment needs: a PostgreSQL seeded by `init`, an MQTT broker, the
`sim` container, and the application with `GRAILS_ENV=demo`, `DB_URL`, `JWT_SECRET` and
`CFG_REPO_URI` pointing at the bare repo `init` produced.

### Configuration

The demo uses its own standalone config repository, built from `demo/config/` into a
bare repo and cloned over `file://` — never a production config repo. The reason is
specific and worth stating: the demo publishes an admin account, and the `appConfig`
GraphQL query returns **every** configuration key to any admin-authenticated caller. Any
secret reachable from the demo's configuration is a published secret.

`demo/config/config.yaml` therefore contains only demo-safe values, and deliberately
omits `telegram.*`, `opsgenie.*` and `push.vapid.*` — each of those degrades cleanly when
absent. Nothing secret may be added to it. Verify from the visitor's side rather than by
reading the file: log in as `demo-admin`, open the app config screen, and confirm there is
nothing there you would not publish.

### Hardening

myHAB was written for a private LAN behind a router. A deployment that faces the open
internet should refuse the endpoints that assume that setting — at the reverse proxy, so
the demo runs the same code everything else does — and rate-limit `/api/login` and
`/api/public/demo/reset`.

---

## Isolation

The demo must not be able to reach anything real: no production database, no LAN
controllers, no third-party clouds.

- Every job that reaches an external system is off in `environments: demo:`
  (`application.yml`): Huawei, NIBE, Navimow, meteo, the HTTP device sync and the config
  sync. Telegram is off via `myhab.telegram.enabled`.
- Hazelcast runs single-node with multicast disabled, on cluster `myhab-demo`.
- The configuration repository is local to the deployment (above).

Network isolation is the deployment's job and should be verified there, not assumed —
the app should have no route off its own network.

---

## Changing the demo

**The dataset.** Edit `demo/seed/demo-entities.sql`, then `./gradlew demoSeedLocal`.
It holds the whole fictional installation: zones, controllers, ports, peripherals,
cabling, scenarios, the scheduled irrigation job, and both accounts' inbox messages.

Two constraints that will bite otherwise:

- **Device codes must not contain hyphens.** Outbound command topics are built from a
  template, but inbound state topics are matched with
  `myhab/(\w+|_+)/(\w+|_+)/(\w+|_+)/state`, and `\w` excludes `-`. A hyphenated code
  publishes commands happily and then silently drops every echo — the control looks
  dead, with nothing in the logs.
- **Peripheral category names must be** `LIGHT`, `SWITCH`, `HEAT`, `SPRINKLER` or
  `TEMP`. `ZoneCombinedView.vue` maps exactly those to cards; anything else renders
  nothing.

Keep `demo/seed/devices.json` in step — the simulator drives everything from it, and a
port that exists in one and not the other simply will not work.

**The /wui screens.** `demo/seed/demo-screens.sql` carries the two floor plans and the
widgets placed on them. The background images are embedded as base64 rather than read
from files, because this SQL is executed by the postgres container, which receives only
the `.sql` files — a path would resolve inside the database container. Easiest way to
change a layout is to drag the widgets in **Admin → Screens** and dump the resulting
`layout_json` back into that file.

**The schema.** `demo/seed/schema.sql` is a tracked DDL snapshot, because the demo has to
be able to build its database from a clean checkout and inside the image. It carries what
`dbCreate=update` cannot create for itself: Quartz's `qrtz_*` tables, the partitioned
`event_log` and `port_values`, and the `archive` schema. Regenerate it with
`pg_dump --schema-only` against an up-to-date database when the domain model changes;
`dbCreate=update` covers simple additions in the meantime.

Regenerating it couples the file to a PostgreSQL major version: a dump restores only
into its own version or newer, and from 18 on `pg_dump` emits `\restrict` /
`\unrestrict`, which older `psql` rejects on sight — the init script dies on its first
line, and because the data directory is no longer empty, every restart afterwards skips
initialisation and serves an empty database. Whatever postgres a deployment runs for the
demo must be at least as new as the machine the dump came from.

**Reset behaviour.** `DemoService` truncates every table mirrored in the `seed` schema
and copies the rows back, then rebases every timestamp by `now() - seed_meta.built_at`
so the charts always end "now" and the seed never has to be rebuilt to stay plausible.

The table list is derived from the `seed` schema at runtime rather than hard-coded, so
adding a domain class does not silently leave a table un-restored.

It refuses to run unless the environment is `demo` **and** a `seed` schema exists.
A normal deployment has neither, so the reset is inert there even if someone
misconfigures the environment.
