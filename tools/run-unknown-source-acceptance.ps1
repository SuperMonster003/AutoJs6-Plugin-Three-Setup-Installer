#Requires -Version 7.2
[CmdletBinding(DefaultParameterSetName = 'Run')]
param(
    [Parameter(Mandatory, ParameterSetName = 'Run')]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$')]
    [string]$Serial,
    [Parameter(ParameterSetName = 'Run')]
    [switch]$AllowPlayProtectScan,
    [Parameter(Mandatory, ParameterSetName = 'SelfTest')]
    [switch]$SelfTest
)

# Run only against a device explicitly selected by the operator. This script does not install APKs,
# alter global scan settings, or approve unknown OEM dialogs. The instrumentation owns its one
# code-free fixture; this driver owns only this plugin's REQUEST_INSTALL_PACKAGES app-op modes.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$pluginPackage = 'io.github.supermonster003.autojs6.plugin.three.setup.installer'
$fixturePackage = 'io.github.supermonster003.autojs6.installer.spike.fixture'
$runner = "$pluginPackage.test/androidx.test.runner.AndroidJUnitRunner"
$testClass = "$pluginPackage.UnknownSourcePermissionDeviceTest"
$testMethod = 'grantingUnknownSourcePermissionInSettingsContinuesTheSameNoneInstallation'
$operation = 'REQUEST_INSTALL_PACKAGES'

function ConvertFrom-AppOps {
    param([Parameter(Mandatory)][AllowEmptyString()][string]$Text)
    $packageMode = $null
    $uidMode = $null
    $noOperations = $false
    $pattern = [regex]::new('^(?<uid>Uid mode: )?REQUEST_INSTALL_PACKAGES: (?<mode>allow|ignore|deny|default|foreground)(?:[; ].*)?$')
    $lines = @($Text -split '\r?\n' | ForEach-Object { $_.Trim() } | Where-Object { $_.Length -gt 0 })
    if ($lines.Count -eq 0) { throw 'The app-op query returned no readable state.' }
    foreach ($line in $lines) {
        if ($line -ceq 'No operations.') {
            if ($noOperations) { throw 'Duplicate app-op absence marker.' }
            $noOperations = $true
            continue
        }
        # AOSP can append this informational line when no per-package record exists.
        if ($line -ceq 'Default mode: default' -and $noOperations) { continue }
        $match = $pattern.Match($line)
        if (!$match.Success) { throw 'Unrecognized app-op query output; no permission change is safe.' }
        if ($match.Groups['uid'].Success) {
            if ($null -ne $uidMode) { throw 'Ambiguous UID app-op modes.' }
            $uidMode = $match.Groups['mode'].Value
        } else {
            if ($null -ne $packageMode) { throw 'Ambiguous package app-op modes.' }
            $packageMode = $match.Groups['mode'].Value
        }
    }
    if ($noOperations -and $null -ne $packageMode) { throw 'Conflicting package app-op records.' }
    [pscustomobject]@{
        PackageMode = $(if ($null -eq $packageMode) { 'default' } else { $packageMode })
        UidMode = $(if ($null -eq $uidMode) { 'default' } else { $uidMode })
    }
}

function ConvertFrom-PackageUids {
    param([Parameter(Mandatory)][string]$Text)
    $result = [System.Collections.Generic.List[object]]::new()
    $seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in ($Text -split '\r?\n' | Where-Object { $_.Trim().Length -gt 0 })) {
        $match = [regex]::Match($line.Trim(), '^package:(?<package>[A-Za-z0-9_.]+) uid:(?<uid>[0-9]+)$')
        if (!$match.Success) { throw 'Cannot safely parse the current user package-to-UID mapping.' }
        $name = $match.Groups['package'].Value
        if (!$seen.Add($name)) { throw 'Duplicate package in the UID mapping.' }
        $result.Add([pscustomobject]@{ Package = $name; Uid = [int]::Parse($match.Groups['uid'].Value) })
    }
    if ($result.Count -eq 0) { throw 'The package-to-UID mapping is empty.' }
    $result.ToArray()
}

