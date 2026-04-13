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

        // Botón del Menú
        val btnOpenMenuAdmin = findViewById<ImageView>(R.id.btnOpenMenuAdmin)
        btnOpenMenuAdmin.setOnClickListener {
            drawerLayoutAdmin.openDrawer(GravityCompat.START)
            // Aquí puedes agregar un Toast temporal para Cerrar Sesión hasta que hagamos el menú formal del admin
            cerrarSesion()
        }

        // Referencias a los botones de navegación
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
        // Actualizamos los números cada vez que el admin regresa al dashboard
        actualizarEstadisticas()
    }

    private fun actualizarEstadisticas() {
        val tvAdminTotalUsers = findViewById<TextView>(R.id.tvAdminTotalUsers)
        val tvAdminTotalKilos = findViewById<TextView>(R.id.tvAdminTotalKilos)
        val tvAdminPendientes = findViewById<TextView>(R.id.tvAdminPendientes)

        val db = dbHelper.readableDatabase

        // 1. Contar Usuarios (Que no sean admin)
        val cursorUsers = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ROLE} != 'admin'", null)
        if (cursorUsers.moveToFirst()) {
            tvAdminTotalUsers.text = cursorUsers.getInt(0).toString()
        }
        cursorUsers.close()

        // 2. Sumar todos los Kilos Aprobados históricamente
        val cursorKilos = db.rawQuery("SELECT SUM(${DatabaseHelper.COLUMN_REPORT_PESO}) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado'", null)
        if (cursorKilos.moveToFirst()) {
            val totalKilos = cursorKilos.getDouble(0)
            tvAdminTotalKilos.text = String.format("%.1f", totalKilos)
        }
        cursorKilos.close()

        // 3. Contar Tickets Pendientes por validar HOY
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