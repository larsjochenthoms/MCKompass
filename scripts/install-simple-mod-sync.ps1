# Run with Minecraft closed. Requires Windows PowerShell 5.1 or PowerShell 7.
# Downloads are prepared and SHA-512 checked before installed files change.
[CmdletBinding()]
param(
    [string]$GameDirectory,
    [string]$ManifestUrl = 'https://github.com/larsjochenthoms/MCKompass/releases/latest/download/simple-mod-sync.json',
    [switch]$PrepareOnly
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Assert-GameClosed {
    # The launcher may remain open; only Java game/server processes block changes.
    $games = @(Get-CimInstance Win32_Process -ErrorAction Stop | Where-Object {
        $_.Name -match '^(javaw?|minecraft)(\.exe)?$' -and
        $_.CommandLine -match 'net\.minecraft|net\.fabricmc|KnotClient|KnotServer|org\.(prismlauncher|multimc)\.EntryPoint|--gameDir|(fabric|minecraft|server)[^\r\n]*\.jar'
    })
    if ($games.Count -gt 0) { throw 'Minecraft is running. Close all Minecraft games and servers, then run this installer again.' }
}
function Receive-HttpsFile([string]$Url, [string]$Output) {
    $uri = [Uri]$Url
    for ($redirect = 0; $redirect -le 8; $redirect++) {
        if ($uri.Scheme -ne 'https' -or $uri.UserInfo) { throw 'Only HTTPS download URLs without credentials are accepted.' }
        $request = [Net.HttpWebRequest]::Create($uri)
        $request.AllowAutoRedirect = $false
        $request.Timeout = 60000
        $request.ReadWriteTimeout = 60000
        $request.UserAgent = 'MCKompass-Installer/1.0'
        $response = $request.GetResponse()
        try {
            $code = [int]$response.StatusCode
            if ($code -in @(301,302,303,307,308)) {
                $uri = [Uri]::new($uri, $response.Headers['Location'])
                continue
            }
            if ($code -ne 200) { throw "Download returned HTTP $code." }
            $inputStream = $response.GetResponseStream()
            $outputStream = [IO.File]::Create($Output)
            try {
                $buffer = New-Object byte[] 65536
                $total = 0L
                while (($count = $inputStream.Read($buffer,0,$buffer.Length)) -gt 0) {
                    $total += $count
                    if ($total -gt 67108864) { throw 'Download exceeds the 64 MiB limit.' }
                    $outputStream.Write($buffer,0,$count)
                }
            } finally { $outputStream.Dispose(); $inputStream.Dispose() }
            return
        } finally { $response.Dispose() }
    }
    throw 'Too many download redirects.'
}

function Read-ModId([string]$Path) {
    $zip = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $zip.GetEntry('fabric.mod.json')
        if ($null -eq $entry) { return '' }
        $reader = New-Object IO.StreamReader($entry.Open())
        try { return (($reader.ReadToEnd() | ConvertFrom-Json).id) } finally { $reader.Dispose() }
    } finally { $zip.Dispose() }
}

