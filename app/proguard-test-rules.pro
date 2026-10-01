# Applied only to the instrumentation APK built with -PandroidTestRelease. Production shrinking
# uses the exact same proguard-rules.pro with or without the property.
-keep class io.github.supermonster003.autojs6.plugin.three.setup.installer.release.** { *; }
