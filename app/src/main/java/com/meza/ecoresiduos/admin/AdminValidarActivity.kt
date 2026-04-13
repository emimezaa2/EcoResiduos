package com.meza.ecoresiduos.admin

import android.content.ContentValues
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class AdminValidarActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_validar)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<TextView>(R.id.btnBackValidar)
        val container = findViewById<LinearLayout>(R.id.containerValidar)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        cargarTicketsPendientes(container)
    }

    private fun cargarTicketsPendientes(container: LinearLayout) {
        // Limpiamos el contenedor por si se recarga la vista
        container.removeAllViews()

        val db = dbHelper.readableDatabase

        // Hacemos un JOIN para obtener los datos del reporte Y el nombre del usuario
        val query = """
            SELECT r.${DatabaseHelper.COLUMN_REPORT_ID}, u.${DatabaseHelper.COLUMN_USER_NAME}, 
                   r.${DatabaseHelper.COLUMN_REPORT_PESO}, r.${DatabaseHelper.COLUMN_REPORT_TIPO}, 
                   u.${DatabaseHelper.COLUMN_USER_ID} 
            FROM ${DatabaseHelper.TABLE_REPORTS} r 
            INNER JOIN ${DatabaseHelper.TABLE_USERS} u 
            ON r.${DatabaseHelper.COLUMN_REPORT_USER_ID} = u.${DatabaseHelper.COLUMN_USER_ID} 
            WHERE r.${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Pendiente'
        """

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val reporteId = cursor.getInt(0)
                val userName = cursor.getString(1)
                val peso = cursor.getDouble(2)
                val tipo = cursor.getString(3)
                val userId = cursor.getInt(4)

                // 1. Crear Tarjeta Flotante
                val cardView = MaterialCardView(this)
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.radius = 24f
                cardView.cardElevation = 8f
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 32)
                cardView.layoutParams = params

                // 2. Layout interno de la tarjeta
                val internalLayout = LinearLayout(this)
                internalLayout.orientation = LinearLayout.VERTICAL
                internalLayout.setPadding(40, 40, 40, 40)

                // 3. Textos (Sin emojis, puro texto técnico)
                val tvHeader = TextView(this)
                tvHeader.text = "Ticket #$reporteId"
                tvHeader.setTextColor(Color.parseColor("#94A3B8"))
                tvHeader.textSize = 12f
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD)

                val tvUser = TextView(this)
                tvUser.text = "Usuario: $userName"
                tvUser.setTextColor(Color.parseColor("#0F172A"))
                tvUser.textSize = 18f
                tvUser.setTypeface(null, android.graphics.Typeface.BOLD)
                tvUser.setPadding(0, 8, 0, 0)

                val tvDetails = TextView(this)
                tvDetails.text = "Carga Declarada: $peso kg\nClasificación: $tipo"
                tvDetails.setTextColor(Color.parseColor("#64748B"))
                tvDetails.textSize = 14f
                tvDetails.setPadding(0, 8, 0, 24)

                // 4. Botones de Acción
                val btnLayout = LinearLayout(this)
                btnLayout.orientation = LinearLayout.HORIZONTAL
                btnLayout.gravity = Gravity.END

                val btnRechazar = Button(this)
                btnRechazar.text = "Rechazar"
                btnRechazar.setBackgroundColor(Color.parseColor("#FEF2F2")) // Rojo muy claro
                btnRechazar.setTextColor(Color.parseColor("#EF4444")) // Texto rojo oscuro
                val btnParamsR = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                btnParamsR.setMargins(0, 0, 16, 0)
                btnRechazar.layoutParams = btnParamsR

                val btnAprobar = Button(this)
                btnAprobar.text = "Aprobar"
                btnAprobar.setBackgroundColor(Color.parseColor("#10B981")) // Verde Éxito
                btnAprobar.setTextColor(Color.WHITE)
                val btnParamsA = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                btnAprobar.layoutParams = btnParamsA

                // --- LÓGICA DE LOS BOTONES ---

                btnRechazar.setOnClickListener {
                    actualizarEstado(reporteId, "Rechazado", userId, peso, false)
                    container.removeView(cardView) // Desaparece la tarjeta visualmente
                }

                btnAprobar.setOnClickListener {
                    actualizarEstado(reporteId, "Aprobado", userId, peso, true)
                    container.removeView(cardView) // Desaparece la tarjeta visualmente
                }

                // Ensamblar la tarjeta
                btnLayout.addView(btnRechazar)
                btnLayout.addView(btnAprobar)
                internalLayout.addView(tvHeader)
                internalLayout.addView(tvUser)
                internalLayout.addView(tvDetails)
                internalLayout.addView(btnLayout)
                cardView.addView(internalLayout)

                container.addView(cardView)

            } while (cursor.moveToNext())
        } else {
            // Si la bandeja está limpia
            val emptyMsg = TextView(this)
            emptyMsg.text = "Bandeja al día.\nNo hay registros pendientes de validación."
            emptyMsg.setTextColor(Color.parseColor("#64748B"))
            emptyMsg.textSize = 16f
            emptyMsg.gravity = Gravity.CENTER
            emptyMsg.setPadding(0, 100, 0, 0)
            container.addView(emptyMsg)
        }
        cursor.close()
    }

    private fun actualizarEstado(reporteId: Int, nuevoEstado: String, userId: Int, peso: Double, sumarKilos: Boolean) {
        val db = dbHelper.writableDatabase

        // 1. Actualizar el estado del reporte
        val valuesReport = ContentValues().apply {
            put(DatabaseHelper.COLUMN_REPORT_STATUS, nuevoEstado)
        }
        db.update(DatabaseHelper.TABLE_REPORTS, valuesReport, "${DatabaseHelper.COLUMN_REPORT_ID} = ?", arrayOf(reporteId.toString()))

        // 2. Si se aprueba, sumarle los kilos al usuario
        if (sumarKilos) {
            val cursorKilos = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
            if (cursorKilos.moveToFirst()) {
                val kilosActuales = cursorKilos.getDouble(0)
                val nuevosKilos = kilosActuales + peso

                val valuesUser = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_USER_KILOS, nuevosKilos)
                }
                db.update(DatabaseHelper.TABLE_USERS, valuesUser, "${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
            }
            cursorKilos.close()
            Toast.makeText(this, "Ticket Aprobado. Impacto sumado al usuario.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ticket Rechazado.", Toast.LENGTH_SHORT).show()
        }
    }
}