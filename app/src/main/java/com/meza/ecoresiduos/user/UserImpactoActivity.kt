package com.meza.ecoresiduos.user

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import java.io.File

class UserImpactoActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var containerLotes: LinearLayout
    private lateinit var tvTotalKilos: TextView
    private lateinit var tvTotalEntregas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_impacto)

        dbHelper = DatabaseHelper(this)
        containerLotes = findViewById(R.id.containerLotes)
        tvTotalKilos = findViewById(R.id.tvTotalKilos)
        tvTotalEntregas = findViewById(R.id.tvTotalEntregas)

        findViewById<TextView>(R.id.btnBackImpacto).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        cargarDatos()
    }

    private fun cargarDatos() {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) return

        val db = dbHelper.readableDatabase

        // 1. Estadísticas Globales
        val cursorStats = db.rawQuery(
            "SELECT SUM(${DatabaseHelper.COLUMN_REPORT_PESO}), COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ?",
            arrayOf(userId.toString())
        )

        if (cursorStats.moveToFirst()) {
            tvTotalKilos.text = String.format("%.1f kg", cursorStats.getDouble(0))
            tvTotalEntregas.text = cursorStats.getInt(1).toString()
        }
        cursorStats.close()

        // 2. Lista de Registros
        containerLotes.removeAllViews()
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? ORDER BY ${DatabaseHelper.COLUMN_REPORT_ID} DESC",
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val peso = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_PESO))
                val tipo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_TIPO))
                val fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_FECHA))
                val status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_STATUS))
                val fotoPath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_FOTO_PATH))

                crearTarjetaRegistro(peso, tipo, fecha, status, fotoPath)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun crearTarjetaRegistro(peso: Double, tipo: String, fecha: String, status: String, foto: String) {
        val card = MaterialCardView(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params

            radius = 48f
            cardElevation = 0f
            strokeWidth = 2
            // CORRECCIÓN 1: La ruta correcta es android.content.res
            setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0")))
            setCardBackgroundColor(Color.WHITE)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Imagen
        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120)
            scaleType = ImageView.ScaleType.CENTER_CROP
            val file = File(foto)
            if (file.exists()) {
                setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } else {
                setImageResource(R.drawable.ic_menu_reporte)
            }
        }

        // Textos
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val paramsCol = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            paramsCol.marginStart = 24
            layoutParams = paramsCol
        }

        val t1 = TextView(this).apply {
            text = "$peso kg de $tipo"
            // CORRECCIÓN 2: Uso directo de 'f' y el método setTypeface
            textSize = 15f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.parseColor("#0F172A"))
        }

        val t2 = TextView(this).apply {
            text = fecha
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
        }

        col.addView(t1)
        col.addView(t2)

        // Estatus
        val badge = TextView(this).apply {
            text = status
            textSize = 10f
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(20, 8, 20, 8)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 100f
                setColor(if (status == "Pendiente") Color.parseColor("#F59E0B") else Color.parseColor("#10B981"))
            }
        }

        layout.addView(img)
        layout.addView(col)
        layout.addView(badge)
        card.addView(layout)
        containerLotes.addView(card)
    }


}