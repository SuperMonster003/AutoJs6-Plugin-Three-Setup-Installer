#Requires -Version 7.2
[CmdletBinding(DefaultParameterSetName = 'Run')]
param(
    [Parameter(Mandatory, ParameterSetName = 'Run')]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$')]
    [string]$Serial,
    [Parameter(Mandatory, ParameterSetName = 'Run')]
    [ValidateSet('none', 'shizuku', 'root')]
    [string]$Authorizer,
    [Parameter(ParameterSetName = 'Run')]
    [ValidateRange(60, 600)][int]$TimeoutSeconds = 240,
    [Parameter(Mandatory, ParameterSetName = 'SelfTest')]
    [switch]$SelfTest
)

# The operator installs the release and official debug host separately. The fixed plugin's enabled
# key may be temporarily set by a host probe that journals and restores its exact original value;
# trust, priority and all other preferences remain unchanged. This driver never grants authorizers.
# It installs only the code-free owned fixture via the official host, leaving confirmation to the operator.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$pluginPackage = 'io.github.supermonster003.autojs6.plugin.three.setup.installer'
$hostPackage = 'org.autojs.autojs6'
$probeComponent = "$hostPackage/org.autojs.autojs.core.plugin.installer.InstallerProcessProbeActivity"
$fixturePackage = 'io.github.supermonster003.autojs6.installer.performance.fixture'

function ConvertFrom-UserPackages {
    param([Parameter(Mandatory)][AllowEmptyString()][string]$Text)
    $lines = @($Text -split '\r?\n' | ForEach-Object { $_.Trim() } | Where-Object { $_.Length -gt 0 })
    if ('package:android' -cnotin $lines -or @($lines | Where-Object { $_ -cnotmatch '^package:[A-Za-z0-9_.]+$' }).Count -gt 0) {
        throw 'Cannot safely enumerate installed packages and retained data for every user.'
    }
    @($lines | ForEach-Object { $_.Substring(8) })
}

function Assert-PerformanceResult {
    param([Parameter(Mandatory)][hashtable]$Result, [Parameter(Mandatory)][string]$ExpectedSha256,
        [Parameter(Mandatory)][string]$ExpectedAuthorizer, [long]$ExpectedBytes,
        [Parameter(Mandatory)][string]$ExpectedCaseId, [int]$ExpectedHostUid)
    if ($Result['completed'] -isnot [bool] -or $Result['completed'] -ne $true -or
        $Result['passed'] -isnot [bool] -or $Result['passed'] -ne $true) { throw 'The probe has no complete successful result.' }
    if ($Result['caseId'] -cne $ExpectedCaseId -or $Result['hostPackage'] -cne $hostPackage -or
        [int]$Result['uid'] -ne $ExpectedHostUid -or [int]$Result['pid'] -le 0) {
        throw 'The evidence does not belong to this run and the actual official host UID.'
    }
    if ($Result['sourceSha256'] -cne $ExpectedSha256) { throw 'The device did not inspect the exact expected source.' }
    $value = $Result['result']
    if ($value -isnot [hashtable] -or $value['ok'] -isnot [bool] -or $value['ok'] -ne $true -or $value['packageName'] -cne $fixturePackage -or
        [long]$value['versionCode'] -ne 1 -or $value['authorizer'] -cne $ExpectedAuthorizer) {
        throw 'The install result does not identify the owned fixture and requested authorizer.'
    }
    foreach ($key in @('inspectElapsedMs', 'getUsersElapsedMs', 'installElapsedMs')) {
        if (!$Result.ContainsKey($key) -or [long]$Result[$key] -lt 0) { throw "A performance sample is missing: $key" }
    }
    if ([long]$Result['bytesWritten'] -ne $ExpectedBytes -or [long]$Result['totalBytes'] -ne $ExpectedBytes) {
        throw 'The actual write progress did not account for the complete 100 MiB source.'
    }
    if ($Result['stageElapsedMs'] -isnot [hashtable] -or !$Result['stageElapsedMs'].ContainsKey('writing')) {
        throw 'The performance sample never reached the real writing stage.'
    }
}

