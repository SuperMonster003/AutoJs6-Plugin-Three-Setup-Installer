package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

/** A debug-only confirmation that can leave an untracked descendant above its result owner. */
class UserActionChildActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "User action lifecycle fixture" })
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_DESCENDANT, false)) {
            startActivity(Intent(this, UserActionChildActivity::class.java)
                .putExtra(EXTRA_FIXTURE_ID, intent.getStringExtra(EXTRA_FIXTURE_ID)))
        }
    }

    companion object {
        const val EXTRA_FIXTURE_ID = "userActionFixtureId"
        const val EXTRA_OPEN_DESCENDANT = "userActionOpenDescendant"
    }
}
