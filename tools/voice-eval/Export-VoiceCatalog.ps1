<#
.SYNOPSIS
    Export the voice-assistant catalog from the local dev database to data/catalog.json.

.DESCRIPTION
    The SQL mirrors VoiceCommandService.buildCatalog(): connected peripherals, zones that
    contain peripherals (recursively), active scenarios and Navimow mowers, with the
    'feature.voice.alias' aliases. The JSON shape matches VoiceTools.catalogJson, so the
    evaluation sees exactly what the LLM sees in production.

    Connection defaults follow doc/db-scripts/sync-from-prod.ps1 (localhost, myhab/myhab).
    The output is installation data: data/ is git-ignored and must stay that way.

.EXAMPLE
    .\Export-VoiceCatalog.ps1
#>
[CmdletBinding()]
param(
    [string]$DbHost     = $(if ($env:LOCAL_HOST)     { $env:LOCAL_HOST }          else { 'localhost' }),
    [int]   $DbPort     = $(if ($env:LOCAL_PORT)     { [int]$env:LOCAL_PORT }     else { 5432 }),
    [string]$DbName     = $(if ($env:LOCAL_DB)       { $env:LOCAL_DB }            else { 'myhab' }),
    [string]$DbUser     = $(if ($env:LOCAL_USER)     { $env:LOCAL_USER }          else { 'myhab' }),
    [string]$DbPassword = $(if ($env:LOCAL_PASSWORD) { $env:LOCAL_PASSWORD }      else { 'myhab' }),
    [string]$OutFile    = (Join-Path $PSScriptRoot 'data\catalog.json'),
    # Optional alias overlay {"P<id>"|"Z<id>": [aliases]} merged into the export, to try aliases
    # before they are in the database. It names installation devices, so it normally lives in
    # the installation's own repo: set VOICE_ALIASES_FILE or pass the path.
    [string]$AliasesFile = $(if ($env:VOICE_ALIASES_FILE) { $env:VOICE_ALIASES_FILE } else { Join-Path $PSScriptRoot 'data\aliases.json' })
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "psql not found on PATH. Add your PostgreSQL bin dir to PATH."
}

$sql = @'
WITH RECURSIVE ztree AS (
    SELECT z.id AS root, z.id AS zid FROM zones z
    UNION ALL
    SELECT t.root, c.id FROM ztree t JOIN zones c ON c.parent_id = t.zid
),
alias AS (
    SELECT c.entity_type, c.entity_id,
           array_agg(DISTINCT trim(a)) FILTER (WHERE trim(a) <> '') AS names
    FROM configurations c, unnest(string_to_array(c.value, ',')) a
    WHERE c.key = 'feature.voice.alias'
    GROUP BY 1, 2
),
periph AS (
    SELECT p.id, p.name, cat.name AS category,
           coalesce((SELECT json_agg(z.name ORDER BY z.name)
                     FROM zones_peripherals_join j JOIN zones z ON z.id = j.zone_id
                     WHERE j.peripheral_id = p.id), '[]'::json) AS zones,
           coalesce(to_json(al.names), '[]'::json) AS aliases
    FROM device_peripherals p
    JOIN device_peripherals_categories cat ON cat.id = p.category_id
    LEFT JOIN alias al ON al.entity_type = 'PERIPHERAL' AND al.entity_id = p.id
    WHERE EXISTS (SELECT 1 FROM device_ports_peripherals_join dp WHERE dp.peripheral_id = p.id)
),
zonep AS (
    SELECT t.root AS id, json_agg(DISTINCT p.name) AS names
    FROM ztree t
    JOIN zones_peripherals_join j ON j.zone_id = t.zid
    JOIN device_peripherals p ON p.id = j.peripheral_id
    WHERE p.name IS NOT NULL
    GROUP BY t.root
)
SELECT json_build_object(
    'peripherals', coalesce((SELECT json_agg(json_build_object(
                        'id', id, 'name', name, 'category', category,
                        'zones', zones, 'aliases', aliases) ORDER BY id) FROM periph), '[]'::json),
    'zones',       coalesce((SELECT json_agg(json_build_object(
                        'id', z.id, 'name', z.name, 'peripherals', zp.names,
                        'aliases', coalesce(to_json(al.names), '[]'::json)) ORDER BY z.id)
                     FROM zones z
                     JOIN zonep zp ON zp.id = z.id
                     LEFT JOIN alias al ON al.entity_type = 'ZONE' AND al.entity_id = z.id), '[]'::json),
    'scenarios',   coalesce((SELECT json_agg(json_build_object(
                        'jobId', id, 'name', name, 'description', description) ORDER BY id)
                     FROM jobs WHERE state = 'ACTIVE'), '[]'::json),
    'mowers',      coalesce((SELECT json_agg(json_build_object(
                        'deviceId', id, 'name', coalesce(name, code)) ORDER BY id)
                     FROM device_controllers WHERE model = 'NAVIMOW_SEGWAY'), '[]'::json)
);
'@

New-Item -ItemType Directory -Force (Split-Path $OutFile) | Out-Null

# psql writes the file itself (-o) so non-ASCII names stay UTF-8 regardless of the console code page.
$env:PGPASSWORD = $DbPassword
$env:PGCLIENTENCODING = 'UTF8'
& psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -X -q -t -A -o $OutFile -c $sql
if ($LASTEXITCODE -ne 0) { throw "psql failed (exit $LASTEXITCODE)" }

$catalog = Get-Content -Raw -Encoding UTF8 $OutFile | ConvertFrom-Json

# Optional overlay: tries aliases without writing them to the database.
$overlayFile = $AliasesFile
if ($overlayFile -and (Test-Path $overlayFile)) {
    $overlay = Get-Content -Raw -Encoding UTF8 $overlayFile | ConvertFrom-Json
    $added = 0
    foreach ($entry in @(@($catalog.peripherals | ForEach-Object { , @("P$($_.id)", $_) }) + @($catalog.zones | ForEach-Object { , @("Z$($_.id)", $_) }))) {
        $extra = @($overlay.($entry[0]))
        if (-not $extra) { continue }
        $entry[1].aliases = @(@($entry[1].aliases) + $extra | Where-Object { $_ } | Select-Object -Unique)
        $added += $extra.Count
    }
    [IO.File]::WriteAllText($OutFile, ($catalog | ConvertTo-Json -Depth 10 -Compress), (New-Object Text.UTF8Encoding($false)))
    Write-Host "Merged $added aliases from $overlayFile"
}

$targets = @($catalog.peripherals).Count + @($catalog.zones).Count

Write-Host "Catalog written to $OutFile" -ForegroundColor Green
Write-Host ("  peripherals: {0}" -f @($catalog.peripherals).Count)
Write-Host ("  zones:       {0}" -f @($catalog.zones).Count)
Write-Host ("  scenarios:   {0}" -f @($catalog.scenarios).Count)
Write-Host ("  mowers:      {0}" -f @($catalog.mowers).Count)
Write-Host ("  size:        {0:N1} KB" -f ((Get-Item $OutFile).Length / 1KB))
if ($targets -gt 254) {
    Write-Warning "peripherals + zones = $targets exceeds Jev's 255-option choice limit (with 'none'); run Invoke-JevEval.ps1 -TargetMode Split."
}