function Assert-EnableJournal {
    param([Parameter(Mandatory)][hashtable]$Journal, [Parameter(Mandatory)][ValidateSet('prepared', 'restored')][string]$Phase,
        [Parameter(Mandatory)][string]$ExpectedCaseId, [int]$ExpectedHostUid, [hashtable]$PreparedJournal)
    if ($Journal['caseId'] -cne $ExpectedCaseId -or $Journal['targetPackage'] -cne $pluginPackage -or
        [int]$Journal['hostUid'] -ne $ExpectedHostUid -or $Journal['phase'] -cne $Phase) {
        throw 'The enable-state journal is not owned by this case, host UID and fixed plugin.'
    }
    foreach ($key in @('pending', 'originalPresent', 'originalEnabled', 'preparedEnabled')) {
        if ($Journal[$key] -isnot [bool]) { throw "The enable-state journal has no typed value for $key." }
    }
    $fingerprints = @($Journal['fingerprints'])
    $normalizedFingerprints = @($fingerprints | Sort-Object -Unique)
    if ($fingerprints.Count -eq 0 -or @($fingerprints | Where-Object { $_ -isnot [string] -or $_ -cnotmatch '^[0-9a-f]{64}$' }).Count -gt 0 -or
        ($fingerprints -join ',') -cne ($normalizedFingerprints -join ',')) {
        throw 'The enable-state journal has no canonical signing-certificate identity.'
    }
    if ($Journal['preparedEnabled'] -ne $true -or $Journal['pending'] -ne ($Phase -ceq 'prepared')) {
        throw 'The enable-state journal has not reached the required phase.'
    }
    if ($null -ne $PreparedJournal -and ($Journal['originalPresent'] -ne $PreparedJournal['originalPresent'] -or
        $Journal['originalEnabled'] -ne $PreparedJournal['originalEnabled'] -or
        ($fingerprints -join ',') -cne (@($PreparedJournal['fingerprints']) -join ','))) {
        throw 'The journal baseline changed after prepare; restoring another baseline is not acceptance.'
    }
    if ($Phase -ceq 'restored') {
        if ($Journal['restoredPresent'] -isnot [bool] -or $Journal['restoredEnabled'] -isnot [bool] -or
            $Journal['restoredPresent'] -ne $Journal['originalPresent'] -or $Journal['restoredEnabled'] -ne $Journal['originalEnabled']) {
            throw 'The host enable preference was not restored to the exact original presence and value.'
        }
    }
}

function Read-ExactStreamSha256 {
    param([Parameter(Mandatory)][IO.Stream]$InputStream, [ValidateRange(0, [long]::MaxValue)][long]$ExpectedBytes,
        [Threading.CancellationToken]$CancellationToken = [Threading.CancellationToken]::None)
    $hash = [Security.Cryptography.IncrementalHash]::CreateHash([Security.Cryptography.HashAlgorithmName]::SHA256)
    $buffer = [byte[]]::new(65536)
    $total = 0L
    try {
        while ($true) {
            $CancellationToken.ThrowIfCancellationRequested()
            $count = $InputStream.ReadAsync($buffer, 0, $buffer.Length, $CancellationToken).GetAwaiter().GetResult()
            if ($count -eq 0) { break }
            $total += $count
            if ($total -gt $ExpectedBytes) { throw 'The binary source stream exceeded the exact expected fixture size.' }
            $hash.AppendData($buffer, 0, $count)
        }
        if ($total -ne $ExpectedBytes) { throw "The binary source stream was truncated: expected $ExpectedBytes bytes, read $total." }
        [Convert]::ToHexString($hash.GetHashAndReset()).ToLowerInvariant()
    } finally { $hash.Dispose() }
}

