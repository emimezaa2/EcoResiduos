package com.meza.ecoresiduos.admin

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class AdminBitacoraActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bitacora)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<TextView>(R.id.btnBackBitacora)
        val container = findViewById<LinearLayout>(R.id.containerBitacora)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        cargarHistorialCompleto(container)
    }

    private fun cargarHistorialCompleto(container: LinearLayout) {
        val db = dbHelper.readableDatabase

        // Hacemos un JOIN para obtener todos los reportes y el nombre de quien lo hizo
        // Ordenamos por ID de forma descendente (DESC) para ver los más nuevos primero
        val query = """
            SELECT r.${DatabaseHelper.COLUMN_REPORT_ID}, u.${DatabaseHelper.COLUMN_USER_NAME}, 
                   r.${DatabaseHelper.COLUMN_REPORT_PESO}, r.${DatabaseHelper.COLUMN_REPORT_TIPO}, 
                   r.${DatabaseHelper.COLUMN_REPORT_STATUS} 
            FROM ${DatabaseHelper.TABLE_REPORTS} r 
            INNER JOIN ${DatabaseHelper.TABLE_USERS} u 
            ON r.${DatabaseHelper.COLUMN_REPORT_USER_ID} = u.${DatabaseHelper.COLUMN_USER_ID}
            ORDER BY r.${DatabaseHelper.COLUMN_REPORT_ID} DESC
        """

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val reporteId = cursor.getInt(0)
                val userName = cursor.getString(1)
                val peso = cursor.getDouble(2)
                val tipo = cursor.getString(3)
                val estado = cursor.getString(4)

                // Determinar colores basados en el estado
                val colorEstado = when (estado) {
                    "Aprobado" -> "#10B981" // Verde
                    "Rechazado" -> "#EF4444" // Rojo
                    else -> "#F59E0B" // Naranja (Pendiente)
                }

                val fondoEstado = when (estado) {
                    "Aprobado" -> "#ECFDF5"
                    "Rechazado" -> "#FEF2F2"
                    else -> "#FFFBEB"
                }

                // 1. Tarjeta Principal
                val cardView = MaterialCardView(this)
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.radius = 24f
                cardView.cardElevation = 2f
                cardView.strokeWidth = 1
                cardView.strokeColor = Color.parseColor("#E2E8F0")
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 24)
                cardView.layoutParams = params

                // 2. Layout Horizontal Interno
                val rowLayout = LinearLayout(this)
                rowLayout.orientation = LinearLayout.HORIZONTAL
                rowLayout.gravity = Gravity.CENTER_VERTICAL
                rowLayout.setPadding(32, 32, 32, 32)

                // 3. Indicador Visual de Estado (Un círculo de color puro)
                val statusIndicator = MaterialCardView(this)
                statusIndicator.setCardBackgroundColor(Color.parseColor(colorEstado))
                statusIndicator.radius = 24f
                statusIndicator.strokeWidth = 0
                val indicatorParams = LinearLayout.LayoutParams(48, 48) // Círculo pequeño
                statusIndicator.layoutParams = indicatorParams

                // 4. Datos del Registro (Centro)
                val textLayout = LinearLayout(this)
                textLayout.orientation = LinearLayout.VERTICAL
                val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textParams.setMargins(32, 0, 16, 0)
                textLayout.layoutParams = textParams

                val tvUser = TextView(this)
                tvUser.text = "Usuario: $userName"
                tvUser.setTextColor(Color.parseColor("#0F172A"))
                tvUser.textSize = 15f
                tvUser.setTypeface(null, android.graphics.Typeface.BOLD)

                val tvDetails = TextView(this)
                tvDetails.text = "ID: #$reporteId | $peso kg ($tipo)"
                tvDetails.setTextColor(Color.parseColor("#64748B"))
                tvDetails.textSize = 13f

                textLayout.addView(tvUser)
                textLayout.addView(tvDetails)

                // 5. Etiqueta de Estado (Derecha)
                val badgeCard = MaterialCardView(this)
                badgeCard.setCardBackgroundColor(Color.parseColor(fondoEstado))
                badgeCard.radius = 16f
                badgeCard.strokeWidth = 0

                val tvBadge = TextView(this)
                tvBadge.text = estado.uppercase()
                tvBadge.setTextColor(Color.parseColor(colorEstado))
                tvBadge.textSize = 10f
                tvBadge.setTypeface(null, android.graphics.Typeface.BOLD)
                tvBadge.setPadding(24, 12, 24, 12)
                badgeCard.addView(tvBadge)

                // Ensamblar la fila
                rowLayout.addView(statusIndicator)
                rowLayout.addView(textLayout)
                rowLayout.addView(badgeCard)

                cardView.addView(rowLayout)
                container.addView(cardView)

            } while (cursor.moveToNext())
        } else {
            // Si el historial está vacío
            val emptyMsg = TextView(this)
            emptyMsg.text = "La bitácora está limpia.\nAún no hay registros en el sistema."
            emptyMsg.setTextColor(Color.parseColor("#64748B"))
            emptyMsg.textSize = 16f
            emptyMsg.gravity = Gravity.CENTER
            emptyMsg.setPadding(0, 100, 0, 0)
            container.addView(emptyMsg)
        }
        cursor.close()
    }
}