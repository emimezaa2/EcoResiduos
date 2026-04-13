package com.meza.ecoresiduos.user

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meza.ecoresiduos.R

class UserEcobotActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_ecobot)

        val btnBack = findViewById<TextView>(R.id.btnBackBot)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<FloatingActionButton>(R.id.btnSend)

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Mensaje inicial del Asistente al abrir la pantalla
        addMessageToChat("Hola. Soy tu Eco Asistente Virtual. ¿Qué material deseas clasificar hoy?", false)

        btnSend.setOnClickListener {
            val userText = etMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                // 1. Dibujar burbuja verde (Usuario)
                addMessageToChat(userText, true)
                etMessage.text.clear()

                // 2. Simular respuesta del bot con un pequeño retraso
                chatContainer.postDelayed({
                    addMessageToChat("De acuerdo. La mayoría de los materiales tipo '$userText' están clasificados como residuos estándar. Te sugiero depositarlos en el contenedor correspondiente de tu zona.", false)
                }, 800) // 800 milisegundos de espera
            }
        }
    }

    // Esta función genera las burbujas Premium de forma dinámica
    private fun addMessageToChat(message: String, isUser: Boolean) {
        val rowLayout = LinearLayout(this)
        rowLayout.orientation = LinearLayout.HORIZONTAL
        rowLayout.gravity = if (isUser) Gravity.END else Gravity.START
        val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        rowParams.setMargins(0, 0, 0, 24)
        rowLayout.layoutParams = rowParams

        val cardView = MaterialCardView(this)
        // Verde primario para el usuario, blanco para el bot
        cardView.setCardBackgroundColor(if (isUser) Color.parseColor("#15803D") else Color.WHITE)
        cardView.radius = 48f
        cardView.cardElevation = if (isUser) 0f else 6f
        cardView.strokeWidth = 0

        val textView = TextView(this)
        textView.text = message
        textView.setTextColor(if (isUser) Color.WHITE else Color.parseColor("#0F172A"))
        textView.textSize = 15f
        textView.setPadding(48, 32, 48, 32)
        textView.maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()

        cardView.addView(textView)
        rowLayout.addView(cardView)
        chatContainer.addView(rowLayout)

        chatScrollView.post {
            chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}