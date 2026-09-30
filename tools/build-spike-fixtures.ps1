param([string]$Sdk = $env:ANDROID_HOME)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$build = Join-Path $repo 'build/spike-fixtures'
$assets = Join-Path $repo 'app/src/androidTest/assets'
$toolset = Join-Path $Sdk 'build-tools/37.0.0'
New-Item -ItemType Directory -Force $build, $assets | Out-Null
$key = Join-Path $build 'fixture.jks'
if (!(Test-Path -LiteralPath $key)) {
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias fixture -keyalg RSA -validity 36500 -dname 'CN=Installer Spike Test Only' -noprompt
    if ($LASTEXITCODE) { throw 'Fixture key generation failed' }
}
foreach ($version in 1, 2) {
    $unsigned = Join-Path $build "fixture-v$version-unsigned.apk"
    $aligned = Join-Path $build "fixture-v$version-aligned.apk"
    $signed = Join-Path $assets "fixture-v$version.apk"
    & "$toolset/aapt2.exe" link -o $unsigned --manifest "$PSScriptRoot/spike-fixtures/v$version/AndroidManifest.xml" -I "$Sdk/platforms/android-37.0/android.jar"
    if ($LASTEXITCODE) { throw 'Fixture resource linking failed' }
    & "$toolset/zipalign.exe" -f 4 $unsigned $aligned
    if ($LASTEXITCODE) { throw 'Fixture alignment failed' }
    & "$toolset/apksigner.bat" sign --ks $key --ks-key-alias fixture --ks-pass pass:android --key-pass pass:android --v4-signing-enabled false --out $signed $aligned
    if ($LASTEXITCODE) { throw 'Fixture signing failed' }
    & "$toolset/apksigner.bat" verify --verbose $signed
    if ($LASTEXITCODE) { throw 'Fixture signature verification failed' }
    Get-FileHash -Algorithm SHA256 -LiteralPath $signed
}
