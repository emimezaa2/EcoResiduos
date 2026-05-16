package com.meza.ecoresiduos.admin

import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class AdminComunidadActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_comunidad)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<TextView>(R.id.btnBackAdminCom)
        val container = findViewById<LinearLayout>(R.id.containerAdminComunidades)
        val etNombre = findViewById<EditText>(R.id.etNombreAdminCom)
        val btnCrear = findViewById<MaterialButton>(R.id.btnCrearGlobal)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        btnCrear.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            if (nombre.isNotEmpty()) {
                guardarComunidad(nombre, container)
                etNombre.text.clear()
            } else {
                Toast.makeText(this, "Por favor ingresa un nombre válido", Toast.LENGTH_SHORT).show()
            }
        }

        cargarComunidades(container)
    }

    private fun guardarComunidad(nombre: String, container: LinearLayout) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COM_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_COM_TIPO, "Global")
            put(DatabaseHelper.COLUMN_COM_CREADOR, 0) // 0 representa al Administrador
            put(DatabaseHelper.COLUMN_COM_PUNTOS, 0.0)
        }
        db.insert(DatabaseHelper.TABLE_COMMUNITIES, null, values)
        Toast.makeText(this, "Comunidad registrada con éxito", Toast.LENGTH_SHORT).show()
        cargarComunidades(container)
    }

    private fun cargarComunidades(container: LinearLayout) {
        container.removeAllViews()
        val db = dbHelper.readableDatabase

        val query = "SELECT ${DatabaseHelper.COLUMN_COM_NOMBRE}, ${DatabaseHelper.COLUMN_COM_TIPO} FROM ${DatabaseHelper.TABLE_COMMUNITIES} ORDER BY ${DatabaseHelper.COLUMN_COM_ID} DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0)
                val tipo = cursor.getString(1)

                val cardView = MaterialCardView(this).apply {
                    setCardBackgroundColor(Color.WHITE)
                    radius = 24f
                    cardElevation = 2f
                    strokeWidth = 1
                    strokeColor = Color.parseColor("#E2E8F0")
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, 24)
                    layoutParams = params
                }

                val layoutInterno = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 40, 40, 40)
                }

                val tvNombre = TextView(this).apply {
                    text = "Comunidad: $nombre"
                    setTextColor(Color.parseColor("#0F172A"))
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                }

                val tvDetalle = TextView(this).apply {
                    text = "Tipo: $tipo"
                    setTextColor(Color.parseColor("#64748B"))
                    textSize = 12f
                    setPadding(0, 4, 0, 0)
                }

                layoutInterno.addView(tvNombre)
                layoutInterno.addView(tvDetalle)
                cardView.addView(layoutInterno)
                container.addView(cardView)

            } while (cursor.moveToNext())
        } else {
            val emptyMsg = TextView(this).apply {
                text = "No hay comunidades registradas."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 0)
            }
            container.addView(emptyMsg)
        }
        cursor.close()
    }
}