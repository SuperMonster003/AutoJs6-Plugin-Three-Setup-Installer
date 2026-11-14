param(
    [Parameter(Mandatory)][string]$Serial,
    [Parameter(Mandatory)][ValidateSet('shizuku', 'root')][string]$Authorizer
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$evidence = Join-Path $repo 'build/spike-evidence'
New-Item -ItemType Directory -Force $evidence | Out-Null
$sdkText = & adb -s $Serial shell getprop ro.build.version.sdk
if ($LASTEXITCODE -or $sdkText -notmatch '^\d+$') { throw 'Cannot read the device SDK' }
$sdk = [int]$sdkText
Write-Output $sdk
& adb -s $Serial shell getprop ro.product.cpu.abi
$versionName = (Get-Content -LiteralPath "$repo/version.properties" | ConvertFrom-StringData).VERSION_NAME
& adb -s $Serial install -r "$repo/app/build/outputs/apk/debug/autojs6-plugin-three-setup-installer-v$versionName.apk"
if ($LASTEXITCODE) { throw 'Plugin installation failed' }
& adb -s $Serial install -r "$repo/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
if ($LASTEXITCODE) { throw 'Test APK installation failed' }
$testClass = 'io.github.supermonster003.autojs6.plugin.three.setup.installer.PrivilegedInstallerDeviceTest'
$methods = @('installUpdateDowngradeUninstallAndRebind', 'rejectsInvalidStreamsAndAbandonsSessions',
    'preferredAndPersistentActivityPermissions')
if ($sdk -in 33..34) { $methods += 'defaultSourceAvoidsRestrictedSettingsAndExplicitSourcesArePreserved' }
$tests = ($methods | ForEach-Object { "$testClass#$_" }) -join ','
$runner = 'io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner'
$result = & adb -s $Serial shell am instrument -w -r -e privilegedAuthorizer $Authorizer -e class $tests $runner 2>&1
$result | Tee-Object -FilePath (Join-Path $evidence "$Serial-$Authorizer.txt")
if ($LASTEXITCODE -or !($result -match "^OK \($($methods.Count) tests\)") -or ($result -match 'INSTRUMENTATION_STATUS_CODE: -4')) {
    throw 'The spike failed or skipped a device check; inspect the evidence log'
}
