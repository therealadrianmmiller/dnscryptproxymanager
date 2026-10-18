package io.github.dnscryptmonitor

import android.content.Context

class Preferences(context: Context) {
    private val store = context.getSharedPreferences("dnscrypt_monitor", Context.MODE_PRIVATE)

    var basePath: String
        get() = store.getString(KEY_BASE_PATH, DEFAULT_BASE_PATH) ?: DEFAULT_BASE_PATH
        set(value) = store.edit().putString(KEY_BASE_PATH, value.trimEnd('/')).apply()

    val logPath get() = "$basePath/blocked-names.log"
    val allowedPath get() = "$basePath/allowed-names.txt"
    val blockedPath get() = "$basePath/blocked-names.txt"

    var restartCommand: String
        get() = store.getString(KEY_RESTART, DEFAULT_RESTART) ?: DEFAULT_RESTART
        set(value) = store.edit().putString(KEY_RESTART, value).apply()

    var dnsEnableCommand: String
        get() = store.getString(KEY_DNS_ON, DEFAULT_DNS_ON) ?: DEFAULT_DNS_ON
        set(value) = store.edit().putString(KEY_DNS_ON, value).apply()

    var dnsDisableCommand: String
        get() = store.getString(KEY_DNS_OFF, DEFAULT_DNS_OFF) ?: DEFAULT_DNS_OFF
        set(value) = store.edit().putString(KEY_DNS_OFF, value).apply()

    var dnsEnabled: Boolean
        get() = store.getBoolean(KEY_DNS_ENABLED, true)
        set(value) = store.edit().putBoolean(KEY_DNS_ENABLED, value).apply()

    var monitoringEnabled: Boolean
        get() = store.getBoolean(KEY_MONITORING, false)
        set(value) = store.edit().putBoolean(KEY_MONITORING, value).apply()

    var startOnBoot: Boolean
        get() = store.getBoolean(KEY_BOOT, true)
        set(value) = store.edit().putBoolean(KEY_BOOT, value).apply()

    var restartAfterAllow: Boolean
        get() = store.getBoolean(KEY_RESTART_AFTER_ALLOW, true)
        set(value) = store.edit().putBoolean(KEY_RESTART_AFTER_ALLOW, value).apply()

    var recentHours: Int
        get() = store.getInt(KEY_RECENT_HOURS, 12)
        set(value) = store.edit().putInt(KEY_RECENT_HOURS, value).apply()

    var recentLimit: Int
        get() = store.getInt(KEY_RECENT_LIMIT, 50)
        set(value) = store.edit().putInt(KEY_RECENT_LIMIT, value).apply()

    fun hasSeen(domain: String): Boolean = store.getStringSet(KEY_SEEN, emptySet())
        ?.contains(domain.lowercase()) == true

    fun markSeen(domain: String) {
        val updated = store.getStringSet(KEY_SEEN, emptySet()).orEmpty().toMutableSet()
        updated += domain.lowercase()
        store.edit().putStringSet(KEY_SEEN, updated).apply()
    }

    fun clearSeen() = store.edit().remove(KEY_SEEN).apply()

    companion object {
        const val DEFAULT_BASE_PATH = "/data/adb/dnscrypt-proxy"
        const val DEFAULT_RESTART = "pkill -HUP dnscrypt-proxy"
        const val DEFAULT_DNS_ON =
            "settings put global private_dns_mode off; setprop net.dns1 127.0.0.1"
        const val DEFAULT_DNS_OFF =
            "setprop net.dns1 1.1.1.1; setprop net.dns2 1.0.0.1"

        private const val KEY_BASE_PATH = "base_path"
        private const val KEY_RESTART = "restart_command"
        private const val KEY_DNS_ON = "dns_enable_command"
        private const val KEY_DNS_OFF = "dns_disable_command"
        private const val KEY_DNS_ENABLED = "dns_enabled"
        private const val KEY_MONITORING = "monitoring"
        private const val KEY_BOOT = "start_on_boot"
        private const val KEY_RESTART_AFTER_ALLOW = "restart_after_allow"
        private const val KEY_RECENT_HOURS = "recent_hours"
        private const val KEY_RECENT_LIMIT = "recent_limit"
        private const val KEY_SEEN = "seen_domains"
    }
}