function Assert-SingleTargetUid {
    param([Parameter(Mandatory)][object[]]$Packages, [int]$UserId, [int]$ExpectedUid = -1)
    $target = @($Packages | Where-Object { $_.Package -ceq $pluginPackage })
    if ($target.Count -ne 1) { throw 'The fixed plugin is not installed exactly once for the selected user.' }
    $uid = $target[0].Uid
    if ([math]::Floor($uid / 100000) -ne $UserId) { throw 'The plugin UID does not belong to the selected Android user.' }
    if (@($Packages | Where-Object { $_.Uid -eq $uid }).Count -ne 1) { throw 'Refusing to change an app-op for a shared UID.' }
    if ($ExpectedUid -ge 0 -and $uid -ne $ExpectedUid) { throw 'The plugin UID changed during this run; refusing to modify the replacement.' }
    $uid
}

function Test-InstrumentationResult {
    param([Parameter(Mandatory)][AllowEmptyString()][string]$Text, [int]$ExitCode)
    $reasons = [System.Collections.Generic.List[string]]::new()
    if ($ExitCode -ne 0) { $reasons.Add("adb exited with code $ExitCode") }
    $events = [System.Collections.Generic.List[object]]::new()
    $status = @{}
    $finalCodes = [System.Collections.Generic.List[int]]::new()
    foreach ($line in ($Text -split '\r?\n')) {
        $field = [regex]::Match($line, '^INSTRUMENTATION_STATUS: (?<key>[A-Za-z][A-Za-z0-9_-]*)=(?<value>.*)$')
        if ($field.Success) { $status[$field.Groups['key'].Value] = $field.Groups['value'].Value; continue }
        $code = [regex]::Match($line, '^INSTRUMENTATION_STATUS_CODE: (?<code>-?[0-9]+)$')
        if ($code.Success) {
            $value = [int]::Parse($code.Groups['code'].Value)
            if ($value -lt 0) { $reasons.Add("test failed or skipped with status $value") }
            if ($status.ContainsKey('class') -or $status.ContainsKey('test')) {
                $events.Add([pscustomobject]@{ Fields = $status; Code = $value })
            }
            $status = @{}
            continue
        }
        $final = [regex]::Match($line, '^INSTRUMENTATION_CODE: (?<code>-?[0-9]+)$')
        if ($final.Success) { $finalCodes.Add([int]::Parse($final.Groups['code'].Value)) }
    }
    if ($finalCodes.Count -ne 1 -or $finalCodes[0] -ne -1) { $reasons.Add('runner did not finish normally with INSTRUMENTATION_CODE: -1') }
    $starts = 0
    $successes = 0
    foreach ($event in $events) {
        $fields = $event.Fields
        if (!$fields.ContainsKey('class') -or !$fields.ContainsKey('test') -or
            $fields['class'] -cne $testClass -or $fields['test'] -cne $testMethod -or
            !$fields.ContainsKey('numtests') -or $fields['numtests'] -cne '1') {
            $reasons.Add('runner reported an unexpected test identity or test count')
            continue
        }
        if ($event.Code -eq 1) { $starts++ }
        elseif ($event.Code -eq 0) { $successes++ }
    }
    if ($starts -ne 1 -or $successes -ne 1) { $reasons.Add('exactly one named test must start and pass') }
    if ([regex]::Matches($Text, '(?m)^OK \(1 tests?\)\r?$').Count -ne 1) { $reasons.Add('JUnit did not report one successful test') }
    if ($Text -match '(?m)^INSTRUMENTATION_(?:FAILED|RESULT: shortMsg=)' -or $Text -match '(?m)^FAILURES!!!') {
        $reasons.Add('runner reported a failure or process crash')
    }
    if ([regex]::Matches($Text, '(?m)^INSTRUMENTATION_STATUS: unknown-source-grant=SUCCESS platformSession=[0-9]+ created=1 systemConfirmationStarts=1 sourcePreserved=true authorizer=none\r?$').Count -ne 1) {
        $reasons.Add('the original none session has no unique authoritative success evidence')
    }
    if ([regex]::Matches($Text, '(?m)^INSTRUMENTATION_STATUS: unknown-source-grant=CLEANUP fixtureAbsent=true ownedSessionSettled=true permissionRestore=driver\r?$').Count -ne 1) {
        $reasons.Add('owned fixture and session cleanup did not finish before permission restoration')
    }
    [pscustomobject]@{ Passed = $reasons.Count -eq 0; Reasons = $reasons.ToArray() }
}

