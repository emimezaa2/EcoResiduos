package com.meza.ecoresiduos.user

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class UserForoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_foro)

        findViewById<TextView>(R.id.btnBackForo).setOnClickListener { finish() }

        val dbHelper = DatabaseHelper(this)
        val container = findViewById<LinearLayout>(R.id.containerPostForo)
        val db = dbHelper.readableDatabase

        // Consultamos los reportes aprobados de TODOS los usuarios para el muro social
        val cursor = db.rawQuery("""
            SELECT u.${DatabaseHelper.COLUMN_USER_NAME}, r.${DatabaseHelper.COLUMN_REPORT_PESO}, 
            r.${DatabaseHelper.COLUMN_REPORT_TIPO}, r.${DatabaseHelper.COLUMN_REPORT_FECHA} 
            FROM ${DatabaseHelper.TABLE_REPORTS} r 
            JOIN ${DatabaseHelper.TABLE_USERS} u ON r.${DatabaseHelper.COLUMN_REPORT_USER_ID} = u.${DatabaseHelper.COLUMN_USER_ID}
            WHERE r.${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado'
            ORDER BY r.${DatabaseHelper.COLUMN_REPORT_ID} DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0).split(" ")[0]
                val peso = cursor.getDouble(1)
                val tipo = cursor.getString(2)
                val fecha = cursor.getString(3)

                val card = MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
                    radius = 40f
                    setCardBackgroundColor(Color.WHITE)
                    cardElevation = 2f
                }

                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 40, 40, 40)
                }

                val txtPost = TextView(this).apply {
                    text = "🌱 $nombre acaba de reciclar $peso kg de $tipo"
                    textSize = 16f
                    setTextColor(Color.parseColor("#0F172A"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val txtFecha = TextView(this).apply {
                    text = "Publicado el $fecha"
                    textSize = 12f
                    setTextColor(Color.GRAY)
                }

                layout.addView(txtPost)
                layout.addView(txtFecha)
                card.addView(layout)
                container.addView(card)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
}