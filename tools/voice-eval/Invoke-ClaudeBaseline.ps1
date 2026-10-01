<#
.SYNOPSIS
    Baseline for Invoke-JevEval.ps1: the production Claude request, scored on the same cases.

.DESCRIPTION
    Sends the request AnthropicIntentProvider builds for the first turn (system prompt, cached
    catalog block, the four tools, one user message) and times only that call: the tool
    selection Jev would replace. The follow-up turn that phrases the spoken reply is not
    measured. Nothing is executed and the myHAB server is not involved.

    Tool calls map to decisions: control_entity -> control, run_scenario -> scenario,
    query_state -> query, mower_command -> mower; no tool and a reply ending in '?' -> clarify;
    anything else -> other. Claude reports no confidence, so there is no calibration section.

    The API key comes from the ANTHROPIC_API_KEY environment variable (process, user or machine).

.EXAMPLE
    .\Invoke-ClaudeBaseline.ps1 -Max 3 -Repeat 1
#>
[CmdletBinding()]
param(
    [int]     $Repeat = 3,
    [string[]]$Langs  = @('en', 'ro', 'ru'),
    [string]  $Filter = '*',
    [int]     $Max    = 0,
    [string]  $Model  = 'claude-haiku-4-5',
    [string]  $Url    = 'https://api.anthropic.com/v1/messages'
)

. (Join-Path $PSScriptRoot 'Common.ps1')

# The user/machine lookup covers a variable set after this shell started.
$apiKey = @($env:ANTHROPIC_API_KEY,
            [Environment]::GetEnvironmentVariable('ANTHROPIC_API_KEY', 'User'),
            [Environment]::GetEnvironmentVariable('ANTHROPIC_API_KEY', 'Machine')) | Where-Object { $_ } | Select-Object -First 1
if (-not $apiKey) { throw 'Set the ANTHROPIC_API_KEY environment variable first.' }

# USD per token for Haiku 4.5: input, output, cache write (5 min), cache read.
$Price = @{ In = 1.0 / 1e6; Out = 5.0 / 1e6; CacheWrite = 1.25 / 1e6; CacheRead = 0.10 / 1e6 }

$catalog = Import-Catalog
$cases = @(Import-Cases -Langs $Langs -Filter $Filter -Max $Max)

# Copied from server/server-core/grails-app/services/org/myhab/services/voice/VoiceTools.groovy
# (SYSTEM_PROMPT and TOOLS); keep in sync if those change.
$systemPrompt = @'
You are the voice assistant of a home-automation system. The user speaks short commands or questions in any language (often English or Romanian). You are given a JSON catalog of controllable peripherals, zones (areas/rooms, each listing the peripherals inside it), runnable scenarios and robotic lawn mowers. Use the tools to act, then give a brief spoken reply.

Rules:
- Match the user's words (names, zones, aliases) to catalog entries, tolerating synonyms, translation, word order and transcription errors. Never use an id that is not in the catalog.
- When the user refers to a place/area (e.g. "the terrace", "downstairs") and it maps to a zone, control the whole ZONE with control_entity(entityType:"ZONE"), not a single peripheral — unless they clearly name one specific device.
- If the request is genuinely ambiguous (several distinct matches and you cannot tell which), do NOT guess: reply with a short clarifying question and call no tool. The user will answer.
- For "run/start <name>" of an automation, use run_scenario.
- For questions about current state ("is the door open?", "temperature?"), use query_state and answer from the returned values; do not change anything.
- To control a robotic lawn mower (Navimow), use mower_command with the mower's deviceId from the catalog and one of START / STOP / PAUSE / RESUME / DOCK ("start mowing" → START, "send the mower home" → DOCK).
- Keep spoken replies short and natural, in the same language the user used.
'@.Trim() -replace "`r`n", "`n"

$tools = @(
    [ordered]@{ name = 'control_entity'; description = 'Turn a peripheral, a whole zone, or a single port on/off or toggle it.'
        input_schema = [ordered]@{ type = 'object'; properties = [ordered]@{
                entityType = [ordered]@{ type = 'string'; enum = @('PERIPHERAL', 'ZONE', 'PORT'); description = 'PERIPHERAL for one device, ZONE for everything in an area, PORT for a raw port.' }
                id         = [ordered]@{ type = 'integer'; description = 'The catalog id of the chosen entity.' }
                action     = [ordered]@{ type = 'string'; enum = @('ON', 'OFF', 'TOGGLE') } }
            required = @('entityType', 'id', 'action') } }
    [ordered]@{ name = 'run_scenario'; description = 'Run a predefined automation/scenario by its job id from the catalog.'
        input_schema = [ordered]@{ type = 'object'; properties = [ordered]@{
                jobId = [ordered]@{ type = 'integer'; description = 'The jobId of the scenario from the catalog.' } }
            required = @('jobId') } }
    [ordered]@{ name = 'query_state'; description = 'Read the current state/value of a peripheral, zone or port to answer a question.'
        input_schema = [ordered]@{ type = 'object'; properties = [ordered]@{
                entityType = [ordered]@{ type = 'string'; enum = @('PERIPHERAL', 'ZONE', 'PORT') }
                id         = [ordered]@{ type = 'integer' } }
            required = @('entityType', 'id') } }
    [ordered]@{ name = 'mower_command'; description = 'Control a Segway Navimow robotic lawn mower: start/stop/pause/resume mowing or send it back to the dock.'
        input_schema = [ordered]@{ type = 'object'; properties = [ordered]@{
                deviceId = [ordered]@{ type = 'integer'; description = 'The deviceId of the mower from the catalog (mowers list).' }
                action   = [ordered]@{ type = 'string'; enum = @('START', 'STOP', 'PAUSE', 'RESUME', 'DOCK') } }
            required = @('deviceId', 'action') } }
)

