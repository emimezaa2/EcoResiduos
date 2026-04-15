package com.meza.ecoresiduos.user // Ajusta tu paquete si es necesario

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class UserPuntosActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbHelper: DatabaseHelper

    // Referencias a la UI inferior
    private lateinit var tvSeleccionaPuntoUser: TextView
    private lateinit var layoutDatosPuntoUser: LinearLayout
    private lateinit var tvNombrePuntoUser: TextView
    private lateinit var tvEstadoPuntoUser: TextView
    private lateinit var pbCapacidadUser: ProgressBar

    // El Carrusel nuevo
    private lateinit var layoutPuntosRapidos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_user_puntos)

        dbHelper = DatabaseHelper(this)

        // Vinculación de Vistas
        map = findViewById(R.id.mapUser)
        tvSeleccionaPuntoUser = findViewById(R.id.tvSeleccionaPuntoUser)
        layoutDatosPuntoUser = findViewById(R.id.layoutDatosPuntoUser)
        tvNombrePuntoUser = findViewById(R.id.tvNombrePuntoUser)
        tvEstadoPuntoUser = findViewById(R.id.tvEstadoPuntoUser)
        pbCapacidadUser = findViewById(R.id.pbCapacidadUser)
        layoutPuntosRapidos = findViewById(R.id.layoutPuntosRapidos)

        findViewById<TextView>(R.id.btnBackUserPuntos).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        configurarMapa()
    }

    private fun configurarMapa() {
        map.setMultiTouchControls(true)
        val tolucaCentro = GeoPoint(19.2826, -99.6557) // Respaldo por si no hay puntos
        map.controller.setZoom(15.0)
        map.controller.setCenter(tolucaCentro)

        cargarPuntosRealesDesdeBD()
    }

    private fun cargarPuntosRealesDesdeBD() {
        map.overlays.removeAll { it is Marker }
        layoutPuntosRapidos.removeAllViews()

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PUNTOS}", null)

        var primerPunto: GeoPoint? = null

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_NOMBRE))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_LAT))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_LON))
                val cap = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_ESTADO))

                val geoPoint = GeoPoint(lat, lon)
                if (primerPunto == null) primerPunto = geoPoint

                // 1. Crear el PIN en el Mapa
                val marker = Marker(map)
                marker.position = geoPoint
                marker.title = nombre

                if (estado == "Lleno" || cap >= 90) {
                    marker.icon.setTint(Color.parseColor("#EF4444")) // Rojo
                } else if (estado == "Mantenimiento") {
                    marker.icon.setTint(Color.parseColor("#64748B")) // Gris
                } else {
                    marker.icon.setTint(Color.parseColor("#10B981")) // Verde
                }

                marker.setOnMarkerClickListener { _, _ ->
                    mostrarDetallePunto(nombre, cap, estado)
                    map.controller.animateTo(marker.position)
                    true
                }
                map.overlays.add(marker)

                // 2. Crear el BOTÓN en el Carrusel Inferior
                val btnChip = TextView(this).apply {
                    text = nombre
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(40, 20, 40, 20)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 50f
                        setColor(Color.parseColor("#10B981")) // Verde Usuario
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 24, 0)
                    }

                    setOnClickListener {
                        map.controller.animateTo(geoPoint)
                        map.controller.setZoom(18.0)
                        mostrarDetallePunto(nombre, cap, estado)
                    }
                }
                layoutPuntosRapidos.addView(btnChip)

            } while (cursor.moveToNext())
        }
        cursor.close()

        // 3. AUTO-FOCO: Viajar al primer contenedor encontrado
        primerPunto?.let {
            map.controller.setCenter(it)
            map.controller.setZoom(16.0)
        }

        map.invalidate()
    }

    private fun mostrarDetallePunto(nombre: String, capacidad: Int, estado: String) {
        tvSeleccionaPuntoUser.visibility = View.GONE
        layoutDatosPuntoUser.visibility = View.VISIBLE

        tvNombrePuntoUser.text = "Ubicación: $nombre"
        pbCapacidadUser.progress = capacidad

        if (estado == "Lleno" || capacidad >= 90) {
            tvEstadoPuntoUser.text = "Capacidad Alta - Evite usar"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#EF4444"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        } else if (estado == "Mantenimiento") {
            tvEstadoPuntoUser.text = "En Mantenimiento"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#64748B"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
        } else {
            tvEstadoPuntoUser.text = "Operativo ($capacidad% lleno)"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#10B981"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}