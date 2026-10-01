param(
    [string]$Sdk = $env:ANDROID_HOME,
    [ValidateRange(8, 3072)][int]$SizeMiB = 2048,
    [string]$BuildToolsVersion = '37.0.0',
    [string]$Platform = 'android-37.0',
    [switch]$KeepIntermediate
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$buildRoot = [IO.Path]::GetFullPath((Join-Path $repo 'build'))
$fixtureRoot = [IO.Path]::GetFullPath((Join-Path $buildRoot "large-install-fixture-$SizeMiB"))
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

# Independent test key. No release signing material is read or copied by this helper.
$key = Join-Path $fixtureRoot 'fixture.jks'
if (!(Test-Path -LiteralPath $key)) {
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android `
        -alias fixture -keyalg RSA -validity 36500 -dname 'CN=Installer Large Fixture Test Only' -noprompt
    if ($LASTEXITCODE) { throw 'Fixture key generation failed' }
}

$pythonPath = Join-Path $fixtureRoot 'package-fixture.py'
$python = @'
import json
import pathlib
import sys
import zipfile

# APKs remain below 2 GiB each. The outer classic ZIP uses unsigned 32-bit offsets, which are
# valid below 4 GiB; Python otherwise conservatively starts ZIP64 at its signed 2 GiB threshold.
zipfile.ZIP64_LIMIT = (1 << 32) - 1
mode = sys.argv[1]
if mode == "payload":
    destination = pathlib.Path(sys.argv[2])
    count = int(sys.argv[3]) * 1024 * 1024
    info = zipfile.ZipInfo("assets/installation-payload.bin", date_time=(2026, 10, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_STORED
    info.file_size = count
    chunk = bytes(1024 * 1024)
    with zipfile.ZipFile(destination, "a", allowZip64=False) as archive:
        with archive.open(info, "w", force_zip64=False) as target:
            for _ in range(count // len(chunk)):
                target.write(chunk)
elif mode == "container":
    root = pathlib.Path(sys.argv[2])
    output = root / "fixture.xapk"
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED, allowZip64=False) as archive:
        for name in ("base.apk", "feature.payload.apk"):
            archive.write(root / name, name)
        archive.writestr("manifest.json", json.dumps({
            "package_name": "io.github.supermonster003.autojs6.installer.large.fixture",
            "version_code": 1,
            "version_name": "1.0",
            "split_apks": [{"file": "base.apk", "id": "base"},
                           {"file": "feature.payload.apk", "id": "feature.payload"}],
        }, separators=(",", ":")))
else:
    raise SystemExit("Unknown fixture operation")
'@
[IO.File]::WriteAllText($pythonPath, $python, [Text.UTF8Encoding]::new($false))

$fixturePackage = 'io.github.supermonster003.autojs6.installer.large.fixture'
$baseMiB = [int][Math]::Floor($SizeMiB / 2.0)
$parts = @(
    @{ Name = 'base'; SizeMiB = $baseMiB; Split = '' },
    @{ Name = 'feature.payload'; SizeMiB = $SizeMiB - $baseMiB; Split = 'split="feature.payload" android:isFeatureSplit="true"' }
)
$metadataParts = @()
foreach ($part in $parts) {
    $manifest = Join-Path $fixtureRoot "$($part.Name)-AndroidManifest.xml"
    $unsigned = Join-Path $fixtureRoot "$($part.Name)-unsigned.apk"
    $aligned = Join-Path $fixtureRoot "$($part.Name)-aligned.apk"
    $signed = Join-Path $fixtureRoot "$($part.Name).apk"
    $xml = @"
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$fixturePackage" android:versionCode="1" android:versionName="1.0" $($part.Split)>
    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="35" />
    <application android:label="3-Setup Large Fixture" android:hasCode="false" android:allowBackup="false" />
</manifest>
"@
    [IO.File]::WriteAllText($manifest, $xml, [Text.UTF8Encoding]::new($false))
    & "$toolset/aapt2.exe" link -o $unsigned --manifest $manifest -I $androidJar
    if ($LASTEXITCODE) { throw "Fixture manifest linking failed: $($part.Name)" }
    & py -3 $pythonPath payload $unsigned $part.SizeMiB
    if ($LASTEXITCODE) { throw "Fixture payload writing failed: $($part.Name)" }
    & "$toolset/zipalign.exe" -f 4 $unsigned $aligned
    if ($LASTEXITCODE) { throw "Fixture alignment failed: $($part.Name)" }
    & "$toolset/apksigner.bat" sign --ks $key --ks-key-alias fixture --ks-pass pass:android --key-pass pass:android `
        --v4-signing-enabled false --out $signed $aligned
    if ($LASTEXITCODE) { throw "Fixture signing failed: $($part.Name)" }
    & "$toolset/apksigner.bat" verify --verbose $signed
    if ($LASTEXITCODE) { throw "Fixture signature verification failed: $($part.Name)" }
    $metadataParts += [ordered]@{
        name = "$($part.Name).apk"
        bytes = (Get-Item -LiteralPath $signed).Length
        sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $signed).Hash.ToLowerInvariant()
    }
    if (!$KeepIntermediate) {
        # These are explicit generated files below the checked fixtureRoot, never a recursive delete.
        Remove-Item -LiteralPath $unsigned, $aligned
    }
}

& py -3 $pythonPath container $fixtureRoot
if ($LASTEXITCODE) { throw 'Fixture container creation failed' }
$xapk = Join-Path $fixtureRoot 'fixture.xapk'
$totalApkBytes = 0L
foreach ($partMetadata in $metadataParts) {
    # OrderedDictionary entries are not adapted properties for Measure-Object on every PowerShell.
    $totalApkBytes += [long]$partMetadata['bytes']
}
$metadata = [ordered]@{
    packageName = $fixturePackage
    versionCode = 1
    payloadBytes = [long]$SizeMiB * 1024 * 1024
    sourceBytes = (Get-Item -LiteralPath $xapk).Length
    apkBytes = $totalApkBytes
    sourceSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $xapk).Hash.ToLowerInvariant()
    parts = $metadataParts
    codeFree = $true
    permissions = @()
    components = @()
}
$metadataPath = Join-Path $fixtureRoot 'fixture-metadata.json'
[IO.File]::WriteAllText($metadataPath, ($metadata | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
if (!$KeepIntermediate) {
    Remove-Item -LiteralPath (Join-Path $fixtureRoot 'base.apk'), (Join-Path $fixtureRoot 'feature.payload.apk')
}
[PSCustomObject]@{ Fixture = $xapk; Metadata = $metadataPath; PayloadBytes = $metadata.payloadBytes; SourceBytes = $metadata.sourceBytes; ApkBytes = $metadata.apkBytes }
