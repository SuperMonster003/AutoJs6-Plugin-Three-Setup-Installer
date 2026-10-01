param([string]$Sdk = $env:ANDROID_HOME)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$fixtureRoot = Join-Path $repoRoot 'build/advanced-fixtures'
$assetsRoot = Join-Path $repoRoot 'app/src/androidTest/assets/advanced-fixtures'
$toolset = Join-Path $Sdk 'build-tools/37.0.0'
$platform = Join-Path $Sdk 'platforms/android-37.0/android.jar'
New-Item -ItemType Directory -Force $fixtureRoot, $assetsRoot | Out-Null

# The sole class is never loaded by an Android component. It has no permission use, I/O or network.
$javaSource = Join-Path $fixtureRoot 'Payload.java'
[IO.File]::WriteAllText($javaSource, @'
package io.github.supermonster003.autojs6.installer.advanced.fixture;
public final class Payload {
    private Payload() { }
    public static int value(int input) { return input * 3 + 1; }
}
'@, [Text.UTF8Encoding]::new($false))
$classesRoot = Join-Path $fixtureRoot 'classes'
$dexRoot = Join-Path $fixtureRoot 'dex'
New-Item -ItemType Directory -Force $classesRoot, $dexRoot | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" --release 8 -d $classesRoot $javaSource
if ($LASTEXITCODE) { throw 'Fixture Java compilation failed' }
$classFile = Join-Path $classesRoot 'io/github/supermonster003/autojs6/installer/advanced/fixture/Payload.class'
& "$toolset/d8.bat" --min-api 24 --lib $platform --output $dexRoot $classFile
if ($LASTEXITCODE) { throw 'Fixture DEX generation failed' }

foreach ($alias in @('primary','other')) {
    $keyFile = Join-Path $fixtureRoot "$alias.jks"
    if (-not (Test-Path -LiteralPath $keyFile)) {
        & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $keyFile -storepass android -keypass android -alias fixture -keyalg RSA -validity 36500 -dname "CN=Installer Advanced Test $alias" -noprompt
        if ($LASTEXITCODE) { throw 'Fixture key generation failed' }
    }
}

$variants = @(
    @{ Name='v1'; Version=1; Key='primary'; Shared='' },
    @{ Name='v2'; Version=2; Key='primary'; Shared='' },
    @{ Name='v2-other'; Version=2; Key='other'; Shared='' },
    @{ Name='shared'; Version=1; Key='primary'; Shared='android:sharedUserId="io.github.supermonster003.autojs6.installer.advanced.shared"' }
)
Add-Type -AssemblyName System.IO.Compression
foreach ($variant in $variants) {
    $manifest = Join-Path $fixtureRoot "$($variant.Name)-manifest.xml"
    $packageName = if ($variant.Name -eq 'shared') { 'io.github.supermonster003.autojs6.installer.advanced.shared.fixture' } else { 'io.github.supermonster003.autojs6.installer.advanced.fixture' }
    $xml = @"
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$packageName"
    android:versionCode="$($variant.Version)" android:versionName="$($variant.Version).0" $($variant.Shared)>
    <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <uses-permission android:name="android.permission.INTERNET" />
    <application android:label="3-Setup Advanced Fixture" android:hasCode="true" android:allowBackup="false" />
</manifest>
"@
    [IO.File]::WriteAllText($manifest, $xml, [Text.UTF8Encoding]::new($false))
    $unsigned = Join-Path $fixtureRoot "$($variant.Name)-unsigned.apk"
    $aligned = Join-Path $fixtureRoot "$($variant.Name)-aligned.apk"
    $signed = Join-Path $assetsRoot "$($variant.Name).apk"
    & "$toolset/aapt2.exe" link -o $unsigned --manifest $manifest -I $platform
    if ($LASTEXITCODE) { throw 'Fixture manifest linking failed' }
    $archive = [IO.Compression.ZipFile]::Open($unsigned, [IO.Compression.ZipArchiveMode]::Update)
    try {
        if ($archive.GetEntry('classes.dex')) { throw 'Unexpected existing fixture DEX' }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $dexRoot 'classes.dex'), 'classes.dex', [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    } finally { $archive.Dispose() }
    & "$toolset/zipalign.exe" -f 4 $unsigned $aligned
    if ($LASTEXITCODE) { throw 'Fixture alignment failed' }
    if ($variant.Name -eq 'v1') { Copy-Item -LiteralPath $aligned -Destination (Join-Path $assetsRoot 'unsigned.apk') }
    & "$toolset/apksigner.bat" sign --ks (Join-Path $fixtureRoot "$($variant.Key).jks") --ks-key-alias fixture --ks-pass pass:android --key-pass pass:android --v4-signing-enabled false --out $signed $aligned
    if ($LASTEXITCODE) { throw 'Fixture signing failed' }
    & "$toolset/apksigner.bat" verify $signed
    if ($LASTEXITCODE) { throw 'Fixture signature verification failed' }
    Get-FileHash -LiteralPath $signed -Algorithm SHA256
}
