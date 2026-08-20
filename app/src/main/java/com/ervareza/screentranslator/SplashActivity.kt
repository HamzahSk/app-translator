package com.ervareza.screentranslator

import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // ISSUE-013 FIX: Lightweight VectorDrawable-based splash instead of a heavy MP4.
        val logo = findViewById<ImageView>(R.id.splashLogo)
        val animDrawable = ContextCompat.getDrawable(this, R.drawable.ic_splash_logo_anim)
        logo.setImageDrawable(animDrawable)
        (animDrawable as? Animatable)?.start()

        // Tap anywhere to skip splash
        findViewById<View>(R.id.splashRoot).setOnClickListener { goToMain() }

        // Auto-navigate once the animation finishes
        logo.postDelayed({ goToMain() }, 1800L)
    }

    private fun goToMain() {
        if (!navigated) {
            navigated = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}