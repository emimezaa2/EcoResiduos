package com.meza.ecoresiduos.admin

import android.content.ContentValues
import android.graphics.Color
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

    // Referencias a la UI
    private lateinit var tvSeleccionaPunto: TextView
    private lateinit var layoutEdicionPunto: LinearLayout
    private lateinit var tvNombrePuntoEdicion: TextView
    private lateinit var spinnerEstado: Spinner
    private lateinit var tvLabelCapacidad: TextView
    private lateinit var sliderCapacidad: Slider
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración necesaria para OpenStreetMap
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_admin_puntos)

        dbHelper = DatabaseHelper(this)

        // Vincular vistas
        map = findViewById(R.id.mapAdmin)
        tvSeleccionaPunto = findViewById(R.id.tvSeleccionaPunto)
        layoutEdicionPunto = findViewById(R.id.layoutEdicionPunto)
        tvNombrePuntoEdicion = findViewById(R.id.tvNombrePuntoEdicion)
        spinnerEstado = findViewById(R.id.spinnerEstadoPunto)
        tvLabelCapacidad = findViewById(R.id.tvLabelCapacidad)
        sliderCapacidad = findViewById(R.id.sliderCapacidad)
        btnGuardar = findViewById(R.id.btnGuardarPunto)

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

        // Receptor de eventos: Clic largo para crear punto, Clic simple para deseleccionar
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
        // Limpiamos marcadores actuales para no duplicar
        map.overlays.removeAll { it is Marker }

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PUNTOS}", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_NOMBRE))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_LAT))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_LON))
                val cap = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PUNTO_ESTADO))

                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.title = nombre

                // Si el contenedor está lleno, pintamos el pin de rojo
                if (cap >= 90) {
                    marker.icon.setTint(Color.RED)
                }

                marker.setOnMarkerClickListener { _, _ ->
                    prepararEdicionPunto(id, nombre, cap, estado)
                    map.controller.animateTo(marker.position)
                    true
                }
                map.overlays.add(marker)
            } while (cursor.moveToNext())
        }
        cursor.close()
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
        Toast.makeText(this, "Punto registrado con éxito", Toast.LENGTH_SHORT).show()
        cargarPuntosDesdeBD()
    }

    private fun prepararEdicionPunto(id: Int, nombre: String, cap: Int, estado: String) {
        idPuntoSeleccionado = id
        tvSeleccionaPunto.visibility = View.GONE
        layoutEdicionPunto.visibility = View.VISIBLE

        tvNombrePuntoEdicion.text = "Punto: $nombre"
        tvLabelCapacidad.text = "Nivel de Llenado: $cap%"
        sliderCapacidad.value = cap.toFloat()

        // Configurar Spinner de estados
        val estados = arrayOf("Disponible", "Lleno", "En Recolección")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        spinnerEstado.adapter = adapter
        spinnerEstado.setSelection(estados.indexOf(estado))
    }

    private fun configurarControlesEdicion() {
        // Listener del Slider para actualizar texto en tiempo real
        sliderCapacidad.addOnChangeListener { _, value, _ ->
            tvLabelCapacidad.text = "Nivel de Llenado: ${value.toInt()}%"
        }

        // Botón Guardar
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

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}