if ($SelfTest) {
    function Expect-Rejected([scriptblock]$Action) {
        $rejected = $false
        try { & $Action | Out-Null } catch { $rejected = $true }
        if (!$rejected) { throw 'A guard accepted incomplete or unsafe evidence.' }
    }
    if ((ConvertFrom-UserPackages "package:android`npackage:example.test").Count -ne 2) { throw 'Package parsing failed.' }
    foreach ($bad in @('', 'Error: permission denied', 'package:example.test', "package:android`nerror")) {
        Expect-Rejected { ConvertFrom-UserPackages $bad }
    }
    $good = @{
        completed = $true; passed = $true; sourceSha256 = ('a' * 64)
        caseId = 'performance-selftest'; hostPackage = $hostPackage; uid = 10123; pid = 2345
        result = @{ ok = $true; packageName = $fixturePackage; versionCode = 1; authorizer = 'root' }
        inspectElapsedMs = 1; getUsersElapsedMs = 2; installElapsedMs = 3
        bytesWritten = 104857601L; totalBytes = 104857601L; stageElapsedMs = @{ writing = 1 }
    }
    Assert-PerformanceResult $good ('a' * 64) 'root' 104857601L 'performance-selftest' 10123
    foreach ($key in @('completed', 'passed', 'sourceSha256', 'result', 'inspectElapsedMs', 'bytesWritten', 'stageElapsedMs', 'caseId', 'hostPackage', 'uid', 'pid')) {
        $invalid = $good.Clone()
        $invalid.Remove($key)
        Expect-Rejected { Assert-PerformanceResult $invalid ('a' * 64) 'root' 104857601L 'performance-selftest' 10123 }
    }
    Expect-Rejected { Assert-PerformanceResult $good ('b' * 64) 'root' 104857601L 'performance-selftest' 10123 }
    Expect-Rejected { Assert-PerformanceResult $good ('a' * 64) 'none' 104857601L 'performance-selftest' 10123 }
    Expect-Rejected { Assert-PerformanceResult $good ('a' * 64) 'root' 104857601L 'another-case' 10123 }
    Expect-Rejected { Assert-PerformanceResult $good ('a' * 64) 'root' 104857601L 'performance-selftest' 20123 }
    $invalid = $good.Clone()
    $invalid['completed'] = 'true'
    Expect-Rejected { Assert-PerformanceResult $invalid ('a' * 64) 'root' 104857601L 'performance-selftest' 10123 }
    $prepared = @{
        caseId = 'performance-selftest'; targetPackage = $pluginPackage; hostUid = 10123; phase = 'prepared'
        pending = $true; originalPresent = $true; originalEnabled = $false; preparedEnabled = $true
        fingerprints = @(('a' * 64))
    }
    Assert-EnableJournal $prepared 'prepared' 'performance-selftest' 10123
    $restored = $prepared.Clone()
    $restored['phase'] = 'restored'; $restored['pending'] = $false
    $restored['restoredPresent'] = $true; $restored['restoredEnabled'] = $false
    Assert-EnableJournal $restored 'restored' 'performance-selftest' 10123 $prepared
    $missingBaseline = $prepared.Clone()
    $missingBaseline['originalPresent'] = $false; $missingBaseline['originalEnabled'] = $true
    Assert-EnableJournal $missingBaseline 'prepared' 'performance-selftest' 10123
    foreach ($key in @('caseId', 'targetPackage', 'hostUid', 'phase', 'pending', 'originalPresent', 'originalEnabled', 'preparedEnabled', 'restoredPresent', 'restoredEnabled', 'fingerprints')) {
        $invalid = $restored.Clone()
        $invalid.Remove($key)
        Expect-Rejected { Assert-EnableJournal $invalid 'restored' 'performance-selftest' 10123 $prepared }
    }
    $invalid = $restored.Clone(); $invalid['restoredEnabled'] = $true
    Expect-Rejected { Assert-EnableJournal $invalid 'restored' 'performance-selftest' 10123 $prepared }
    $invalid = $restored.Clone(); $invalid['originalEnabled'] = $true; $invalid['restoredEnabled'] = $true
    Expect-Rejected { Assert-EnableJournal $invalid 'restored' 'performance-selftest' 10123 $prepared }
    $invalid = $restored.Clone(); $invalid['fingerprints'] = @(('b' * 64))
    Expect-Rejected { Assert-EnableJournal $invalid 'restored' 'performance-selftest' 10123 $prepared }
    $invalid = $restored.Clone(); $invalid['fingerprints'] = @(('a' * 64), ('a' * 64))
    Expect-Rejected { Assert-EnableJournal $invalid 'restored' 'performance-selftest' 10123 $prepared }
    $binary = [IO.MemoryStream]::new([byte[]]@(0, 255, 10, 13, 128, 1, 0), $false)
    try {
        if ((Read-ExactStreamSha256 $binary 7) -cne '61c9f4fea01ab4be0c05008b493a2a2e3b0a6add7dc406b46cd399499510e8f9') {
            throw 'Binary SHA-256 changed non-text bytes or line endings.'
        }
        $binary.Position = 0
        Expect-Rejected { Read-ExactStreamSha256 $binary 8 }
        $binary.Position = 0
        Expect-Rejected { Read-ExactStreamSha256 $binary 6 }
        $cancelled = [Threading.CancellationTokenSource]::new()
        try {
            $cancelled.Cancel()
            $binary.Position = 0
            Expect-Rejected { Read-ExactStreamSha256 $binary 7 $cancelled.Token }
        } finally { $cancelled.Dispose() }
    } finally { $binary.Dispose() }
    Write-Host 'PASS: ownership, performance, preference restoration and bounded binary SHA-256 guards. No adb command was executed.'
    return
}

