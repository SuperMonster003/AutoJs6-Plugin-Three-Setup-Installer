package io.github.supermonster003.autojs6.plugin.three.setup.installer.release;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.Process;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** No dependency on Kotlin, AndroidX, JUnit or obfuscatable implementation classes in either APK. */
public final class ReleaseContractDeviceTest {
    private final Instrumentation instrumentation;
    private final Context context;

    ReleaseContractDeviceTest(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.context = instrumentation.getTargetContext();
    }

    @SuppressWarnings("deprecation")
    public void productionArtifactIsSignedNonDebuggableAndHasNoDebugEntryPointsOrNativeLibraries() throws Exception {
        assertRelease();
        int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS |
                PackageManager.GET_PROVIDERS | PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.GET_SIGNATURES;
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), flags);
        check(info.signatures != null && info.signatures.length > 0, "The installed release must be signed");
        requireProductionComponents(info.activities);
        requireProductionComponents(info.services);
        requireProductionComponents(info.receivers);
        requireProductionComponents(info.providers);
        File source = new File(context.getApplicationInfo().sourceDir);
        long apkBytes = source.length();
        check(apkBytes > 0, "The installed APK is empty");
        try (ZipFile archive = new ZipFile(source)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                check(!(name.startsWith("lib/") && name.endsWith(".so")), "The release contains a native library: " + name);
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(source)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
        }
        StringBuilder sha256 = new StringBuilder();
        for (byte part : digest.digest()) sha256.append(String.format(java.util.Locale.ROOT, "%02x", part & 0xff));
        Bundle evidence = new Bundle();
        evidence.putString("releaseArtifact", "model=" + Build.MODEL + " api=" + Build.VERSION.SDK_INT + " version=" + info.versionName +
                " versionCode=" + versionCode(info) + " apkBytes=" + apkBytes + " sha256=" + sha256 + " debuggable=false nativeLibraries=0");
        instrumentation.sendStatus(0, evidence);
    }

    @SuppressWarnings("deprecation")
    public void realCrossUidInfoAndInstallerMetadataRoundTripPreservesCallerChecks() throws Exception {
        assertRelease();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<IBinder> remote = new AtomicReference<>();
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) { remote.set(binder); connected.countDown(); }
            @Override public void onServiceDisconnected(ComponentName name) { }
            @Override public void onNullBinding(ComponentName name) { connected.countDown(); }
        };
        String testPackage = instrumentation.getContext().getPackageName();
        ComponentName component = new ComponentName(testPackage, ReleaseContractProbeService.class.getName());
        equal(testPackage + ":release_contract", context.getPackageManager().getServiceInfo(component, 0).processName, "Probe process");
        check(context.bindService(new Intent().setComponent(component), connection, Context.BIND_AUTO_CREATE), "Could not bind the separate test process");
        try {
            check(connected.await(15, TimeUnit.SECONDS), "Probe binding timed out");
            IBinder binder = Objects.requireNonNull(remote.get(), "The probe returned a null Binder");
            check(binder.queryLocalInterface("android.os.IMessenger") == null, "The probe must be a real Binder proxy");
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<Bundle> reply = new AtomicReference<>();
            Messenger response = new Messenger(new Handler(Looper.getMainLooper(), message -> {
                Bundle value = message.getData();
                value.setClassLoader(context.getClassLoader());
                reply.set(value);
                completed.countDown();
                return true;
            }));
            Message request = Message.obtain(null, ReleaseContractProbeService.PROBE);
            request.replyTo = response;
            new Messenger(binder).send(request);
            check(completed.await(40, TimeUnit.SECONDS), "The separate process did not finish its service calls");
            Bundle result = Objects.requireNonNull(reply.get());
            check(result.getString("failure") == null, "Remote probe: " + result.getString("failure"));
            check(result.getInt("probeUid") != Process.myUid(), "The probe reused the plugin UID");
            check(result.getInt("probePid") != Process.myPid(), "The probe reused the plugin PID");
            equal(context.getPackageManager().getApplicationInfo(testPackage, 0).uid, result.getInt("probeUid"), "Test APK UID");
            PackageInfo installed = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            for (String key : new String[]{"info", "installerInfo"}) {
                Parcelable info = Objects.requireNonNull(result.getParcelable(key), "Missing " + key);
                equal("org.autojs.plugin.common.api.PluginInfo", info.getClass().getName(), "Parcelable API name");
                equal("3-Setup Installer", property(info, "getName"), "Plugin name");
                equal("three-setup-installer", property(info, "getId"), "Plugin ID");
                equal("installer", property(info, "getEngine"), "Plugin engine");
                equal("default", property(info, "getVariant"), "Plugin variant");
                equal(installed.versionName, property(info, "getVersionName"), "Version name");
                equal(versionCode(installed), property(info, "getVersionCode"), "Version code");
                Object instruction = property(info, "getInstruction");
                check(instruction instanceof String && !((String) instruction).trim().isEmpty(), "Plugin instructions are missing");
                check(Arrays.equals(new String[0], (String[]) property(info, "getSupportedAbis")), "No ABI restriction must be explicit");
                assertCapabilities((Bundle) property(info, "getCapabilities"));
            }
            assertCapabilities(result.getBundle("capabilities"));
            for (String name : new String[]{"authorizer", "users", "default", "persistent", "inspect", "session"}) {
                check(result.getBoolean("rejected:" + name), "The caller guard accepted " + name);
            }
            Bundle evidence = new Bundle();
            evidence.putString("releaseRoundTrip", "api=" + Build.VERSION.SDK_INT + " pluginUid=" + Process.myUid() +
                    " probeUid=" + result.getInt("probeUid") + " pluginPid=" + Process.myPid() + " probePid=" + result.getInt("probePid") +
                    " infoMillis=" + result.getLong("infoRoundTripMillis") + " totalMillis=" + result.getLong("totalRoundTripMillis") +
                    " guardedOperations=6 nativeLibraries=0");
            instrumentation.sendStatus(0, evidence);
        } finally { context.unbindService(connection); }
    }

    private void assertRelease() {
        equal(ReleaseContractProbeService.PLUGIN, context.getPackageName(), "Target package");
        equal(context.getApplicationInfo().uid, Process.myUid(), "Target UID");
        equal(0, context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE,
                "Use -PandroidTestRelease and install the signed release before running this suite");
    }

    private static void requireProductionComponents(ComponentInfo[] components) {
        if (components == null) return;
        for (ComponentInfo component : components) {
            check(!component.name.contains(".spike.") && !component.name.contains(".release.") && !component.name.contains(".debug."),
                    "A debug probe entered the release APK: " + component.name);
        }
    }

    private static void assertCapabilities(Bundle capabilities) {
        Objects.requireNonNull(capabilities, "Capabilities are missing");
        equal(5299L, capabilities.getLong("requiresHostVersion"), "Minimum host build");
        equal(1, capabilities.getInt("installerContractVersion"), "Installer contract version");
        equal(2, capabilities.getInt("installerMaxContractVersion"), "Maximum installer contract version");
        check(Arrays.equals(new String[]{"none", "shizuku", "root", "dhizuku"}, capabilities.getStringArray("installerAuthorizers")), "Authorizers differ from contract V2");
        equal(32, capabilities.getInt("installerMaxBatch"), "Maximum batch size");
        equal(64, capabilities.getInt("installerMaxSplits"), "Maximum split count");
        equal(new HashSet<>(Arrays.asList("batch", "splits", "inspect", "users", "silent-uninstall", "delete-source", "default-installer", "persistent-default-installer")),
                new HashSet<>(Arrays.asList(Objects.requireNonNull(capabilities.getStringArray("installerFeatures")))), "Installer features");
    }

    private static Object property(Object value, String method) throws Exception { return value.getClass().getMethod(method).invoke(value); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
    }
    @SuppressWarnings("deprecation")
    private static long versionCode(PackageInfo info) { return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode; }
}