function Assert-Unlocked([string]$Path) {
    $handle = [IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    $handle.Dispose()
}

function ConvertTo-Hashtable($Value) {
    if ($null -ne $Value -and $Value.GetType().FullName -eq 'System.Management.Automation.PSCustomObject') {
        $table = @{}
        foreach ($property in $Value.PSObject.Properties) { $table[$property.Name] = ConvertTo-Hashtable $property.Value }
        return $table
    }
    if ($Value -is [array]) {
        $items = New-Object object[] $Value.Count
        for ($index = 0; $index -lt $Value.Count; $index++) { $items[$index] = ConvertTo-Hashtable $Value[$index] }
        return ,$items
    }
    return $Value
}

if (-not $GameDirectory) {
    Write-Host 'Open the Fabric 26.3 installation in the launcher and copy its Game Directory.'
    $GameDirectory = Read-Host 'Minecraft game directory (not the mods directory)'
}
$GameDirectory = $GameDirectory.Trim().Trim('"')
if (-not (Test-Path -LiteralPath $GameDirectory -PathType Container)) { throw 'The game directory does not exist.' }
$GameDirectory = (Resolve-Path -LiteralPath $GameDirectory).Path
if ((Split-Path -Leaf $GameDirectory) -eq 'mods') { throw 'Choose the game directory one level above mods.' }
Assert-GameClosed
$mods = Join-Path $GameDirectory 'mods'
$configDir = Join-Path $GameDirectory 'config'
$configPath = Join-Path $configDir 'simplemodsync.json'
foreach ($directory in @($mods,$configDir,(Join-Path $GameDirectory '.kompass-backups'))) {
    if ((Test-Path -LiteralPath $directory) -and -not (Test-Path -LiteralPath $directory -PathType Container)) { throw "Not a directory: $directory" }
    if ((Test-Path -LiteralPath $directory) -and ((Get-Item -LiteralPath $directory).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw "Linked directories are not supported: $directory" }
}
if ((Test-Path -LiteralPath $configPath) -and ((Get-Item -LiteralPath $configPath).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'A linked Simple Mod Sync config is not supported.' }
$stage = Join-Path $GameDirectory ('.kompass-install-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($stage) | Out-Null
$installed = New-Object 'System.Collections.Generic.List[string]'
$backedUp = New-Object 'System.Collections.Generic.List[object]'
$backup = $null
try {
    Write-Host 'Downloading the current Kompass release manifest...'
    $manifestPath = Join-Path $stage 'manifest.json'
    Receive-HttpsFile $ManifestUrl $manifestPath
    $manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
    if ($manifest.sync_version -ne 3 -or $manifest.minecraft_version -ne '26.3') { throw 'This installer requires the Minecraft 26.3 sync-version-3 manifest.' }
    $entries = @($manifest.sync)
    if ($entries.Count -ne 2 -or @($entries | Where-Object { $_.name -eq 'kompass4mc' }).Count -ne 1 -or @($entries | Where-Object { $_.name -eq 'fabric-api' }).Count -ne 1) { throw 'The manifest must contain exactly kompass4mc and fabric-api.' }
    $entries += [pscustomobject]@{
        name='simplemodsync'; version='1.4.1'; type='mod'
        url='https://cdn.modrinth.com/data/8IFHiuMI/versions/AVuvo48S/simplemodsync-fabric-26.3-1.4.1.jar'
        sha512='87d3b8abba706aab9bf942df683b631ae0060c3f034a0c7d7d59231e1717384032745d32f042d0f02436d2223c3176e21bed279d4171ccaf816a263537e273d4'
    }
    $prepared = @()
    foreach ($entry in $entries) {
        if ($entry.type -ne 'mod' -or $entry.version -notmatch '^[0-9A-Za-z][0-9A-Za-z.+_-]{0,79}$' -or $entry.sha512 -notmatch '^[0-9a-fA-F]{128}$') { throw 'Invalid mod entry or missing SHA-512 in manifest.' }
        $fileName = $entry.name + '-' + $entry.version + '.jar'
        $filePath = Join-Path $stage $fileName
        Write-Host ('Preparing ' + $fileName)
        Receive-HttpsFile $entry.url $filePath
        if ((Get-FileHash -LiteralPath $filePath -Algorithm SHA512).Hash -ne $entry.sha512) { throw "SHA-512 mismatch: $fileName. Installed files have not been changed." }
        $wantedId = if ($entry.name -eq 'simplemodsync') { 'simple-mod-sync' } else { $entry.name }
        if ((Read-ModId $filePath) -ne $wantedId) { throw "Unexpected Fabric mod ID in $fileName." }
        $prepared += [pscustomobject]@{Path=$filePath; Name=$fileName}
    }
    # Keep unrelated settings. Decline to replace another modpack's sync source.
    $config = @{}
    if (Test-Path -LiteralPath $configPath) {
        $config = Get-Content -Raw -LiteralPath $configPath | ConvertFrom-Json
        if ($null -eq $config -or $config.GetType().FullName -ne 'System.Management.Automation.PSCustomObject') { throw 'Existing Simple Mod Sync configuration is not a JSON object.' }
        $config = ConvertTo-Hashtable $config
    }
    foreach ($section in @('seen','sync')) {
        if (-not $config.ContainsKey($section)) { $config[$section] = @{} }
        if ($config[$section] -isnot [hashtable]) { throw "Invalid existing config section: $section" }
    }
    $previousUrl = $config['sync']['schema_file_url']
    if (-not $previousUrl -and $config.ContainsKey('download_url')) { $previousUrl = $config['download_url'] }
    if ($previousUrl -and $previousUrl -ne '-' -and $previousUrl -ne $ManifestUrl) { throw 'Simple Mod Sync already manages another modpack. Use a separate Fabric game directory for Kompass.' }
    $config.Remove('download_url'); $config.Remove('auto_download')
    $config['sync']['schema_file_url'] = $ManifestUrl
    $config['sync']['sync_on_startup'] = $true
    $config['seen']['init_screen'] = $true
    $config['seen']['allow_sync_progress_popup'] = $true
    $config['seen']['allow_needs_restart_popup'] = $true
    [IO.File]::WriteAllText((Join-Path $stage 'simplemodsync.json'), ($config | ConvertTo-Json -Depth 30), (New-Object Text.UTF8Encoding($false)))
    $oldJars = @()
    if (Test-Path -LiteralPath $mods) {
        foreach ($file in Get-ChildItem -LiteralPath $mods -Filter '*.jar' -File) {
            if ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Linked mod files are not supported: $($file.Name)" }
            try { $id = Read-ModId $file.FullName } catch {
                if ($file.Name -match '^(kompass4mc|fabric-api|simplemodsync)-.*\.jar$') { throw "Cannot inspect managed mod $($file.Name). Move it out of mods manually." }
                continue
            }
            if ($id -in @('kompass4mc','fabric-api','simple-mod-sync')) { $oldJars += $file.FullName }
        }
    }
    foreach ($entry in $prepared) {
        $destination = Join-Path $mods $entry.Name
        if ((Test-Path -LiteralPath $destination) -and $destination -notin $oldJars) { throw "An unrelated file occupies the destination: $destination" }
    }
    Assert-GameClosed
    foreach ($path in $oldJars) { Assert-Unlocked $path }
    if (Test-Path -LiteralPath $configPath) { Assert-Unlocked $configPath }
    if ($PrepareOnly) { Write-Host 'Preparation and checks passed. No installed files were changed.'; return }
    $backup = Join-Path (Join-Path $GameDirectory '.kompass-backups') ((Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
    [IO.Directory]::CreateDirectory($backup) | Out-Null
    [IO.Directory]::CreateDirectory($mods) | Out-Null
    [IO.Directory]::CreateDirectory($configDir) | Out-Null
    foreach ($path in @($oldJars) + @($configPath | Where-Object { Test-Path -LiteralPath $_ })) {
        $destination = Join-Path $backup ([IO.Path]::GetFileName($path))
        [IO.File]::Move($path,$destination)
        $backedUp.Add([pscustomobject]@{Original=$path; Backup=$destination})
    }
    foreach ($entry in $prepared) {
        $destination = Join-Path $mods $entry.Name
        [IO.File]::Move($entry.Path,$destination)
        $installed.Add($destination)
    }
    [IO.File]::Move((Join-Path $stage 'simplemodsync.json'),$configPath)
    $installed.Add($configPath)
    Write-Host "Ready. Start your existing Fabric 26.3 profile. Backups: $backup"
    Write-Host 'Simple Mod Sync checks for new releases at startup. Restart Minecraft after an update prompt.'
    Write-Host 'If an update fails, close Minecraft and run this installer again.'
} catch {
    # Roll back only this transaction's files; worlds and unrelated mods are never touched.
    foreach ($path in $installed) { if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force } }
    foreach ($entry in $backedUp) { if (Test-Path -LiteralPath $entry.Backup) { [IO.File]::Move($entry.Backup,$entry.Original) } }
    throw
} finally {
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
