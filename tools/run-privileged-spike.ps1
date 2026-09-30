param(
    [Parameter(Mandatory)][string]$Serial,
    [Parameter(Mandatory)][ValidateSet('shizuku', 'root')][string]$Authorizer
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$evidence = Join-Path $repo 'build/spike-evidence'
New-Item -ItemType Directory -Force $evidence | Out-Null
& adb -s $Serial shell getprop ro.build.version.sdk
& adb -s $Serial shell getprop ro.product.cpu.abi
& adb -s $Serial install -r "$repo/app/build/outputs/apk/debug/autojs6-plugin-three-setup-installer-v1.0.0.apk"
if ($LASTEXITCODE) { throw 'Plugin installation failed' }
& adb -s $Serial install -r "$repo/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
if ($LASTEXITCODE) { throw 'Test APK installation failed' }
$testClass = 'io.github.supermonster003.autojs6.plugin.three.setup.installer.PrivilegedInstallerDeviceTest'
$runner = 'io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner'
$result = & adb -s $Serial shell am instrument -w -r -e privilegedAuthorizer $Authorizer -e class $testClass $runner 2>&1
$result | Tee-Object -FilePath (Join-Path $evidence "$Serial-$Authorizer.txt")
if ($LASTEXITCODE -or !($result -match '^OK \(3 tests\)') -or ($result -match 'INSTRUMENTATION_STATUS_CODE: -4')) {
    throw 'The spike failed or skipped a device check; inspect the evidence log'
}
