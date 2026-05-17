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

        // Mantenemos el bloqueo definitivo de modo oscuro
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_user_dash)

        dbHelper = DatabaseHelper(this)
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val btnOpenMenu = findViewById<ImageView>(R.id.btnOpenMenu)

        // Referencias de la UI originales e intactas
        val tvUserNameDash = findViewById<TextView>(R.id.tvUserNameDash)
        val tvUserTotalKilos = findViewById<TextView>(R.id.tvUserTotalKilos)
        val cardReporte = findViewById<MaterialCardView>(R.id.cardReporte)
        val cardIA = findViewById<MaterialCardView>(R.id.cardIA)
        val cardImpacto = findViewById<MaterialCardView>(R.id.cardImpacto)
        val cardEcoBot = findViewById<MaterialCardView>(R.id.cardEcoBot)
        val cardPuntos = findViewById<MaterialCardView>(R.id.cardPuntos)
        val cardComunidadDash = findViewById<MaterialCardView>(R.id.cardComunidadDash)
        val cardForoDash = findViewById<MaterialCardView>(R.id.cardForoDash)
        val cardRecompensasDash = findViewById<MaterialCardView>(R.id.cardRecompensasDash)

        // Redirección segura multi-grupo unificada
        cardComunidadDash.setOnClickListener { startActivity(Intent(this, UserComunidadActivity::class.java)) }
        cardForoDash.setOnClickListener { startActivity(Intent(this, UserComunidadActivity::class.java)) }
        cardRecompensasDash.setOnClickListener { startActivity(Intent(this, UserRecompensasActivity::class.java)) }

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
                R.id.nav_premium -> startActivity(Intent(this, SuscripcionActivity::class.java))
                R.id.nav_perfil -> startActivity(Intent(this, UserPerfilActivity::class.java))
                R.id.nav_recompensas -> startActivity(Intent(this, UserRecompensasActivity::class.java))

                R.id.nav_comunidad -> startActivity(Intent(this, UserComunidadActivity::class.java))
                R.id.nav_foro -> startActivity(Intent(this, UserComunidadActivity::class.java))

                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 3. Atajos rápidos centrales
        cardReporte.setOnClickListener { startActivity(Intent(this, UserReporteActivity::class.java)) }
        cardIA.setOnClickListener { startActivity(Intent(this, AnalisisIAActivity::class.java)) }
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

            // Referencias añadidas de forma segura para las nuevas tarjetas
            val tvTokens = findViewById<TextView>(R.id.tvUserTokensDash)
            val tvPremios = findViewById<TextView>(R.id.tvUserPremiosDash)
            val tvStatRegistros = findViewById<TextView>(R.id.tvStatRegistros)
            val tvStatValidados = findViewById<TextView>(R.id.tvStatValidados)
            val tvStatComunidades = findViewById<TextView>(R.id.tvStatComunidades)

            // EXTRAER LA CABECERA DEL MENÚ LATERAL DINÁMICAMENTE
            val navView = findViewById<NavigationView>(R.id.navView)
            val headerView = navView?.getHeaderView(0)
            val tvUserNameMenu = headerView?.findViewById<TextView>(R.id.tvUserNameMenu)

            val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_NAME}, ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))

            if (cursor.moveToFirst()) {
                val nombreCompleto = cursor.getString(0)
                val kilos = cursor.getDouble(1)
                val primerNombre = nombreCompleto.split(" ").firstOrNull() ?: "Usuario"

                // Inyectamos el primer nombre en el Dashboard (Ej: "emiliano")
                tvName.text = primerNombre
                tvKilos.text = String.format("%.1f kg", kilos)

                // ¡CORRECCIÓN AQUÍ! Inyectamos el nombre completo en la cabecera del menú lateral
                if (tvUserNameMenu != null) {
                    tvUserNameMenu.text = nombreCompleto
                }

                if (tvTokens != null) tvTokens.text = "150"
                if (tvPremios != null) tvPremios.text = "3"
            }
            cursor.close()

            // Actualización automática del módulo de estadísticas triples
            val cursorReg = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ?", arrayOf(userId.toString()))
            if (cursorReg.moveToFirst() && tvStatRegistros != null) tvStatRegistros.text = cursorReg.getInt(0).toString()
            cursorReg.close()

            val cursorVal = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? AND ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Aprobado'", arrayOf(userId.toString()))
            if (cursorVal.moveToFirst() && tvStatValidados != null) tvStatValidados.text = cursorVal.getInt(0).toString()
            cursorVal.close()

            val cursorCom = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_MIEMBROS} WHERE ${DatabaseHelper.COLUMN_MIEMBRO_USER_ID} = ?", arrayOf(userId.toString()))
            if (cursorCom.moveToFirst() && tvStatComunidades != null) tvStatComunidades.text = cursorCom.getInt(0).toString()
            cursorCom.close()
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