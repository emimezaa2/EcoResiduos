package com.meza.ecoresiduos // Ajusta tu paquete

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.meza.ecoresiduos.auth.LoginActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animación de aparición (Fade In)
        val container = findViewById<LinearLayout>(R.id.logoContainer)
        val animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = 1500 // 1.5 segundos de animación
        container.startAnimation(animation)

        // Espera 3 segundos y salta al Login
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 3000)
    }
}