$system = @(
    [ordered]@{ type = 'text'; text = $systemPrompt }
    [ordered]@{ type = 'text'; text = "Catalog (JSON):`n$($catalog.Json)"; cache_control = [ordered]@{ type = 'ephemeral' } }
)

function New-Body([string]$Text) {
    [ordered]@{
        model = $Model; max_tokens = 512; system = $system; tools = $tools
        messages = @([ordered]@{ role = 'user'; content = $Text })
    } | ConvertTo-Json -Depth 12 -Compress
}

function ConvertTo-Decision($Body) {
    $pred = [ordered]@{ intent = 'other'; target = $null; action = $null; scenario = $null; mowerAction = $null }
    $calls = @($Body.content | Where-Object { $_.type -eq 'tool_use' })
    $text = (@($Body.content | Where-Object { $_.type -eq 'text' } | ForEach-Object { $_.text }) -join ' ').Trim()
    $tc = $calls | Select-Object -First 1
    $prefix = @{ PERIPHERAL = 'P'; ZONE = 'Z'; PORT = 'PORT' }
    switch ($tc.name) {
        'control_entity' { $pred.intent = 'control'; $pred.target = "$($prefix[$tc.input.entityType])$($tc.input.id)"; $pred.action = $tc.input.action }
        'query_state'    { $pred.intent = 'query'; $pred.target = "$($prefix[$tc.input.entityType])$($tc.input.id)" }
        'run_scenario'   { $pred.intent = 'scenario'; $pred.scenario = "S$($tc.input.jobId)" }
        'mower_command'  { $pred.intent = 'mower'; $pred.mowerAction = $tc.input.action }
        default          { if ($text.EndsWith('?')) { $pred.intent = 'clarify' } }
    }
    $top = if ($calls.Count -gt 1) { "$($calls.Count) tool calls: " + (($calls | ForEach-Object { "$($_.name)($($_.input | ConvertTo-Json -Compress))" }) -join ', ') }
           elseif (-not $calls) { "reply: $text" } else { $null }
    [pscustomobject]@{ Pred = [pscustomobject]$pred; Top = $top }
}

New-Item -ItemType Directory -Force $script:ResultsDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$jsonl = Join-Path $script:ResultsDir "claude-$stamp.jsonl"
$summary = Join-Path $script:ResultsDir "claude-$stamp-summary.md"
$headers = @{ 'x-api-key' = $apiKey; 'anthropic-version' = '2023-06-01' }
$client = New-EvalHttpClient

Write-Host "Claude baseline: $($cases.Count) cases x $Repeat repeats, model $Model"
# The warm-up also writes the catalog prompt cache, so measured calls are cache hits (best case).
$warm = Invoke-JsonPost $client $Url $headers (New-Body $cases[0].Text)
if (-not $warm.Ok) { throw "Warm-up failed: HTTP $($warm.Status) $($warm.Error)" }
Write-Host ("Warm-up: {0:N0} ms, cache write {1}, cache read {2}" -f $warm.Ms, $warm.Body.usage.cache_creation_input_tokens, $warm.Body.usage.cache_read_input_tokens)

$records = @()
for ($r = 1; $r -le $Repeat; $r++) {
    foreach ($c in $cases) {
        $res = Invoke-JsonPost $client $Url $headers (New-Body $c.Text)
        $rec = [pscustomobject]@{
            CaseId = $c.Id; Intent = $c.Intent; Lang = $c.Lang; Text = $c.Text; Accept = $c.Accept; Repeat = $r
            Ok = $res.Ok; Ms = [math]::Round($res.Ms, 1); Retries = $res.Retries; Error = $res.Error
            Model = $null; Pred = $null; Confidence = $null; Top = $null; Score = $null; InputTokens = $null; CostUsd = $null
        }
        if ($res.Ok) {
            $d = ConvertTo-Decision $res.Body
            $u = $res.Body.usage
            $rec.Model = $res.Body.model; $rec.Pred = $d.Pred; $rec.Top = $d.Top
            $rec.Score = Test-Prediction $d.Pred $c.Accept
            $rec.InputTokens = [int]$u.input_tokens + [int]$u.cache_read_input_tokens + [int]$u.cache_creation_input_tokens
            $rec.CostUsd = $u.input_tokens * $Price.In + $u.output_tokens * $Price.Out +
                           $u.cache_creation_input_tokens * $Price.CacheWrite + $u.cache_read_input_tokens * $Price.CacheRead
        }
        $records += $rec
        Save-JsonLine $jsonl ($rec | Select-Object CaseId, Lang, Repeat, Ok, Ms, Retries, Model, Pred, Top, Score, InputTokens, CostUsd, Error)
        $mark = if (-not $res.Ok) { 'ERR ' } elseif ($rec.Score.Full) { 'ok  ' } else { 'MISS' }
        $desc = if ($res.Ok) { Get-PredictionKey $rec.Pred } else { "HTTP $($res.Status)" }
        Write-Host ('[{0}/{1}] {2} {3,-32} {4,6:N0} ms  {5}' -f $r, $Repeat, $mark, $c.Id, $res.Ms, $desc)
    }
}
$client.Dispose()

$text = Write-EvalSummary $records "Claude baseline ($Model, first turn only, $stamp)" $summary
Write-Host ''
Write-Host $text
Write-Host "`nRaw: $jsonl`nSummary: $summary" -ForegroundColor Green
