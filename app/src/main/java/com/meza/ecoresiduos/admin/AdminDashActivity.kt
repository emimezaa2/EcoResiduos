package com.meza.ecoresiduos.admin

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

class AdminDashActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var drawerLayoutAdmin: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dash)

        dbHelper = DatabaseHelper(this)
        drawerLayoutAdmin = findViewById(R.id.drawerLayoutAdmin)

        // 1. Botón para abrir el menú (Hamburguesa)
        val btnOpenMenuAdmin = findViewById<ImageView>(R.id.btnOpenMenuAdmin)
        btnOpenMenuAdmin.setOnClickListener {
            drawerLayoutAdmin.openDrawer(GravityCompat.START)
        }

        // 2. Lógica del Menú Lateral
        val navViewAdmin = findViewById<NavigationView>(R.id.navViewAdmin)
        navViewAdmin.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_admin_home -> drawerLayoutAdmin.closeDrawer(GravityCompat.START)
                R.id.nav_admin_validar -> startActivity(Intent(this, AdminValidarActivity::class.java))
                R.id.nav_admin_comunidad -> startActivity(Intent(this, AdminComunidadActivity::class.java))
                R.id.nav_admin_puntos -> startActivity(Intent(this, AdminPuntosActivity::class.java))
                R.id.nav_admin_bitacora -> startActivity(Intent(this, AdminBitacoraActivity::class.java))
                R.id.nav_admin_logout -> cerrarSesion()
            }
            drawerLayoutAdmin.closeDrawer(GravityCompat.START)
            true
        }

        // 3. Referencias a las tarjetas centrales (Grid)
        val cardValidar = findViewById<MaterialCardView>(R.id.cardValidar)
        val cardComunidad = findViewById<MaterialCardView>(R.id.cardComunidad)
        val cardPuntosAdmin = findViewById<MaterialCardView>(R.id.cardPuntosAdmin)
        val cardBitacora = findViewById<MaterialCardView>(R.id.cardBitacora)

        cardValidar.setOnClickListener { startActivity(Intent(this, AdminValidarActivity::class.java)) }
        cardComunidad.setOnClickListener { startActivity(Intent(this, AdminComunidadActivity::class.java)) }
        cardPuntosAdmin.setOnClickListener { startActivity(Intent(this, AdminPuntosActivity::class.java)) }
        cardBitacora.setOnClickListener { startActivity(Intent(this, AdminBitacoraActivity::class.java)) }

        actualizarEstadisticas()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadisticas()
    }

    private fun actualizarEstadisticas() {
        val tvAdminTotalUsers = findViewById<TextView>(R.id.tvAdminTotalUsers)
        val tvAdminTotalKilos = findViewById<TextView>(R.id.tvAdminTotalKilos)
        val tvAdminPendientes = findViewById<TextView>(R.id.tvAdminPendientes)

        val db = dbHelper.readableDatabase

        // Contar Usuarios
        val cursorUsers = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ROLE} != 'admin'", null)
        if (cursorUsers.moveToFirst()) {
            tvAdminTotalUsers.text = cursorUsers.getInt(0).toString()
        }
        cursorUsers.close()

        // Sumar Kilos
        val cursorKilos = db.rawQuery("SELECT SUM(${DatabaseHelper.COLUMN_REPORT_PESO}) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado'", null)
        if (cursorKilos.moveToFirst()) {
            val totalKilos = cursorKilos.getDouble(0)
            tvAdminTotalKilos.text = String.format("%.1f", totalKilos)
        }
        cursorKilos.close()

        // Contar Pendientes
        val cursorPendientes = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Pendiente'", null)
        if (cursorPendientes.moveToFirst()) {
            tvAdminPendientes.text = cursorPendientes.getInt(0).toString()
        }
        cursorPendientes.close()
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Sesión de Administrador cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}