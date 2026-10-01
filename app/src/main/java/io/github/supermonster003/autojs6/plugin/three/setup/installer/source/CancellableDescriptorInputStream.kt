package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import java.io.InputStream

/** Poll before reading pipes so a stalled host cannot hold a cancelled session indefinitely. */
internal class CancellableDescriptorInputStream(descriptor: ParcelFileDescriptor, private val checkActive: () -> Unit) : InputStream() {
    private val owned = ParcelFileDescriptor.dup(descriptor.fileDescriptor)
    private val input = ParcelFileDescriptor.AutoCloseInputStream(owned)
    private val poll = StructPollfd().apply { fd = owned.fileDescriptor; events = OsConstants.POLLIN.toShort() }

    override fun read(): Int {
        val byte = ByteArray(1)
        return if (read(byte, 0, 1) < 0) -1 else byte[0].toInt() and 0xff
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset > bytes.size - length) throw IndexOutOfBoundsException()
        if (length == 0) return 0
        while (true) {
            checkActive()
            try {
                if (Os.poll(arrayOf(poll), 100) == 0) continue
            } catch (failure: ErrnoException) {
                if (failure.errno == OsConstants.EINTR) continue
                throw java.io.IOException("Cannot poll package source", failure)
            }
            checkActive()
            return input.read(bytes, offset, length)
        }
    }

    override fun close() = input.close()
}
