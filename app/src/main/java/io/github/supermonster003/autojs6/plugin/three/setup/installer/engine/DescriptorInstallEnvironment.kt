package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerResolver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageSource
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.SeekableSource
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Duplicates host-owned descriptors before scheduling; all source bytes remain read-only. */
internal class DescriptorInstallEnvironment private constructor(
    context: Context,
    private val descriptors: List<ParcelFileDescriptor>,
    private val confirmation: (PreparedPackage, InstallSession.Target, Long, () -> Unit) -> Unit,
    private val configuration: ((Int, PreparedPackage, InstallSession.Target, InstallRequest, Long, () -> Unit) -> InstallSession.Selection)?,
    private val preparedListener: (Int, PreparedPackage) -> Unit,
    private val installed: (Int, InstallOptions) -> InstallSession.SourceCleanup,
    private val sourceLoader: ((SourceEntry, () -> Unit) -> SourceDescriptor)? = null,
    private val availability: () -> Unit = {},
    private val userAction: (Intent) -> Unit = { UserActionLauncher.launch(context, it) },
) : InstallSession.Environment {
    /** A lazily opened external source; prepare owns and closes its descriptor immediately. */
    data class SourceDescriptor(val descriptor: ParcelFileDescriptor, val displayName: String, val size: Long)
    private val context = context.applicationContext
    private var directory: File? = null
    private val closed = AtomicBoolean()
    private val stagingCancellation = AtomicBoolean()
    private val openedSources = mutableMapOf<Int, MutableList<SeekableSource>>()
    private var deadline = Long.MAX_VALUE

    override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target {
        deadline = deadlineMillis
        checkActive()
        val authorizer = io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences.resolveAuthorizer(context, request.options.authorizer)
        if (!authorizer.privileged && (request.interaction == InstallerContract.INTERACTION_SILENT || request.options.privilegedOptions.isNotEmpty())) {
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Silent installation and privileged options require Shizuku or Root")
        }
        checkActive()
        val userId = DeviceUsers(context).resolve(request.options.user, authorizer, remaining())
        return InstallSession.Target(authorizer, userId,
            when {
                authorizer == Authorizer.DHIZUKU -> DhizukuInstallEngine(context)
                authorizer.privileged -> PrivilegedInstallEngine(context, authorizer)
                else -> NoneInstallEngine(context)
            })
    }

    override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit): PreparedPackage {
        checkActive()
        val root = directory ?: PackageStaging.newDirectory(context).also { directory = it }
        val item = PackageStaging.newChildDirectory(root, "item-$index")
        val device = PackageDeviceSpec.from(context)
        val parts = sources.map { source ->
            checkActive()
            val folder = PackageStaging.newChildDirectory(item, "source-${source.descriptor}")
            val loaded = sourceLoader?.invoke(source, checkActive)
                ?: SourceDescriptor(descriptors[source.descriptor], source.displayName, source.size)
            val opened = loaded.descriptor.use {
                PackageStaging.open(context, folder, PackageSource.Descriptor(it, loaded.displayName, loaded.size), stagingCancellation, checkActive = checkActive)
            }
            openedSources.getOrPut(index) { mutableListOf() } += opened
            checkActive()
            ArchiveOpener.open(opened.file, loaded.displayName, device, folder, prepareForInstallation = true, checkActive = checkActive)
        }
        checkActive()
        return ArchiveOpener.combine(parts).also { preparedListener(index, it) }
    }

    override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version? {
        if (target.authorizer.privileged && target.authorizer != Authorizer.DHIZUKU) {
            val info = PrivilegedClient.get(context).acquire(target.authorizer, remaining()).getInstalledVersion(packageName, target.userId)
            return if (info.containsKey("versionCode")) InstallSession.Version(info.getString("versionName"), info.getLong("versionCode")) else null
        }
        check(target.userId == DeviceUsers(context).currentId)
        return try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            InstallSession.Version(info.versionName, if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
        } catch (_: PackageManager.NameNotFoundException) { null }
    }

    override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) = confirmation(prepared, target, deadlineMillis, checkActive)
    override fun configure(index: Int, prepared: PreparedPackage, target: InstallSession.Target, request: InstallRequest,
        deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Selection =
        configuration?.invoke(index, prepared, target, request, deadlineMillis, checkActive)
            ?: super<InstallSession.Environment>.configure(index, prepared, target, request, deadlineMillis, checkActive)
    override fun onInstalled(index: Int, options: InstallOptions) = installed(index, options)
    override fun checkAvailable() = availability()
    override fun onUserAction(intent: Intent) = userAction(intent)
    override fun discardItem(index: Int) {
        openedSources.remove(index)?.forEach { runCatching { it.close() } }
        directory?.let { File(it, "item-$index").deleteRecursively() }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        openedSources.values.flatten().forEach { runCatching { it.close() } }
        openedSources.clear()
        descriptors.forEach { runCatching { it.close() } }
        PackageStaging.discard(directory)
    }

    private fun remaining(): Long = (deadline - SystemClock.elapsedRealtime()).coerceIn(1, PrivilegedClient.BIND_TIMEOUT_MILLIS)

    companion object {
        /** External sources are opened in each item's prepare phase, preserving batch failures. */
        fun acquireSources(context: Context,
            sourceLoader: (SourceEntry, () -> Unit) -> SourceDescriptor,
            configuration: (Int, PreparedPackage, InstallSession.Target, InstallRequest, Long, () -> Unit) -> InstallSession.Selection,
            preparedListener: (Int, PreparedPackage) -> Unit,
            installed: (Int, InstallOptions) -> InstallSession.SourceCleanup,
            availability: () -> Unit = {},
            userAction: (Intent) -> Unit = { UserActionLauncher.launch(context, it) },
        ): DescriptorInstallEnvironment = DescriptorInstallEnvironment(context, emptyList(),
            confirmation = { _, _, _, _ -> throw InstallFailure(InstallerErrorCodes.INTERNAL, "External confirmation configuration is missing") },
            configuration = configuration, preparedListener = preparedListener, installed = installed, sourceLoader = sourceLoader,
            availability = availability, userAction = userAction)

        fun acquire(context: Context, descriptors: List<ParcelFileDescriptor>,
            configuration: ((Int, PreparedPackage, InstallSession.Target, InstallRequest, Long, () -> Unit) -> InstallSession.Selection)? = null,
            preparedListener: (Int, PreparedPackage) -> Unit = { _, _ -> },
            installed: (Int, InstallOptions) -> InstallSession.SourceCleanup = { _, _ -> InstallSession.SourceCleanup() },
            confirmation: (PreparedPackage, InstallSession.Target, Long, () -> Unit) -> Unit = { _, _, _, _ ->
                throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Plugin confirmation UI is not connected")
            },
            availability: () -> Unit = {},
            userAction: (Intent) -> Unit = { UserActionLauncher.launch(context, it) },
        ): DescriptorInstallEnvironment {
            if (descriptors.isEmpty() || descriptors.size > InstallerContract.MAX_BATCH_SOURCES * InstallerContract.MAX_SPLITS_PER_PACKAGE) {
                throw RequestDocuments.invalid("Invalid source descriptor count")
            }
            val owned = mutableListOf<ParcelFileDescriptor>()
            try {
                descriptors.forEach { owned += ParcelFileDescriptor.dup(it.fileDescriptor) }
                return DescriptorInstallEnvironment(context, owned, confirmation, configuration, preparedListener, installed,
                    availability = availability, userAction = userAction)
            } catch (failure: Exception) {
                owned.forEach { runCatching { it.close() } }
                throw RequestDocuments.invalid("Package source descriptor is closed or invalid")
            }
        }
    }
}
