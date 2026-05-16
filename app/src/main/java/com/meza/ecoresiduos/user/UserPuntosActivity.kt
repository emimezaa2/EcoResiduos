package com.meza.ecoresiduos.user

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class UserPuntosActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbHelper: DatabaseHelper
    private var miUbicacionReal: GeoPoint? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    // UI Inferior
    private lateinit var tvSeleccionaPuntoUser: TextView
    private lateinit var layoutDatosPuntoUser: LinearLayout
    private lateinit var tvNombrePuntoUser: TextView
    private lateinit var tvEstadoPuntoUser: TextView
    private lateinit var pbCapacidadUser: ProgressBar
    private lateinit var layoutPuntosRapidos: LinearLayout

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        } else {
            simularContenedorInteligente(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_user_puntos)

        dbHelper = DatabaseHelper(this)

        map = findViewById(R.id.mapUser)
        tvSeleccionaPuntoUser = findViewById(R.id.tvSeleccionaPuntoUser)
        layoutDatosPuntoUser = findViewById(R.id.layoutDatosPuntoUser)
        tvNombrePuntoUser = findViewById(R.id.tvNombrePuntoUser)
        tvEstadoPuntoUser = findViewById(R.id.tvEstadoPuntoUser)
        pbCapacidadUser = findViewById(R.id.pbCapacidadUser)
        layoutPuntosRapidos = findViewById(R.id.layoutPuntosRapidos)

        findViewById<TextView>(R.id.btnBackUserPuntos).setOnClickListener { finish() }

        // Botones Flotantes
        findViewById<FloatingActionButton>(R.id.fabEscanearQR).setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Apunta al código QR del contenedor")
            options.setCameraId(0)
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)
            options.setOrientationLocked(false)
            barcodeLauncher.launch(options)
        }

        findViewById<FloatingActionButton>(R.id.fabAnalisisIA).setOnClickListener {
            startActivity(Intent(this, AnalisisIAActivity::class.java))
        }

        findViewById<FloatingActionButton>(R.id.fabPremium).setOnClickListener {
            startActivity(Intent(this, SuscripcionActivity::class.java))
        }

        configurarMapa()
        solicitarPermisosGPS()
    }

    private fun configurarMapa() {
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
    }

    // ==========================================
    // LÓGICA DE GPS REAL
    // ==========================================
    private fun solicitarPermisosGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            obtenerMiUbicacion()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerMiUbicacion()
            } else {
                Toast.makeText(this, "Se requiere GPS para trazar las rutas", Toast.LENGTH_LONG).show()
                map.controller.setCenter(GeoPoint(19.2826, -99.6557))
                cargarPuntosEnMapa()
            }
        }
    }

    private fun obtenerMiUbicacion() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                miUbicacionReal = GeoPoint(location.latitude, location.longitude)

                // PIN de ubicación del usuario en azul técnico
                val miMarker = Marker(map)
                miMarker.position = miUbicacionReal
                miMarker.title = "Ubicacion actual"
                miMarker.icon.setTint(Color.parseColor("#3B82F6"))
                map.overlays.add(miMarker)

                map.controller.setCenter(miUbicacionReal)
                map.controller.setZoom(16.0)

                cargarPuntosEnMapa()
            } else {
                Toast.makeText(this, "Buscando senal GPS...", Toast.LENGTH_SHORT).show()
                map.controller.setCenter(GeoPoint(19.2826, -99.6557))
                cargarPuntosEnMapa()
            }
        }
    }

    // ==========================================
    // CARGAR PUNTOS DESDE LA TABLA CORRECTA
    // ==========================================
    private fun cargarPuntosEnMapa() {
        layoutPuntosRapidos.removeAllViews()
        // Remover marcadores previos para evitar duplicaciones
        map.overlays.removeAll { it is Marker && it.title != "Ubicacion actual" }
        map.overlays.removeAll { it is Polyline }

        val db = dbHelper.readableDatabase

        // CONSULTA CORREGIDA: Apunta exactamente a la tabla y columnas de tus puntos de recolección
        val query = """
            SELECT ${DatabaseHelper.COLUMN_PUNTO_NOMBRE}, ${DatabaseHelper.COLUMN_PUNTO_LAT}, 
                   ${DatabaseHelper.COLUMN_PUNTO_LON}, ${DatabaseHelper.COLUMN_PUNTO_CAPACIDAD}, 
                   ${DatabaseHelper.COLUMN_PUNTO_ESTADO} 
            FROM ${DatabaseHelper.TABLE_PUNTOS}
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0)
                val lat = cursor.getDouble(1)
                val lon = cursor.getDouble(2)
                val capacidad = cursor.getInt(3)
                val estado = cursor.getString(4)

                val puntoDestino = GeoPoint(lat, lon)

                // 1. Crear marcador del punto
                val marker = Marker(map).apply {
                    position = puntoDestino
                    title = nombre
                    subDescription = "Estado: $estado | Capacidad: $capacidad%"
                }

                // Color según estado técnico
                if (estado == "Lleno" || capacidad >= 90) {
                    marker.icon.setTint(Color.parseColor("#EF4444")) // Rojo
                } else if (estado == "Mantenimiento") {
                    marker.icon.setTint(Color.parseColor("#64748B")) // Gris
                } else {
                    marker.icon.setTint(Color.parseColor("#10B981")) // Verde
                }

                marker.setOnMarkerClickListener { _, _ ->
                    mostrarDetallePunto(nombre, capacidad, estado)
                    map.controller.animateTo(marker.position)
                    true
                }
                map.overlays.add(marker)

                // 2. TRAZAR LA LÍNEA DESDE TU GPS REAL HASTA EL PIN
                if (miUbicacionReal != null) {
                    val linea = Polyline().apply {
                        color = Color.parseColor("#10B981")
                        width = 6f
                        setPoints(listOf(miUbicacionReal, puntoDestino))
                    }
                    map.overlays.add(linea)
                }

                // 3. Agregar botón de acceso rápido inferior
                val btnChip = TextView(this).apply {
                    text = nombre
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(40, 20, 40, 20)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 50f
                        setColor(Color.parseColor("#10B981"))
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 24, 0)
                    }

                    setOnClickListener {
                        map.controller.animateTo(puntoDestino)
                        map.controller.setZoom(17.0)
                        mostrarDetallePunto(nombre, capacidad, estado)
                    }
                }
                layoutPuntosRapidos.addView(btnChip)

            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "No hay puntos de recoleccion registrados", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
        map.invalidate() // Actualizar el mapa visualmente
    }

    private fun mostrarDetallePunto(nombre: String, capacidad: Int, estado: String) {
        tvSeleccionaPuntoUser.visibility = View.GONE
        layoutDatosPuntoUser.visibility = View.VISIBLE

        tvNombrePuntoUser.text = "Ubicacion: $nombre"
        pbCapacidadUser.progress = capacidad

        if (estado == "Lleno" || capacidad >= 90) {
            tvEstadoPuntoUser.text = "Capacidad limite alcanzada"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#EF4444"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        } else if (estado == "Mantenimiento") {
            tvEstadoPuntoUser.text = "Fuera de servicio por mantenimiento"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#64748B"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
        } else {
            tvEstadoPuntoUser.text = "Disponible - Capacidad al $capacidad%"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#10B981"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        }
    }

    private fun simularContenedorInteligente(qrLeido: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Contenedor Detectado")
        val mensaje = "Informacion del QR: $qrLeido"
        builder.setMessage(mensaje)
        builder.setPositiveButton("Registrar Residuos Aqui") { _, _ ->
            val nombreContenedor = qrLeido.replace("ECO-PUNTO:", "")
            val intent = Intent(this, UserReporteActivity::class.java)
            intent.putExtra("CONTENEDOR_SELECCIONADO", nombreContenedor)
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("Cerrar", null)
        builder.show()
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}