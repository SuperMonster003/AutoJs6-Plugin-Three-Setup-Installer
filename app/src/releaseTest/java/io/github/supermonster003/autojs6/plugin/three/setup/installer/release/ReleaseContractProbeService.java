package io.github.supermonster003.autojs6.plugin.three.setup.installer.release;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import dalvik.system.PathClassLoader;

/**
 * Calls the production services from the test APK's distinct UID and process. This service itself
 * needs only Java/Android classes: AGP need not copy target APK libraries into the test APK. Public
 * AAR classes are loaded from the installed target, whose normal release rules preserve that API.
 */
public final class ReleaseContractProbeService extends Service {
    public static final int PROBE = 1;
    public static final String PLUGIN = "io.github.supermonster003.autojs6.plugin.three.setup.installer";
    private static final String INFO = "org.autojs.plugin.common.api.IPluginInfoProvider";
    private static final String INSTALLER = "org.autojs.plugin.installer.api.IInstallerPlugin";
    private static final String CALLBACK = "org.autojs.plugin.installer.api.IInstallerCallback";
    private static final String SESSION_CALLBACK = "org.autojs.plugin.installer.api.IInstallerSessionCallback";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Messenger endpoint = new Messenger(new Handler(Looper.getMainLooper(), message -> {
        Messenger reply = message.replyTo;
        if (message.what == PROBE && reply != null) worker.execute(() -> {
            Bundle result;
            try {
                result = probe();
            } catch (Exception | AssertionError | LinkageError failure) {
                result = new Bundle();
                result.putString("failure", failure.getClass().getName() + ": " + failure.getMessage());
            }
            Message answer = Message.obtain(null, PROBE);
            answer.setData(result);
            try { reply.send(answer); } catch (android.os.RemoteException ignored) { }
        });
        return true;
    }));

    @Override public IBinder onBind(Intent intent) { return endpoint.getBinder(); }

    @Override public void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private Bundle probe() throws Exception {
        Bundle result = new Bundle();
        result.putInt("probeUid", Process.myUid());
        result.putInt("probePid", Process.myPid());
        ClassLoader contractLoader = new PathClassLoader(getPackageManager().getApplicationInfo(PLUGIN, 0).sourceDir,
                ClassLoader.getSystemClassLoader());
        long started = SystemClock.elapsedRealtime();
        try (RemoteService service = new RemoteService(this, PLUGIN + ".ThreeSetupInstallerPluginInfoService")) {
            requireRemote(service.binder, INFO);
            Class<?> contract = contractLoader.loadClass(INFO);
            if (contract.getClassLoader() != contractLoader) throw new AssertionError("INFO contract did not come from the installed release APK");
            Object api = contractLoader.loadClass(INFO + "$Stub").getMethod("asInterface", IBinder.class).invoke(null, service.binder);
            result.putParcelable("info", (Parcelable) invoke(contract, api, "getInfo", new Class<?>[0]));
        }
        result.putLong("infoRoundTripMillis", SystemClock.elapsedRealtime() - started);
        try (RemoteService service = new RemoteService(this, PLUGIN + ".ThreeSetupInstallerPluginService")) {
            requireRemote(service.binder, INSTALLER);
            Class<?> contract = contractLoader.loadClass(INSTALLER);
            if (contract.getClassLoader() != contractLoader) throw new AssertionError("INSTALLER contract did not come from the installed release APK");
            Class<?> callback = contractLoader.loadClass(CALLBACK);
            Class<?> sessionCallback = contractLoader.loadClass(SESSION_CALLBACK);
            Object api = contractLoader.loadClass(INSTALLER + "$Stub").getMethod("asInterface", IBinder.class).invoke(null, service.binder);
            result.putParcelable("installerInfo", (Parcelable) invoke(contract, api, "getInfo", new Class<?>[0]));
            result.putBundle("capabilities", (Bundle) invoke(contract, api, "getCapabilities", new Class<?>[0]));
            Bundle request = new Bundle();
            // Rejection must precede request decoding or allocation. No operation is authorized.
            mustReject(result, "authorizer", () -> invoke(contract, api, "getAuthorizerState", new Class<?>[]{String.class}, "none"));
            mustReject(result, "users", () -> invoke(contract, api, "getUsers", new Class<?>[]{Bundle.class, callback}, request, null));
            mustReject(result, "default", () -> invoke(contract, api, "getDefaultInstallerState", new Class<?>[0]));
            mustReject(result, "persistent", () -> invoke(contract, api, "setDefaultInstallerV2", new Class<?>[]{boolean.class, Bundle.class, callback}, false, request, null));
            mustReject(result, "inspect", () -> invoke(contract, api, "inspect", new Class<?>[]{ParcelFileDescriptor.class, Bundle.class, callback}, null, request, null));
            mustReject(result, "session", () -> invoke(contract, api, "openSession", new Class<?>[]{ParcelFileDescriptor[].class, Bundle.class, sessionCallback}, null, request, null));
        }
        result.putLong("totalRoundTripMillis", SystemClock.elapsedRealtime() - started);
        return result;
    }

    private static Object invoke(Class<?> contract, Object api, String method, Class<?>[] parameters, Object... arguments) throws Exception {
        try {
            return contract.getMethod(method, parameters).invoke(api, arguments);
        } catch (InvocationTargetException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw wrapper;
        }
    }

    private static void requireRemote(IBinder binder, String descriptor) throws android.os.RemoteException {
        if (binder.queryLocalInterface(descriptor) != null) throw new AssertionError("Expected a real remote " + descriptor);
        if (!descriptor.equals(binder.getInterfaceDescriptor())) throw new AssertionError("Wrong interface descriptor");
    }

    private interface Operation { void run() throws Exception; }

    private static void mustReject(Bundle result, String name, Operation operation) throws Exception {
        try {
            operation.run();
            throw new AssertionError(name + " accepted the non-host test UID");
        } catch (SecurityException expected) {
            result.putBoolean("rejected:" + name, true);
        }
    }

    private static final class RemoteService implements Closeable {
        private final Context context;
        private final CountDownLatch connected = new CountDownLatch(1);
        private final AtomicReference<IBinder> reference = new AtomicReference<>();
        private final ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder service) { reference.set(service); connected.countDown(); }
            @Override public void onServiceDisconnected(ComponentName name) { }
            @Override public void onNullBinding(ComponentName name) { connected.countDown(); }
        };
        final IBinder binder;

        RemoteService(Context context, String className) throws InterruptedException {
            this.context = context;
            if (!context.bindService(new Intent().setComponent(new ComponentName(PLUGIN, className)), connection, Context.BIND_AUTO_CREATE)) {
                throw new AssertionError("bindService failed for " + className);
            }
            try {
                if (!connected.await(15, TimeUnit.SECONDS)) throw new AssertionError("Timed out binding " + className);
                binder = reference.get();
                if (binder == null) throw new AssertionError(className + " returned a null Binder");
            } catch (InterruptedException | RuntimeException | Error failure) {
                context.unbindService(connection);
                throw failure;
            }
        }

        @Override public void close() { context.unbindService(connection); }
    }
}
