<#
.SYNOPSIS
    Simulate a BERT -> Jev -> Claude cascade from existing run results.

.DESCRIPTION
    For every (case, repeat) the three runs share, a stage answers when its decision is
    actionable (control / scenario / query / mower) and its confidence is at least that stage's
    gate; otherwise the request falls through to the next stage. Claude always answers, so
    clarifications and off-topic requests end up there. Latency is the sum of the stages the
    request passed through (Claude's is its first call only; production adds a second call
    for the spoken reply).

    Correctness treats "clarify" and "other" as the same no-action outcome, because the Claude
    baseline cannot tell them apart reliably.

.EXAMPLE
    .\Simulate-Cascade.ps1 -Bert results\embed-...jsonl -Jev results\jev-...jsonl -Claude results\claude-...jsonl
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Bert,
    [Parameter(Mandatory)][string]$Jev,
    [Parameter(Mandatory)][string]$Claude,
    [double[]]$BertGates = @(1.01, 0.95, 0.9, 0.8, 0.7, 0.6),
    [double[]]$JevGates  = @(1.01, 0.95, 0.9, 0.8, 0.7),
    [string]  $OutFile   = (Join-Path $PSScriptRoot 'results\cascade-summary.md')
)

. (Join-Path $PSScriptRoot 'Common.ps1')

$cases = @{}
foreach ($c in @(Import-Cases -Langs 'en', 'ro', 'ru')) { $cases[$c.Id] = $c }
$Actionable = @('control', 'scenario', 'query', 'mower')

function Get-NoActionNormalized($Pred) {
    $q = $Pred.PSObject.Copy(); if ($q.intent -in 'clarify', 'other') { $q.intent = 'noaction' }; $q
}
$acceptCache = @{}
function Get-Accept([string]$CaseId) {
    if (-not $acceptCache.ContainsKey($CaseId)) {
        $acceptCache[$CaseId] = @($cases[$CaseId].Accept | ForEach-Object {
            $a = $_.PSObject.Copy(); if ($a.intent -in 'clarify', 'other') { $a.intent = 'noaction' }; $a })
    }
    $acceptCache[$CaseId]
}

function Import-Run([string]$Path) {
    $map = @{}
    foreach ($line in Get-Content -Encoding UTF8 $Path) {
        if (-not $line.Trim()) { continue }
        $r = $line | ConvertFrom-Json
        if (-not $r.Ok) { continue }
        $map["$($r.CaseId)#$($r.Repeat)"] = [pscustomobject]@{
            Pred = $r.Pred; Conf = $(if ($null -ne $r.Confidence) { [double]$r.Confidence } else { 1.0 }); Ms = [double]$r.Ms
            Full = (Test-Prediction (Get-NoActionNormalized $r.Pred) (Get-Accept $r.CaseId)).Full
            Actionable = ($r.Pred.intent -in $Actionable)
        }
    }
    $map
}

$runs = @{ bert = Import-Run $Bert; jev = Import-Run $Jev; claude = Import-Run $Claude }
$keys = @($runs.bert.Keys | Where-Object { $runs.jev.ContainsKey($_) -and $runs.claude.ContainsKey($_) } | Sort-Object)
if (-not $keys) { throw 'The three runs share no (case, repeat) keys.' }

function Measure-Cascade([double]$BertGate, [double]$JevGate) {
    $ms = New-Object System.Collections.Generic.List[double]
    $correct = 0; $wrongExec = 0; $byStage = @{ bert = 0; jev = 0; claude = 0 }
    foreach ($k in $keys) {
        $total = 0.0; $final = $null; $stage = $null
        foreach ($s in @(@('bert', $BertGate), @('jev', $JevGate), @('claude', -1))) {
            if ($s[1] -gt 1) { continue }   # stage switched off
            $r = $runs[$s[0]][$k]
            $total += $r.Ms
            if ($s[0] -eq 'claude' -or ($r.Actionable -and $r.Conf -ge $s[1])) { $final = $r; $stage = $s[0]; break }
        }
        $ms.Add($total); $byStage[$stage]++
        if ($final.Full) { $correct++ } elseif ($final.Actionable) { $wrongExec++ }
    }
    $arr = [double[]]$ms.ToArray(); $n = $keys.Count
    [pscustomobject]@{
        BertGate = $BertGate; JevGate = $JevGate; N = $n
        Correct = 100.0 * $correct / $n; WrongExec = 100.0 * $wrongExec / $n
        Bert = 100.0 * $byStage.bert / $n; Jev = 100.0 * $byStage.jev / $n; Claude = 100.0 * $byStage.claude / $n
        P50 = Get-Percentile $arr 50; P90 = Get-Percentile $arr 90; Mean = ($arr | Measure-Object -Average).Average
    }
}

function Format-Gate([double]$g) { if ($g -gt 1) { 'off' } else { '{0:N2}' -f $g } }

$rows = foreach ($bg in $BertGates) { foreach ($jg in $JevGates) { Measure-Cascade $bg $jg } }

$L = New-Object System.Collections.Generic.List[string]
$L.Add('# Cascade simulation (BERT -> Jev -> Claude)'); $L.Add('')
$L.Add("Runs: BERT ``$(Split-Path $Bert -Leaf)``, Jev ``$(Split-Path $Jev -Leaf)``, Claude ``$(Split-Path $Claude -Leaf)``; $($keys.Count) shared calls.")
$L.Add('Gate "off" = stage skipped. Latency = sum of the stages passed (Claude first call only).'); $L.Add('')
$L.Add('| BERT gate | Jev gate | correct | wrong executed | BERT / Jev / Claude share | p50 ms | p90 ms | mean ms |')
$L.Add('|---|---|---|---|---|---|---|---|')
foreach ($r in $rows) {
    $L.Add(('| {0} | {1} | {2:N1}% | {3:N1}% | {4:N0} / {5:N0} / {6:N0}% | {7:N0} | {8:N0} | {9:N0} |' -f
        (Format-Gate $r.BertGate), (Format-Gate $r.JevGate), $r.Correct, $r.WrongExec, $r.Bert, $r.Jev, $r.Claude, $r.P50, $r.P90, $r.Mean))
}
$text = $L -join "`n"
[IO.File]::WriteAllText($OutFile, $text, (New-Object Text.UTF8Encoding($false)))
Write-Host $text
Write-Host "`nSummary: $OutFile" -ForegroundColor Green
