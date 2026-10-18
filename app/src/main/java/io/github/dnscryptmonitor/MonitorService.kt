package io.github.dnscryptmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.io.BufferedReader

class MonitorService : Service() {
    private lateinit var preferences: Preferences
    @Volatile private var running = false
    private var process: Process? = null
    private var worker: Thread? = null

    override fun onCreate() {
        super.onCreate()
        preferences = Preferences(this)
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            preferences.monitoringEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        preferences.monitoringEnabled = true
        startForeground(SERVICE_NOTIFICATION_ID, serviceNotification("Watching blocked-names.log"))
        if (!running) startWatcher()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        process?.destroy()
        worker?.interrupt()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWatcher() {
        running = true
        worker = Thread {
            var retry = 1_000L
            while (running) {
                try {
                    // Android's toybox tail supports -f, while -F is not universal.
                    val command = "tail -n 0 -f ${RootShell.quote(preferences.logPath)}"
                    process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
                    val reader: BufferedReader = process!!.inputStream.bufferedReader()
                    reader.lineSequence().takeWhile { running }.forEach(::handleLine)
                    retry = (retry * 2).coerceAtMost(60_000L)
                } catch (_: Exception) {
                    retry = (retry * 2).coerceAtMost(60_000L)
                }
                if (running) runCatching { Thread.sleep(retry) }
            }
        }.apply {
            name = "DnscryptLogWatcher"
            isDaemon = true
            start()
        }
    }

    private fun handleLine(line: String) {
        LogParser.parse(line).forEach { item ->
            if (!preferences.hasSeen(item.domain)) {
                preferences.markSeen(item.domain)
                showBlockedNotification(item)
            }
        }
    }

    private fun showBlockedNotification(item: BlockedDomain) {
        val allowIntent = Intent(this, ActionReceiver::class.java).apply {
            action = ActionReceiver.ACTION_ALLOW
            putExtra(ActionReceiver.EXTRA_DOMAIN, item.domain)
        }
        val allowPending = PendingIntent.getBroadcast(
            this,
            item.domain.hashCode(),
            allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openPending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val detail = item.pattern?.let { "Matched $it" } ?: "Blocked by dnscrypt-proxy"
        val notification = Notification.Builder(this, BLOCKED_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(item.domain)
            .setContentText(detail)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .addAction(Notification.Action.Builder(null, "Allow domain", allowPending).build())
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(item.domain.hashCode(), notification)
    }

    private fun serviceNotification(message: String): Notification {
        val openPending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPending = PendingIntent.getService(
            this,
            1,
            Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, SERVICE_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("DNSCrypt monitoring active")
            .setContentText(message)
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stopPending).build())
            .build()
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL,
                "Monitoring status",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                BLOCKED_CHANNEL,
                "Blocked domains",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when dnscrypt-proxy blocks a new domain"
                enableVibration(true)
            },
        )
    }

    companion object {
        const val ACTION_STOP = "io.github.dnscryptmonitor.STOP"
        private const val SERVICE_CHANNEL = "monitor_status"
        private const val BLOCKED_CHANNEL = "blocked_domains"
        private const val SERVICE_NOTIFICATION_ID = 100
    }
}
