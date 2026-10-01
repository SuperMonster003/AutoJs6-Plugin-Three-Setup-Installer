param([string]$Sdk = $env:ANDROID_HOME)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$build = Join-Path $repo 'build/core-fixtures'
$assets = Join-Path $repo 'app/src/androidTest/assets/core-fixtures'
$toolset = Join-Path $Sdk 'build-tools/37.0.0'
New-Item -ItemType Directory -Force $build, $assets | Out-Null
$key = Join-Path $build 'fixture.jks'
if (!(Test-Path -LiteralPath $key)) {
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias fixture -keyalg RSA -validity 36500 -dname 'CN=Installer Core Test Only' -noprompt
    if ($LASTEXITCODE) { throw 'Fixture key generation failed' }
}
# These apps contain no executable code, permissions, launcher or exported components.
$variants = @(
    @{ Name = 'test-only'; Package = 'testonly'; Version = 1; Target = 28; Application = 'android:testOnly="true"' },
    @{ Name = 'low-target'; Package = 'lowtarget'; Version = 1; Target = 22 },
    @{ Name = 'debug-v1'; Package = 'downgrade'; Version = 1; Target = 28; Application = 'android:debuggable="true"' },
    @{ Name = 'debug-v2'; Package = 'downgrade'; Version = 2; Target = 28; Application = 'android:debuggable="true"' },
    @{ Name = 'release-v1'; Package = 'release'; Version = 1; Target = 28 },
    @{ Name = 'release-v2'; Package = 'release'; Version = 2; Target = 28 },
    @{ Name = 'split-base'; Package = 'splits'; Version = 1; Target = 28 },
    @{ Name = 'split-feature'; Package = 'splits'; Version = 1; Target = 28; Split = 'split="feature.extra" android:isFeatureSplit="true"' }
)
foreach ($variant in $variants) {
    $manifest = Join-Path $build "$($variant.Name)-AndroidManifest.xml"
    $unsigned = Join-Path $build "$($variant.Name)-unsigned.apk"
    $aligned = Join-Path $build "$($variant.Name)-aligned.apk"
    $signed = Join-Path $assets "$($variant.Name).apk"
    $xml = @"
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="io.github.supermonster003.autojs6.installer.core.$($variant.Package)"
    android:versionCode="$($variant.Version)" android:versionName="$($variant.Version).0" $($variant.Split)>
    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="$($variant.Target)" />
    <application android:label="3-Setup Core Fixture" android:hasCode="false" android:allowBackup="false" $($variant.Application) />
</manifest>
"@
    [IO.File]::WriteAllText($manifest, $xml, [Text.UTF8Encoding]::new($false))
    & "$toolset/aapt2.exe" link -o $unsigned --manifest $manifest -I "$Sdk/platforms/android-37.0/android.jar"
    if ($LASTEXITCODE) { throw "Fixture resource linking failed: $($variant.Name)" }
    & "$toolset/zipalign.exe" -f 4 $unsigned $aligned
    if ($LASTEXITCODE) { throw 'Fixture alignment failed' }
    & "$toolset/apksigner.bat" sign --ks $key --ks-key-alias fixture --ks-pass pass:android --key-pass pass:android --v4-signing-enabled false --out $signed $aligned
    if ($LASTEXITCODE) { throw 'Fixture signing failed' }
    & "$toolset/apksigner.bat" verify $signed
    if ($LASTEXITCODE) { throw 'Fixture signature verification failed' }
    Get-FileHash -Algorithm SHA256 -LiteralPath $signed
}
