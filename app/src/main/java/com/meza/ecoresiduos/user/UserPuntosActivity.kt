package com.meza.ecoresiduos.user

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.meza.ecoresiduos.R

class UserPuntosActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Regla obligatoria de OpenStreetMap: Identificar nuestra app
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_user_puntos)

        val btnBack = findViewById<TextView>(R.id.btnBackPuntos)
        val container = findViewById<LinearLayout>(R.id.containerPuntos)
        map = findViewById(R.id.map)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        configurarMapa()
        cargarTarjetas(container)
    }

    private fun configurarMapa() {
        // Activamos el zoom con los dedos
        map.setMultiTouchControls(true)

        // Coordenadas de Toluca
        val tolucaCentro = GeoPoint(19.2826, -99.6557)

        // Configuramos el zoom y centramos la cámara
        val mapController = map.controller
        mapController.setZoom(15.0)
        mapController.setCenter(tolucaCentro)

        // 1. Añadimos el Punto del Parque Vicente Guerrero
        val marker1 = Marker(map)
        marker1.position = GeoPoint(19.2845, -99.6640)
        marker1.title = "Contenedor: Parque V. Guerrero"
        map.overlays.add(marker1)

        // 2. Añadimos el Punto de la Alameda
        val marker2 = Marker(map)
        marker2.position = GeoPoint(19.2885, -99.6600)
        marker2.title = "Contenedor: Alameda 2000"
        map.overlays.add(marker2)

        // 3. Añadimos el Punto por C.U.
        val marker3 = Marker(map)
        marker3.position = GeoPoint(19.2780, -99.6700)
        marker3.title = "Contenedor: Zona Universitaria"
        map.overlays.add(marker3)

        // Refrescamos el mapa para que muestre los marcadores
        map.invalidate()
    }

    private fun cargarTarjetas(container: LinearLayout) {
        val puntosRed = listOf(
            mapOf("nombre" to "Parque Vicente Guerrero", "distancia" to "0.8 km", "estado" to "Operativo"),
            mapOf("nombre" to "Alameda Central", "distancia" to "1.5 km", "estado" to "Capacidad Alta"),
            mapOf("nombre" to "Zona Universitaria", "distancia" to "2.2 km", "estado" to "Operativo")
        )

        for (punto in puntosRed) {
            val cardView = TextView(this)
            cardView.text = "Ubicación: ${punto["nombre"]}\nDistancia: ${punto["distancia"]}\nEstatus: ${punto["estado"]}"
            cardView.textSize = 15f
            cardView.setTextColor(Color.parseColor("#0F172A"))
            cardView.setPadding(40, 40, 40, 40)
            cardView.setBackgroundColor(Color.WHITE)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            cardView.layoutParams = params

            container.addView(cardView)
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume() // Requerido por osmdroid
    }

    override fun onPause() {
        super.onPause()
        map.onPause() // Requerido por osmdroid
    }
}