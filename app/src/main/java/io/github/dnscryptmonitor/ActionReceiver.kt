package io.github.dnscryptmonitor

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALLOW) return
        val domain = intent.getStringExtra(EXTRA_DOMAIN) ?: return
        val pending = goAsync()
        Thread {
            val result = DnscryptRepository(Preferences(context)).allow(domain)
            context.getSystemService(NotificationManager::class.java).cancel(domain.hashCode())
            val text = if (result.ok) "$domain allowed" else "Could not allow $domain"
            android.os.Handler(context.mainLooper).post {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
            pending.finish()
        }.start()
    }

    companion object {
        const val ACTION_ALLOW = "io.github.dnscryptmonitor.ALLOW"
        const val EXTRA_DOMAIN = "domain"
    }
}
