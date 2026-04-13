package com.meza.ecoresiduos.admin

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class AdminComunidadActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_comunidad)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<TextView>(R.id.btnBackComunidad)
        val container = findViewById<LinearLayout>(R.id.containerComunidad)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        cargarDirectorio(container)
    }

    private fun cargarDirectorio(container: LinearLayout) {
        val db = dbHelper.readableDatabase

        // Buscamos a todos los usuarios que NO sean administradores, ordenados por los que tienen más kilos
        val query = "SELECT ${DatabaseHelper.COLUMN_USER_NAME}, ${DatabaseHelper.COLUMN_USER_EMAIL}, ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ROLE} != 'admin' ORDER BY ${DatabaseHelper.COLUMN_USER_KILOS} DESC"

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0)
                val email = cursor.getString(1)
                val kilos = cursor.getDouble(2)

                // 1. Tarjeta Principal Flotante
                val cardView = MaterialCardView(this)
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.radius = 24f
                cardView.cardElevation = 4f
                cardView.strokeWidth = 1
                cardView.strokeColor = Color.parseColor("#E2E8F0") // Borde sutil
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 24) // Separación entre tarjetas
                cardView.layoutParams = params

                // 2. Layout Horizontal Interno
                val rowLayout = LinearLayout(this)
                rowLayout.orientation = LinearLayout.HORIZONTAL
                rowLayout.gravity = Gravity.CENTER_VERTICAL
                rowLayout.setPadding(32, 32, 32, 32)

                // 3. Avatar del Usuario (Círculo morado claro)
                val avatarCard = MaterialCardView(this)
                avatarCard.setCardBackgroundColor(Color.parseColor("#F3E8FF"))
                avatarCard.radius = 50f
                avatarCard.strokeWidth = 0
                val avatarParams = LinearLayout.LayoutParams(100, 100) // Tamaño fijo para el círculo
                avatarCard.layoutParams = avatarParams

                val avatarIcon = ImageView(this)
                avatarIcon.setImageResource(R.drawable.ic_profile)
                avatarIcon.setColorFilter(Color.parseColor("#9333EA")) // Icono morado oscuro
                avatarIcon.setPadding(20, 20, 20, 20)
                avatarCard.addView(avatarIcon)

                // 4. Datos del Usuario (Centro)
                val textLayout = LinearLayout(this)
                textLayout.orientation = LinearLayout.VERTICAL
                val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textParams.setMargins(24, 0, 16, 0)
                textLayout.layoutParams = textParams

                val tvName = TextView(this)
                tvName.text = nombre
                tvName.setTextColor(Color.parseColor("#0F172A"))
                tvName.textSize = 16f
                tvName.setTypeface(null, android.graphics.Typeface.BOLD)

                val tvEmail = TextView(this)
                tvEmail.text = email
                tvEmail.setTextColor(Color.parseColor("#64748B"))
                tvEmail.textSize = 12f

                textLayout.addView(tvName)
                textLayout.addView(tvEmail)

                // 5. Impacto en Kg (Derecha)
                val kgLayout = LinearLayout(this)
                kgLayout.orientation = LinearLayout.VERTICAL
                kgLayout.gravity = Gravity.END

                val tvKilos = TextView(this)
                tvKilos.text = "${String.format("%.1f", kilos)} kg"
                tvKilos.setTextColor(Color.parseColor("#10B981")) // Verde esmeralda brillante
                tvKilos.textSize = 18f
                tvKilos.setTypeface(null, android.graphics.Typeface.BOLD)

                val tvLabel = TextView(this)
                tvLabel.text = "Acumulado"
                tvLabel.setTextColor(Color.parseColor("#94A3B8"))
                tvLabel.textSize = 10f

                kgLayout.addView(tvKilos)
                kgLayout.addView(tvLabel)

                // Ensamblar la fila
                rowLayout.addView(avatarCard)
                rowLayout.addView(textLayout)
                rowLayout.addView(kgLayout)

                cardView.addView(rowLayout)
                container.addView(cardView)

            } while (cursor.moveToNext())
        } else {
            // Si no hay usuarios aún
            val emptyMsg = TextView(this)
            emptyMsg.text = "La comunidad está vacía.\nAún no hay usuarios registrados."
            emptyMsg.setTextColor(Color.parseColor("#64748B"))
            emptyMsg.textSize = 16f
            emptyMsg.gravity = Gravity.CENTER
            emptyMsg.setPadding(0, 100, 0, 0)
            container.addView(emptyMsg)
        }
        cursor.close()
    }
}