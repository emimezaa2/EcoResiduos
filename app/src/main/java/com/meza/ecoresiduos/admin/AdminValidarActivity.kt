package com.meza.ecoresiduos.admin

import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
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

        // Usamos finish() para volver de forma limpia a la pantalla anterior
        btnBack.setOnClickListener { finish() }

        cargarTicketsPendientes(container)
    }

    private fun cargarTicketsPendientes(container: LinearLayout) {
        container.removeAllViews()
        val db = dbHelper.readableDatabase

        // Consulta unificada: Trae reportes pendientes y el nombre de quién lo hizo
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

                // 1. Tarjeta principal
                val cardView = MaterialCardView(this).apply {
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, 32)
                    layoutParams = params
                    setCardBackgroundColor(Color.WHITE)
                    radius = 24f
                    cardElevation = 4f
                }

                // 2. Contenedor interno con padding
                val internalLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 40, 40, 40)
                }

                // 3. Textos formateados
                val tvHeader = TextView(this).apply {
                    text = "Ticket #$reporteId"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                }

                val tvUser = TextView(this).apply {
                    text = "Usuario: $userName"
                    setTextColor(Color.parseColor("#0F172A"))
                    textSize = 18f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, 8, 0, 0)
                }

                val tvDetails = TextView(this).apply {
                    text = "Carga Declarada: $peso kg\nClasificación: $tipo"
                    setTextColor(Color.parseColor("#64748B"))
                    textSize = 14f
                    setPadding(0, 8, 0, 24)
                }

                // 4. Botones alineados a la derecha
                val btnLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val btnRechazar = MaterialButton(this).apply {
                    text = "Rechazar"
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2"))
                    setTextColor(Color.parseColor("#EF4444"))
                    val btnParamsR = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    btnParamsR.setMargins(0, 0, 16, 0)
                    layoutParams = btnParamsR
                }

                val btnAprobar = MaterialButton(this).apply {
                    text = "Aprobar"
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                // 5. Lógica de clics (Desaparece la tarjeta tras presionar)
                btnRechazar.setOnClickListener {
                    actualizarEstado(reporteId, "Rechazado", userId, peso, false)
                    container.removeView(cardView)
                }

                btnAprobar.setOnClickListener {
                    actualizarEstado(reporteId, "Aprobado", userId, peso, true)
                    container.removeView(cardView)
                }

                // 6. Ensamblaje final
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
            // Mensaje si no hay nada que aprobar
            val emptyMsg = TextView(this).apply {
                text = "Bandeja al día.\nNo hay registros pendientes de validación."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 0)
            }
            container.addView(emptyMsg)
        }
        cursor.close()
    }

    // ==========================================
    // FUNCIÓN CENTRALIZADA (Reemplaza a aprobarReporte y rechazarReporte)
    // ==========================================
    private fun actualizarEstado(reporteId: Int, nuevoEstado: String, userId: Int, peso: Double, sumarKilos: Boolean) {
        val db = dbHelper.writableDatabase

        // Iniciamos la transacción para proteger la base de datos
        db.beginTransaction()
        try {
            // 1. Cambiar estado en la tabla de reportes
            val valuesReport = ContentValues().apply { put(DatabaseHelper.COLUMN_REPORT_STATUS, nuevoEstado) }
            db.update(DatabaseHelper.TABLE_REPORTS, valuesReport, "${DatabaseHelper.COLUMN_REPORT_ID} = ?", arrayOf(reporteId.toString()))

            // 2. Si es Aprobado, sumamos el peso al perfil del usuario
            if (sumarKilos) {
                db.execSQL("UPDATE ${DatabaseHelper.TABLE_USERS} SET ${DatabaseHelper.COLUMN_USER_KILOS} = ${DatabaseHelper.COLUMN_USER_KILOS} + ? WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(peso, userId))
            }

            // Confirmamos que todo salió bien
            db.setTransactionSuccessful()

            // Mostramos feedback visual al Administrador
            val mensaje = if (sumarKilos) "✅ Ticket Aprobado. Impacto sumado." else "❌ Ticket Rechazado."
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar la solicitud en la base de datos.", Toast.LENGTH_SHORT).show()
        } finally {
            // Cerramos la transacción sin importar lo que pase
            db.endTransaction()
        }
    }
}