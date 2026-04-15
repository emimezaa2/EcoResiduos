package com.meza.ecoresiduos.admin

import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class AdminPuntosActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbHelper: DatabaseHelper
    private var idPuntoSeleccionado: Int = -1

    // UI Panel de Edición
    private lateinit var tvSeleccionaPunto: TextView
    private lateinit var layoutEdicionPunto: LinearLayout
    private lateinit var tvNombrePuntoEdicion: TextView
    private lateinit var spinnerEstado: Spinner
    private lateinit var sliderCapacidad: Slider
    private lateinit var tvLabelCapacidad: TextView
    private lateinit var btnGuardar: Button

    // Carrusel de Navegación
    private lateinit var layoutPuntosRapidos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_admin_puntos)

        dbHelper = DatabaseHelper(this)

        // Vinculación
        map = findViewById(R.id.mapAdmin)
        tvSeleccionaPunto = findViewById(R.id.tvSeleccionaPunto)
        layoutEdicionPunto = findViewById(R.id.layoutEdicionPunto)
        tvNombrePuntoEdicion = findViewById(R.id.tvNombrePuntoEdicion)
        spinnerEstado = findViewById(R.id.spinnerEstadoPunto)
        sliderCapacidad = findViewById(R.id.sliderCapacidad)
        tvLabelCapacidad = findViewById(R.id.tvLabelCapacidad)
        btnGuardar = findViewById(R.id.btnGuardarPunto)
        layoutPuntosRapidos = findViewById(R.id.layoutPuntosRapidos)

        findViewById<TextView>(R.id.btnBackAdminPuntos).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        configurarMapa()
        configurarControlesEdicion()
    }

    private fun configurarMapa() {
        map.setMultiTouchControls(true)
        val tolucaCentro = GeoPoint(19.2826, -99.6557)
        map.controller.setZoom(16.0)
        map.controller.setCenter(tolucaCentro)

        val mReceive = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                layoutEdicionPunto.visibility = View.GONE
                tvSeleccionaPunto.visibility = View.VISIBLE
                idPuntoSeleccionado = -1
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { mostrarDialogoNuevoPunto(it.latitude, it.longitude) }
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(mReceive))

        cargarPuntosDesdeBD()
    }

    private fun cargarPuntosDesdeBD() {
        map.overlays.removeAll { it is Marker }
        layoutPuntosRapidos.removeAllViews()

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PUNTOS}", null)

        var primerPunto: GeoPoint? = null

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_NOMBRE))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_LAT))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_LON))
                val cap = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_ESTADO))

                val geoPoint = GeoPoint(lat, lon)
                if (primerPunto == null) primerPunto = geoPoint

                // Marcador en el mapa
                val marker = Marker(map)
                marker.position = geoPoint
                marker.title = nombre
                if (cap >= 90) marker.icon.setTint(Color.RED)

                marker.setOnMarkerClickListener { _, _ ->
                    prepararEdicionPunto(id, nombre, cap, estado)
                    map.controller.animateTo(marker.position)
                    true
                }
                map.overlays.add(marker)

                // Botón en el Carrusel Inferior
                val btnChip = TextView(this).apply {
                    text = nombre
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(40, 20, 40, 20)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 50f
                        setColor(Color.parseColor("#1E293B"))
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 24, 0)
                    }

                    setOnClickListener {
                        map.controller.animateTo(geoPoint)
                        map.controller.setZoom(18.0)
                        prepararEdicionPunto(id, nombre, cap, estado)
                    }
                }
                layoutPuntosRapidos.addView(btnChip)

            } while (cursor.moveToNext())
        }
        cursor.close()

        // Auto-foco
        primerPunto?.let {
            map.controller.setCenter(it)
            map.controller.setZoom(16.0)
        }
        map.invalidate()
    }

    private fun mostrarDialogoNuevoPunto(lat: Double, lon: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nuevo Punto de Recolección")

        val input = EditText(this)
        input.hint = "Nombre del lugar (ej. Parque Alameda)"
        builder.setView(input)

        builder.setPositiveButton("Crear") { _, _ ->
            val nombre = input.text.toString()
            if (nombre.isNotEmpty()) {
                guardarNuevoPuntoBD(nombre, lat, lon)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
    private fun guardarNuevoPuntoBD(nombre: String, lat: Double, lon: Double) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PUNTO_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_PUNTO_LAT, lat)
            put(DatabaseHelper.COLUMN_PUNTO_LON, lon)
            put(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD, 0)
            put(DatabaseHelper.COLUMN_PUNTO_ESTADO, "Disponible")
        }
        db.insert(DatabaseHelper.TABLE_PUNTOS, null, values)
        Toast.makeText(this, "Punto registrado", Toast.LENGTH_SHORT).show()
        cargarPuntosDesdeBD()
    }
    private fun prepararEdicionPunto(id: Int, nombre: String, cap: Int, estado: String) {
        idPuntoSeleccionado = id
        tvSeleccionaPunto.visibility = View.GONE
        layoutEdicionPunto.visibility = View.VISIBLE

        tvNombrePuntoEdicion.text = "Punto: $nombre"
        sliderCapacidad.value = cap.toFloat()
        tvLabelCapacidad.text = "Nivel de Llenado: $cap%" // Set inicial al tocar el pin

        val estados = arrayOf("Disponible", "Lleno", "Mantenimiento")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        spinnerEstado.adapter = adapter
        spinnerEstado.setSelection(estados.indexOf(estado))
    }

    private fun configurarControlesEdicion() {
        // Actualización dinámica del texto al mover el slider
        sliderCapacidad.addOnChangeListener { _, value, _ ->
            tvLabelCapacidad.text = "Nivel de Llenado: ${value.toInt()}%"
        }

        btnGuardar.setOnClickListener {
            if (idPuntoSeleccionado != -1) {
                val nuevaCap = sliderCapacidad.value.toInt()
                val nuevoEstado = spinnerEstado.selectedItem.toString()

                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD, nuevaCap)
                    put(DatabaseHelper.COLUMN_PUNTO_ESTADO, nuevoEstado)
                }

                db.update(DatabaseHelper.TABLE_PUNTOS, values,
                    "${DatabaseHelper.COLUMN_PUNTO_ID} = ?", arrayOf(idPuntoSeleccionado.toString()))

                Toast.makeText(this, "Infraestructura actualizada", Toast.LENGTH_SHORT).show()
                layoutEdicionPunto.visibility = View.GONE
                tvSeleccionaPunto.visibility = View.VISIBLE
                cargarPuntosDesdeBD()
            }
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}