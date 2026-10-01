# Shared helpers for the voice intent evaluation runners (dot-sourced).
# Windows PowerShell 5.1 compatible.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$script:EvalRoot    = $PSScriptRoot
$script:DataDir     = Join-Path $PSScriptRoot 'data'
$script:ResultsDir  = Join-Path $PSScriptRoot 'results'
$script:Intents     = @('control', 'scenario', 'query', 'mower', 'clarify', 'other')
$script:Actions     = @('ON', 'OFF', 'TOGGLE')
$script:MowerActions = @('START', 'STOP', 'PAUSE', 'RESUME', 'DOCK')

function Read-Utf8Json([string]$Path) {
    if (-not (Test-Path $Path)) { throw "Missing $Path - run Export-VoiceCatalog.ps1 / create data/cases.json first." }
    Get-Content -Raw -Encoding UTF8 $Path | ConvertFrom-Json
}

function Import-Catalog {
    $path = Join-Path $script:DataDir 'catalog.json'
    [pscustomobject]@{
        Object = Read-Utf8Json $path
        Json   = (Get-Content -Raw -Encoding UTF8 $path).Trim()
    }
}

# Expands data/cases.json ({id, accept, text:{en,ro,ru}}) into one case per language.
function Import-Cases([string[]]$Langs, [string]$Filter = '*', [int]$Max = 0, [string]$Path = (Join-Path $script:DataDir 'cases.json')) {
    $all = @()
    foreach ($c in (Read-Utf8Json $Path)) {
        if ($c.id -notlike $Filter) { continue }
        foreach ($lang in $Langs) {
            $text = $c.text.$lang
            if (-not $text) { continue }
            $all += [pscustomobject]@{ Id = "$($c.id).$lang"; Intent = $c.id; Lang = $lang; Text = $text; Accept = @($c.accept) }
        }
    }
    if ($Max -gt 0) { $all = @($all | Select-Object -First $Max) }
    if (-not $all) { throw "No cases matched (Filter='$Filter', Langs=$($Langs -join ','))." }
    return $all
}

function New-EvalHttpClient {
    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromSeconds(60)
    return $client
}

# POSTs JSON and returns {Ok, Status, Ms, Body, Retries, Error}. Ms covers only the final
# attempt, so rate-limit backoff does not distort latency; retries are counted separately.
function Invoke-JsonPost($Client, [string]$Url, [hashtable]$Headers, [string]$Json, [int]$MaxRetries = 4) {
    $retries = 0
    while ($true) {
        $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $Url)
        foreach ($k in $Headers.Keys) { [void]$req.Headers.TryAddWithoutValidation($k, $Headers[$k]) }
        $req.Content = New-Object System.Net.Http.StringContent($Json, [Text.Encoding]::UTF8, 'application/json')
        $sw = [Diagnostics.Stopwatch]::StartNew()
        try {
            $resp = $Client.SendAsync($req).GetAwaiter().GetResult()
            $bytes = $resp.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
        } catch {
            $sw.Stop()
            if ($retries -lt $MaxRetries) { $retries++; Start-Sleep -Milliseconds (500 * [math]::Pow(2, $retries)); continue }
            return [pscustomobject]@{ Ok = $false; Status = 0; Ms = $sw.Elapsed.TotalMilliseconds; Body = $null; Retries = $retries; Error = $_.Exception.Message }
        }
        $sw.Stop()
        $status = [int]$resp.StatusCode
        $text = [Text.Encoding]::UTF8.GetString($bytes)
        if (($status -eq 429 -or $status -eq 529 -or $status -ge 500) -and $retries -lt $MaxRetries) {
            $retries++
            Start-Sleep -Milliseconds (500 * [math]::Pow(2, $retries))
            continue
        }
        $body = $null
        try { $body = $text | ConvertFrom-Json } catch { }
        return [pscustomobject]@{
            Ok = ($status -lt 400); Status = $status; Ms = $sw.Elapsed.TotalMilliseconds
            Body = $body; Retries = $retries; Error = $(if ($status -ge 400) { $text } else { $null })
        }
    }
}

# Canonical key for consistency checks: identical keys = identical decision.
function Get-PredictionKey($Pred) {
    '{0}|{1}|{2}|{3}|{4}' -f $Pred.intent, $Pred.target, $Pred.action, $Pred.scenario, $Pred.mowerAction
}