if ($SelfTest) {
    function Expect-Rejected([scriptblock]$Action) {
        $rejected = $false
        try { & $Action | Out-Null } catch { $rejected = $true }
        if (!$rejected) { throw 'A parser accepted an unsafe or ambiguous fixture.' }
    }
    $empty = ConvertFrom-AppOps "No operations.`nDefault mode: default"
    if ($empty.PackageMode -cne 'default' -or $empty.UidMode -cne 'default') { throw 'Absent modes did not resolve to default.' }
    $modes = ConvertFrom-AppOps "Uid mode: REQUEST_INSTALL_PACKAGES: ignore`nREQUEST_INSTALL_PACKAGES: allow; time=+2m ago"
    if ($modes.PackageMode -cne 'allow' -or $modes.UidMode -cne 'ignore') { throw 'The two mode layers were not preserved.' }
    $uidOnly = ConvertFrom-AppOps "Uid mode: REQUEST_INSTALL_PACKAGES: deny`nNo operations."
    if ($uidOnly.PackageMode -cne 'default' -or $uidOnly.UidMode -cne 'deny') { throw 'UID-only state was not preserved.' }
    foreach ($invalid in @('', 'Error: package not found', 'REQUEST_INSTALL_PACKAGES: mystery',
        "REQUEST_INSTALL_PACKAGES: allow`nREQUEST_INSTALL_PACKAGES: deny",
        "No operations.`nREQUEST_INSTALL_PACKAGES: allow", "REQUEST_INSTALL_PACKAGES: allow`nSecurityException")) {
        Expect-Rejected { ConvertFrom-AppOps $invalid }
    }
    $packages = @(ConvertFrom-PackageUids "package:android uid:1000`npackage:$pluginPackage uid:10123")
    if ((Assert-SingleTargetUid -Packages $packages -UserId 0) -ne 10123) { throw 'The plugin UID was not selected.' }
    Expect-Rejected { Assert-SingleTargetUid -Packages $packages -UserId 1 }
    Expect-Rejected { Assert-SingleTargetUid -Packages $packages -UserId 0 -ExpectedUid 10456 }
    $shared = @(ConvertFrom-PackageUids "package:$pluginPackage uid:10123`npackage:other.package uid:10123")
    Expect-Rejected { Assert-SingleTargetUid -Packages $shared -UserId 0 }
    Expect-Rejected { ConvertFrom-PackageUids 'package:example uid:not-a-number' }
    $good = @"
INSTRUMENTATION_STATUS: class=$testClass
INSTRUMENTATION_STATUS: test=$testMethod
INSTRUMENTATION_STATUS: numtests=1
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: unknown-source-grant=SUCCESS platformSession=123 created=1 systemConfirmationStarts=1 sourcePreserved=true authorizer=none
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: unknown-source-grant=CLEANUP fixtureAbsent=true ownedSessionSettled=true permissionRestore=driver
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: class=$testClass
INSTRUMENTATION_STATUS: test=$testMethod
INSTRUMENTATION_STATUS: numtests=1
INSTRUMENTATION_STATUS_CODE: 0
OK (1 test)
INSTRUMENTATION_CODE: -1
"@
    if (!(Test-InstrumentationResult -Text $good -ExitCode 0).Passed) { throw 'A complete passing result was rejected.' }
    foreach ($bad in @($good.Replace('INSTRUMENTATION_CODE: -1', 'INSTRUMENTATION_CODE: 0'),
        $good.Replace('numtests=1', 'numtests=2'), $good.Replace("test=$testMethod", 'test=otherTest'),
        $good.Replace('permissionRestore=driver', 'permissionRestore=instrumentation'),
        $good.Replace('authorizer=none', 'authorizer=root'),
        "$good`nINSTRUMENTATION_STATUS_CODE: -4", "$good`nINSTRUMENTATION_STATUS_CODE: -3",
        "$good`nINSTRUMENTATION_RESULT: shortMsg=Process crashed.", $good.Replace('OK (1 test)', 'OK (2 tests)'))) {
        if ((Test-InstrumentationResult -Text $bad -ExitCode 0).Passed) { throw 'A skipped, crashed, ambiguous or incomplete result was accepted.' }
    }
    if ((Test-InstrumentationResult -Text $good -ExitCode 1).Passed) { throw 'An adb failure was accepted.' }
    Write-Host 'PASS: app-op parsing, unique UID guard, and strict one-test acceptance parsing. No adb command was executed.'
    return
}

