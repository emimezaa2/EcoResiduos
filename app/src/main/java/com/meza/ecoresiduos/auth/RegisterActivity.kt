package com.meza.ecoresiduos.auth

import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        val etName = findViewById<TextInputEditText>(R.id.etRegisterName)
        val etEmail = findViewById<TextInputEditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegisterPassword)
        val btnFinalize = findViewById<Button>(R.id.btnFinalizeRegister)
        val btnBack = findViewById<TextView>(R.id.btnBackRegister)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnFinalize.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos de registro.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Detenemos la ejecución si falta algo
            }

            registrarUsuarioEnBaseDeDatos(name, email, password)
        }
    }

    // 4. Función de inserción en SQLite
    private fun registrarUsuarioEnBaseDeDatos(nombre: String, correo: String, contrasena: String) {
        val db = dbHelper.writableDatabase

        // Preparamos los valores para la tabla
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_NAME, nombre)
            put(DatabaseHelper.COLUMN_USER_EMAIL, correo)
            put(DatabaseHelper.COLUMN_USER_PASSWORD, contrasena)
            put(DatabaseHelper.COLUMN_USER_KILOS, 0.0) // Todos empiezan con 0 kilos de impacto
            put(DatabaseHelper.COLUMN_USER_ROLE, "user") // Nivel de permisos estándar
        }

        // Insertamos y obtenemos el ID resultante
        val newRowId = db.insert(DatabaseHelper.TABLE_USERS, null, values)

        if (newRowId != -1L) {
            Toast.makeText(this, "Cuenta creada exitosamente. Bienvenido, $nombre.", Toast.LENGTH_LONG).show()
            // Cerramos la pantalla de registro para volver automáticamente al Login
            finish()
        } else {
            Toast.makeText(this, "Error del sistema al crear la cuenta. Intenta nuevamente.", Toast.LENGTH_LONG).show()
        }
    }
}