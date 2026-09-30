package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.app.Activity
import android.os.Bundle

/** Debug-only resolver target. Never installs or reads an incoming URI. */
class SpikeInstallerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
