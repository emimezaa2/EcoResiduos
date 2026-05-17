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
                Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show()
            }
        }

        cargarDatos()
    }

    private fun cargarDatos() {
        val containerMis = findViewById<LinearLayout>(R.id.containerMisComunidades)
        val containerExplorar = findViewById<LinearLayout>(R.id.containerComunidades)
        containerMis.removeAllViews()
        containerExplorar.removeAllViews()

        val db = dbHelper.readableDatabase

        // --- 1. CARGAR MIS COMUNIDADES ---
        val queryMis = """
            SELECT c.${DatabaseHelper.COLUMN_COM_ID}, c.${DatabaseHelper.COLUMN_COM_NOMBRE}, c.${DatabaseHelper.COLUMN_COM_TIPO}, c.${DatabaseHelper.COLUMN_COM_CREADOR} 
            FROM ${DatabaseHelper.TABLE_COMMUNITIES} c 
            INNER JOIN ${DatabaseHelper.TABLE_MIEMBROS} m ON c.${DatabaseHelper.COLUMN_COM_ID} = m.${DatabaseHelper.COLUMN_MIEMBRO_COM_ID} 
            WHERE m.${DatabaseHelper.COLUMN_MIEMBRO_USER_ID} = ?
        """
        val cursorMis = db.rawQuery(queryMis, arrayOf(userId.toString()))

        if (cursorMis.moveToFirst()) {
            do {
                val comId = cursorMis.getInt(0)
                val comNombre = cursorMis.getString(1)
                val comTipo = cursorMis.getString(2)
                val creadorId = cursorMis.getInt(3)

                val stats = obtenerEstadisticasReales(comId)
                dibujarTarjetaComunidad(containerMis, comId, comNombre, comTipo, stats.first, stats.second, true, creadorId == userId)
            } while (cursorMis.moveToNext())
        } else {
            val emptyMsg = TextView(this).apply {
                text = "No perteneces a ninguna comunidad aún."
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, 20, 0, 40)
            }
            containerMis.addView(emptyMsg)
        }
        cursorMis.close()

        // --- 2. CARGAR COMUNIDADES PARA EXPLORAR ---
        val queryOtras = """
            SELECT * FROM ${DatabaseHelper.TABLE_COMMUNITIES} 
            WHERE ${DatabaseHelper.COLUMN_COM_ID} NOT IN (SELECT ${DatabaseHelper.COLUMN_MIEMBRO_COM_ID} FROM ${DatabaseHelper.TABLE_MIEMBROS} WHERE ${DatabaseHelper.COLUMN_MIEMBRO_USER_ID} = ?)
        """
        val cursorOtras = db.rawQuery(queryOtras, arrayOf(userId.toString()))

        if (cursorOtras.moveToFirst()) {
            do {
                val comId = cursorOtras.getInt(cursorOtras.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_ID))
                val comNombre = cursorOtras.getString(cursorOtras.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_NOMBRE))
                val comTipo = cursorOtras.getString(cursorOtras.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COM_TIPO))

                val stats = obtenerEstadisticasReales(comId)
                dibujarTarjetaComunidad(containerExplorar, comId, comNombre, comTipo, stats.first, stats.second, false, false)
            } while (cursorOtras.moveToNext())
        }
        cursorOtras.close()
    }

    private fun obtenerEstadisticasReales(comunidadId: Int): Pair<Int, Double> {
        val db = dbHelper.readableDatabase
        var totalUsuarios = 0
        var totalKg = 0.0

        val cursorUsers = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_MIEMBROS} WHERE ${DatabaseHelper.COLUMN_MIEMBRO_COM_ID} = ?", arrayOf(comunidadId.toString()))
        if (cursorUsers.moveToFirst()) totalUsuarios = cursorUsers.getInt(0)
        cursorUsers.close()

        val queryKilos = """
            SELECT SUM(r.${DatabaseHelper.COLUMN_REPORT_PESO}) 
            FROM ${DatabaseHelper.TABLE_REPORTS} r 
            INNER JOIN ${DatabaseHelper.TABLE_PUNTOS} p ON r.${DatabaseHelper.COLUMN_REPORT_PUNTO_ID} = p.${DatabaseHelper.COLUMN_PUNTO_ID}
            WHERE p.${DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID} = ? AND r.${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado'
        """
        val cursorKilos = db.rawQuery(queryKilos, arrayOf(comunidadId.toString()))
        if (cursorKilos.moveToFirst()) totalKg = cursorKilos.getDouble(0)
        cursorKilos.close()

        return Pair(totalUsuarios, totalKg)
    }

    private fun dibujarTarjetaComunidad(container: LinearLayout, comId: Int, nombreStr: String, tipo: String, usuarios: Int, kilos: Double, esMia: Boolean, soyCreador: Boolean) {
        val card = MaterialCardView(this).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
            radius = 32f
            cardElevation = 2f
            setCardBackgroundColor(if (esMia) Color.parseColor("#ECFDF5") else Color.WHITE)
            strokeWidth = 1
            strokeColor = Color.parseColor("#E2E8F0")
        }

        val layoutInterno = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val colTextos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Diseño limpio sin códigos visibles
        val tvNombre = TextView(this).apply {
            text = nombreStr
            textSize = 18f
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(null, Typeface.BOLD)
        }

        val tvImpacto = TextView(this).apply {
            text = "Alcance: $tipo | Miembros: $usuarios | Total: $kilos kg"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 8, 0, 0)
        }

        colTextos.addView(tvNombre)
        colTextos.addView(tvImpacto)
        rowLayout.addView(colTextos)

        if (esMia) {
            val layoutBotonesAccion = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 32, 0, 0)
                }
            }

            val btnMiembros = MaterialButton(this).apply {
                text = "Miembros"
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                setTextColor(Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, 16, 0)
                }
                setOnClickListener { mostrarDialogoMiembros(comId, nombreStr) }
            }

            val btnMuro = MaterialButton(this).apply {
                text = "Chat Social"
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0F172A"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val intent = android.content.Intent(this@UserComunidadActivity, UserForoActivity::class.java)
                    intent.putExtra("COMUNIDAD_ID", comId)
                    startActivity(intent)
                }
            }

            layoutBotonesAccion.addView(btnMiembros)
            layoutBotonesAccion.addView(btnMuro)

            layoutInterno.addView(rowLayout)
            layoutInterno.addView(layoutBotonesAccion)
        } else {
            val btnUnirse = MaterialButton(this).apply {
                text = "Unirse"
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0F172A"))
                setTextColor(Color.WHITE)
                setOnClickListener { unirseAComunidad(comId) }
            }
            rowLayout.addView(btnUnirse)
            layoutInterno.addView(rowLayout)
        }

        if (soyCreador) {
            val btnValidarTickets = MaterialButton(this).apply {
                text = "Validar Tickets del Grupo"
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 16, 0, 0)
                }
                setOnClickListener {
                    val intent = android.content.Intent(this@UserComunidadActivity, CreadorValidarActivity::class.java)
                    intent.putExtra("COMUNIDAD_ID", comId)
                    startActivity(intent)
                }
            }
            layoutInterno.addView(btnValidarTickets)
        }

        card.addView(layoutInterno)
        container.addView(card)
    }

    private fun mostrarDialogoMiembros(comunidadId: Int, nombreComunidad: String) {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT u.${DatabaseHelper.COLUMN_USER_NAME}, u.${DatabaseHelper.COLUMN_USER_KILOS} 
            FROM ${DatabaseHelper.TABLE_USERS} u
            INNER JOIN ${DatabaseHelper.TABLE_MIEMBROS} m ON u.${DatabaseHelper.COLUMN_USER_ID} = m.${DatabaseHelper.COLUMN_MIEMBRO_USER_ID}
            WHERE m.${DatabaseHelper.COLUMN_MIEMBRO_COM_ID} = ?
            ORDER BY u.${DatabaseHelper.COLUMN_USER_KILOS} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(comunidadId.toString()))
        var listaVecinos = ""

        var ranking = 1
        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0)
                val kilos = cursor.getDouble(1)
                listaVecinos += "$ranking. $nombre ($kilos kg)\n\n"
                ranking++
            } while (cursor.moveToNext())
        } else {
            listaVecinos = "Aún no hay miembros aquí."
        }
        cursor.close()

        android.app.AlertDialog.Builder(this)
            .setTitle("Miembros de $nombreComunidad")
            .setMessage(listaVecinos)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun crearComunidad(nombre: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COM_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_COM_TIPO, "Privada")
            put(DatabaseHelper.COLUMN_COM_CREADOR, userId)
            put(DatabaseHelper.COLUMN_COM_PUNTOS, 0.0)
        }
        val nuevaComunidadId = db.insert(DatabaseHelper.TABLE_COMMUNITIES, null, values)

        unirseAComunidad(nuevaComunidadId.toInt())
        Toast.makeText(this, "Comunidad creada", Toast.LENGTH_SHORT).show()
    }

    private fun unirseAComunidad(comunidadId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_MIEMBRO_USER_ID, userId)
            put(DatabaseHelper.COLUMN_MIEMBRO_COM_ID, comunidadId)
        }
        db.insert(DatabaseHelper.TABLE_MIEMBROS, null, values)
        Toast.makeText(this, "Te has unido al grupo", Toast.LENGTH_SHORT).show()

        cargarDatos()
    }
}