package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv;

import android.content.ComponentName;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

/** Private to the plugin. The host never receives this Binder. */
interface IPrivilegedInstaller {
    int createSession(in Bundle params, String installerPackageName, int userId) = 0;
    ParcelFileDescriptor openWrite(int sessionId, String name, long length) = 1;
    void commit(int sessionId, in IntentSender sender) = 2;
    void abandon(int sessionId) = 3;
    void uninstall(String packageName, int flags, int userId, in IntentSender sender) = 4;
    int setDefaultInstaller(in ComponentName component, boolean enable) = 5;
    Bundle getUsers() = 6;
    int getUid() = 7;
    void attachClient(IBinder token) = 8;
    // Release local ownership after a terminal result, without aborting the platform session.
    void release(int sessionId) = 9;
    // Only version metadata for the explicitly selected user; never substitute the app's user.
    Bundle getInstalledVersion(String packageName, int userId) = 10;
    // Reserved by Shizuku: transaction 16777115 (AIDL adds FIRST_CALL_TRANSACTION).
    void destroy() = 16777114;
    // Nonblocking lookup of a pipe writer's original failure after the client observes EPIPE.
    void checkWriteStatus(int sessionId) = 11;
    // Read-only identity of this attached service process, not the authorization server.
    Bundle getProcessIdentity() = 12;
    // Captured only for a session created by this instance. Null on APIs without full identity.
    Bundle getSessionRecoveryInfo(int sessionId) = 13;
    // A replacement process checks exact platform identity before abandoning this one session.
    void abandonRecoveredSession(int sessionId, in Bundle expected) = 14;
}
