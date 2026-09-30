package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class SpikeStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        results[intent.action]?.offer(intent)
    }

    companion object {
        val results = ConcurrentHashMap<String, LinkedBlockingQueue<Intent>>()
    }
}
