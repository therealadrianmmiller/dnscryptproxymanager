package io.github.dnscryptmonitor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var preferences: Preferences
    private lateinit var repository: DnscryptRepository
    private lateinit var content: LinearLayout
    private lateinit var monitoringButton: android.widget.Button
    private lateinit var dnsButton: android.widget.Button
    private lateinit var rootStatus: TextView
    private lateinit var recentList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = Preferences(this)
        repository = DnscryptRepository(preferences)
        window.statusBarColor = Ui.surface
        window.navigationBarColor = Ui.surface
        buildScreen()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updateStateLabels()
        checkStatus()
        loadRecent()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Ui.surface)
            isFillViewport = true
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(40))
        }
        scroll.addView(content)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titles.addView(TextView(this).apply {
            text = "DNSCRYPT"
            textStyle(12f, Ui.accent, true)
            letterSpacing = .18f
        })
        titles.addView(TextView(this).apply {
            text = "Local monitor"
            textStyle(30f, Ui.ink, true)
        })
        header.addView(titles)
        header.addView(Ui.button(this, "Settings").apply {
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        })
        content.addView(header)

        val statusCard = Ui.card(this)
        val statusTop = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val statusText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusText.addView(TextView(this).apply {
            text = "Protection status"
            textStyle(18f, Ui.ink, true)
        })
        rootStatus = TextView(this).apply {
            text = "Checking root access…"
            textStyle(13f, Ui.muted)
        }
        statusText.addView(rootStatus)
        statusTop.addView(statusText)
        val dot = TextView(this).apply {
            text = "●"
            textStyle(24f, Ui.accent)
        }
        statusTop.addView(dot)
        statusCard.addView(statusTop)
        statusCard.addView(divider())
        monitoringButton = Ui.button(this, "", true).apply {
            setOnClickListener { toggleMonitoring() }
        }
        statusCard.addView(monitoringButton)
        content.addView(statusCard)

        val controls = Ui.card(this)
        controls.addView(TextView(this).apply {
            text = "QUICK CONTROLS"
            textStyle(12f, Ui.muted, true)
            letterSpacing = .12f
        })
        controls.addView(space(12))
        dnsButton = Ui.button(this, "").apply {
            setOnClickListener { runAction("Changing DNS…") { repository.toggleDns() } }
        }
        controls.addView(dnsButton)
        controls.addView(space(8))
        controls.addView(Ui.button(this, "Restart dnscrypt-proxy").apply {
            setOnClickListener { runAction("Restarting service…") { repository.restart() } }
        })
        content.addView(controls)

        val allowCard = Ui.card(this)
        allowCard.addView(TextView(this).apply {
            text = "Allow a domain"
            textStyle(18f, Ui.ink, true)
        })
        allowCard.addView(TextView(this).apply {
            text = "Adds an exact domain to allowed-names.txt"
            textStyle(13f, Ui.muted)
        })
        allowCard.addView(space(12))
        val input = EditText(this).apply {
            hint = "example.com"
            setHintTextColor(Ui.muted)
            setTextColor(Ui.ink)
            setSingleLine()
            setPadding(dp(14), 0, dp(14), 0)
            rounded(Ui.raisedAlt, 14)
            minHeight = dp(52)
        }
        allowCard.addView(input)
        allowCard.addView(space(10))
        allowCard.addView(Ui.button(this, "Add to allowlist", true).apply {
            setOnClickListener {
                val domain = input.text.toString()
                runAction("Allowing $domain…", onSuccess = {
                    input.text.clear()
                    loadRecent()
                }) { repository.allow(domain) }
            }
        })
        content.addView(allowCard)

        val recentHeader = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, dp(10), 2, dp(12))
        }
        recentHeader.addView(TextView(this).apply {
            text = "Recent blocks"
            textStyle(21f, Ui.ink, true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        recentHeader.addView(Ui.button(this, "Refresh").apply {
            setOnClickListener { loadRecent() }
        })
        content.addView(recentHeader)
        recentList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(recentList)
        content.addView(Ui.button(this, "Reset notification history").apply {
            setOnClickListener {
                preferences.clearSeen()
                toast("Previously seen domains will notify again")
            }
        })
    }

    private fun updateStateLabels() {
        monitoringButton.text =
            if (preferences.monitoringEnabled) "Stop monitoring" else "Start monitoring"
        dnsButton.text =
            if (preferences.dnsEnabled) "Disable local DNS" else "Enable local DNS"
    }

    private fun toggleMonitoring() {
        val intent = Intent(this, MonitorService::class.java)
        if (preferences.monitoringEnabled) {
            intent.action = MonitorService.ACTION_STOP
            startService(intent)
        } else {
            startForegroundService(intent)
        }
        preferences.monitoringEnabled = !preferences.monitoringEnabled
        updateStateLabels()
    }

    private fun checkStatus() {
        thread {
            val root = repository.checkRoot()
            val files = if (root.ok) repository.checkFiles() else ShellResult(-1, "")
            runOnUiThread {
                rootStatus.text = when {
                    !root.ok -> "Root unavailable — grant access in your root manager"
                    !files.ok -> "Root ready — check the file path in Settings"
                    else -> "Root ready · local files accessible"
                }
                rootStatus.setTextColor(if (root.ok && files.ok) Ui.accent else Ui.danger)
            }
        }
    }

    private fun loadRecent() {
        recentList.removeAllViews()
        recentList.addView(TextView(this).apply {
            text = "Reading blocked-names.log…"
            textStyle(14f, Ui.muted)
            setPadding(dp(4), dp(10), 0, dp(20))
        })
        thread {
            val domains = repository.recentDomains()
            runOnUiThread {
                recentList.removeAllViews()
                if (domains.isEmpty()) {
                    recentList.addView(Ui.card(this).apply {
                        addView(TextView(this@MainActivity).apply {
                            text = "No recent blocked domains"
                            textStyle(15f, Ui.muted)
                        })
                    })
                } else {
                    domains.forEach { recentList.addView(domainRow(it)) }
                }
            }
        }
    }

    private fun domainRow(item: BlockedDomain): View {
        val row = Ui.card(this)
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        labels.addView(TextView(this).apply {
            text = item.domain
            textStyle(15f, Ui.ink, true)
        })
        labels.addView(TextView(this).apply {
            text = item.pattern?.let { "Wildcard · $it" } ?: "Exact match"
            textStyle(12f, if (item.pattern == null) Ui.muted else Ui.danger)
        })
        top.addView(labels)
        top.addView(Ui.button(this, "Allow").apply {
            setOnClickListener {
                runAction("Allowing ${item.domain}…", onSuccess = ::loadRecent) {
                    repository.allow(item.domain)
                }
            }
        })
        row.addView(top)
        return row
    }

    private fun runAction(
        progress: String,
        onSuccess: () -> Unit = {},
        action: () -> ShellResult,
    ) {
        toast(progress)
        thread {
            val result = action()
            runOnUiThread {
                if (result.ok) {
                    toast("Done")
                    updateStateLabels()
                    onSuccess()
                } else {
                    toast(result.output.ifBlank { "Command failed (${result.exitCode})" })
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Ui.raisedAlt)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            setMargins(0, dp(18), 0, dp(18))
        }
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

}