$adbExecutable = (Get-Command adb -CommandType Application -ErrorAction Stop).Source
$repoRoot = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$fixturePath = Join-Path $repoRoot 'build/performance-install-fixture-100/fixture.apk'
$metadataPath = Join-Path $repoRoot 'build/performance-install-fixture-100/fixture-metadata.json'
if (!(Test-Path -LiteralPath $fixturePath) -or !(Test-Path -LiteralPath $metadataPath)) {
    throw 'Build the fixed source with tools/build-performance-install-fixture.ps1 first.'
}
$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json -AsHashtable
$sourceBytes = (Get-Item -LiteralPath $fixturePath).Length
$sourceHash = (Get-FileHash -LiteralPath $fixturePath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($metadata['packageName'] -cne $fixturePackage -or $metadata['versionCode'] -ne 1 -or
    $metadata['payloadBytes'] -ne 104857600 -or $metadata['singleApk'] -ne $true -or $metadata['codeFree'] -ne $true -or
    @($metadata['permissions']).Count -ne 0 -or @($metadata['components']).Count -ne 0 -or
    $metadata['sha256'] -cne $sourceHash -or [long]$metadata['apkBytes'] -ne $sourceBytes -or
    $sourceBytes -lt 104857600 -or $sourceBytes -gt 105906176) {
    throw 'Fixture metadata, payload size or source digest does not match the guarded single APK.'
}

$caseId = "performance-$([guid]::NewGuid().ToString('N'))"
$privateDirectory = "files/p6-installer-probe/$caseId"
$privateSource = "$privateDirectory/fixture.apk"
$remoteSource = "/data/local/tmp/$caseId.apk"
$runFolder = Join-Path $repoRoot "build/release-performance/$Serial-$Authorizer-$caseId"
New-Item -ItemType Directory -Path $runFolder -Force | Out-Null
$commandLog = Join-Path $runFolder 'commands.log'

function Invoke-Adb {
    param([Parameter(Mandatory)][string[]]$Arguments, [switch]$AllowFailure)
    $start = [Diagnostics.ProcessStartInfo]::new($adbExecutable)
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.ArgumentList.Add('-s')
    $start.ArgumentList.Add($Serial)
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::Start($start)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if (!$process.WaitForExit(60000)) { $process.Kill(); throw 'An adb operation exceeded 60 seconds.' }
    $output = $stdout.GetAwaiter().GetResult() + $stderr.GetAwaiter().GetResult()
    $exitCode = $process.ExitCode
    $process.Dispose()
    [IO.File]::AppendAllText($commandLog, "$($Arguments -join ' ')`n$output`nexit=$exitCode`n", [Text.UTF8Encoding]::new($false))
    if (!$AllowFailure -and $exitCode -ne 0) { throw "adb failed (exit $exitCode): $($Arguments -join ' '): $output" }
    [pscustomobject]@{ Text = $output.Trim(); ExitCode = $exitCode }
}

function Get-RemoteSourceSha256 {
    # Android 7 need not ship sha256sum. Stream raw bytes on the PC, never through a text reader
    # or a 100 MiB in-memory buffer. The byte count also catches exec-out's error text on stdout.
    $arguments = @('exec-out', 'run-as', $hostPackage, 'cat', $privateSource)
    $start = [Diagnostics.ProcessStartInfo]::new($adbExecutable)
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.ArgumentList.Add('-s')
    $start.ArgumentList.Add($Serial)
    foreach ($argument in $arguments) { $start.ArgumentList.Add($argument) }
    $deadline = [Threading.CancellationTokenSource]::new(60000)
    $process = $null
    try {
        $process = [Diagnostics.Process]::Start($start)
        $stderr = $process.StandardError.ReadToEndAsync()
        $digest = Read-ExactStreamSha256 $process.StandardOutput.BaseStream $sourceBytes $deadline.Token
        $null = $process.WaitForExitAsync($deadline.Token).GetAwaiter().GetResult()
        $errorText = $stderr.WaitAsync($deadline.Token).GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0 -or ![string]::IsNullOrWhiteSpace($errorText)) {
            throw "Binary source read failed (exit $($process.ExitCode)): $($errorText.Trim())"
        }
        [IO.File]::AppendAllText($commandLog, "$($arguments -join ' ')`nBinary stream bytes=$sourceBytes sha256=$digest exit=0`n", [Text.UTF8Encoding]::new($false))
        $digest
    } catch {
        [IO.File]::AppendAllText($commandLog, "$($arguments -join ' ')`nBinary stream failed: $($_.Exception.Message)`n", [Text.UTF8Encoding]::new($false))
        throw
    } finally {
        if ($null -ne $process) {
            if (!$process.HasExited) { $process.Kill() }
            $process.StandardOutput.Dispose()
            $process.StandardError.Dispose()
            $process.Dispose()
        }
        $deadline.Dispose()
    }
}

