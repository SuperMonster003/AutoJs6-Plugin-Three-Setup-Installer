import org.autojs.build.alignment.VerifyNativePageAlignment
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
val buildTypeDebug = "debug"
val buildTypeRelease = "release"
val testRelease = providers.gradleProperty("androidTestRelease").isPresent

// ---------------------------------------------------------------------------
// Host protocol AARs are consumed only from libs/ and are pinned by locks/host-api-aars.lock.
// The build refuses missing files, debug artifacts, placeholder hashes, and digest mismatches.
// ---------------------------------------------------------------------------

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Properties.requiredValue(key: String): String =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: error("Missing required lock value: $key")

fun File.loadUniqueLock(): Properties {
    val lock = Properties()
    useLines(Charsets.UTF_8) { lines ->
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                return@forEachIndexed
            }
            val separator = trimmed.indexOf('=')
            require(separator > 0) { "Malformed lock line ${index + 1} in $name" }
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            require(!lock.containsKey(key)) { "Duplicate lock key in $name: $key" }
            lock.setProperty(key, value)
        }
    }
    return lock
}

val sha256Pattern = Regex("[0-9a-f]{64}")
val hostApiLockFile = rootProject.file("locks/host-api-aars.lock")
require(hostApiLockFile.isFile) {
    "Missing host API lock: ${hostApiLockFile.relativeTo(rootProject.projectDir)}"
}
val hostApiLock = hostApiLockFile.loadUniqueLock()
val hostApiIds = listOf("common-plugin-api", "package-archive-parser", "installer-api")
val expectedHostApiLockKeys = setOf("format") + hostApiIds.flatMap { id -> listOf("$id.file", "$id.sha256") }
require(hostApiLock.stringPropertyNames() == expectedHostApiLockKeys) {
    "Host API AAR lock must contain exactly these keys: ${expectedHostApiLockKeys.sorted()}"
}
require(hostApiLock.requiredValue("format") == "1") {
    "Unsupported host API AAR lock format"
}

fun lockedHostApiAar(id: String): File {
    val fileName = hostApiLock.requiredValue("$id.file")
    val expectedSha256 = hostApiLock.requiredValue("$id.sha256").lowercase()
    require(fileName == File(fileName).name && fileName.endsWith(".aar")) {
        "Invalid $id.file in ${hostApiLockFile.name}"
    }
    require(!fileName.endsWith("-debug.aar")) {
        "Debug AARs are forbidden: $fileName"
    }
    require(sha256Pattern.matches(expectedSha256)) {
        "Replace $id.sha256 with the audited release AAR SHA-256 before Gradle configuration"
    }
    val artifact = rootProject.file("libs/$fileName")
    require(artifact.isFile) {
        "Missing locked host API AAR: ${artifact.relativeTo(rootProject.projectDir)}"
    }
    val actualSha256 = artifact.sha256()
    require(actualSha256 == expectedSha256) {
        "SHA-256 mismatch for $fileName: expected $expectedSha256, actual $actualSha256"
    }
    return artifact
}

val hostApiAars = hostApiIds.map(::lockedHostApiAar)

