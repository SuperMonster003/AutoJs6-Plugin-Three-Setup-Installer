package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import org.autojs.plugin.installer.api.InstallerContract
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A confirmation draft belongs to the session, so replacing the Activity never submits it. */
internal class InstallChoices(
    initialOptions: InstallOptions,
    apks: List<Apk>,
    private val canDeleteSource: Boolean,
) {
    data class Apk(val name: String, val base: Boolean)
    data class Choice(val options: InstallOptions, val selectedApkNames: Set<String>)

    private val available = apks.associateBy { it.name }
    private val selected = apks.mapTo(linkedSetOf()) { it.name }
    private var options = initialOptions
    private var userDraft = initialOptions.user

    init {
        require(apks.isNotEmpty() && apks.count { it.base } == 1)
        require(available.size == apks.size)
    }

    @Synchronized fun snapshot(): Choice = Choice(options, selected.toSet())
    @Synchronized fun userInput(): String = userDraft
    @Synchronized fun valid(): Boolean = isUser(userDraft)

    @Synchronized fun setUser(value: String) {
        userDraft = value
        if (isUser(value)) options = options.copy(user = value)
    }

    @Synchronized fun updateOptions(value: InstallOptions) {
        // A host descriptor conveys no authority to delete the host's original file. Preserve
        // its original request value, but never offer a new promise of deletion from this UI.
        if (value.user != options.user) userDraft = value.user
        options = if (canDeleteSource) value else value.copy(deleteSource = options.deleteSource)
    }

    @Synchronized fun select(name: String, checked: Boolean): Boolean {
        val apk = available[name] ?: return false
        if (apk.base && !checked) return false
        if (checked) selected += name else selected -= name
        return true
    }

    private fun isUser(value: String) = value == InstallerContract.USER_CURRENT || value == InstallerContract.USER_ALL || value.toIntOrNull()?.let { it >= 0 } == true
}

/** Exactly one action wins, including a click racing cancellation or Activity destruction. */
internal class InstallDecision<T> {
    private val ready = CountDownLatch(1)
    @Volatile private var answered = false
    @Volatile private var value: T? = null

    @Synchronized fun answer(value: T?): Boolean {
        if (answered) return false
        this.value = value
        answered = true
        ready.countDown()
        return true
    }

    fun await(timeoutMillis: Long): Boolean = ready.await(timeoutMillis, TimeUnit.MILLISECONDS)
    fun result(): T? = value
}