function Read-ProbeEvidence {
    $answer = Invoke-Adb @('shell', 'run-as', $hostPackage, 'cat', "$privateDirectory/evidence.json") -AllowFailure
    if ($answer.ExitCode -ne 0 -or !$answer.Text.StartsWith('{')) { return $null }
    $answer.Text | ConvertFrom-Json -AsHashtable
}

function Read-EnableJournal {
    $answer = Invoke-Adb @('shell', 'run-as', $hostPackage, 'cat', "$privateDirectory/enable-restore.json") -AllowFailure
    if ($answer.ExitCode -ne 0 -or !$answer.Text.StartsWith('{')) { return $null }
    $answer.Text | ConvertFrom-Json -AsHashtable
}

function Get-InstalledForUser([string]$UserId) {
    ConvertFrom-UserPackages (Invoke-Adb @('shell', 'pm', 'list', 'packages', '-u', '--user', $UserId)).Text
}

function Assert-OwnedFixtureAbsent {
    foreach ($user in $users) {
        if ($fixturePackage -cin (Get-InstalledForUser $user)) { throw "The fixture or retained data already exists for user $user; no package will be changed." }
    }
}

$sdk = (Invoke-Adb @('shell', 'getprop', 'ro.build.version.sdk')).Text
$model = (Invoke-Adb @('shell', 'getprop', 'ro.product.model')).Text
$abi = (Invoke-Adb @('shell', 'getprop', 'ro.product.cpu.abi')).Text
$currentUser = (Invoke-Adb @('shell', 'am', 'get-current-user')).Text
if ($sdk -cnotmatch '^\d+$' -or [int]$sdk -lt 24 -or $currentUser -cnotmatch '^\d+$') { throw 'Unsupported API or unreadable current user.' }
$users = @([regex]::Matches((Invoke-Adb @('shell', 'pm', 'list', 'users')).Text, 'UserInfo\{(\d+):') | ForEach-Object { $_.Groups[1].Value })
if ($users.Count -eq 0 -or $currentUser -cnotin $users) { throw 'Cannot enumerate all users for ownership checks.' }
Assert-OwnedFixtureAbsent
$pluginDump = (Invoke-Adb @('shell', 'dumpsys', 'package', $pluginPackage)).Text
if ($pluginDump -cnotmatch ('Package \[' + [regex]::Escape($pluginPackage) + '\]') -or
    $pluginDump -match '(?m)^\s*(?:pkgFlags|flags)=\[[^\r\n\]]*DEBUGGABLE') {
    throw 'The plugin must be installed as the signed non-debuggable release before this run.'
}
$hostDump = (Invoke-Adb @('shell', 'dumpsys', 'package', $hostPackage)).Text
if ($hostDump -notmatch '(?m)^\s*(?:pkgFlags|flags)=\[[^\r\n\]]*DEBUGGABLE') {
    throw 'The official debug host with the guarded process probe must be installed separately.'
}
$hostUidText = (Invoke-Adb @('shell', 'run-as', $hostPackage, 'id', '-u')).Text
if ($hostUidText -cnotmatch '^\d+$' -or [math]::Floor([long]$hostUidText / 100000) -ne [int]$currentUser) {
    throw 'The run-as host UID does not belong to the selected Android user.'
}
$hostUid = [int]$hostUidText
Copy-Item -LiteralPath $metadataPath -Destination (Join-Path $runFolder 'fixture-metadata.json')
[IO.File]::WriteAllText((Join-Path $runFolder 'device.json'), (@{
    serial = $Serial; model = $model; sdk = [int]$sdk; abi = $abi; currentUser = [int]$currentUser
    authorizer = $Authorizer; caseId = $caseId; sourceSha256 = $sourceHash; sourceBytes = $sourceBytes
    hostUid = $hostUid; privateDirectory = $privateDirectory; remoteSource = $remoteSource
    measurement = 'inspect and first getUsers measured separately; install includes requested UI and preparation; first getUsers is cold only if independently verified'
} | ConvertTo-Json), [Text.UTF8Encoding]::new($false))

