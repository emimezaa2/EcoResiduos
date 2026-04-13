package com.meza.ecoresiduos.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.auth.LoginActivity
import com.meza.ecoresiduos.db.DatabaseHelper

class UserDashActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dash)

        dbHelper = DatabaseHelper(this)
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val btnOpenMenu = findViewById<ImageView>(R.id.btnOpenMenu)

        // Referencias de la UI
        val tvUserNameDash = findViewById<TextView>(R.id.tvUserNameDash)
        val tvUserTotalKilos = findViewById<TextView>(R.id.tvUserTotalKilos)
        val cardReporte = findViewById<MaterialCardView>(R.id.cardReporte)
        val cardImpacto = findViewById<MaterialCardView>(R.id.cardImpacto)
        val cardEcoBot = findViewById<MaterialCardView>(R.id.cardEcoBot)
        val cardPuntos = findViewById<MaterialCardView>(R.id.cardPuntos)

        cargarDatosUsuario(tvUserNameDash, tvUserTotalKilos)

        // 1. Botón para ABRIR el menú lateral
        btnOpenMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 2. Manejar los clics DENTRO del menú lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_reporte -> startActivity(Intent(this, UserReporteActivity::class.java))
                R.id.nav_impacto -> startActivity(Intent(this, UserImpactoActivity::class.java))
                R.id.nav_bot -> startActivity(Intent(this, UserEcobotActivity::class.java))
                R.id.nav_puntos -> startActivity(Intent(this, UserPuntosActivity::class.java))
                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 3. Mantener los clics de las tarjetas centrales (para atajos rápidos)
        cardReporte.setOnClickListener { startActivity(Intent(this, UserReporteActivity::class.java)) }
        cardImpacto.setOnClickListener { startActivity(Intent(this, UserImpactoActivity::class.java)) }
        cardEcoBot.setOnClickListener { startActivity(Intent(this, UserEcobotActivity::class.java)) }
        cardPuntos.setOnClickListener { startActivity(Intent(this, UserPuntosActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        val tvUserNameDash = findViewById<TextView>(R.id.tvUserNameDash)
        val tvUserTotalKilos = findViewById<TextView>(R.id.tvUserTotalKilos)
        cargarDatosUsuario(tvUserNameDash, tvUserTotalKilos)
    }

    private fun cargarDatosUsuario(tvName: TextView, tvKilos: TextView) {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId != -1) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_NAME}, ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))

            if (cursor.moveToFirst()) {
                val nombreCompleto = cursor.getString(0)
                val kilos = cursor.getDouble(1)
                val primerNombre = nombreCompleto.split(" ").firstOrNull() ?: "Usuario"

                tvName.text = primerNombre
                tvKilos.text = "$kilos kg"
            }
            cursor.close()
        }
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}