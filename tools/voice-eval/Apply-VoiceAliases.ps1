<#
.SYNOPSIS
    Write voice aliases (feature.voice.alias) to a myHAB server from an aliases JSON file.

.DESCRIPTION
    The file (-AliasesFile, default $env:VOICE_ALIASES_FILE, else data/aliases.json) maps
    catalog keys to alias lists:
        { "P12": ["office light", "lumina din birou"], "Z3": ["kitchen", "кухня"] }
    P<id> is a peripheral, Z<id> a zone.

    For each entry the script checks the entity exists (and prints its name), reads the
    aliases already stored, and saves the result through the same GraphQL mutation the
    Peripheral/Zone edit screens use (savePropertyValue). By default it MERGES: stored
    aliases are kept and the file's are appended, de-duplicated case-insensitively.
    -Replace stores exactly the file's list instead. Entities whose aliases would not change
    are not written.

    The NLU sidecar picks the change up on its own: the voiceNluSync job sends the new
    catalog within its interval (default 5 minutes) and the sidecar retrains.

.EXAMPLE
    $env:VOICE_ALIASES_FILE = 'D:\my-installation\voice-nlu\aliases.json'
    .\Apply-VoiceAliases.ps1 -BaseUrl https://myhab.example.com -WhatIf     # preview, writes nothing
.EXAMPLE
    .\Apply-VoiceAliases.ps1 -BaseUrl https://myhab.example.com -Only P6810,P30683
.EXAMPLE
    $cred = Get-Credential; .\Apply-VoiceAliases.ps1 -BaseUrl http://localhost:8181 -Credential $cred -Replace
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][string]$BaseUrl,
    # Installation data (it names your devices): keep it in the installation's repo and set
    # VOICE_ALIASES_FILE, or pass the path.
    [string]$AliasesFile = $(if ($env:VOICE_ALIASES_FILE) { $env:VOICE_ALIASES_FILE } else { Join-Path $PSScriptRoot 'data\aliases.json' }),
    [pscredential]$Credential,
    # Only these catalog keys (wildcards allowed, e.g. P184*).
    [string[]]$Only = @('*'),
    [switch]$Replace
)

. (Join-Path $PSScriptRoot 'Common.ps1')

$AliasKey = 'feature.voice.alias'
$BaseUrl = $BaseUrl.TrimEnd('/')

# ---------------------------------------------------------------- input
$entries = (Read-Utf8Json $AliasesFile).PSObject.Properties |
    Where-Object { $n = $_.Name; @($Only | Where-Object { $n -like $_ }).Count } |
    ForEach-Object {
        if ($_.Name -notmatch '^([PZ])(\d+)$') { throw "Unsupported key '$($_.Name)': expected P<peripheralId> or Z<zoneId>." }
        $aliases = @($_.Value | ForEach-Object { "$_".Trim() } | Where-Object { $_ })
        $withComma = @($aliases | Where-Object { $_.Contains(',') })
        if ($withComma) { throw "$($_.Name): aliases are stored comma-separated and cannot contain a comma: $($withComma -join ' | ')" }
        [pscustomobject]@{
            Key        = $_.Name
            EntityType = $(if ($Matches[1] -eq 'P') { 'PERIPHERAL' } else { 'ZONE' })
            Id         = [long]$Matches[2]
            Aliases    = $aliases
        }
    }
if (-not $entries) { throw "No entries in $AliasesFile match -Only $($Only -join ',')." }

# ---------------------------------------------------------------- session
if (-not $Credential) { $Credential = Get-Credential -Message "myHAB login for $BaseUrl" }
$client = New-EvalHttpClient
$login = Invoke-JsonPost $client "$BaseUrl/api/login" @{} (@{
        username = $Credential.UserName; password = $Credential.GetNetworkCredential().Password } | ConvertTo-Json -Compress) -MaxRetries 0
if (-not $login.Ok) { throw "Login failed: HTTP $($login.Status)" }
$token = $(if ($login.Body.access_token) { $login.Body.access_token } else { $login.Body.token })
if (-not $token) { throw 'Login response has no access_token.' }
$auth = @{ Authorization = "Bearer $token" }

function Invoke-Gql([string]$Query, [hashtable]$Variables) {
    $res = Invoke-JsonPost $client "$BaseUrl/graphql" $auth (@{ query = $Query; variables = $Variables } | ConvertTo-Json -Depth 5 -Compress)
    if (-not $res.Ok) { throw "GraphQL HTTP $($res.Status): $($res.Error)" }
    if ($res.Body.errors) { throw "GraphQL error: $(($res.Body.errors | ForEach-Object { $_.message }) -join '; ')" }
    $res.Body.data
}

$qPeripheral = 'query ($id: Long!) { devicePeripheral(id: $id) { id name } }'
$qZone = 'query ($id: Long!) { zone(id: $id) { id name } }'
$qAliases = 'query ($key: String!, $entityId: Long!, $entityType: EntityType!) {
    configListByKey(key: $key, entityId: $entityId, entityType: $entityType) { id value } }'
$mSave = 'mutation ($key: String!, $entityId: Long!, $entityType: EntityType!, $value: String!) {
    savePropertyValue(key: $key, entityId: $entityId, entityType: $entityType, value: $value) { id value } }'

# ---------------------------------------------------------------- apply
$counts = [ordered]@{ written = 0; unchanged = 0; missing = 0; skipped = 0; failed = 0 }
foreach ($e in $entries) {
    try {
        $entity = if ($e.EntityType -eq 'PERIPHERAL') { (Invoke-Gql $qPeripheral @{ id = $e.Id }).devicePeripheral }
                  else { (Invoke-Gql $qZone @{ id = $e.Id }).zone }
        if (-not $entity) {
            Write-Warning "$($e.Key): no $($e.EntityType.ToLower()) with id $($e.Id) - skipped"
            $counts.missing++
            continue
        }

        $stored = @((Invoke-Gql $qAliases @{ key = $AliasKey; entityId = $e.Id; entityType = $e.EntityType }).configListByKey |
            ForEach-Object { "$($_.value)".Split(',') } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $target = New-Object System.Collections.Generic.List[string]
        $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
        foreach ($a in @($(if ($Replace) { @() } else { $stored })) + $e.Aliases) { if ($seen.Add($a)) { $target.Add($a) } }

        $label = "$($e.Key) '$($entity.name)'"
        if (($target -join ', ') -eq ($stored -join ', ')) {
            Write-Host ("= {0}: unchanged ({1} aliases)" -f $label, $stored.Count)
            $counts.unchanged++
            continue
        }
        $added = @($target | Where-Object { $stored -notcontains $_ })
        $removed = @($stored | Where-Object { $target -notcontains $_ })
        $change = (@($added | ForEach-Object { "+$_" }) + @($removed | ForEach-Object { "-$_" })) -join '; '
        if ($PSCmdlet.ShouldProcess($label, "set voice aliases ($change)")) {
            Invoke-Gql $mSave @{ key = $AliasKey; entityId = $e.Id; entityType = $e.EntityType; value = ($target -join ', ') } | Out-Null
            Write-Host ("+ {0}: {1}" -f $label, $change) -ForegroundColor Green
            $counts.written++
        } else {
            $counts.skipped++
        }
    } catch {
        Write-Warning "$($e.Key): $($_.Exception.Message)"
        $counts.failed++
    }
}
$client.Dispose()

Write-Host ''
Write-Host (($counts.GetEnumerator() | ForEach-Object { "$($_.Key): $($_.Value)" }) -join ', ')
if ($counts.failed) { exit 1 }
