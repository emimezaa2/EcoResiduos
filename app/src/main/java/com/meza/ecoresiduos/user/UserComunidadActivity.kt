package com.meza.ecoresiduos.user

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class UserComunidadActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_comunidad)

        dbHelper = DatabaseHelper(this)

        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)

        findViewById<TextView>(R.id.btnBackComunidad).setOnClickListener { finish() }

        val btnCrear = findViewById<Button>(R.id.btnCrearComunidad)
        val etNuevaComunidad = findViewById<EditText>(R.id.etNuevaComunidad)

        btnCrear.setOnClickListener {
            val nombre = etNuevaComunidad.text.toString().trim()
            if (nombre.isNotEmpty()) {
                crearComunidad(nombre)
                etNuevaComunidad.text.clear()
            } else {
                Toast.makeText(this, "Escribe un nombre primero", Toast.LENGTH_SHORT).show()
            }
        }

        cargarDatos()
    }

    private fun cargarDatos() {
        val tvMiComunidadNombre = findViewById<TextView>(R.id.tvMiComunidadNombre)
        val tvMiComunidadPuntos = findViewById<TextView>(R.id.tvMiComunidadPuntos)
        val container = findViewById<LinearLayout>(R.id.containerComunidades)
        container.removeAllViews()

        val db = dbHelper.readableDatabase

        // 1. Obtener la comunidad actual del usuario
        var comunidadIdActual = -1
        val cursorUser = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_COMUNIDAD_ID} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
        if (cursorUser.moveToFirst()) {
            comunidadIdActual = cursorUser.getInt(0)
        }
        cursorUser.close()

        // 2. Mostrar datos de la comunidad actual
        if (comunidadIdActual != -1) {
            val cursorMiCom = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_COM_NOMBRE}, ${DatabaseHelper.COLUMN_COM_PUNTOS} FROM ${DatabaseHelper.TABLE_COMMUNITIES} WHERE ${DatabaseHelper.COLUMN_COM_ID} = ?", arrayOf(comunidadIdActual.toString()))
            if (cursorMiCom.moveToFirst()) {
                tvMiComunidadNombre.text = "🌟 " + cursorMiCom.getString(0)
                tvMiComunidadPuntos.text = "Impacto Grupal: ${cursorMiCom.getDouble(1)} kg reciclados"
            }
            cursorMiCom.close()
        } else {
            tvMiComunidadNombre.text = "No estás en ninguna comunidad"
            tvMiComunidadPuntos.text = "Crea una o únete a las de abajo"
        }

        // 3. Listar TODAS las demás comunidades para unirse
        val cursorComs = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_COMMUNITIES} WHERE ${DatabaseHelper.COLUMN_COM_ID} != ? ORDER BY ${DatabaseHelper.COLUMN_COM_PUNTOS} DESC", arrayOf(comunidadIdActual.toString()))

        if (cursorComs.moveToFirst()) {
            do {
                val comId = cursorComs.getInt(cursorComs.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_ID))
                val comNombre = cursorComs.getString(cursorComs.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_NOMBRE))
                val comPuntos = cursorComs.getDouble(cursorComs.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_PUNTOS))
                val comTipo = cursorComs.getString(cursorComs.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_TIPO))

                val card = MaterialCardView(this).apply {
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, 24)
                    layoutParams = params
                    radius = 32f
                    cardElevation = 2f
                    setCardBackgroundColor(Color.WHITE)
                }

                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(40, 40, 40, 40)
                    gravity = Gravity.CENTER_VERTICAL
                }

                val colTextos = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val nombre = TextView(this).apply {
                    text = comNombre
                    textSize = 16f
                    setTextColor(Color.parseColor("#0F172A"))
                    setTypeface(null, Typeface.BOLD)
                }

                val impacto = TextView(this).apply {
                    text = "Tipo: $comTipo | 📊 $comPuntos kg"
                    textSize = 12f
                    setTextColor(Color.parseColor("#64748B"))
                    setPadding(0, 8, 0, 0)
                }

                val btnUnirse = MaterialButton(this).apply {
                    text = "Unirse"
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                    setTextColor(Color.parseColor("#0F172A"))
                    setOnClickListener { unirseAComunidad(comId) }
                }

                colTextos.addView(nombre)
                colTextos.addView(impacto)
                layout.addView(colTextos)
                layout.addView(btnUnirse)
                card.addView(layout)
                container.addView(card)

            } while (cursorComs.moveToNext())
        }
        cursorComs.close()
    }

    private fun crearComunidad(nombre: String) {
        val db = dbHelper.writableDatabase

        // 1. Insertar la comunidad en la tabla
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COM_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_COM_TIPO, "Privada") // Las de usuarios son privadas
            put(DatabaseHelper.COLUMN_COM_CREADOR, userId)
            put(DatabaseHelper.COLUMN_COM_PUNTOS, 0.0)
        }
        val nuevaComunidadId = db.insert(DatabaseHelper.TABLE_COMMUNITIES, null, values)

        // 2. Asignar al usuario a esta nueva comunidad
        unirseAComunidad(nuevaComunidadId.toInt())
        Toast.makeText(this, "¡Comunidad creada con éxito!", Toast.LENGTH_SHORT).show()
    }

    private fun unirseAComunidad(comunidadId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_COMUNIDAD_ID, comunidadId)
        }
        db.update(DatabaseHelper.TABLE_USERS, values, "${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
        Toast.makeText(this, "¡Te has unido a la comunidad!", Toast.LENGTH_SHORT).show()

        // Recargar la pantalla para ver los cambios
        cargarDatos()
    }
}