package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences

/** The same persisted defaults consumed by both the local entry points and the settings page. */
internal object InstallDefaults {
    fun options(context: Context) = InstallerPreferences.read(context).options
    fun interaction(context: Context) = InstallerPreferences.read(context).interaction
}
