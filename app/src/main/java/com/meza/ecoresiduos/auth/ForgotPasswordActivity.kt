package com.meza.ecoresiduos.auth

import android.content.ContentValues
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        dbHelper = DatabaseHelper(this)

        val etEmail = findViewById<TextInputEditText>(R.id.etForgotEmail)
        val etNewPassword = findViewById<TextInputEditText>(R.id.etForgotNewPassword)
        val btnReset = findViewById<MaterialButton>(R.id.btnResetPassword)
        val tvBack = findViewById<TextView>(R.id.tvBackToLogin)

        tvBack.setOnClickListener {
            finish() // Cierra la pantalla y vuelve al login
        }

        btnReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()

            if (email.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Ingresa un correo electrónico válido"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            actualizarContrasena(email, newPassword)
        }
    }

    private fun actualizarContrasena(email: String, newPassword: String) {
        val db = dbHelper.writableDatabase

        // Primero verificamos si el correo existe en la base de datos
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_EMAIL} = ?",
            arrayOf(email)
        )

        if (cursor.moveToFirst()) {
            // El correo existe, actualizamos la contraseña
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_USER_PASSWORD, newPassword)
            }

            val filasAfectadas = db.update(
                DatabaseHelper.TABLE_USERS,
                values,
                "${DatabaseHelper.COLUMN_USER_EMAIL} = ?",
                arrayOf(email)
            )

            if (filasAfectadas > 0) {
                Toast.makeText(this, "Contraseña actualizada exitosamente", Toast.LENGTH_LONG).show()
                finish() // Cierra y regresa al login
            } else {
                Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
            }
        } else {
            // El correo no está registrado
            Toast.makeText(this, "No se encontró ninguna cuenta con este correo", Toast.LENGTH_LONG).show()
        }
        cursor.close()
    }
}