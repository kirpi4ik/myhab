<#
.SYNOPSIS
    Evaluate TypeSafe Jev as the voice intent resolver: speed, correctness, consistency, languages.

.DESCRIPTION
    One POST /v1/systemone per case, the transcript as `state`, and parallel choice questions:
      intent        control | scenario | query | mower | clarify | other
      target        every catalog peripheral (P<id>) and zone (Z<id>), plus none
      action        ON | OFF | TOGGLE | none
      scenario      every active scenario (S<jobId>), plus none
      mower_action  START | STOP | PAUSE | RESUME | DOCK | none
    Only the answers relevant to the chosen intent form the decision; its confidence is the
    minimum confidence of those questions.

    Reads data/catalog.json + data/cases.json, writes results/jev-<ts>.jsonl and -summary.md.
    The API key comes from the JEV_API_KEY environment variable (process, user or machine).

.EXAMPLE
    .\Invoke-JevEval.ps1 -Max 3 -Repeat 1        # smoke test
.EXAMPLE
    .\Invoke-JevEval.ps1                         # full run, 5 repeats, en/ro/ru
#>
[CmdletBinding()]
param(
    [int]     $Repeat = 5,
    [string[]]$Langs  = @('en', 'ro', 'ru'),
    [string]  $Filter = '*',
    [int]     $Max    = 0,
    [string]  $Model  = 'jev-latest',
    [string]  $Url    = 'https://api.typesafe.ai/v1/systemone'
)

. (Join-Path $PSScriptRoot 'Common.ps1')

# The user/machine lookup covers a variable set after this shell started.
$apiKey = @($env:JEV_API_KEY,
            [Environment]::GetEnvironmentVariable('JEV_API_KEY', 'User'),
            [Environment]::GetEnvironmentVariable('JEV_API_KEY', 'Machine')) | Where-Object { $_ } | Select-Object -First 1
if (-not $apiKey) { throw 'Set the JEV_API_KEY environment variable first.' }

$PricePerInputToken = 0.042 / 1e6
$catalog = (Import-Catalog).Object
$cases = @(Import-Cases -Langs $Langs -Filter $Filter -Max $Max)

# ---------------------------------------------------------------- questions (static per catalog)
function Join-Some([object[]]$Names, [int]$Limit) {
    $n = @($Names | Where-Object { $_ })
    if ($n.Count -le $Limit) { return $n -join ', ' }
    return (($n | Select-Object -First $Limit) -join ', ') + ", ... ($($n.Count) total)"
}

$targets = [ordered]@{}
foreach ($p in $catalog.peripherals) {
    $d = "DEVICE: $($p.name) ($($p.category))"
    if (@($p.zones).Count) { $d += " in $(@($p.zones) -join ', ')" }
    if (@($p.aliases).Count) { $d += "; also called $(@($p.aliases) -join ', ')" }
    $targets["P$($p.id)"] = $d
}
foreach ($z in $catalog.zones) {
    $d = "AREA (everything in it): $($z.name); contains $(Join-Some $z.peripherals 12)"
    if (@($z.aliases).Count) { $d += "; also called $(@($z.aliases) -join ', ')" }
    $targets["Z$($z.id)"] = $d
}
$targets['none'] = 'Nothing in the list matches'
if ($targets.Count -gt 255) { throw "Target options ($($targets.Count)) exceed Jev's 255-option limit; split the question." }

$scenarios = [ordered]@{}
foreach ($s in $catalog.scenarios) { $scenarios["S$($s.jobId)"] = "$($s.name): $($s.description)" }
$scenarios['none'] = 'No scenario is requested'

$questions = [ordered]@{
    intent = [ordered]@{
        type = 'choice'
        instructions = 'The text is a spoken request to a home-automation assistant (English, Romanian or Russian; it may contain speech-recognition errors). What kind of request is it?'
        criteria = [ordered]@{
            control  = 'Turn on/off, toggle, open/close or start/stop a device or a whole area (lights, heating, water valves, gates, sprinklers, switches, ventilation)'
            scenario = 'Run a predefined automation/scenario, named or described'
            query    = 'Ask about a current state or value (temperature, is something on/open?) without changing anything'
            mower    = 'Command the robotic lawn mower: start, stop, pause or resume mowing, or send it to its dock'
            clarify  = 'A home command that is ambiguous: it could equally mean several different devices or rooms, so the user must say which'
            other    = 'Not a home-automation request (chit-chat, general knowledge)'
        }
    }
    target = [ordered]@{
        type = 'choice'
        instructions = 'Which single device or area does the user want to control or ask about? Catalog names are Romanian; the user may speak English, Romanian or Russian. Choose an AREA when the user means everything in a place, a DEVICE when they mean one specific device or one kind of device in a room.'
        criteria = $targets
    }
    action = [ordered]@{
        type = 'choice'
        instructions = 'Which on/off action does the user request?'
        criteria = [ordered]@{
            ON     = 'Turn on, switch on, light up, open (a valve, water, gate), start. Romanian: aprinde, pornește, deschide, dă drumul. Russian: включи, зажги, открой, запусти'
            OFF    = 'Turn off, switch off, close or shut off (a valve, water), stop. Romanian: stinge, oprește, închide, taie. Russian: выключи, погаси, закрой, перекрой, останови'
            TOGGLE = 'Toggle or flip to the opposite state. Romanian: comută, schimbă. Russian: переключи'
            none   = 'No on/off action is requested'
        }
    }
    mower_action = [ordered]@{
        type = 'choice'
        instructions = 'Which robotic lawn mower command is requested?'
        criteria = [ordered]@{
            START  = 'Start mowing'
            STOP   = 'Stop mowing'
            PAUSE  = 'Pause mowing'
            RESUME = 'Resume mowing'
            DOCK   = 'Return to the charging dock / go home'
            none   = 'No mower command'
        }
    }
}
if ($catalog.scenarios) {
    $questions.scenario = [ordered]@{
        type = 'choice'
        instructions = 'Which predefined automation scenario should be run?'
        criteria = $scenarios
    }
}