$started = $false
$prepareRequested = $false
$preparedJournal = $null
$enableRestored = $false
$privateCreated = $false
$fixtureAbsentAfter = $false
$probeSettled = $false
$sourcePreserved = $false
$passed = $false
$lastEvidence = $null
try {
    $exists = Invoke-Adb @('shell', 'run-as', $hostPackage, 'test', '-e', $privateDirectory) -AllowFailure
    if ($exists.ExitCode -eq 0) { throw 'The supposedly unique probe directory already exists.' }
    Invoke-Adb @('shell', 'run-as', $hostPackage, 'mkdir', '-p', $privateDirectory) | Out-Null
    $privateCreated = $true
    Invoke-Adb @('push', $fixturePath, $remoteSource) | Out-Null
    Invoke-Adb @('shell', 'run-as', $hostPackage, 'cp', $remoteSource, $privateSource) | Out-Null
    Invoke-Adb @('shell', 'run-as', $hostPackage, 'chmod', '400', $privateSource) | Out-Null
    Assert-OwnedFixtureAbsent
    $prepareRequested = $true
    $prepare = Invoke-Adb @('shell', 'am', 'start', '-W', '-f', '0x18000000', '-n', $probeComponent, '--es', 'caseId', $caseId, '--es', 'mode', 'prepare')
    if ($prepare.Text -match '(?m)^(Error|Exception)|SecurityException') { throw "The guarded host preparation did not start: $($prepare.Text)" }
    $prepareDeadline = [DateTime]::UtcNow.AddSeconds(30)
    while ([DateTime]::UtcNow -lt $prepareDeadline) {
        $journal = Read-EnableJournal
        if ($null -ne $journal -and $journal['phase'] -ceq 'prepared') {
            Assert-EnableJournal $journal 'prepared' $caseId $hostUid
            $preparedJournal = $journal
            [IO.File]::WriteAllText((Join-Path $runFolder 'enable-prepared.json'), ($journal | ConvertTo-Json -Depth 30), [Text.UTF8Encoding]::new($false))
            break
        }
        Start-Sleep -Milliseconds 200
    }
    if ($null -eq $preparedJournal) { throw 'The fixed plugin enable key was not safely journaled and prepared.' }
    $started = $true
    $launch = Invoke-Adb @('shell', 'am', 'start', '-W', '-f', '0x18000000', '-n', $probeComponent, '--es', 'caseId', $caseId, '--es', 'mode', 'performance', '--es', 'authorizer', $Authorizer)
    if ($launch.Text -match '(?m)^(Error|Exception)|SecurityException') { throw "The guarded host probe did not start: $($launch.Text)" }
    if ($Authorizer -eq 'none') { Write-Host 'Waiting for the fixed 3-Setup Performance Fixture confirmation. Install timing includes this operator wait.' }
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        $lastEvidence = Read-ProbeEvidence
        if ($null -ne $lastEvidence -and $lastEvidence['completed'] -eq $true) { break }
        Start-Sleep -Milliseconds 250
    }
    if ($null -eq $lastEvidence) { throw 'The official host produced no performance evidence.' }
    [IO.File]::WriteAllText((Join-Path $runFolder 'probe-evidence.json'), ($lastEvidence | ConvertTo-Json -Depth 30), [Text.UTF8Encoding]::new($false))
    Assert-PerformanceResult $lastEvidence $sourceHash $Authorizer $sourceBytes $caseId $hostUid
    $hashResult = Get-RemoteSourceSha256
    $sourcePreserved = $hashResult -ceq $sourceHash
    if (!$sourcePreserved) { throw 'The supplied source did not survive installation unchanged.' }
    if ($fixturePackage -cnotin (Get-InstalledForUser $currentUser)) { throw 'A success callback has no installed-package confirmation.' }
    Invoke-Adb @('shell', 'dumpsys', 'package', $fixturePackage) | ForEach-Object {
        [IO.File]::WriteAllText((Join-Path $runFolder 'installed-package.txt'), $_.Text, [Text.UTF8Encoding]::new($false))
    }
    # This sample is post-operation, not a privileged cold-start claim and not a clean idle PSS.
    Invoke-Adb @('shell', 'dumpsys', 'meminfo', $pluginPackage) | ForEach-Object {
        [IO.File]::WriteAllText((Join-Path $runFolder 'post-operation-meminfo.txt'), $_.Text, [Text.UTF8Encoding]::new($false))
    }
    $passed = $true
} finally {
    $cleanupErrors = [System.Collections.Generic.List[string]]::new()
    $privateRemoved = !$privateCreated
    $remoteRemoved = $false
    # Every cleanup step is recorded even if an earlier step fails. A stopped callback alone is
    # not a remote-worker barrier: failed/unconfirmed probes keep their private source for recovery.
    try {
        if ($started) {
            Invoke-Adb @('shell', 'am', 'start', '-W', '-f', '0x18000000', '-n', $probeComponent, '--es', 'caseId', $caseId, '--es', 'mode', 'cleanup') | Out-Null
            $settleDeadline = [DateTime]::UtcNow.AddSeconds(30)
            while ([DateTime]::UtcNow -lt $settleDeadline) {
                $cleanup = Read-ProbeEvidence
                if ($null -ne $cleanup -and $cleanup['phase'] -ceq 'cleaned') {
                    # A stale or unrelated cleaned marker must never authorize fixture removal.
                    Assert-PerformanceResult $cleanup $sourceHash $Authorizer $sourceBytes $caseId $hostUid
                    $probeSettled = $true
                    [IO.File]::WriteAllText((Join-Path $runFolder 'cleanup-evidence.json'), ($cleanup | ConvertTo-Json -Depth 30), [Text.UTF8Encoding]::new($false))
                    break
                }
                Start-Sleep -Milliseconds 200
            }
            if (!$probeSettled) { throw "The owned host session did not settle; its fixture and private source are preserved: $privateDirectory" }
        } else { $probeSettled = $true }
    } catch { $cleanupErrors.Add($_.Exception.Message) }
    if ($probeSettled) {
        try {
            if ($started -and @($users | Where-Object { $fixturePackage -cin (Get-InstalledForUser $_) }).Count -gt 0) {
                $removal = Invoke-Adb @('shell', 'pm', 'uninstall', $fixturePackage)
                if ($removal.Text -cne 'Success') { throw "Owned fixture uninstall failed: $($removal.Text)" }
            }
            Assert-OwnedFixtureAbsent
            $fixtureAbsentAfter = $true
        } catch { $cleanupErrors.Add($_.Exception.Message) }
    }
    # Restoring the one host preference is independent of operation cleanup. Even a pending
    # installation must not leave the user's plugin enabled; its source stays for recovery.
    try {
        if ($prepareRequested) {
            Invoke-Adb @('shell', 'am', 'start', '-W', '-f', '0x18000000', '-n', $probeComponent, '--es', 'caseId', $caseId, '--es', 'mode', 'restore') | Out-Null
            $restoreDeadline = [DateTime]::UtcNow.AddSeconds(30)
            while ([DateTime]::UtcNow -lt $restoreDeadline) {
                $journal = Read-EnableJournal
                if ($null -ne $journal -and $journal['phase'] -ceq 'restored') {
                    Assert-EnableJournal $journal 'restored' $caseId $hostUid $preparedJournal
                    $enableRestored = $true
                    [IO.File]::WriteAllText((Join-Path $runFolder 'enable-restored.json'), ($journal | ConvertTo-Json -Depth 30), [Text.UTF8Encoding]::new($false))
                    break
                }
                Start-Sleep -Milliseconds 200
            }
            if (!$enableRestored) { throw "The fixed plugin's original enable preference has not been restored; keep the journal for recovery: $privateDirectory/enable-restore.json" }
        } else { $enableRestored = $true }
    } catch { $cleanupErrors.Add($_.Exception.Message) }
    if ($probeSettled -and $enableRestored -and $privateCreated -and (!$started -or $fixtureAbsentAfter)) {
        try {
            # Exact owned files only; a changed layout remains for review instead of recursive removal.
            foreach ($name in @('fixture.apk', 'evidence.json', 'evidence.json.bak', 'evidence.json.new', 'error.json', 'error.json.bak', 'error.json.new',
                'enable-restore.json', 'enable-restore.json.bak', 'enable-restore.json.new')) {
                Invoke-Adb @('shell', 'run-as', $hostPackage, 'rm', '-f', "$privateDirectory/$name") | Out-Null
            }
            Invoke-Adb @('shell', 'run-as', $hostPackage, 'rmdir', $privateDirectory) | Out-Null
            $privateRemoved = $true
        } catch { $cleanupErrors.Add($_.Exception.Message) }
    }
    try {
        Invoke-Adb @('shell', 'rm', '-f', $remoteSource) | Out-Null
        $remoteRemoved = $true
    } catch { $cleanupErrors.Add($_.Exception.Message) }
    [IO.File]::WriteAllText((Join-Path $runFolder 'result.json'), (@{
        passed = $passed -and $probeSettled -and $fixtureAbsentAfter -and $sourcePreserved -and $enableRestored -and $privateRemoved -and $remoteRemoved -and $cleanupErrors.Count -eq 0
        probeSettled = $probeSettled; fixtureAbsentAfter = $fixtureAbsentAfter; sourcePreserved = $sourcePreserved
        runFolder = $runFolder; caseId = $caseId; authorizer = $Authorizer
        privateDirectory = $privateDirectory; privateRemoved = $privateRemoved; remoteRemoved = $remoteRemoved
        enableRestored = $enableRestored; prepareRequested = $prepareRequested
        cleanupErrors = $cleanupErrors.ToArray()
    } | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    if ($cleanupErrors.Count -gt 0) { throw "Performance cleanup requires recovery: $($cleanupErrors -join '; '). Evidence: $runFolder" }
}
Write-Host "PASS: official-host $Authorizer installation of the signed 100 MiB fixture; cleanup verified. Evidence: $runFolder"
