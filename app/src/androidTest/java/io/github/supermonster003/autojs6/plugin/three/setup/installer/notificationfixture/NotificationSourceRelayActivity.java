package io.github.supermonster003.autojs6.plugin.three.setup.installer.notificationfixture;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.Objects;

/** Shell/DUMP only. Grants one validated fixed-fixture URI to the real external NoDisplay entry. */
public final class NotificationSourceRelayActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (savedInstanceState == null) {
                final Uri uri = Objects.requireNonNull(getIntent().getData());
                NotificationSourceProvider.nonce(uri, getPackageName());
                final Intent target = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, NotificationSourceProvider.MIME)
                        .setComponent(new ComponentName(NotificationSourceProvider.TARGET, NotificationSourceProvider.TARGET + ".ui.ExternalInstallActivity"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                target.setClipData(ClipData.newRawUri("Fixed notification fixture", uri));
                startActivity(target);
            }
        } finally { finish(); }
    }
}
