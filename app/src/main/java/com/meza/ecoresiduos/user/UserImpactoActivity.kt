package com.meza.ecoresiduos.user

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class UserImpactoActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_impacto)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<TextView>(R.id.btnBackImpacto)
        val tvTotalKilos = findViewById<TextView>(R.id.tvTotalKilos)
        val tvTotalEntregas = findViewById<TextView>(R.id.tvTotalEntregas)
        val containerLotes = findViewById<LinearLayout>(R.id.containerLotes)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        cargarDatosDeImpacto(tvTotalKilos, tvTotalEntregas, containerLotes)
    }

    private fun cargarDatosDeImpacto(tvKilos: TextView, tvEntregas: TextView, container: LinearLayout) {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) return

        val db = dbHelper.readableDatabase

        // 1. Obtener kilos aprobados
        val cursorKilos = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
        if (cursorKilos.moveToFirst()) {
            tvKilos.text = "${cursorKilos.getDouble(0)} kg"
        }
        cursorKilos.close()

        // 2. Contar entregas aprobadas
        val cursorEntregas = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? AND ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado'", arrayOf(userId.toString()))
        if (cursorEntregas.moveToFirst()) {
            tvEntregas.text = cursorEntregas.getInt(0).toString()
        }
        cursorEntregas.close()

        // 3. DIBUJAR LOS LOTES PENDIENTES REALES
        val queryPendientes = "SELECT ${DatabaseHelper.COLUMN_REPORT_ID}, ${DatabaseHelper.COLUMN_REPORT_TIPO}, ${DatabaseHelper.COLUMN_REPORT_PESO} FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? AND ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Pendiente'"
        val cursorPendientes = db.rawQuery(queryPendientes, arrayOf(userId.toString()))

        if (cursorPendientes.moveToFirst()) {
            do {
                val reporteId = cursorPendientes.getInt(0)
                val tipo = cursorPendientes.getString(1)
                val peso = cursorPendientes.getDouble(2)

                // Crear Tarjeta contenedora
                val cardView = MaterialCardView(this)
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.radius = 32f
                cardView.cardElevation = 0f
                cardView.strokeWidth = 2
                cardView.strokeColor = Color.parseColor("#E2E8F0")
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 24)
                cardView.layoutParams = params

                // Layout interno de la tarjeta
                val internalLayout = LinearLayout(this)
                internalLayout.orientation = LinearLayout.VERTICAL
                internalLayout.setPadding(48, 48, 48, 48)

                // Fila Superior (Título y Porcentaje)
                val topRow = LinearLayout(this)
                topRow.orientation = LinearLayout.HORIZONTAL
                val titleView = TextView(this)
                titleView.text = "Registro #$reporteId - $tipo\nCarga: $peso kg"
                titleView.setTextColor(Color.parseColor("#0F172A"))
                titleView.textSize = 15f
                titleView.setTypeface(null, android.graphics.Typeface.BOLD)
                titleView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val statusView = TextView(this)
                statusView.text = "En Revisión"
                statusView.setTextColor(Color.parseColor("#F59E0B")) // Naranja oscuro
                statusView.textSize = 12f
                statusView.setTypeface(null, android.graphics.Typeface.BOLD)
                statusView.gravity = Gravity.END

                topRow.addView(titleView)
                topRow.addView(statusView)

                // Barra de progreso elegante
                val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
                progressBar.isIndeterminate = false
                progressBar.max = 100
                progressBar.progress = 50 // Fijamos 50% para simular "En proceso"
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                val progressParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24)
                progressParams.setMargins(0, 24, 0, 0)
                progressBar.layoutParams = progressParams

                // Ensamblar todo
                internalLayout.addView(topRow)
                internalLayout.addView(progressBar)
                cardView.addView(internalLayout)
                container.addView(cardView)

            } while (cursorPendientes.moveToNext())
        } else {
            // Si no tiene pendientes, mostrar un mensaje limpio
            val emptyMsg = TextView(this)
            emptyMsg.text = "No tienes procesos pendientes de revisión."
            emptyMsg.setTextColor(Color.parseColor("#64748B"))
            emptyMsg.setPadding(0, 32, 0, 0)
            container.addView(emptyMsg)
        }
        cursorPendientes.close()
    }
}