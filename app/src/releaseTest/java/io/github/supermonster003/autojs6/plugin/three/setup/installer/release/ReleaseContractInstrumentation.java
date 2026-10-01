package io.github.supermonster003.autojs6.plugin.three.setup.installer.release;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

/**
 * Platform-only runner for the two release checks. AGP removes tested-app dependency artifacts
 * from the test APK, while R8 can remove their unused classes from the app. Depending on AndroidX
 * (or Kotlin/JUnit) here would therefore require test-only production keep rules. This runner
 * needs none; all release assertions and the remote probe use only Java/Android platform classes.
 */
public final class ReleaseContractInstrumentation extends Instrumentation {
    private Bundle arguments;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        this.arguments = arguments == null ? new Bundle() : new Bundle(arguments);
        start();
    }

    @Override public void onStart() {
        String className = ReleaseContractDeviceTest.class.getName();
        String selected = arguments.getString("class");
        if (selected != null && !className.equals(selected)) {
            Bundle rejected = new Bundle();
            rejected.putString("shortMsg", "This release runner only accepts the complete " + className + " suite");
            finish(Activity.RESULT_CANCELED, rejected);
            return;
        }
        String[] methods = {
                "productionArtifactIsSignedNonDebuggableAndHasNoDebugEntryPointsOrNativeLibraries",
                "realCrossUidInfoAndInstallerMetadataRoundTripPreservesCallerChecks"
        };
        ReleaseContractDeviceTest tests = new ReleaseContractDeviceTest(this);
        int failures = 0;
        for (int index = 0; index < methods.length; index++) {
            Bundle status = new Bundle();
            status.putString("class", className);
            status.putString("test", methods[index]);
            status.putInt("numtests", methods.length);
            status.putInt("current", index + 1);
            sendStatus(1, status);
            try {
                if (index == 0) tests.productionArtifactIsSignedNonDebuggableAndHasNoDebugEntryPointsOrNativeLibraries();
                else tests.realCrossUidInfoAndInstallerMetadataRoundTripPreservesCallerChecks();
                sendStatus(0, status);
            } catch (Exception | AssertionError | LinkageError failure) {
                failures++;
                status.putString("stack", Log.getStackTraceString(failure));
                sendStatus(-2, status);
            }
        }
        Bundle result = new Bundle();
        result.putInt("releaseChecks", methods.length);
        result.putInt("releaseFailures", failures);
        result.putString("stream", failures == 0 ? "\nOK (2 tests)\n" : "\nFAILURES!!! Tests run: 2, Failures: " + failures + "\n");
        finish(failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
}