android {
    // The host can select any bundled locale independently of the Android system language.
    bundle { language { enableSplit = false } }
    // Keep only the app's 10 languages (res/xml/locales_config.xml) from AndroidX/Material resources.
    androidResources { localeFilters += setOf("en", "ar", "es", "fr", "ja", "ko", "ru", "zh", "zh-rCN", "zh-rHK", "zh-rTW") }
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    // Exercise the signed, non-debuggable R8 artifact without exposing production internals to
    // instrumentation or adding test-only keep rules to it. This opt-in uses only public IPC.
    if (testRelease) {
        require(signs.isValid) { "-PandroidTestRelease requires the configured release signing key" }
        testBuildType = buildTypeRelease
        sourceSets.named("androidTest") {
            java.setSrcDirs(listOf("src/releaseTest/java"))
            kotlin.setSrcDirs(listOf("src/releaseTest/java"))
            assets.setSrcDirs(emptyList<String>())
            manifest.srcFile("src/releaseTest/AndroidManifest.xml")
        }
    }

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = if (testRelease) "$globalApplicationId.release.ReleaseContractInstrumentation"
            else "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_engine", "installer")
        resValue("string", "plugin_id", "three-setup-installer")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    lint {
        abortOnError = true
        // Product text intentionally uses ASCII punctuation in every locale.
        disable += "TypographyEllipsis"
        // core-ktx arrives only transitively with AppCompat; the code base keeps plain platform APIs.
        disable += "UseKtx"
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            // This file shrinks the instrumentation APK only; the app keeps its release rules.
            testProguardFiles("proguard-test-rules.pro")
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        resValues = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // No ABI splits on purpose (roadmap D13): the plugin has no native code of its own (Shizuku, libsu and
    // HiddenApiBypass carry no native libraries); the only native libraries are Compose's graphics-path ones, packaged
    // for four ABIs (see nativeAlignment below). Every device installs the same single APK and getInfo() reports
    // supportedAbis = emptyArray().
    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    // PluginInfo, IPluginInfoProvider and the shared plugin constants (host module plugin-api/common-plugin-api).
    implementation(files(hostApiAars))
    implementation(libs.gson)
    // Shizuku authorizer: permission request, binder wrapper and the privileged UserService (roadmap D11).
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.dhizuku.api)
    // Root authorizer: su shell and the RootService hosting the same privileged installer (roadmap D11).
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    // Hidden package installer / package manager interfaces used by the privileged service (roadmap D15).
    implementation(libs.hiddenapibypass)
    // Material 3 widgets and AppCompat theming for the standalone app (roadmap P3 / P5).
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose, only for the release history page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md); the BOM is the
    // single version source and also manages activity-compose.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.activity)

    testImplementation(libs.junit)

    if (!testRelease) {
        androidTestImplementation(libs.test.runner)
        androidTestImplementation(libs.test.rules)
        androidTestImplementation(libs.test.ext.junit)
        // ui-test brings espresso-core 3.5.0, which fails to reflect InputManager.getInstance on API 36; pin 3.7.0.
        androidTestImplementation(libs.test.espresso.core)
        androidTestImplementation(platform(libs.compose.bom))
        androidTestImplementation(libs.compose.ui.test.junit4)
        debugImplementation(libs.compose.ui.test.manifest)
    }
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register<Copy>("appendDigestToReleasedFiles") {
        description = "Appends CRC32 digest to the released 3-Setup Installer APK file"
        dependsOn("assembleRelease", "verifyReleaseNativeLibraries")

        val ext = utils.FILE_EXTENSION_APK
        val src = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease")
        val dst = file("$rootDir/${buildTypeRelease}s")
        val releaseSigningReady = signs.isValid
        val expectedApkNames = listOf(
            "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}.$ext".lowercase(),
        )

        from(src)
        into(dst)
        include("*.$ext")
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        doFirst {
            require(releaseSigningReady) {
                "Release signing is not configured (sign.properties); refusing to collect an unsigned APK"
            }
            val actualApkNames = src.get().asFile.listFiles { file -> file.extension == ext }
                .orEmpty()
                .map { it.name }
                .sorted()
            require(actualApkNames == expectedApkNames) {
                "Unexpected release APK set: expected $expectedApkNames, actual $actualApkNames"
            }
        }

        eachFile {
            val digest = utils.digestCRC32(file)
            relativePath = RelativePath(true, "${name.removeSuffix(".$ext")}-$digest.$ext")
        }

        doLast { println("Destination: $dst") }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}

// Bundle the license texts so the About screen (roadmap P5.3) can show them offline.
val bundledLegalAssets = tasks.register<Sync>("bundleLegalAssets") {
    from(listOf(rootProject.file("LICENSE"), rootProject.file("THIRD_PARTY_NOTICES.md"))) { into("legal") }
    into(layout.buildDirectory.dir("generated/legal-assets"))
}
android.sourceSets.getByName("main").assets.directories.add(layout.buildDirectory.dir("generated/legal-assets").get().asFile.path)
tasks.named("preBuild").configure { dependsOn(bundledLegalAssets) }

// Native code inventory: the plugin has no native code of its own, but Compose ui-graphics depends on
// androidx.graphics:graphics-path, whose libandroidx.graphics.path.so is packaged for four ABIs. The APK stays one
// universal package without ABI splits; the shared gate keeps the 16 KB alignment checks, and
// verifyReleaseNativeLibraries below asserts that exactly these libraries are present and stored uncompressed.
val nativeLibraryAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val allowedNativeLibraries = nativeLibraryAbis.map { "lib/$it/libandroidx.graphics.path.so" }
nativeAlignment { expectNoNativeLibraries.set(false) }

tasks.register("verifyReleaseNativeLibraries") {
    description = "Checks that the release APK carries exactly the Compose graphics-path libraries, stored uncompressed"
    dependsOn("assembleRelease")
    val apkDir = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease")
    doLast {
        val apks = apkDir.get().asFile.listFiles { file -> file.extension == "apk" }.orEmpty().toList()
        check(apks.size == 1) { "Expected one release APK, found ${apks.map { it.name }}" }
        ZipFile(apks.single()).use { zip ->
            val natives = zip.entries().asSequence().filter { it.name.startsWith("lib/") }.toList()
            check(natives.map { it.name }.sorted() == allowedNativeLibraries.sorted()) {
                "Unexpected native library set: ${natives.map { it.name }.sorted()}"
            }
            check(natives.all { it.method == ZipEntry.STORED }) { "Native libraries must be stored uncompressed" }
        }
    }
}

tasks.withType<VerifyNativePageAlignment>().configureEach {
    // The 1.8.0 plugin also finalizes assemble*UnitTest, which never produces an APK.
    // Keep verification on APK variants and let standalone checks build their inputs.
    val variantSuffix = name.removePrefix("verify").removeSuffix("NativePageAlignment")
    if (variantSuffix.endsWith("UnitTest")) {
        enabled = false
    } else {
        dependsOn("assemble$variantSuffix")
    }
}
