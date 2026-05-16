package com.meza.ecoresiduos.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.auth.LoginActivity
import com.meza.ecoresiduos.db.DatabaseHelper

class UserPerfilActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_perfil)

        dbHelper = DatabaseHelper(this)

        findViewById<TextView>(R.id.btnBackPerfil).setOnClickListener { finish() }

        val btnLogout = findViewById<MaterialButton>(R.id.btnLogoutPerfil)
        btnLogout.setOnClickListener { cerrarSesion() }

        cargarDatosPerfil()
    }

    private fun cargarDatosPerfil() {
        val tvNombre = findViewById<TextView>(R.id.tvNombrePerfil)
        val tvNivel = findViewById<TextView>(R.id.tvNivelEco)

        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId != -1) {
            val db = dbHelper.readableDatabase

            // 1. Obtener Nombre
            val cursorNombre = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_NAME} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
            if (cursorNombre.moveToFirst()) {
                tvNombre.text = cursorNombre.getString(0)
            }
            cursorNombre.close()

            // 2. Obtener Kilos para el Nivel
            val cursorStats = db.rawQuery("SELECT SUM(${DatabaseHelper.COLUMN_REPORT_PESO}) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ?", arrayOf(userId.toString()))
            var totalKilos = 0.0
            if (cursorStats.moveToFirst()) {
                totalKilos = cursorStats.getDouble(0)
            }
            cursorStats.close()

            // Lógica de Gamificación (Niveles)
            tvNivel.text = when {
                totalKilos > 50 -> "Nivel: Guardián del Planeta 🌍"
                totalKilos > 20 -> "Nivel: Árbol Fuerte 🌳"
                totalKilos > 5 -> "Nivel: Brote Verde 🌱"
                else -> "Nivel: Semilla 🌰"
            }
        }
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}