# ---------------------------------------------------------------- decision mapping
function Get-Top($Answer, [int]$N = 3) {
    if (-not $Answer.probabilities) { return '' }
    ($Answer.probabilities.PSObject.Properties | Sort-Object Value -Descending | Select-Object -First $N |
        ForEach-Object { '{0}={1:N2}' -f $_.Name, $_.Value }) -join ' '
}

function ConvertTo-Decision($Answers) {
    $intent = $Answers.intent.choice
    $pred = [ordered]@{ intent = $intent; target = $null; action = $null; scenario = $null; mowerAction = $null }
    $used = @('intent')
    switch ($intent) {
        'control'  { $pred.target = $Answers.target.choice; $pred.action = $Answers.action.choice; $used += 'target', 'action' }
        'query'    { $pred.target = $Answers.target.choice; $used += 'target' }
        'scenario' { $pred.scenario = $Answers.scenario.choice; $used += 'scenario' }
        'mower'    { $pred.mowerAction = $Answers.mower_action.choice; $used += 'mower_action' }
    }
    foreach ($k in @($pred.Keys)) { if ($pred[$k] -eq 'none') { $pred[$k] = $null } }
    $conf = ($used | ForEach-Object { [double]$Answers.$_.confidence } | Measure-Object -Minimum).Minimum
    $top = (@("intent: $(Get-Top $Answers.intent 2)") + @($used | Where-Object { $_ -ne 'intent' } | ForEach-Object { "${_}: $(Get-Top $Answers.$_)" })) -join ' / '
    [pscustomobject]@{ Pred = [pscustomobject]$pred; Confidence = $conf; Top = $top }
}

# ---------------------------------------------------------------- run
New-Item -ItemType Directory -Force $script:ResultsDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$jsonl = Join-Path $script:ResultsDir "jev-$stamp.jsonl"
$summary = Join-Path $script:ResultsDir "jev-$stamp-summary.md"
$headers = @{ Authorization = "Bearer $apiKey" }
$client = New-EvalHttpClient

function New-Body([string]$Text) {
    [ordered]@{ state = $Text; model = $Model; questions = $questions } | ConvertTo-Json -Depth 10 -Compress
}

Write-Host "Jev eval: $($cases.Count) cases x $Repeat repeats, $($targets.Count) target options, model $Model"
$warm = Invoke-JsonPost $client $Url $headers (New-Body $cases[0].Text)
if (-not $warm.Ok) { throw "Warm-up failed: HTTP $($warm.Status) $($warm.Error)" }
Write-Host ("Warm-up: {0:N0} ms, model {1}, {2} input tokens" -f $warm.Ms, $warm.Body.model, $warm.Body.usage.input_tokens)

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
            $d = ConvertTo-Decision $res.Body.answers
            $rec.Model = $res.Body.model; $rec.Pred = $d.Pred; $rec.Confidence = $d.Confidence; $rec.Top = $d.Top
            $rec.Score = Test-Prediction $d.Pred $c.Accept
            $rec.InputTokens = [int]$res.Body.usage.input_tokens
            $rec.CostUsd = $rec.InputTokens * $PricePerInputToken
        }
        $records += $rec
        Save-JsonLine $jsonl ($rec | Select-Object CaseId, Lang, Repeat, Ok, Ms, Retries, Model, Pred, Confidence, Top, Score, InputTokens, Error)
        $mark = if (-not $res.Ok) { 'ERR ' } elseif ($rec.Score.Full) { 'ok  ' } else { 'MISS' }
        $desc = if ($res.Ok) { '{0} conf={1:N2}' -f (Get-PredictionKey $rec.Pred), $rec.Confidence } else { "HTTP $($res.Status)" }
        Write-Host ('[{0}/{1}] {2} {3,-32} {4,6:N0} ms  {5}' -f $r, $Repeat, $mark, $c.Id, $res.Ms, $desc)
    }
}
$client.Dispose()

$text = Write-EvalSummary $records "Jev evaluation ($Model, $stamp)" $summary
Write-Host ''
Write-Host $text
Write-Host "`nRaw: $jsonl`nSummary: $summary" -ForegroundColor Green
