package com.ervareza.screentranslator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val error = intent.getStringExtra(EXTRA_ERROR) ?: "Unknown error"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        root.addView(
            TextView(this).apply {
                text = "Aplikasi mengalami error"
                textSize = 20f
                gravity = Gravity.CENTER
            },
        )
        root.addView(
            ScrollView(this).apply {
                addView(
                    TextView(this@CrashActivity).apply {
                        text = error
                        setTextIsSelectable(true)
                    },
                )
            },
            LinearLayout.LayoutParams(-1, 0, 1f),
        )
        val copy = Button(this).apply {
            text = "Salin Error"
            setOnClickListener {
                (
                    getSystemService(
                        CLIPBOARD_SERVICE,
                    ) as ClipboardManager
                    ).setPrimaryClip(ClipData.newPlainText("Crash", error))
            }
        }
        val share = Button(this).apply {
            text = "Bagikan"
            setOnClickListener {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, error)
                        },
                        "Bagikan error",
                    ),
                )
            }
        }
        root.addView(copy)
        root.addView(share)
        setContentView(root)
    }
    companion object {
        const val EXTRA_ERROR = "crash_error"
    }
}
