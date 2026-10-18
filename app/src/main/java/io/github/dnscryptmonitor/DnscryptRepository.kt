package io.github.dnscryptmonitor

class DnscryptRepository(private val preferences: Preferences) {
    fun checkRoot(): ShellResult = RootShell.execute("id")

    fun checkFiles(): ShellResult = RootShell.execute(
        "test -r ${RootShell.quote(preferences.logPath)} && " +
            "test -w ${RootShell.quote(preferences.allowedPath)}",
    )

    fun recentDomains(): List<BlockedDomain> {
        // A bounded tail keeps large long-running logs responsive on mobile.
        val result = RootShell.execute("tail -n 10000 ${RootShell.quote(preferences.logPath)}", 45)
        if (!result.ok) return emptyList()
        val allowed = allowedDomains()
        val cutoff = System.currentTimeMillis() - preferences.recentHours * 3_600_000L
        val latest = linkedMapOf<String, BlockedDomain>()
        result.output.lineSequence().forEach { line ->
            LogParser.parse(line).forEach { item ->
                if (item.domain.lowercase() !in allowed) latest[item.domain] = item
            }
        }
        val inWindow = latest.values.filter { it.timestamp == 0L || it.timestamp >= cutoff }
        val source = if (inWindow.isEmpty()) latest.values else inWindow
        return source.sortedByDescending { it.timestamp }.take(preferences.recentLimit)
    }

    fun allowedDomains(): Set<String> {
        val result = RootShell.execute("cat ${RootShell.quote(preferences.allowedPath)}")
        if (!result.ok) return emptySet()
        return result.output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.lowercase() }
            .toSet()
    }

    fun allow(domain: String): ShellResult {
        val clean = domain.trim().lowercase()
        if (!isValidDomain(clean)) return ShellResult(-1, "Enter a valid domain")
        val command = "printf '%s\\n' ${RootShell.quote(clean)} >> ${RootShell.quote(preferences.allowedPath)}"
        val added = RootShell.execute(command)
        if (!added.ok || !preferences.restartAfterAllow) return added
        return restart()
    }

    fun restart(): ShellResult = RootShell.execute(preferences.restartCommand)

    fun toggleDns(): ShellResult {
        val target = !preferences.dnsEnabled
        val command = if (target) preferences.dnsEnableCommand else preferences.dnsDisableCommand
        val result = RootShell.execute(command)
        if (result.ok) preferences.dnsEnabled = target
        return result
    }

    fun clearLog(): ShellResult =
        RootShell.execute(": > ${RootShell.quote(preferences.logPath)}")

    private fun isValidDomain(domain: String): Boolean {
        if (domain.length !in 3..253 || domain.contains(' ') || !domain.contains('.')) return false
        return domain.split('.').all { label ->
            label.isNotEmpty() && label.length <= 63 &&
                label.first() != '-' && label.last() != '-' &&
                label.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        }
    }
}
