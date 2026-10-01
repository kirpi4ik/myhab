<#
.SYNOPSIS
    Score a results/*.jsonl file written by any runner and write its -summary.md.

.DESCRIPTION
    Each line needs CaseId, Lang, Repeat, Ok, Ms, Pred {intent, target, action, scenario,
    mowerAction} and optionally Confidence, Top, InputTokens, CostUsd, Error. Scoring uses
    data/cases.json and the shared functions in Common.ps1, so every model is measured the
    same way.

.EXAMPLE
    .\Summarize-Results.ps1 -File results\setfit-20261001-101500.jsonl -Title 'SetFit (MiniLM)'
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$File,
    [string]$Title = [IO.Path]::GetFileNameWithoutExtension($File),
    # The case file the run used, e.g. a held-out set; defaults to data/cases.json.
    [string]$Cases
)

. (Join-Path $PSScriptRoot 'Common.ps1')

$caseArgs = @{ Langs = @('en', 'ro', 'ru') }
if ($Cases) { $caseArgs.Path = $Cases }
$byId = @{}
foreach ($c in @(Import-Cases @caseArgs)) { $byId[$c.Id] = $c }

$records = @(Get-Content -Encoding UTF8 $File | Where-Object { $_.Trim() } | ForEach-Object {
    $r = $_ | ConvertFrom-Json
    $c = $byId[$r.CaseId]
    [pscustomobject]@{
        CaseId = $r.CaseId; Intent = $c.Intent; Lang = $r.Lang; Text = $c.Text; Accept = $c.Accept; Repeat = $r.Repeat
        Ok = $r.Ok; Ms = $r.Ms; Retries = [int]$r.Retries; Error = $r.Error; Pred = $r.Pred
        Confidence = $r.Confidence; Top = $r.Top; InputTokens = $r.InputTokens; CostUsd = $r.CostUsd
        Score = $(if ($r.Ok) { Test-Prediction $r.Pred $c.Accept } else { $null })
    }
})

$summary = [IO.Path]::ChangeExtension($File, $null).TrimEnd('.') + '-summary.md'
$text = Write-EvalSummary $records $Title $summary
Write-Host $text
Write-Host "`nSummary: $summary" -ForegroundColor Green
