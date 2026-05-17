package com.meza.ecoresiduos.user

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserForoActivity : AppCompatActivity() {

    private var userId: Int = -1
    private var comunidadId: Int = -1
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_foro)

        dbHelper = DatabaseHelper(this)

        // TRUCO SENIOR: Crear tabla de chat en tiempo de ejecución para no romper la BD existente
        dbHelper.writableDatabase.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_foro (
                id_mensaje INTEGER PRIMARY KEY AUTOINCREMENT,
                comunidad_id INTEGER,
                user_id INTEGER,
                mensaje TEXT,
                fecha TEXT,
                timestamp LONG
            )
        """.trimIndent())

        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)
        comunidadId = intent.getIntExtra("COMUNIDAD_ID", -1)

        findViewById<TextView>(R.id.btnBackForo).setOnClickListener { finish() }

        val btnEnviar = findViewById<FloatingActionButton>(R.id.btnEnviarMensaje)
        val etMensaje = findViewById<EditText>(R.id.etMensajeChat)

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensajeChat(texto)
                etMensaje.text.clear()
            }
        }

        if (comunidadId != -1) {
            cargarMuroCompleto()
        } else {
            Toast.makeText(this, "Error de comunidad", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun enviarMensajeChat(mensaje: String) {
        val db = dbHelper.writableDatabase
        val fechaFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val timestamp = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("comunidad_id", comunidadId)
            put("user_id", userId)
            put("mensaje", mensaje)
            put("fecha", fechaFormat)
            put("timestamp", timestamp)
        }
        db.insert("chat_foro", null, values)
        cargarMuroCompleto()
    }

    private fun cargarMuroCompleto() {
        val container = findViewById<LinearLayout>(R.id.containerPostForo)
        container.removeAllViews()
        val db = dbHelper.readableDatabase

        // 1. CARGAR EVENTOS DE RECICLAJE (Automatizados)
        val queryReportes = """
            SELECT u.${DatabaseHelper.COLUMN_USER_NAME}, r.${DatabaseHelper.COLUMN_REPORT_PESO}, 
                   r.${DatabaseHelper.COLUMN_REPORT_TIPO}, r.${DatabaseHelper.COLUMN_REPORT_FECHA}, 
                   u.${DatabaseHelper.COLUMN_USER_ID}
            FROM ${DatabaseHelper.TABLE_REPORTS} r 
            INNER JOIN ${DatabaseHelper.TABLE_USERS} u ON r.${DatabaseHelper.COLUMN_REPORT_USER_ID} = u.${DatabaseHelper.COLUMN_USER_ID}
            INNER JOIN ${DatabaseHelper.TABLE_PUNTOS} p ON r.${DatabaseHelper.COLUMN_REPORT_PUNTO_ID} = p.${DatabaseHelper.COLUMN_PUNTO_ID}
            WHERE r.${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado' AND p.${DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID} = ?
        """
        val cursorR = db.rawQuery(queryReportes, arrayOf(comunidadId.toString()))
        if (cursorR.moveToFirst()) {
            do {
                val nombre = cursorR.getString(0).split(" ")[0]
                val peso = cursorR.getDouble(1)
                val tipo = cursorR.getString(2)
                val msgAutom = "♻️ Recicle $peso kg de $tipo"
                dibujarBurbuja(msgAutom, nombre, cursorR.getInt(4) == userId, cursorR.getString(3), true)
            } while (cursorR.moveToNext())
        }
        cursorR.close()

        // 2. CARGAR MENSAJES DEL CHAT (Escritos por usuarios)
        val queryChat = """
            SELECT u.${DatabaseHelper.COLUMN_USER_NAME}, c.mensaje, c.fecha, c.user_id 
            FROM chat_foro c
            INNER JOIN ${DatabaseHelper.TABLE_USERS} u ON c.user_id = u.${DatabaseHelper.COLUMN_USER_ID}
            WHERE c.comunidad_id = ? ORDER BY c.timestamp ASC
        """
        val cursorC = db.rawQuery(queryChat, arrayOf(comunidadId.toString()))
        if (cursorC.moveToFirst()) {
            do {
                val nombre = cursorC.getString(0).split(" ")[0]
                val msj = cursorC.getString(1)
                val fecha = cursorC.getString(2)
                dibujarBurbuja(msj, nombre, cursorC.getInt(3) == userId, fecha, false)
            } while (cursorC.moveToNext())
        }
        cursorC.close()

        // Bajar el scroll al final
        val scroll = findViewById<ScrollView>(R.id.scrollMuro)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun dibujarBurbuja(texto: String, nombre: String, esMio: Boolean, fecha: String, esSistema: Boolean) {
        val container = findViewById<LinearLayout>(R.id.containerPostForo)
        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = if (esMio) Gravity.END else Gravity.START
            setPadding(0, 0, 0, 24)
        }

        val bubble = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.75).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            radius = 32f
            cardElevation = 2f
            // Verde claro si es mío, blanco si es de otro, amarillo tenue si es del sistema (reciclaje)
            setCardBackgroundColor(if (esSistema) Color.parseColor("#FEF3C7") else if (esMio) Color.parseColor("#DCFCE7") else Color.WHITE)
            strokeWidth = if (esMio || esSistema) 0 else 1
            strokeColor = Color.parseColor("#E2E8F0")
        }

        val bubbleContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val txtNombre = TextView(this).apply {
            text = if (esMio) "Tú" else nombre
            textSize = 12f
            setTextColor(if (esMio) Color.parseColor("#15803D") else Color.parseColor("#64748B"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val txtPost = TextView(this).apply {
            text = texto
            textSize = 16f
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 8, 0, 8)
        }

        val txtFecha = TextView(this).apply {
            text = fecha
            textSize = 10f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.END
        }

        bubbleContent.addView(txtNombre)
        bubbleContent.addView(txtPost)
        bubbleContent.addView(txtFecha)
        bubble.addView(bubbleContent)
        rowLayout.addView(bubble)
        container.addView(rowLayout)
    }
}