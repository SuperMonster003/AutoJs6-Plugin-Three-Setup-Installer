package io.github.supermonster003.autojs6.plugin.three.setup.installer.notificationfixture;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Uses only Android/Java: the independently launched test APK has no target APK Kotlin runtime. */
public final class NotificationSourceProvider extends ContentProvider {
    public static final String TARGET = "io.github.supermonster003.autojs6.plugin.three.setup.installer";
    public static final String MIME = "application/vnd.android.package-archive";
    public static final String SHA256 = "fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69";

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return MIME; }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final File file = source(uri);
        android.util.Log.i("NotificationSourceAudit", "query nonce=" + nonce(uri, Objects.requireNonNull(getContext()).getPackageName()));
        final String[] columns = projection != null ? projection : new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        final MatrixCursor cursor = new MatrixCursor(columns);
        final Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            switch (columns[i]) {
                case OpenableColumns.DISPLAY_NAME: values[i] = "fixture.apk"; break;
                case OpenableColumns.SIZE: values[i] = file.length(); break;
                case "provider_uid": values[i] = Process.myUid(); break;
                case "caller_uid": values[i] = Binder.getCallingUid(); break;
                default: values[i] = null;
            }
        }
        cursor.addRow(values);
        return cursor;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only fixed fixture");
        final File file = source(uri);
        android.util.Log.i("NotificationSourceAudit", "open nonce=" + nonce(uri, Objects.requireNonNull(getContext()).getPackageName()));
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private synchronized File source(Uri uri) {
        final Context owner = Objects.requireNonNull(getContext());
        final String nonce = nonce(uri, owner.getPackageName());
        final File file = new File(owner.getCacheDir(), "notification-source-" + nonce + ".apk");
        try {
            if (!file.exists()) {
                final byte[] bytes;
                try (InputStream input = owner.getAssets().open("fixture-v1.apk")) { bytes = read(input); }
                if (!SHA256.equals(digest(bytes)) || !file.createNewFile()) throw new IOException("Invalid fixed fixture");
                try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); }
            }
            try (InputStream input = new FileInputStream(file)) {
                if (!SHA256.equals(digest(read(input)))) throw new IOException("Fixture contents changed");
            }
            return file;
        } catch (IOException failure) { throw new IllegalStateException("Cannot prepare the fixed fixture", failure); }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Read-only"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Read-only"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Read-only"); }

    public static String nonce(Uri uri, String owner) {
        final List<String> parts = uri.getPathSegments();
        if (!"content".equals(uri.getScheme()) || !(owner + ".notification-source").equals(uri.getAuthority()) ||
                uri.getQuery() != null || uri.getFragment() != null || parts.size() != 2 || !"fixture.apk".equals(parts.get(1)) ||
                !UUID.fromString(parts.get(0)).toString().equals(parts.get(0))) throw new IllegalArgumentException("Invalid fixture URI");
        return parts.get(0);
    }

    public static String digest(byte[] bytes) {
        try {
            final StringBuilder text = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) text.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return text.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static byte[] read(InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }
}
