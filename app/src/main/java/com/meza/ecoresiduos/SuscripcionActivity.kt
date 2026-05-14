package com.meza.ecoresiduos.user // Cambia por tu paquete real

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.meza.ecoresiduos.R

class SuscripcionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suscripcion) // Conecta con el diseño que hicimos antes

        // Botón para volver atrás
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Botón de compra simulada (El Efecto Wow de monetización)
        findViewById<MaterialButton>(R.id.btnSuscribirse).setOnClickListener {
            Toast.makeText(this, "¡Suscripción Eco Pro activada con éxito!", Toast.LENGTH_LONG).show()
            // Aquí en el futuro cambiaremos la variable isPremium a true en Firebase
            finish()
        }
    }
}