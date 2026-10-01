param(
    [string]$Sdk = $env:ANDROID_HOME,
    [string]$BuildToolsVersion = '37.0.0',
    [string]$Platform = 'android-37.0',
    [switch]$KeepIntermediate
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$buildRoot = [IO.Path]::GetFullPath((Join-Path $repo 'build'))
$fixtureRoot = [IO.Path]::GetFullPath((Join-Path $buildRoot 'performance-install-fixture-100'))
$expectedPrefix = $buildRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
if (!$fixtureRoot.StartsWith($expectedPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Fixture output must stay inside the repository build directory'
}
if ([string]::IsNullOrWhiteSpace($Sdk)) { throw 'Set ANDROID_HOME or pass -Sdk' }
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { throw 'Set JAVA_HOME to the configured JDK' }
$toolset = Join-Path $Sdk "build-tools/$BuildToolsVersion"
$androidJar = Join-Path $Sdk "platforms/$Platform/android.jar"
foreach ($tool in @('aapt2.exe', 'zipalign.exe', 'apksigner.bat')) {
    if (!(Test-Path -LiteralPath (Join-Path $toolset $tool))) { throw "Missing Android build tool: $tool" }
}
if (!(Test-Path -LiteralPath $androidJar)) { throw 'The selected Android platform is not installed' }
Get-Command py -ErrorAction Stop | Out-Null
New-Item -ItemType Directory -Force -Path $fixtureRoot | Out-Null

# A separate fixture key; the plugin's production signing files are never read.
$key = Join-Path $fixtureRoot 'fixture.jks'
if (!(Test-Path -LiteralPath $key)) {
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android `
        -alias fixture -keyalg RSA -validity 36500 -dname 'CN=Installer Performance Fixture Test Only' -noprompt
    if ($LASTEXITCODE) { throw 'Fixture key generation failed' }
}

$fixturePackage = 'io.github.supermonster003.autojs6.installer.performance.fixture'
$manifest = Join-Path $fixtureRoot 'AndroidManifest.xml'
$unsigned = Join-Path $fixtureRoot 'fixture-unsigned.apk'
$aligned = Join-Path $fixtureRoot 'fixture-aligned.apk'
$signed = Join-Path $fixtureRoot 'fixture.apk'
$xml = @"
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$fixturePackage" android:versionCode="1" android:versionName="1.0">
    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="35" />
    <application android:label="3-Setup Performance Fixture" android:hasCode="false" android:allowBackup="false" />
</manifest>
"@
[IO.File]::WriteAllText($manifest, $xml, [Text.UTF8Encoding]::new($false))
& "$toolset/aapt2.exe" link -o $unsigned --manifest $manifest -I $androidJar
if ($LASTEXITCODE) { throw 'Fixture manifest linking failed' }

$pythonPath = Join-Path $fixtureRoot 'package-fixture.py'
$python = @'
import pathlib
import sys
import zipfile

target = pathlib.Path(sys.argv[1])
payload_bytes = 100 * 1024 * 1024
entry = zipfile.ZipInfo("assets/installation-payload.bin", date_time=(2026, 10, 1, 0, 0, 0))
entry.compress_type = zipfile.ZIP_STORED
entry.file_size = payload_bytes
with zipfile.ZipFile(target, "a", allowZip64=False) as archive:
    with archive.open(entry, "w", force_zip64=False) as output:
        chunk = bytes(1024 * 1024)
        for _ in range(100):
            output.write(chunk)
with zipfile.ZipFile(target) as archive:
    payload = archive.getinfo(entry.filename)
    assert payload.file_size == payload_bytes
    assert payload.compress_size == payload_bytes
    assert payload.compress_type == zipfile.ZIP_STORED
    assert archive.testzip() is None
    assert not any(name.endswith(".dex") or name.startswith("lib/") for name in archive.namelist())
'@
[IO.File]::WriteAllText($pythonPath, $python, [Text.UTF8Encoding]::new($false))
& py -3 $pythonPath $unsigned
if ($LASTEXITCODE) { throw 'Fixture payload writing or verification failed' }
& "$toolset/zipalign.exe" -f 4 $unsigned $aligned
if ($LASTEXITCODE) { throw 'Fixture alignment failed' }
& "$toolset/apksigner.bat" sign --ks $key --ks-key-alias fixture --ks-pass pass:android --key-pass pass:android `
    --v4-signing-enabled false --out $signed $aligned
if ($LASTEXITCODE) { throw 'Fixture signing failed' }
& "$toolset/apksigner.bat" verify --verbose $signed
if ($LASTEXITCODE) { throw 'Fixture signature verification failed' }

$metadata = [ordered]@{
    packageName = $fixturePackage
    label = '3-Setup Performance Fixture'
    versionCode = 1
    targetSdk = 35
    payloadBytes = 100L * 1024 * 1024
    apkBytes = (Get-Item -LiteralPath $signed).Length
    sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $signed).Hash.ToLowerInvariant()
    singleApk = $true
    payloadCompression = 'stored'
    codeFree = $true
    permissions = @()
    components = @()
}
$metadataPath = Join-Path $fixtureRoot 'fixture-metadata.json'
[IO.File]::WriteAllText($metadataPath, ($metadata | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
if (!$KeepIntermediate) {
    # Only explicit files generated in the checked output directory, no recursive deletion.
    Remove-Item -LiteralPath $unsigned, $aligned
}
[PSCustomObject]@{ Fixture = $signed; Metadata = $metadataPath; PayloadBytes = $metadata.payloadBytes; ApkBytes = $metadata.apkBytes; Sha256 = $metadata.sha256 }
