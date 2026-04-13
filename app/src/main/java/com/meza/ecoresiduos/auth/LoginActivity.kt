package com.meza.ecoresiduos.auth

import android.content.ContentValues // <-- IMPORTANTE ASEGURAR QUE ESTÉ
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.admin.AdminDashActivity
import com.meza.ecoresiduos.db.DatabaseHelper
import com.meza.ecoresiduos.user.UserDashActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        // ¡EL SALVAVIDAS! Verificamos e insertamos el admin forzosamente si no existe
        crearAdminSiNoExiste()

        // Referencias a los componentes
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnUserLogin)
        val tvRegister = findViewById<TextView>(R.id.tvGoToRegister)

        // Ir a Registro
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Lógica de Inicio de Sesión
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            validarAcceso(email, password)
        }
    }

    // --- FUNCIÓN DE RESCATE ---
    private fun crearAdminSiNoExiste() {
        val db = dbHelper.writableDatabase

        // Buscamos si ya hay alguien con el rol de 'admin'
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ROLE} = 'admin'", null)

        if (!cursor.moveToFirst()) {
            // Si el cursor está vacío, significa que NO hay administrador. Lo creamos:
            val adminValues = ContentValues().apply {
                put(DatabaseHelper.COLUMN_USER_NAME, "Administrador Maestro")
                put(DatabaseHelper.COLUMN_USER_EMAIL, "ad")
                put(DatabaseHelper.COLUMN_USER_PASSWORD, "ad")
                put(DatabaseHelper.COLUMN_USER_ROLE, "admin")
                put(DatabaseHelper.COLUMN_USER_KILOS, 0.0)
            }

            val resultado = db.insert(DatabaseHelper.TABLE_USERS, null, adminValues)
            if (resultado != -1L) {
                // Pequeño aviso en consola/pantalla para que sepas que el truco funcionó
                Toast.makeText(this, "Sistema: Admin maestro generado", Toast.LENGTH_SHORT).show()
            }
        }
        cursor.close()
    }
    // --------------------------

    private fun validarAcceso(email: String, password: String) {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_EMAIL} = ? AND ${DatabaseHelper.COLUMN_USER_PASSWORD} = ?",
            arrayOf(email, password)
        )

        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID))
            val role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ROLE))
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME))

            val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt("user_id", id)
            editor.putString("user_role", role)
            editor.putString("user_name", nombre)
            editor.apply()

            if (role == "admin") {
                Toast.makeText(this, "Acceso de Administrador", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AdminDashActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, UserDashActivity::class.java)
                startActivity(intent)
            }
            finish()
        } else {
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
    }
}