$adbExecutable = (Get-Command adb -CommandType Application -ErrorAction Stop).Source
$repoRoot = Split-Path $PSScriptRoot -Parent
$runId = [guid]::NewGuid().ToString('N')
$runFolder = Join-Path $repoRoot ("build/unknown-source-acceptance/{0}-{1}" -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $runId)
New-Item -ItemType Directory -Path $runFolder -Force | Out-Null
$planPath = Join-Path $runFolder 'restore-plan.json'
$instrumentationLog = Join-Path $runFolder 'instrumentation.log'
Write-Host "Evidence directory: $runFolder"

function Invoke-AdbProcess {
    param([Parameter(Mandatory)][string[]]$Arguments, [int]$TimeoutSeconds = 30, [string]$LogPath = '')
    $start = [System.Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $adbExecutable
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.ArgumentList.Add('-s')
    $start.ArgumentList.Add($Serial)
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $start
    $stdout = $null
    $stderr = $null
    $output = ''
    $errors = ''
    $exitCode = -1
    $processStarted = $false
    try {
        if (!$process.Start()) { throw 'Could not start adb.' }
        $processStarted = $true
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
        $nextProgress = [DateTime]::UtcNow.AddSeconds(15)
        while (!$process.WaitForExit(1000)) {
            if ([DateTime]::UtcNow -ge $deadline) { throw "adb exceeded its $TimeoutSeconds second deadline." }
            if ($LogPath.Length -gt 0 -and [DateTime]::UtcNow -ge $nextProgress) {
                Write-Host 'The fixture test is still running; the saved app-op restoration plan remains active.'
                $nextProgress = [DateTime]::UtcNow.AddSeconds(15)
            }
        }
        $output = $stdout.GetAwaiter().GetResult()
        $errors = $stderr.GetAwaiter().GetResult()
        $exitCode = $process.ExitCode
    } finally {
        if ($processStarted -and !$process.HasExited) {
            $process.Kill($true)
            [void]$process.WaitForExit(5000)
        }
        if ($null -ne $stdout -and $stdout.IsCompleted) { $output = $stdout.GetAwaiter().GetResult() }
        if ($null -ne $stderr -and $stderr.IsCompleted) { $errors = $stderr.GetAwaiter().GetResult() }
        if ($LogPath.Length -gt 0) { [IO.File]::WriteAllText($LogPath, "$output`n$errors", [Text.UTF8Encoding]::new($false)) }
        $process.Dispose()
    }
    [pscustomobject]@{ ExitCode = $exitCode; Output = $output; Error = $errors; Text = "$output`n$errors" }
}

function Invoke-AdbText {
    param([Parameter(Mandatory)][string[]]$Arguments)
    $reply = Invoke-AdbProcess -Arguments $Arguments
    if ($reply.ExitCode -ne 0 -or $reply.Error.Trim().Length -ne 0) { throw "adb command failed (exit $($reply.ExitCode)): $($reply.Error.Trim())" }
    $reply.Output.Trim()
}

function Read-UserId {
    $value = Invoke-AdbText -Arguments @('shell', 'am', 'get-current-user')
    if ($value -cnotmatch '^[0-9]+$') { throw 'Cannot safely determine the current Android user.' }
    [int]::Parse($value)
}

function Read-TargetUid {
    param([int]$UserId, [int]$ExpectedUid = -1)
    $text = Invoke-AdbText -Arguments @('shell', 'cmd', 'package', 'list', 'packages', '-U', '--user', "$UserId")
    $packages = @(ConvertFrom-PackageUids $text)
    Assert-SingleTargetUid -Packages $packages -UserId $UserId -ExpectedUid $ExpectedUid
}

function Assert-FixtureAbsent {
    $userText = Invoke-AdbText -Arguments @('shell', 'pm', 'list', 'users')
    $users = @([regex]::Matches($userText, 'UserInfo\{(?<id>[0-9]+):') | ForEach-Object { [int]::Parse($_.Groups['id'].Value) } | Sort-Object -Unique)
    if ($users.Count -eq 0 -or $userText -match '(?i)error|exception') { throw 'Cannot enumerate users for the fixture ownership guard.' }
    foreach ($id in $users) {
        $text = Invoke-AdbText -Arguments @('shell', 'pm', 'list', 'packages', '-u', '--user', "$id")
        $lines = @($text -split '\r?\n' | Where-Object { $_.Trim().Length -gt 0 })
        if (!($lines -ccontains 'package:android') -or @($lines | Where-Object { $_ -cnotmatch '^package:[A-Za-z0-9_.]+$' }).Count -ne 0) {
            throw "Cannot safely enumerate packages for user $id."
        }
        if ($lines -ccontains "package:$fixturePackage") { throw "The fixed fixture or retained data exists for user $id; ownership cleanup is not proven." }
    }
}

function Set-PluginMode {
    param([bool]$Uid, [ValidateSet('allow', 'ignore', 'deny', 'default', 'foreground')][string]$Mode)
    [void](Read-TargetUid -UserId $targetUser -ExpectedUid $targetUid)
    $arguments = @('shell', 'cmd', 'appops', 'set', '--user', "$targetUser")
    if ($Uid) { $arguments += '--uid' }
    $arguments += @($pluginPackage, $operation, $Mode)
    $reply = Invoke-AdbText -Arguments $arguments
    if ($reply.Length -ne 0) { throw "The app-op mutation returned an unexpected response: $reply" }
}

function Read-PluginModes {
    param([string]$SavePath = '')
    $text = Invoke-AdbText -Arguments @('shell', 'cmd', 'appops', 'get', '--user', "$targetUser", $pluginPackage, $operation)
    if ($SavePath.Length -gt 0) { [IO.File]::WriteAllText($SavePath, $text, [Text.UTF8Encoding]::new($false)) }
    ConvertFrom-AppOps $text
}

$targetUser = -1
$targetUid = -1
$original = $null
$restoreRequired = $false
$instrumentationStarted = $false
$instrumentationExited = $false
$testPassed = $false
$restored = $false
$fixtureClean = $false
$primaryFailure = $null
$cleanupFailures = [System.Collections.Generic.List[string]]::new()
try {
    if ((Invoke-AdbText -Arguments @('get-state')) -cne 'device') { throw 'The explicit adb target is not online.' }
    $sdkText = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.version.sdk')
    if ($sdkText -cnotmatch '^[0-9]+$' -or [int]::Parse($sdkText) -lt 26) { throw 'The grant acceptance requires Android API 26 or newer.' }
    $targetUser = Read-UserId
    $targetUid = Read-TargetUid -UserId $targetUser
    $registered = Invoke-AdbText -Arguments @('shell', 'pm', 'list', 'instrumentation')
    $registration = "instrumentation:$runner (target=$pluginPackage)"
    if (@($registered -split '\r?\n' | Where-Object { $_.Trim() -ceq $registration }).Count -ne 1) {
        throw 'Install the matching plugin and test APKs before running this driver.'
    }
    Assert-FixtureAbsent
    $original = Read-PluginModes -SavePath (Join-Path $runFolder 'appops-before.txt')
    $plan = [ordered]@{
        Format = 1; RunId = $runId; SavedAtUtc = [DateTime]::UtcNow.ToString('o')
        Serial = $Serial; Api = [int]::Parse($sdkText); UserId = $targetUser; Uid = $targetUid
        Package = $pluginPackage; Operation = $operation; Original = $original
        FixturePackage = $fixturePackage; FixtureAbsentBefore = $true
        AllowPlayProtectScan = [bool]$AllowPlayProtectScan
        Test = "$testClass#$testMethod"
        RestoreArguments = @(
            @('-s', $Serial, 'shell', 'cmd', 'appops', 'set', '--user', "$targetUser", $pluginPackage, $operation, $original.PackageMode),
            @('-s', $Serial, 'shell', 'cmd', 'appops', 'set', '--user', "$targetUser", '--uid', $pluginPackage, $operation, $original.UidMode)
        )
    }
    [IO.File]::WriteAllText($planPath, ($plan | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
    Write-Host "Saved restoration plan: $planPath"
    # Set this before the first mutation so a timeout or partial write still enters restoration.
    $restoreRequired = $true
    Set-PluginMode -Uid $true -Mode 'default'
    Set-PluginMode -Uid $false -Mode 'deny'
    $denied = Read-PluginModes -SavePath (Join-Path $runFolder 'appops-precondition.txt')
    if ($denied.PackageMode -cne 'deny' -or $denied.UidMode -cne 'default') { throw 'The external denied precondition did not take effect exactly.' }
    if ((Read-UserId) -ne $targetUser) { throw 'The active Android user changed before the UI test.' }
    $arguments = @('shell', 'am', 'instrument', '--user', "$targetUser", '-w', '-r',
        '-e', 'class', "$testClass#$testMethod", '-e', 'unknownSourceGrant', 'true',
        '-e', 'unknownSourcePermissionRestore', 'driver')
    if ($AllowPlayProtectScan) { $arguments += @('-e', 'allowPlayProtectScan', 'true') }
    $arguments += $runner
    $instrumentationStarted = $true
    $reply = Invoke-AdbProcess -Arguments $arguments -TimeoutSeconds 260 -LogPath $instrumentationLog
    $instrumentationExited = $true
    $assessment = Test-InstrumentationResult -Text $reply.Text -ExitCode $reply.ExitCode
    $testPassed = $assessment.Passed
    if (!$testPassed) { throw ($assessment.Reasons -join '; ') }
} catch {
    $primaryFailure = $_.Exception.Message
    Write-Warning "Acceptance did not pass: $primaryFailure"
} finally {
    if ($restoreRequired) {
        # A killed adb client does not guarantee the remote instrumentation stopped. Stop only
        # this fixed target before restoring, so a delayed Settings action cannot re-grant it.
        if ($instrumentationStarted -and !$instrumentationExited) {
            try {
                [void](Read-TargetUid -UserId $targetUser -ExpectedUid $targetUid)
                [void](Invoke-AdbText -Arguments @('shell', 'am', 'force-stop', '--user', "$targetUser", $pluginPackage))
            } catch { $cleanupFailures.Add("stop unfinished instrumentation: $($_.Exception.Message)") }
        }
        try { Set-PluginMode -Uid $false -Mode $original.PackageMode }
        catch { $cleanupFailures.Add("restore package mode: $($_.Exception.Message)") }
        try { Set-PluginMode -Uid $true -Mode $original.UidMode }
        catch { $cleanupFailures.Add("restore UID mode: $($_.Exception.Message)") }
        try {
            [void](Read-TargetUid -UserId $targetUser -ExpectedUid $targetUid)
            $after = Read-PluginModes -SavePath (Join-Path $runFolder 'appops-after.txt')
            if ($after.PackageMode -cne $original.PackageMode -or $after.UidMode -cne $original.UidMode) {
                throw 'The final package and UID modes differ from the saved originals.'
            }
            $restored = $true
        } catch { $cleanupFailures.Add("verify restored app-ops: $($_.Exception.Message)") }
        try { Assert-FixtureAbsent; $fixtureClean = $true }
        catch { $cleanupFailures.Add("verify fixture cleanup: $($_.Exception.Message)") }
    }
    $accepted = $testPassed -and $restored -and $fixtureClean -and $cleanupFailures.Count -eq 0 -and $null -eq $primaryFailure
    $report = [ordered]@{
        Passed = $accepted; TestPassed = $testPassed; AppOpsRestored = $restored; FixtureAbsentAfter = $fixtureClean
        Serial = $Serial; UserId = $targetUser; Uid = $targetUid; Plan = $planPath; InstrumentationLog = $instrumentationLog
        Failure = $primaryFailure; CleanupFailures = $cleanupFailures.ToArray(); CompletedAtUtc = [DateTime]::UtcNow.ToString('o')
    }
    [IO.File]::WriteAllText((Join-Path $runFolder 'result.json'), ($report | ConvertTo-Json -Depth 6), [Text.UTF8Encoding]::new($false))
}
if (!$accepted) {
    $detail = @($primaryFailure) + $cleanupFailures.ToArray() | Where-Object { ![string]::IsNullOrEmpty($_) }
    throw "Unknown-source acceptance failed. $($detail -join '; ') Evidence: $runFolder"
}
Write-Host "PASS: the single grant test succeeded without skips; fixture cleanup and both original app-op modes were verified. Evidence: $runFolder"
