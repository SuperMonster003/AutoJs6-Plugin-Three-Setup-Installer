-dontwarn kotlinx.parcelize.Parcelize

# Host discovery entry points are looked up by class name from the manifest.
-keep class io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPluginInfoService { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPluginService { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.setup.installer.WakeActivity { *; }

# Host contract AARs: parcelables and AIDL stubs are resolved reflectively across processes.
-keep class org.autojs.plugin.common.api.** { *; }
-keep class org.autojs.plugin.installer.api.** { *; }

# Shizuku and libsu instantiate the privileged services and their AIDL stubs by name (roadmap P0.2).
-keep class rikka.shizuku.** { *; }
-keep class com.topjohnwu.superuser.** { *; }
-keep class * extends com.topjohnwu.superuser.ipc.RootService { *; }
-keep class io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.ShizukuUserService { *; }
