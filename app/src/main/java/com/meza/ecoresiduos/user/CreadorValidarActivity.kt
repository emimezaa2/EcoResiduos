package com.meza.ecoresiduos.user

import android.content.ContentValues
import android.content.Context
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

class CreadorValidarActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userIdActual: Int = -1
    private var comunidadIdSeleccionada: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creador_validar)

        dbHelper = DatabaseHelper(this)

        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        userIdActual = prefs.getInt("user_id", -1)

        // Atrapamos el ID de la comunidad que el creador seleccionó
        comunidadIdSeleccionada = intent.getIntExtra("COMUNIDAD_ID", -1)

        findViewById<TextView>(R.id.btnBackCreadorValidar).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.containerCreadorValidar)
        cargarTicketsDeMiComunidad(container)
    }

    private fun cargarTicketsDeMiComunidad(container: LinearLayout) {
        container.removeAllViews()
        val db = dbHelper.readableDatabase

        // DOBLE CANDADO: Que el reporte sea de la comunidad exacta que se tocó, y que yo sea el creador.
        val query = """
            SELECT r.${DatabaseHelper.COLUMN_REPORT_ID}, u.${DatabaseHelper.COLUMN_USER_NAME}, 
                   r.${DatabaseHelper.COLUMN_REPORT_PESO}, r.${DatabaseHelper.COLUMN_REPORT_TIPO}, 
                   u.${DatabaseHelper.COLUMN_USER_ID} 
            FROM ${DatabaseHelper.TABLE_REPORTS} r 
            INNER JOIN ${DatabaseHelper.TABLE_USERS} u ON r.${DatabaseHelper.COLUMN_REPORT_USER_ID} = u.${DatabaseHelper.COLUMN_USER_ID} 
            INNER JOIN ${DatabaseHelper.TABLE_PUNTOS} p ON r.${DatabaseHelper.COLUMN_REPORT_PUNTO_ID} = p.${DatabaseHelper.COLUMN_PUNTO_ID}
            INNER JOIN ${DatabaseHelper.TABLE_COMMUNITIES} c ON p.${DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID} = c.${DatabaseHelper.COLUMN_COM_ID}
            WHERE r.${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Pendiente' 
            AND c.${DatabaseHelper.COLUMN_COM_ID} = ? 
            AND c.${DatabaseHelper.COLUMN_COM_CREADOR} = ?
        """

        val cursor = db.rawQuery(query, arrayOf(comunidadIdSeleccionada.toString(), userIdActual.toString()))

        if (cursor.moveToFirst()) {
            do {
                val reporteId = cursor.getInt(0)
                val userName = cursor.getString(1)
                val peso = cursor.getDouble(2)
                val tipo = cursor.getString(3)
                val userReportoId = cursor.getInt(4)

                val cardView = MaterialCardView(this).apply {
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, 32)
                    layoutParams = params
                    setCardBackgroundColor(Color.WHITE)
                    radius = 24f
                    cardElevation = 4f
                }

                val internalLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 40, 40, 40)
                }

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
                    text = "Carga Declarada: $peso kg\nClasificacion: $tipo"
                    setTextColor(Color.parseColor("#64748B"))
                    textSize = 14f
                    setPadding(0, 8, 0, 24)
                }

                val btnLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val btnRechazar = MaterialButton(this).apply {
                    text = "Rechazar"
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2"))
                    setTextColor(Color.parseColor("#EF4444"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, 16, 0) }
                }

                val btnAprobar = MaterialButton(this).apply {
                    text = "Aprobar"
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                btnRechazar.setOnClickListener {
                    actualizarEstado(reporteId, "Rechazado", userReportoId, peso, false)
                    container.removeView(cardView)
                }

                btnAprobar.setOnClickListener {
                    actualizarEstado(reporteId, "Aprobado", userReportoId, peso, true)
                    container.removeView(cardView)
                }

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
            val emptyMsg = TextView(this).apply {
                text = "Bandeja al dia.\nNo hay registros pendientes."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 0)
            }
            container.addView(emptyMsg)
        }
        cursor.close()
    }

    private fun actualizarEstado(reporteId: Int, nuevoEstado: String, userId: Int, peso: Double, sumarKilos: Boolean) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val valuesReport = ContentValues().apply { put(DatabaseHelper.COLUMN_REPORT_STATUS, nuevoEstado) }
            db.update(DatabaseHelper.TABLE_REPORTS, valuesReport, "${DatabaseHelper.COLUMN_REPORT_ID} = ?", arrayOf(reporteId.toString()))

            if (sumarKilos) {
                db.execSQL("UPDATE ${DatabaseHelper.TABLE_USERS} SET ${DatabaseHelper.COLUMN_USER_KILOS} = ${DatabaseHelper.COLUMN_USER_KILOS} + ? WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(peso, userId))
            }
            db.setTransactionSuccessful()
            Toast.makeText(this, if (sumarKilos) "Ticket Aprobado" else "Ticket Rechazado", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }
}