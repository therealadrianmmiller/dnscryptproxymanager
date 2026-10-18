package io.github.dnscryptmonitor

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var preferences: Preferences
    private lateinit var basePath: EditText
    private lateinit var restartCommand: EditText
    private lateinit var dnsOnCommand: EditText
    private lateinit var dnsOffCommand: EditText
    private lateinit var recentHours: EditText
    private lateinit var recentLimit: EditText
    private lateinit var bootSwitch: Switch
    private lateinit var restartSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = Preferences(this)
        window.statusBarColor = Ui.surface
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Ui.surface)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(40))
        }
        scroll.addView(content)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        header.addView(TextView(this).apply {
            text = "Settings"
            textStyle(30f, Ui.ink, true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(Ui.button(this, "Close").apply { setOnClickListener { finish() } })
        content.addView(header)

        content.addView(sectionLabel("LOCAL FILES"))
        basePath = field(content, "DNSCrypt base folder", preferences.basePath)
        content.addView(help("The folder must contain blocked-names.log, blocked-names.txt and allowed-names.txt. Root access is used; no remote connection is made."))

        content.addView(sectionLabel("ROOT COMMANDS"))
        restartCommand = field(content, "Restart / reload command", preferences.restartCommand, multiline = true)
        dnsOnCommand = field(content, "Enable local DNS command", preferences.dnsEnableCommand, multiline = true)
        dnsOffCommand = field(content, "Disable local DNS command", preferences.dnsDisableCommand, multiline = true)
        content.addView(help("Commands run locally through su -c. Defaults are conservative placeholders; match these to your dnscrypt-proxy installation."))

        content.addView(sectionLabel("MONITORING"))
        recentHours = field(content, "Recent domain age (hours)", preferences.recentHours.toString(), number = true)
        recentLimit = field(content, "Maximum domains", preferences.recentLimit.toString(), number = true)
        bootSwitch = switchRow(content, "Resume monitoring after boot", preferences.startOnBoot)
        restartSwitch = switchRow(content, "Restart after allowing a domain", preferences.restartAfterAllow)

        content.addView(space(10))
        content.addView(Ui.button(this, "Save settings", true).apply {
            setOnClickListener { save() }
        })
        content.addView(space(10))
        content.addView(Ui.button(this, "Clear blocked log").apply {
            setOnClickListener {
                Thread {
                    val result = DnscryptRepository(preferences).clearLog()
                    runOnUiThread {
                        Toast.makeText(
                            this@SettingsActivity,
                            if (result.ok) "Blocked log cleared" else "Could not clear log",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }.start()
            }
        })
    }

    private fun save() {
        preferences.basePath = basePath.text.toString().trim()
        preferences.restartCommand = restartCommand.text.toString().trim()
        preferences.dnsEnableCommand = dnsOnCommand.text.toString().trim()
        preferences.dnsDisableCommand = dnsOffCommand.text.toString().trim()
        preferences.recentHours = recentHours.text.toString().toIntOrNull()?.coerceIn(1, 720) ?: 12
        preferences.recentLimit = recentLimit.text.toString().toIntOrNull()?.coerceIn(1, 500) ?: 50
        preferences.startOnBoot = bootSwitch.isChecked
        preferences.restartAfterAllow = restartSwitch.isChecked
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun sectionLabel(label: String) = TextView(this).apply {
        text = label
        textStyle(12f, Ui.accent, true)
        letterSpacing = .14f
        setPadding(2, dp(18), 0, dp(8))
    }

    private fun field(
        parent: LinearLayout,
        label: String,
        value: String,
        multiline: Boolean = false,
        number: Boolean = false,
    ): EditText {
        parent.addView(TextView(this).apply {
            text = label
            textStyle(13f, Ui.muted, true)
            setPadding(2, dp(8), 0, dp(6))
        })
        return EditText(this).apply {
            setText(value)
            setTextColor(Ui.ink)
            setHintTextColor(Ui.muted)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            rounded(Ui.raised, 14)
            minHeight = dp(if (multiline) 76 else 52)
            setSingleLine(!multiline)
            inputType = when {
                number -> InputType.TYPE_CLASS_NUMBER
                multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else -> InputType.TYPE_CLASS_TEXT
            }
            parent.addView(this)
        }
    }

    private fun switchRow(parent: LinearLayout, label: String, checked: Boolean): Switch {
        return Switch(this).apply {
            text = label
            isChecked = checked
            textStyle(15f, Ui.ink)
            setPadding(dp(14), dp(8), dp(8), dp(8))
            rounded(Ui.raised, 14)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(0, dp(10), 0, 0) }
            parent.addView(this)
        }
    }

    private fun help(textValue: String) = TextView(this).apply {
        text = textValue
        textStyle(12f, Ui.muted)
        setPadding(3, dp(8), 3, dp(8))
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

}
