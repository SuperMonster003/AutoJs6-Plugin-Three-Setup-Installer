package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess

internal object SourceDescriptors {
    fun validate(source: ParcelFileDescriptor?) {
        if (source == null) throw RequestDocuments.invalid("A source descriptor is missing")
        try {
            val fd = source.fileDescriptor
            val mode = Os.fstat(fd).st_mode
            if (!OsConstants.S_ISREG(mode) && !OsConstants.S_ISFIFO(mode)) throw RequestDocuments.invalid("Source must be a regular file or a pipe")
            if (HiddenApiAccess.accessMode(fd) != OsConstants.O_RDONLY) throw RequestDocuments.invalid("Source must be read-only")
        } catch (_: Exception) { throw RequestDocuments.invalid("Source descriptor is closed, writable or invalid") }
    }
}
