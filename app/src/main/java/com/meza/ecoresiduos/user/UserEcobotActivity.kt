package com.meza.ecoresiduos.user

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.bot.EcoBotEngine

class UserEcobotActivity : AppCompatActivity() {

    private lateinit var motorBot: EcoBotEngine
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollChat: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Conectado a tu archivo XML original
        setContentView(R.layout.activity_user_ecobot)

        motorBot = EcoBotEngine(this)

        chatContainer = findViewById(R.id.chatContainer)
        scrollChat = findViewById(R.id.scrollChat)
        val etMensajeBot = findViewById<EditText>(R.id.etMensajeBot)
        val btnEnviarBot = findViewById<MaterialCardView>(R.id.btnEnviarBot)
        val btnBackBot = findViewById<TextView>(R.id.btnBackBot)

        btnBackBot.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Mensaje inicial del bot
        agregarBurbujaChat("¡Hola! Soy EcoBot. ¿En qué te puedo ayudar hoy con tus residuos?", esUsuario = false)

        btnEnviarBot.setOnClickListener {
            val mensajeUsuario = etMensajeBot.text.toString().trim()

            if (mensajeUsuario.isNotEmpty()) {
                // Usuario pregunta
                agregarBurbujaChat(mensajeUsuario, esUsuario = true)
                etMensajeBot.text.clear()

                // Bot piensa y responde
                val respuestaDelBot = motorBot.procesarMensaje(mensajeUsuario)
                scrollChat.postDelayed({
                    agregarBurbujaChat(respuestaDelBot, esUsuario = false)
                }, 500)
            }
        }
    }

    private fun agregarBurbujaChat(mensaje: String, esUsuario: Boolean) {
        val fila = LinearLayout(this)
        fila.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        fila.orientation = LinearLayout.HORIZONTAL
        fila.gravity = if (esUsuario) Gravity.END else Gravity.START
        fila.setPadding(0, 8, 0, 8)

        val burbuja = MaterialCardView(this)
        burbuja.radius = 32f
        burbuja.cardElevation = 2f
        burbuja.strokeWidth = 0

        if (esUsuario) {
            burbuja.setCardBackgroundColor(Color.parseColor("#10B981"))
        } else {
            burbuja.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        val tvMensaje = TextView(this)
        tvMensaje.text = mensaje
        tvMensaje.textSize = 15f
        tvMensaje.setPadding(36, 24, 36, 24)

        if (esUsuario) {
            tvMensaje.setTextColor(Color.WHITE)
        } else {
            tvMensaje.setTextColor(Color.parseColor("#0F172A"))
        }

        burbuja.addView(tvMensaje)
        fila.addView(burbuja)
        chatContainer.addView(fila)

        scrollChat.post {
            scrollChat.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}