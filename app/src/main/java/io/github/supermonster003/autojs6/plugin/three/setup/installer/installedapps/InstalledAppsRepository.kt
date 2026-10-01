package io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Holds application context only. The Activity owns and closes it with its list workers. */
internal class InstalledAppsRepository(context: Context) : Closeable {
    private val packages = context.applicationContext.packageManager
    private val closed = AtomicBoolean()
    private val main = Handler(Looper.getMainLooper())
    private val icons = BoundedIconCache<String, Bitmap>(ICON_CACHE_BYTES) { it.allocationByteCount }
    private val iconWorkers = ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(32))

    @Suppress("DEPRECATION")
    fun load(checkActive: () -> Unit): List<InstalledApp> {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Installed package loading must run on a worker" }
        checkActive()
        val installed = if (Build.VERSION.SDK_INT >= 33) packages.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            else packages.getInstalledPackages(0)
        return installed.mapNotNull { info ->
            checkActive()
            val application = info.applicationInfo ?: return@mapNotNull null
            if (application.flags and ApplicationInfo.FLAG_INSTALLED == 0) return@mapNotNull null
            InstalledApp(
                packageName = info.packageName,
                label = runCatching { packages.getApplicationLabel(application).toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: info.packageName,
                versionName = info.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
                system = application.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
            )
        }
    }

    fun cachedIcon(app: InstalledApp): Bitmap? = icons[iconKey(app)]

    /** Bounded queue plus per-row cancellation keeps fast flings from retaining off-screen rows. */
    fun requestIcon(app: InstalledApp, onLoaded: (Bitmap) -> Unit): Closeable {
        val cancelled = AtomicBoolean()
        val key = iconKey(app)
        val task = FutureTask<Unit> {
            if (cancelled.get() || closed.get()) return@FutureTask Unit
            val bitmap = icons[key] ?: runCatching {
                val drawable = packages.getApplicationIcon(app.packageName)
                Bitmap.createBitmap(ICON_EDGE, ICON_EDGE, Bitmap.Config.ARGB_8888).also { target ->
                    drawable.setBounds(0, 0, ICON_EDGE, ICON_EDGE)
                    drawable.draw(Canvas(target))
                }
            }.getOrNull()
            if (bitmap != null && !cancelled.get() && !closed.get()) {
                synchronized(icons) { if (!closed.get() && !cancelled.get()) icons.put(key, bitmap) }
                main.post { if (!cancelled.get() && !closed.get()) onLoaded(bitmap) }
            }
        }
        if (!closed.get()) {
            try { iconWorkers.execute(task) }
            catch (_: RejectedExecutionException) { cancelled.set(true) }
        }
        return Closeable {
            cancelled.set(true)
            task.cancel(true)
            iconWorkers.remove(task)
        }
    }

    override fun close() {
        closed.set(true)
        iconWorkers.shutdownNow()
        main.removeCallbacksAndMessages(null)
        icons.clear()
    }

    private fun iconKey(app: InstalledApp) = "${app.packageName}:${app.versionCode}:${app.lastUpdateTime}"

    companion object {
        const val ICON_CACHE_BYTES = 4 * 1024 * 1024
        private const val ICON_EDGE = 96
    }
}
