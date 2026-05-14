package com.meza.ecoresiduos.user // IMPORTANTE: Ajusta esto a tu paquete real

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class UserPuntosActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbHelper: DatabaseHelper

    // UI Inferior
    private lateinit var tvSeleccionaPuntoUser: TextView
    private lateinit var layoutDatosPuntoUser: LinearLayout
    private lateinit var tvNombrePuntoUser: TextView
    private lateinit var tvEstadoPuntoUser: TextView
    private lateinit var pbCapacidadUser: ProgressBar
    private lateinit var layoutPuntosRapidos: LinearLayout

    // LANZADOR DEL ESCÁNER QR
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

        // Vinculación
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

        // ==========================================
        // CONFIGURACIÓN DE LOS BOTONES FLOTANTES
        // ==========================================

        // 1. Lector QR
        val fabEscanearQR = findViewById<FloatingActionButton>(R.id.fabEscanearQR)
        fabEscanearQR.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Apunta al código QR del contenedor")
            options.setCameraId(0)
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)
            options.setOrientationLocked(false) // <--- ESTA LÍNEA EVITA QUE SE ACUESTE LA PANTALLA
            barcodeLauncher.launch(options)
        }

        // 2. Escáner con IA
        val fabAnalisisIA = findViewById<FloatingActionButton>(R.id.fabAnalisisIA)
        fabAnalisisIA.setOnClickListener {
            startActivity(Intent(this, AnalisisIAActivity::class.java))
        }

        // 3. Suscripción Premium
        val fabPremium = findViewById<FloatingActionButton>(R.id.fabPremium)
        fabPremium.setOnClickListener {
            startActivity(Intent(this, SuscripcionActivity::class.java))
        }

        configurarMapa()
    }

    private fun configurarMapa() {
        map.setMultiTouchControls(true)
        val tolucaCentro = GeoPoint(19.2826, -99.6557)
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

                // Crear el PIN
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

                // Crear el Chip en el Carrusel
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
                        map.controller.animateTo(geoPoint)
                        map.controller.setZoom(18.0)
                        mostrarDetallePunto(nombre, cap, estado)
                    }
                }
                layoutPuntosRapidos.addView(btnChip)

            } while (cursor.moveToNext())
        }
        cursor.close()

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

    private fun simularContenedorInteligente(qrLeido: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("✅ Contenedor Detectado")

        val mensaje = """
            📍 Ubicación: Contenedor Registrado
            📊 Capacidad Actual: 45% (Espacio disponible)
            🛠️ Estatus: Operativo
            
            Información del QR: $qrLeido
        """.trimIndent()

        builder.setMessage(mensaje)
        builder.setPositiveButton("Registrar Residuos Aquí") { _, _ ->
            // 1. Limpiamos el texto del QR (quitamos el "ECO-PUNTO:" para tener solo el nombre)
            val nombreContenedor = qrLeido.replace("ECO-PUNTO:", "")

            // 2. Preparamos el viaje a tu pantalla de registro
            val intent = Intent(this, UserReporteActivity::class.java)

            // 3. Le mandamos el dato como si fuera un paquete de paquetería
            intent.putExtra("CONTENEDOR_SELECCIONADO", nombreContenedor)

            // 4. Iniciamos el viaje y cerramos el mapa
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("Cerrar", null)
        builder.show()
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}