# A prediction is fully correct when it matches every field of any accepted outcome.
# Component scores are "does some accepted outcome agree on this field".
function Test-Prediction($Pred, $Accept) {
    $full = $false
    foreach ($a in $Accept) {
        $ok = $true
        foreach ($f in 'intent', 'target', 'action', 'scenario', 'mowerAction') {
            if ($null -ne $a.$f -and $a.$f -ne $Pred.$f) { $ok = $false }
        }
        if ($ok) { $full = $true; break }
    }
    $withTarget = @($Accept | Where-Object { $null -ne $_.target })
    $withAction = @($Accept | Where-Object { $null -ne $_.action })
    [pscustomobject]@{
        Full   = $full
        Intent = [bool](@($Accept | Where-Object { $_.intent -eq $Pred.intent }).Count)
        Target = $(if ($withTarget) { [bool](@($withTarget | Where-Object { $_.target -eq $Pred.target }).Count) } else { $null })
        Action = $(if ($withAction) { [bool](@($withAction | Where-Object { $_.action -eq $Pred.action }).Count) } else { $null })
    }
}

function Get-Percentile([double[]]$Values, [double]$P) {
    if (-not $Values) { return $null }
    $sorted = $Values | Sort-Object
    $idx = [math]::Ceiling($P / 100 * $sorted.Count) - 1
    return $sorted[[math]::Max(0, [math]::Min($idx, $sorted.Count - 1))]
}

function Format-Pct($Num, $Den) { if ($Den -gt 0) { '{0:N1}% ({1}/{2})' -f (100 * $Num / $Den), $Num, $Den } else { 'n/a' } }

function Format-Accept($Accept) {
    ($Accept | ForEach-Object {
        $a = $_; (@('intent', 'target', 'action', 'scenario', 'mowerAction') | Where-Object { $null -ne $a.$_ } | ForEach-Object { $a.$_ }) -join ' '
    }) -join ' | '
}

