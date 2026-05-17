package com.meza.ecoresiduos.admin

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
    private var miUbicacionReal: GeoPoint? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_admin_puntos)

        dbHelper = DatabaseHelper(this)
        map = findViewById(R.id.mapAdmin)

        findViewById<TextView>(R.id.btnBackAdminPuntos)?.setOnClickListener { finish() }

        configurarMapa()
        configurarEventosDeToque()
        solicitarPermisosGPS()
    }

    private fun configurarMapa() {
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
    }

    // --- LÓGICA DE GPS RECUPERADA ---
    private fun solicitarPermisosGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            obtenerMiUbicacion()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerMiUbicacion()
        } else {
            map.controller.setCenter(GeoPoint(19.2826, -99.6557))
            cargarPuntosExistentes()
        }
    }

    private fun obtenerMiUbicacion() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                miUbicacionReal = GeoPoint(location.latitude, location.longitude)

                val miMarker = Marker(map).apply {
                    position = miUbicacionReal
                    title = "Mi Ubicación (Admin)"
                }
                miMarker.icon.setTint(Color.parseColor("#3B82F6")) // Azul estándar para ubicación
                map.overlays.add(miMarker)

                map.controller.setCenter(miUbicacionReal)
                map.controller.setZoom(16.0)
            }
            cargarPuntosExistentes()
        }
    }

    // --- LÓGICA DE MAPA Y SEGURIDAD INTACTA ---
    private fun configurarEventosDeToque() {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { mostrarDialogoCrearPunto(it) }
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(receiver))
    }

    private fun mostrarDialogoCrearPunto(puntoGps: GeoPoint) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_COM_ID}, ${DatabaseHelper.COLUMN_COM_NOMBRE} FROM ${DatabaseHelper.TABLE_COMMUNITIES} WHERE ${DatabaseHelper.COLUMN_COM_TIPO} = 'Global'", null)

        val listaNombres = mutableListOf<String>()
        val listaIds = mutableListOf<Int>()

        if (cursor.moveToFirst()) {
            do {
                listaIds.add(cursor.getInt(0))
                listaNombres.add(cursor.getString(1))
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (listaIds.isEmpty()) {
            Toast.makeText(this, "Debe crear una comunidad Global primero", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nuevo Punto Global")

        val layoutFormulario = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etNombrePunto = EditText(this).apply {
            hint = "Nombre del contenedor (Ej. Centro Principal)"
            setPadding(40, 20, 40, 20)
            background = getDrawable(R.drawable.bg_input)
        }
        layoutFormulario.addView(etNombrePunto)

        builder.setView(layoutFormulario)

        var seleccionIndex = 0
        builder.setSingleChoiceItems(listaNombres.toTypedArray(), 0) { _, index -> seleccionIndex = index }

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nombre = etNombrePunto.text.toString().trim()
            if (nombre.isNotEmpty()) {
                guardarPuntoEnBaseDatos(nombre, puntoGps, listaIds[seleccionIndex])
                dialog.dismiss()
            } else {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun guardarPuntoEnBaseDatos(nombre: String, gps: GeoPoint, comunidadId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PUNTO_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_PUNTO_LAT, gps.latitude)
            put(DatabaseHelper.COLUMN_PUNTO_LON, gps.longitude)
            put(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD, 0)
            put(DatabaseHelper.COLUMN_PUNTO_ESTADO, "Disponible")
            put(DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID, comunidadId)
        }
        db.insert(DatabaseHelper.TABLE_PUNTOS, null, values)
        Toast.makeText(this, "Punto global asignado correctamente", Toast.LENGTH_SHORT).show()
        cargarPuntosExistentes()
    }

    private fun cargarPuntosExistentes() {
        map.overlays.removeAll { it is Marker && it.title != "Mi Ubicación (Admin)" }

        val db = dbHelper.readableDatabase
        val query = """
            SELECT p.${DatabaseHelper.COLUMN_PUNTO_NOMBRE}, p.${DatabaseHelper.COLUMN_PUNTO_LAT}, 
                   p.${DatabaseHelper.COLUMN_PUNTO_LON}, c.${DatabaseHelper.COLUMN_COM_NOMBRE}
            FROM ${DatabaseHelper.TABLE_PUNTOS} p
            INNER JOIN ${DatabaseHelper.TABLE_COMMUNITIES} c ON p.${DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID} = c.${DatabaseHelper.COLUMN_COM_ID}
            WHERE c.${DatabaseHelper.COLUMN_COM_TIPO} = 'Global'
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val nombrePunto = cursor.getString(0)
                val lat = cursor.getDouble(1)
                val lon = cursor.getDouble(2)
                val nombreComunidad = cursor.getString(3)

                val marker = Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    title = "$nombrePunto"
                    subDescription = "Comunidad: $nombreComunidad"
                }
                marker.icon.setTint(Color.parseColor("#1D4ED8")) // Azul corporativo
                map.overlays.add(marker)

            } while (cursor.moveToNext())
        }
        cursor.close()
        map.invalidate()
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}