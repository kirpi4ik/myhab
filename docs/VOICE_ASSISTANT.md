# Voice Assistant

The voice assistant lets users control myHAB by speaking (or typing) natural-language
commands in any language — primarily **English and Romanian**. An LLM "agent" maps a
phrase to concrete actions against your real devices, executes them through the same
engine the web UI uses, and replies out loud.

> Examples: *"turn off the terrace lights"*, *"aprinde lumina din birou"*,
> *"run movie mode"*, *"is the garage door open?"*, *"start mowing"* / *"pornește tunsul"*.

This document covers how it was designed, how to configure and use it, and how to
extend it with new commands.

---

## Table of Contents

- [1. Architecture & Design](#1-architecture--design)
  - [1.1 End-to-end flow](#11-end-to-end-flow)
  - [1.2 The agentic tool-use loop](#12-the-agentic-tool-use-loop)
  - [1.3 Grounding: the catalog](#13-grounding-the-catalog)
  - [1.4 Tools and how they execute](#14-tools-and-how-they-execute)
  - [1.5 Provider abstraction (Anthropic / OpenAI)](#15-provider-abstraction-anthropic--openai)
  - [1.6 Conversation state](#16-conversation-state)
  - [1.7 Text-to-speech](#17-text-to-speech)
  - [1.8 Key design decisions](#18-key-design-decisions)
- [2. Configuration](#2-configuration)
  - [2.1 Config keys](#21-config-keys)
  - [2.2 LLM provider & API key](#22-llm-provider--api-key)
  - [2.3 Google neural TTS](#23-google-neural-tts)
  - [2.4 Language](#24-language)
- [3. Usage](#3-usage)
- [4. Extending with new commands](#4-extending-with-new-commands)
  - [4.1 Add a new tool](#41-add-a-new-tool-most-common)
  - [4.2 Add a new LLM provider](#42-add-a-new-llm-provider)
  - [4.3 Add a new TTS provider](#43-add-a-new-tts-provider)
- [5. Operations & troubleshooting](#5-operations--troubleshooting)
- [6. Security](#6-security)
- [7. Limitations / out of scope](#7-limitations--out-of-scope)
- [8. File reference](#8-file-reference)
- [9. Tests & verification](#9-tests--verification)

---

## 1. Architecture & Design

### 1.1 End-to-end flow

```
┌─────────────── Browser / PWA ───────────────┐        ┌──────────────── Backend ─────────────────┐
│ VoiceControl.vue                             │        │ Mutation.voiceCommand (GraphQL)           │
│  • Web Speech API (STT, ro-RO / en-US)       │ GraphQL│   → VoiceCommandService.handleTranscript  │
│  • transcript ──────────────────────────────┼───────▶│       1. build catalog (live DB)          │
│  • plays reply (neural MP3 or browser TTS)   │◀───────┼───    2. agentic loop with the LLM        │
│  • keeps sessionId for multi-turn            │ result │       3. execute tool calls               │
└──────────────────────────────────────────────┘        │       4. (optional) synthesize TTS        │
                                                         └───────────────────┬───────────────────────┘
                                                                             │ tools call existing services
                                              ┌──────────────────────────────┼──────────────────────────────┐
                                              ▼                              ▼                              ▼
                                   evt_switch → UIMessageService   SchedulerService.triggerJob   NavimowCommandService
                                   → PowerService → MQTT           (run scenario)                 (mower REST)
```

- **Speech-to-text** happens **in the browser** via the Web Speech API
  (`webkitSpeechRecognition`). The recognition language follows the app's UI language
  (see [2.4](#24-language)). Users can also **type** a command (STT-independent).
- The transcript is sent to the backend through a single GraphQL mutation,
  `voiceCommand(transcript, locale, sessionId)`.
- The backend is **client-agnostic** — any future client (native app, etc.) can reuse
  the same mutation.

### 1.2 The agentic tool-use loop

The core is **not** a one-shot classifier. `VoiceCommandService.handleTranscript`
runs a bounded **tool-use loop** (`MAX_ITERATIONS = 6`):

1. Build the [catalog](#13-grounding-the-catalog) of controllable entities.
2. Load prior conversation messages for the `sessionId` (or start fresh).
3. Append the user transcript and call the configured LLM provider's `converse(...)`.
4. The model returns either **tool calls** (execute them, feed results back, loop) or a
   **final text** reply (done).
5. Persist the updated conversation; synthesize TTS if enabled; return the result.

**Clarification is emergent.** When a request is ambiguous, the model simply ends its
turn with a question (no tool call) instead of guessing. The client speaks the
question, keeps the `sessionId`, and the user's next utterance continues the
conversation. No dedicated "ask" tool is needed.

`awaitingReply` in the result is `true` only when nothing was actioned **and** the reply
ends with `?` — the client uses it to auto-reopen the mic after a clarifying question
(but not after a plain answer).

### 1.3 Grounding: the catalog

The LLM never invents ids. On every request the backend builds a fresh **catalog** from
the live database and passes it (as JSON) in the prompt. The model may only choose ids
that appear in it, and the server re-validates every id before acting (hallucination
guard). Catalog shape (`VoiceCommandService.buildCatalog`):

```jsonc
{
  "peripherals": [ { "id", "name", "category", "zones": [..], "aliases": [..] } ],
  "zones":       [ { "id", "name", "peripherals": [names], "aliases": [..] } ],
  "scenarios":   [ { "jobId", "name", "description" } ],   // ACTIVE jobs with a scenario
  "mowers":      [ { "deviceId", "name" } ]                // Devices with model NAVIMOW_SEGWAY
}
```

- **Peripherals** = those with at least one connected port.
- **Zones** include their peripheral names (recursively) so the model can target a whole
  area; only zones that contain peripherals are listed.
- **Aliases** are optional alternate names (e.g. Romanian) stored in the `Configuration`
  sidecar under key `feature.voice.alias`, surfaced here to improve matching.

At home scale the whole catalog is a few KB; **prompt caching** (Anthropic
`cache_control: ephemeral` on the system+catalog block) keeps repeated commands cheap.
There is intentionally **no RAG/vector store and no MCP** — they add failure modes and
cost without solving a real capacity problem at this scale.

### 1.4 Tools and how they execute

Tools are defined vendor-neutrally in `VoiceTools.TOOLS` and executed in
`VoiceCommandService.executeTool`. Each executor validates ids against the catalog and
returns `{ content, stateChange, action }` (the `content` string is fed back to the
model).

| Tool | Args | Executes via | Notes |
|------|------|--------------|-------|
| `control_entity` | `entityType` ∈ PERIPHERAL/ZONE/PORT, `id`, `action` ∈ ON/OFF/TOGGLE | publishes an **`evt_switch`** event → `UIMessageService.handleSwitchEvent` → `PowerService` → MQTT | ZONE switches every peripheral in the area recursively. Same path the Web UI uses, incl. audit logging. |
| `run_scenario` | `jobId` | `SchedulerService.triggerJob(jobId)` | Only ACTIVE jobs with a scenario are in the catalog. |
| `query_state` | `entityType`, `id` | reads `DevicePort.state` / `DevicePort.value` | Read-only; answers questions, changes nothing. |
| `mower_command` | `deviceId`, `action` ∈ START/STOP/PAUSE/RESUME/DOCK | `NavimowCommandService.execute([deviceId, action])` | Segway Navimow over its cloud REST API. |

The **execution seam** is deliberately the existing `evt_switch` event (handled by
`UIMessageService`) rather than calling `PowerService` directly — this reuses the
generic PERIPHERAL/PORT/ZONE port-resolution and the audit log for free.

If a tool throws (e.g. the mower cloud rejects a command), the loop catches it, feeds an
`ERROR: ...` result back to the model (so it can explain to the user), and keeps the
conversation consistent — it does **not** abort the turn.

### 1.5 Provider abstraction (Anthropic / OpenAI)

`VoiceIntentProvider` is the vendor seam. The orchestration (loop, tools, history) lives
in `VoiceCommandService`; a provider is a **pure translator**:

```groovy
interface VoiceIntentProvider {
    String name()          // 'anthropic' | 'openai'  — matches feature.voice.llm.provider
    String defaultModel()
    LlmTurn converse(String systemPrompt, String catalogJson, List<Map> messages,
                     List<Map> tools, String model, String apiKey)
}
```

- The conversation history is kept in a **neutral** shape (vendor-independent):
  - `{ role:'user', text }`
  - `{ role:'assistant', text, toolCalls:[{id,name,input}] }`
  - `{ role:'tool', toolResults:[{id,name,content}] }`
- Each provider translates this neutral list into its native request per call
  (`AnthropicIntentProvider` → Messages API `tool_use`/`tool_result`;
  `OpenAiIntentProvider` → Chat Completions `tool_calls` / `role:tool`), and normalizes
  the response into an `LlmTurn` (`toolCalls` + `finalText`).
- Providers are plain classes instantiated in the `VoiceCommandService.providers` map;
  selection is by `feature.voice.llm.provider`.

All outbound HTTP for the LLM/TTS uses a **dedicated Unirest instance** (`VoiceHttp`)
with a generous 60s read timeout, isolated from the global 2s timeout that
`ConfigProvider` sets for fast Gitea config pings.

### 1.6 Conversation state

Multi-turn state is stored **server-side** in **Hazelcast**, keyed by a client
`sessionId`:

- Map name `voiceSessions`, TTL `SESSION_TTL_SEC = 300` (5 min), capped at
  `MAX_HISTORY = 40` messages (trimmed only at a clean user-message boundary so
  `tool_use`/`tool_result` pairs stay intact).
- The client passes the returned `sessionId` back on each follow-up. The backend owns
  the (neutral) history, so vendor message shapes never leak to the SPA.
- On a failed turn the history is **not** persisted, so a broken sequence can never
  poison the next turn.

### 1.7 Text-to-speech

The spoken reply can be rendered by a neural voice instead of the browser's robotic one:

- `VoiceTtsProvider` is the vendor seam; `GoogleTtsProvider` calls Google Cloud
  Text-to-Speech and returns MP3 bytes (base64 in the result as `audioContent`).
- Google TTS **does not accept API keys** — it requires a **service account**.
  `GoogleAuth` exchanges a service-account JSON for a short-lived OAuth2 token
  (JWT-bearer grant, RS256, pure JDK), cached ~1h, and the TTS call uses a `Bearer`
  token.
- The client plays `audioContent` if present, otherwise falls back to the browser's
  `SpeechSynthesis`. If TTS errors for any reason, the result simply omits audio and the
  browser voice is used — never a hard failure.

### 1.8 Key design decisions

- **Agentic loop over one-shot classification** — enables zone targeting, scenarios,
  Q&A, multi-step, and clarification with one mechanism.
- **Reuse the existing execution engine** (`evt_switch`/`UIMessageService`,
  `SchedulerService`, `NavimowCommandService`) — no parallel control path to maintain.
- **Catalog grounding + server-side validation** — the model can't act on a device that
  doesn't exist; names need not be globally unique.
- **Vendor-neutral core** — swap LLM by config; both vendors do the same thing because
  the prompt, tools and JSON schema are shared (`VoiceTools`).
- **Prompt caching, not RAG/MCP** — right tool for home scale.
- **Graceful degradation** — TTS failure → browser voice; tool failure → spoken error;
  STT unsupported → typed input still works.

---

## 2. Configuration

All configuration lives in the **git-backed `ConfigProvider`** (your trusted internal
Gitea), with environment-variable fallbacks for secrets. The feature is **disabled by
default**.

### 2.1 Config keys

Defined in `CfgKey.VOICE` (`server/server-libs/server-config/.../CfgKey.groovy`). Code
defaults apply when a key is absent.

| Key | Default | Purpose |
|-----|---------|---------|
| `feature.voice.enabled` | `false` | Master on/off switch for the feature. |
| `feature.voice.llm.provider` | `anthropic` | LLM vendor: `anthropic` or `openai`. |
| `feature.voice.llm.model` | provider default (`claude-haiku-4-5` / `gpt-4o-mini`) | Model id override. |
| `feature.voice.llm.apikey` | – (env fallback) | LLM API key; falls back to `ANTHROPIC_API_KEY` / `OPENAI_API_KEY`. |
| `feature.voice.tts.enabled` | `false` | Enable server-side neural TTS. |
| `feature.voice.tts.provider` | `google` | TTS vendor (currently `google`). |
| `feature.voice.tts.apikey` | – (env fallback) | **Service-account JSON** (or a path to it); falls back to `GOOGLE_TTS_API_KEY`. |
| `feature.voice.tts.voice.ro` | – (Google default) | Optional ro-RO voice name, e.g. `ro-RO-Wavenet-A`. |
| `feature.voice.tts.voice.en` | – (Google default) | Optional en-US voice name. |
| `feature.voice.alias` | – | Per-entity alias list (comma-separated), stored on a PERIPHERAL/ZONE via the `Configuration` sidecar — set it from the entity's edit screen, not as a global key. |

### 2.2 LLM provider & API key

The intent layer needs an **Anthropic Console** API key or an **OpenAI** key — note that
a **Claude Max subscription does not provide an API key** (it's a separate, usage-billed
Console product).

- **Anthropic:** create a key at console.anthropic.com → put it in
  `feature.voice.llm.apikey` (or `ANTHROPIC_API_KEY`). Set
  `feature.voice.llm.provider = anthropic`.
- **OpenAI:** create a key at platform.openai.com → same field (or `OPENAI_API_KEY`).
  Set `feature.voice.llm.provider = openai` and (optionally)
  `feature.voice.llm.model = gpt-4o-mini`.

### 2.3 Google neural TTS

1. In Google Cloud: create a **service account**, **enable the Cloud Text-to-Speech
   API** (and billing). No special IAM role is normally required.
2. Create a **JSON key** for the service account (downloads a `.json` file).
3. Provide the credential to `feature.voice.tts.apikey` (or `GOOGLE_TTS_API_KEY`) as
   **either**:
   - the **inline JSON** (minify to one line, e.g. `jq -c . key.json`), or
   - a **path** to the JSON file on the server (e.g. `/opt/myhab/gcp-tts.json`).
4. Set `feature.voice.tts.enabled = true`. Optionally pick voices via
   `feature.voice.tts.voice.ro` / `.en`.

If the credential is wrong/missing the assistant logs a warning and falls back to the
browser voice.

### 2.4 Language

- The browser STT and the spoken reply follow the **app UI language**
  (`SPEECH_LANGS` in `VoiceControl.vue`: `en → en-US`, `ro → ro-RO`).
- Set your language in **Settings** (per-user preference, saved to the account;
  see `locale.service.js`). Leave on *Automatic* to follow the browser.
- The LLM is multilingual regardless; only **spoken** input depends on the recognition
  language being correct. Typed input works in any language.
- To add another spoken language, add an entry to `SPEECH_LANGS` (and ship that UI
  locale). The backend needs no change.

---

## 3. Usage

- Open the **Voice** page (`/voice`, or the microphone item in the sidebar) on
  **Chrome on Android over HTTPS** (Web Speech API requirement).
- Tap the mic and speak, or type into the command box and press send.
- The assistant replies out loud and shows a chat log. Use **New conversation** to reset
  the session.

Example commands (EN / RO):

| Capability | English | Romanian |
|-----------|---------|----------|
| Peripheral on/off | "turn on the office light" | "aprinde lumina din birou" |
| Whole zone | "turn off everything on the terrace" | "stinge tot pe terasă" |
| Scenario | "run movie mode" | "pornește modul film" |
| State question | "is the garage door open?" | "e deschisă ușa de la garaj?" |
| Mower start | "start mowing" | "pornește tunsul" / "tunde gazonul" |
| Mower dock | "send the mower to the dock" | "trimite mașina la bază" |

**Ambiguity:** if several entities match, the assistant asks (e.g. *"Which light — the
terrace or the kitchen?"*); just answer and it continues.

**Aliases:** to make Romanian (or nickname) matching reliable, open a peripheral or zone
edit screen and fill **Voice aliases** (comma-separated). These are fed to the model.
(Mower devices don't have an alias field yet — see [§7](#7-limitations--out-of-scope).)

---

## 4. Extending with new commands

### 4.1 Add a new tool (most common)

Adding a capability = adding a **tool**. Follow the `mower_command` precedent. Steps:

1. **Declare the tool** in `VoiceTools`
   (`server/.../services/voice/VoiceTools.groovy`):
   - add a `static final String MY_TOOL = 'my_tool'` constant,
   - append an entry to `TOOLS` with a JSON-schema `input_schema`,
   - add a short rule to `SYSTEM_PROMPT` describing when to use it.
2. **(If needed) extend the catalog** in `VoiceCommandService.buildCatalog` so the model
   has the ids/names it must choose from (and mention the new list in the prompt).
3. **Implement the executor** in `VoiceCommandService`:
   ```groovy
   private Map execMyTool(ToolCall tc, Map catalog) {
       // 1. read & coerce args from tc.input
       // 2. validate ids against the catalog (hallucination guard)
       // 3. call the existing service that does the work (may throw)
       // 4. return [content: "...", stateChange: true|false, action: "..."]
   }
   ```
   and wire it into the `executeTool` switch (`case VoiceTools.MY_TOOL: return execMyTool(...)`).
4. **Inject any service** you call as a field on `VoiceCommandService` (Grails autowiring).
5. **Add a Spock test** to `VoiceCommandServiceSpec` — mock `provider.converse` to return
   a scripted tool turn then a text turn, and assert the service was called and/or the
   right event was published (see the existing mower/scenario tests).
6. Compile + test: `./gradlew server-core:compileGroovy server-core:compileTestGroovy server-core:test`.

No GraphQL or frontend changes are needed — new tools flow through the existing
`voiceCommand` mutation and the conversational UI.

**Conventions to keep:**
- Always **validate against the catalog** before acting; return a friendly `content`
  string when an id is unknown (don't throw for "not found").
- `stateChange = true` only when something was actually changed (drives `awaitingReply`
  and the actions/audit list). Read-only tools use `false`.
- Let genuine failures **throw** — the loop converts them into an error result fed back
  to the model, keeping the conversation consistent.

### 4.2 Add a new LLM provider

1. Implement `VoiceIntentProvider` (translate the neutral messages ↔ your vendor's
   format, return an `LlmTurn`). Use `VoiceHttp.INSTANCE` for HTTP.
2. Register it in the `VoiceCommandService.providers` map (keyed by `name()`).
3. Resolve its API key in `apiKey(providerName)` (config key + env fallback).
4. Select it via `feature.voice.llm.provider`.

Keep the **tool/JSON-schema and system prompt shared** (`VoiceTools`) so all vendors
behave identically. Preserve the `tool_use`/`tool_result` (or `tool_calls`/`role:tool`)
pairing when replaying history.

### 4.3 Add a new TTS provider

1. Implement `VoiceTtsProvider.synthesizeMp3(text, languageCode, voiceName, credential)`.
2. Register it in `VoiceCommandService.ttsProviders` (keyed by `name()`).
3. Select it via `feature.voice.tts.provider`; resolve credentials in `ttsApiKey()`.

---

## 5. Operations & troubleshooting

**Logs** — look at `org.myhab.services.VoiceCommandService` (one INFO line per command
with the executed actions) and the provider classes
(`AnthropicIntentProvider` / `OpenAiIntentProvider` / `GoogleTtsProvider` / `GoogleAuth`).

| Symptom | Cause / fix |
|--------|-------------|
| "Voice feature is disabled" | Set `feature.voice.enabled = true`. |
| "Unknown LLM provider '…'" | `feature.voice.llm.provider` must be `anthropic` or `openai`. |
| `Could not understand the command: … HTTP 401` | LLM API key wrong/missing. |
| `Read timed out` on LLM call | Should not happen (voice uses the isolated 60s `VoiceHttp`); if it does, the model is very slow — check provider status. |
| TTS warns `API keys are not supported by this API` | Google TTS needs a **service account JSON**, not an API key (see [2.3](#23-google-neural-tts)). |
| TTS warns `… must be a service account JSON … or a path` | The value isn't valid JSON and isn't a readable file path — paste the minified JSON or a correct path. |
| TTS `403 PERMISSION_DENIED` | Enable the **Cloud Text-to-Speech API** (+ billing) on the project. |
| Spoken Romanian mis-transcribed | App language isn't Romanian → STT is en-US. Switch language in Settings, or type the command. |
| Reply read in a wrong-sounding voice | Browser fallback voice; enable Google TTS for natural ro-RO. |

**Cost** — the catalog + system prompt are cached (Anthropic `cache_control`); marginal
per-command cost is small. Catalog size grows with the number of peripherals/zones/
scenarios/mowers; it stays well within one prompt at home scale.

---

## 6. Security

- LLM/TTS credentials are read from the **git-backed config** (trusted internal Gitea)
  or **environment variables**. Treating the Gitea repo as a secret store is a
  deliberate, accepted decision for this deployment.
- Because secrets can live in git history, **rotation = issue a new key and revoke the
  old one** (don't rely on overwriting the config value).
- The `voiceCommand` mutation requires authentication (the GraphQL endpoint is
  `isAuthenticated()`); the executing user is recorded on the `evt_switch` event for
  audit.

---

## 7. Limitations / out of scope

- **Hands-free / wake word / native app** — not implemented; entry is a tap on the Voice
  page (PWA). Deferred by design.
- **Authoring scenarios by voice** — only *running* existing scenarios is supported.
- **Set-value / dimming / RGB** by voice — not exposed (only ON/OFF/TOGGLE);
  the `evt_light_set_color` path stays manual.
- **Mower aliases** — peripherals and zones support voice aliases; mower **devices** do
  not yet (matching relies on the device name + the single-mower case). Extending aliases
  to devices is a small follow-up.
- **STT coverage** — limited to what the browser provides (Chrome-on-Android covers
  ro-RO and many more); recognition is single-language per session (no auto-detect).

---

## 8. File reference

**Backend** (`server/server-core/grails-app/services/org/myhab/`)

| File | Role |
|------|------|
| `services/VoiceCommandService.groovy` | Orchestration: catalog, agentic loop, tool execution, sessions, TTS attach. |
| `services/voice/VoiceTools.groovy` | Shared system prompt + tool JSON-schemas + catalog JSON helper. |
| `services/voice/VoiceIntentProvider.groovy` | LLM vendor interface (`converse`). |
| `services/voice/AnthropicIntentProvider.groovy` | Anthropic Messages API provider. |
| `services/voice/OpenAiIntentProvider.groovy` | OpenAI Chat Completions provider. |
| `services/voice/LlmTurn.groovy`, `ToolCall.groovy` | Normalized LLM-turn / tool-call types. |
| `services/voice/VoiceHttp.groovy` | Isolated Unirest instance (long timeout) for LLM/TTS. |
| `services/voice/VoiceTtsProvider.groovy` | TTS vendor interface. |
| `services/voice/GoogleTtsProvider.groovy` | Google Cloud TTS provider. |
| `services/voice/GoogleAuth.groovy` | Service-account JSON → cached OAuth2 token (RS256 JWT). |
| `graphql/fetchers/Mutation.groovy` | `voiceCommand` fetcher (passes `sessionId`, principal). |
| `services/UIMessageService.groovy` | Executes `evt_switch` (PERIPHERAL/ZONE/PORT) — reused, not modified. |
| `services/SchedulerService.groovy` | `triggerJob` — used by `run_scenario`. |
| `services/dsl/action/NavimowCommandService.groovy` | Mower REST — used by `mower_command`. |

**Config / schema**

| File | Role |
|------|------|
| `server/server-libs/server-config/.../config/CfgKey.groovy` | `VOICE` config-key enum. |
| `server/server-core/src/main/resources/schema.graphqls` | `voiceCommand` mutation + `VoiceCommandResult` type. |

**Frontend** (`client/web-vue3/src/`)

| File | Role |
|------|------|
| `pages/VoiceControl.vue` | Voice page: STT, chat log, audio playback, session, auto-reply. |
| `graphql/queries/voiceCommand.js` | `VOICE_COMMAND` mutation. |
| `_services/locale.service.js` | UI-language resolution (drives STT/TTS language). |
| `pages/infra/peripheral/PeripheralEdit.vue`, `pages/infra/zone/ZoneEdit.vue` | "Voice aliases" field. |

---

## 9. Tests & verification

- **Unit:** `VoiceCommandServiceSpec` (`server/server-core/src/test/groovy/...`) mocks the
  provider, scheduler, Navimow service and Hazelcast, and covers: zone control,
  ambiguous→`awaitingReply`, run_scenario, query rejection, mower command + rejection,
  tool-failure-does-not-abort, and the feature toggle.
- **Build:** `./gradlew server-core:compileGroovy server-core:compileTestGroovy server-core:test`;
  frontend `cd client/web-vue3 && yarn build`.
- **Live smoke test** (with `feature.voice.enabled=true` + keys, on Chrome-Android/HTTPS):
  turn a zone off, ask an ambiguous question then answer it, run a scenario, ask a state
  question, and "start mowing" / "pornește tunsul". With Google TTS enabled, confirm the
  reply plays as natural neural audio.
```