# Records: {CaseId, Intent, Lang, Repeat, Ok, Ms, Retries, Pred, Confidence, Top, Score, InputTokens, CostUsd, Error}
function Write-EvalSummary([object[]]$Records, [string]$Title, [string]$OutFile) {
    $ok = @($Records | Where-Object { $_.Ok })
    $langs = @($Records | Select-Object -ExpandProperty Lang -Unique)
    $L = New-Object System.Collections.Generic.List[string]
    $L.Add("# $Title"); $L.Add('')
    $L.Add("Calls: $($Records.Count), failed: $($Records.Count - $ok.Count), retried: $(@($Records | Where-Object { $_.Retries -gt 0 }).Count)")
    $L.Add('')

    $L.Add('## Latency (ms, client wall time per call)'); $L.Add('')
    $L.Add('| scope | n | p50 | p90 | p99 | max | mean |'); $L.Add('|---|---|---|---|---|---|---|')
    foreach ($scope in @('all') + $langs) {
        $set = @(if ($scope -eq 'all') { $ok } else { $ok | Where-Object { $_.Lang -eq $scope } })
        $ms = [double[]]@($set | ForEach-Object { $_.Ms })
        if (-not $ms) { continue }
        $L.Add(('| {0} | {1} | {2:N0} | {3:N0} | {4:N0} | {5:N0} | {6:N0} |' -f $scope, $ms.Count,
            (Get-Percentile $ms 50), (Get-Percentile $ms 90), (Get-Percentile $ms 99),
            ($ms | Measure-Object -Maximum).Maximum, ($ms | Measure-Object -Average).Average))
    }
    $L.Add('')

    $L.Add('## Correctness (all repeats)'); $L.Add('')
    $L.Add('| lang | fully correct | intent | target | action |'); $L.Add('|---|---|---|---|---|')
    foreach ($scope in @('all') + $langs) {
        $set = @(if ($scope -eq 'all') { $ok } else { $ok | Where-Object { $_.Lang -eq $scope } })
        $t = @($set | Where-Object { $null -ne $_.Score.Target }); $a = @($set | Where-Object { $null -ne $_.Score.Action })
        $L.Add(('| {0} | {1} | {2} | {3} | {4} |' -f $scope,
            (Format-Pct @($set | Where-Object { $_.Score.Full }).Count $set.Count),
            (Format-Pct @($set | Where-Object { $_.Score.Intent }).Count $set.Count),
            (Format-Pct @($t | Where-Object { $_.Score.Target }).Count $t.Count),
            (Format-Pct @($a | Where-Object { $_.Score.Action }).Count $a.Count)))
    }
    $L.Add('')

    $L.Add('## Consistency (same decision on every repeat)'); $L.Add('')
    $groups = @($ok | Group-Object CaseId)
    $unstable = @($groups | Where-Object { @($_.Group | ForEach-Object { Get-PredictionKey $_.Pred } | Select-Object -Unique).Count -gt 1 })
    $L.Add("Consistent cases: $(Format-Pct ($groups.Count - $unstable.Count) $groups.Count)")
    foreach ($g in $unstable) {
        $variants = ($g.Group | Group-Object { Get-PredictionKey $_.Pred } | ForEach-Object { "$($_.Name) x$($_.Count)" }) -join '; '
        $L.Add("- ``$($g.Name)``: $variants")
    }
    $L.Add('')

    $withConf = @($ok | Where-Object { $null -ne $_.Confidence })
    if ($withConf) {
        $L.Add('## Calibration (decision confidence = min over the questions used)'); $L.Add('')
        $L.Add('| band | n | fully correct |'); $L.Add('|---|---|---|')
        foreach ($band in @(@('>= 0.9', 0.9, 1.01), @('0.5 - 0.9', 0.5, 0.9), @('< 0.5', -1, 0.5))) {
            $set = @($withConf | Where-Object { $_.Confidence -ge $band[1] -and $_.Confidence -lt $band[2] })
            $L.Add(('| {0} | {1} | {2} |' -f $band[0], $set.Count, (Format-Pct @($set | Where-Object { $_.Score.Full }).Count $set.Count)))
        }
        $confWrong = @($withConf | Where-Object { $_.Confidence -ge 0.9 -and -not $_.Score.Full })
        $ctl = @($withConf | Where-Object { $_.Pred.intent -eq 'control' })
        $ctlWrong = @($confWrong | Where-Object { $_.Pred.intent -eq 'control' })
        $L.Add('')
        $L.Add("Wrong with confidence >= 0.9: $($confWrong.Count); of those, control actions (would switch the wrong thing): $(Format-Pct $ctlWrong.Count $ctl.Count)")
        $L.Add('')
    }

    $tok = @($ok | Where-Object { $_.InputTokens })
    if ($tok) {
        $L.Add('## Cost'); $L.Add('')
        $meanTokens = ($tok | Measure-Object InputTokens -Average).Average
        $meanCost = ($tok | Measure-Object CostUsd -Average).Average
        $L.Add(('Mean input tokens/call: {0:N0}; mean cost/command: ${1:N6}; per 1000 commands: ${2:N3}' -f $meanTokens, $meanCost, (1000 * $meanCost)))
        $L.Add('')
    }

    $L.Add('## Misses (distinct wrong decisions)'); $L.Add('')
    $misses = @($ok | Where-Object { -not $_.Score.Full } | Group-Object { "$($_.CaseId)#$(Get-PredictionKey $_.Pred)" })
    if (-not $misses) { $L.Add('None.') }
    foreach ($m in $misses) {
        $r = $m.Group[0]
        $conf = if ($null -ne $r.Confidence) { ' conf={0:N2}' -f $r.Confidence } else { '' }
        $L.Add("- ``$($r.CaseId)`` x$($m.Count) ""$($r.Text)"" -> **$(Get-PredictionKey $r.Pred)**$conf; expected: $(Format-Accept $r.Accept)")
        if ($r.Top) { $L.Add("  - top: $($r.Top)") }
    }
    $errs = @($Records | Where-Object { -not $_.Ok } | Select-Object -First 5)
    if ($errs) {
        $L.Add(''); $L.Add('## Errors (first 5)'); $L.Add('')
        foreach ($e in $errs) { $L.Add("- ``$($e.CaseId)``: $($e.Error)") }
    }

    $text = $L -join "`n"
    [IO.File]::WriteAllText($OutFile, $text, (New-Object Text.UTF8Encoding($false)))
    return $text
}

function Save-JsonLine([string]$Path, $Object) {
    [IO.File]::AppendAllText($Path, ($Object | ConvertTo-Json -Depth 10 -Compress) + "`n", (New-Object Text.UTF8Encoding($false)))
}
