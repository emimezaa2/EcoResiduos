package com.meza.ecoresiduos.user

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class UserPuntosActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbHelper: DatabaseHelper
    private var miUbicacionReal: GeoPoint? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var userId: Int = -1

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

        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)

        map = findViewById(R.id.mapUser)
        tvSeleccionaPuntoUser = findViewById(R.id.tvSeleccionaPuntoUser)
        layoutDatosPuntoUser = findViewById(R.id.layoutDatosPuntoUser)
        tvNombrePuntoUser = findViewById(R.id.tvNombrePuntoUser)
        tvEstadoPuntoUser = findViewById(R.id.tvEstadoPuntoUser)
        pbCapacidadUser = findViewById(R.id.pbCapacidadUser)
        layoutPuntosRapidos = findViewById(R.id.layoutPuntosRapidos)

        findViewById<TextView>(R.id.btnBackUserPuntos).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fabEscanearQR).setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Apunta al codigo QR del contenedor")
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
        configurarGestionDePuntos()
        solicitarPermisosGPS()
    }

    private fun configurarMapa() {
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
    }

    private fun configurarGestionDePuntos() {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { coordenadas ->
                    verificarYMostrarDialogoCreacion(coordenadas)
                }
                return true
            }
        }
        val eventsOverlay = MapEventsOverlay(receiver)
        map.overlays.add(eventsOverlay)
    }

    private fun verificarYMostrarDialogoCreacion(gps: GeoPoint) {
        val db = dbHelper.readableDatabase
        val query = "SELECT ${DatabaseHelper.COLUMN_COM_ID}, ${DatabaseHelper.COLUMN_COM_NOMBRE} FROM ${DatabaseHelper.TABLE_COMMUNITIES} WHERE ${DatabaseHelper.COLUMN_COM_CREADOR} = ?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        val comunidadesNombres = mutableListOf<String>()
        val comunidadesIds = mutableListOf<Int>()

        if (cursor.moveToFirst()) {
            do {
                comunidadesIds.add(cursor.getInt(0))
                comunidadesNombres.add(cursor.getString(1))
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (comunidadesIds.isEmpty()) {
            Toast.makeText(this, "Solo los creadores pueden agregar puntos", Toast.LENGTH_SHORT).show()
            return
        }

        // --- CONSTRUCCIÓN DEL DIÁLOGO MODERNO ---
        val dialog = android.app.Dialog(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 48f // Esquinas fuertemente redondeadas
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val title = TextView(this).apply {
            text = "Nuevo Punto de Acopio"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 40)
        }

        val spinnerComunidades = Spinner(this).apply {
            adapter = ArrayAdapter(this@UserPuntosActivity, android.R.layout.simple_spinner_dropdown_item, comunidadesNombres)
            // Reutilizamos tu diseño técnico limpio
            background = getDrawable(R.drawable.bg_input)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val etNombre = EditText(this).apply {
            hint = "Nombre del contenedor (Ej. Casa C)"
            background = getDrawable(R.drawable.bg_input)
            setPadding(40, 0, 40, 0)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140).apply {
                setMargins(0, 0, 0, 48)
            }
        }

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnCancelar = TextView(this).apply {
            text = "Cancelar"
            setTextColor(Color.parseColor("#64748B"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(40, 20, 40, 20)
            setOnClickListener { dialog.dismiss() }
        }

        val btnGuardar = MaterialButton(this).apply {
            text = "Guardar Punto"
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
            setTextColor(Color.WHITE)
            cornerRadius = 24
            setOnClickListener {
                val nombrePunto = etNombre.text.toString().trim()
                if (nombrePunto.isNotEmpty()) {
                    val comIdAsignada = comunidadesIds[spinnerComunidades.selectedItemPosition]
                    guardarNuevoPunto(nombrePunto, gps, comIdAsignada)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this@UserPuntosActivity, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnLayout.addView(btnCancelar)
        btnLayout.addView(btnGuardar)

        mainLayout.addView(title)
        mainLayout.addView(spinnerComunidades)
        mainLayout.addView(etNombre)
        mainLayout.addView(btnLayout)

        dialog.setContentView(mainLayout)
        // Volvemos el fondo de la ventana transparente para que se vea nuestro diseño redondeado
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun guardarNuevoPunto(nombre: String, gps: GeoPoint, comId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PUNTO_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_PUNTO_LAT, gps.latitude)
            put(DatabaseHelper.COLUMN_PUNTO_LON, gps.longitude)
            put(DatabaseHelper.COLUMN_PUNTO_CAPACIDAD, 0)
            put(DatabaseHelper.COLUMN_PUNTO_ESTADO, "Disponible")
            put(DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID, comId)
        }
        db.insert(DatabaseHelper.TABLE_PUNTOS, null, values)
        Toast.makeText(this, "Punto agregado a tu comunidad", Toast.LENGTH_SHORT).show()
        cargarPuntosEnMapa()
    }

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
            cargarPuntosEnMapa()
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
                    title = "Ubicacion actual"
                }
                miMarker.icon.setTint(Color.parseColor("#3B82F6"))
                map.overlays.add(miMarker)

                map.controller.setCenter(miUbicacionReal)
                map.controller.setZoom(16.0)
            }
            cargarPuntosEnMapa()
        }
    }

    private fun cargarPuntosEnMapa() {
        layoutPuntosRapidos.removeAllViews()
        map.overlays.removeAll { it is Marker && it.title != "Ubicacion actual" }
        map.overlays.removeAll { it is Polyline }

        val db = dbHelper.readableDatabase

        // CONSULTA ORDENADA: Agrupa por tipo (Global primero) y luego por nombre de comunidad
        val query = """
            SELECT p.${DatabaseHelper.COLUMN_PUNTO_ID}, p.${DatabaseHelper.COLUMN_PUNTO_NOMBRE}, 
                   p.${DatabaseHelper.COLUMN_PUNTO_LAT}, p.${DatabaseHelper.COLUMN_PUNTO_LON}, 
                   p.${DatabaseHelper.COLUMN_PUNTO_CAPACIDAD}, p.${DatabaseHelper.COLUMN_PUNTO_ESTADO}, 
                   c.${DatabaseHelper.COLUMN_COM_TIPO}, c.${DatabaseHelper.COLUMN_COM_CREADOR},
                   c.${DatabaseHelper.COLUMN_COM_NOMBRE}
            FROM ${DatabaseHelper.TABLE_PUNTOS} p
            INNER JOIN ${DatabaseHelper.TABLE_COMMUNITIES} c ON p.${DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID} = c.${DatabaseHelper.COLUMN_COM_ID}
            WHERE p.${DatabaseHelper.COLUMN_PUNTO_COMUNIDAD_ID} IN (
                SELECT ${DatabaseHelper.COLUMN_MIEMBRO_COM_ID} 
                FROM ${DatabaseHelper.TABLE_MIEMBROS} 
                WHERE ${DatabaseHelper.COLUMN_MIEMBRO_USER_ID} = ?
            )
            ORDER BY c.${DatabaseHelper.COLUMN_COM_TIPO} ASC, c.${DatabaseHelper.COLUMN_COM_NOMBRE} ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var comunidadActual = ""

        if (cursor.moveToFirst()) {
            do {
                val puntoId = cursor.getInt(0)
                val nombre = cursor.getString(1)
                val lat = cursor.getDouble(2)
                val lon = cursor.getDouble(3)
                val capacidad = cursor.getInt(4)
                val estado = cursor.getString(5)
                val tipoComunidad = cursor.getString(6)
                val creadorId = cursor.getInt(7)
                val nombreComunidad = cursor.getString(8) // <-- Atrapamos el nombre de la comunidad

                val puntoDestino = GeoPoint(lat, lon)
                val esGlobal = (tipoComunidad == "Global")

                val marker = Marker(map).apply {
                    position = puntoDestino
                    title = if (esGlobal) "$nombre (Verificado)" else nombre
                    subDescription = "Comunidad: $nombreComunidad | Estado: $estado"
                }

                marker.icon.setTint(if (esGlobal) Color.parseColor("#1D4ED8") else Color.parseColor("#10B981"))

                marker.setOnMarkerClickListener { _, _ ->
                    mostrarDetallePunto(puntoId, nombre, capacidad, estado, creadorId == userId)
                    map.controller.animateTo(marker.position)
                    true
                }
                map.overlays.add(marker)

                if (miUbicacionReal != null) {
                    val linea = Polyline().apply {
                        color = if (esGlobal) Color.parseColor("#1D4ED8") else Color.parseColor("#10B981")
                        width = 6f
                        setPoints(listOf(miUbicacionReal, puntoDestino))
                    }
                    map.overlays.add(linea)
                }

                // ==========================================
                // LÓGICA DE AGRUPACIÓN EN EL CARRUSEL
                // ==========================================
                if (nombreComunidad != comunidadActual) {
                    // Si la comunidad cambió, insertamos un título separador en el carrusel
                    val tituloComunidad = TextView(this).apply {
                        text = if (esGlobal) "🌍 Red Global" else "🏡 Grupo: $nombreComunidad"
                        setTextColor(Color.parseColor("#0F172A"))
                        textSize = 12f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 16, 0)
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                            setMargins(if (comunidadActual.isEmpty()) 0 else 32, 0, 0, 0)
                        }
                    }
                    layoutPuntosRapidos.addView(tituloComunidad)
                    comunidadActual = nombreComunidad
                }

                // Insertamos el botón (chip) de forma normal
                val btnChip = TextView(this).apply {
                    text = nombre
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(40, 20, 40, 20)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 50f
                        setColor(if (esGlobal) Color.parseColor("#1D4ED8") else Color.parseColor("#10B981"))
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 16, 0)
                    }
                    setOnClickListener {
                        map.controller.animateTo(puntoDestino)
                        mostrarDetallePunto(puntoId, nombre, capacidad, estado, creadorId == userId)
                    }
                }
                layoutPuntosRapidos.addView(btnChip)

            } while (cursor.moveToNext())
        }
        cursor.close()
        map.invalidate()
    }

    private fun mostrarDetallePunto(puntoId: Int, nombre: String, capacidad: Int, estado: String, soyElCreador: Boolean) {
        tvSeleccionaPuntoUser.visibility = View.GONE
        layoutDatosPuntoUser.visibility = View.VISIBLE

        tvNombrePuntoUser.text = "Ubicacion: $nombre"
        pbCapacidadUser.progress = capacidad

        if (estado == "Lleno" || capacidad >= 90) {
            tvEstadoPuntoUser.text = "Capacidad limite alcanzada"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#EF4444"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        } else {
            tvEstadoPuntoUser.text = "Disponible - Capacidad al $capacidad%"
            tvEstadoPuntoUser.setTextColor(Color.parseColor("#10B981"))
            pbCapacidadUser.progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        }

        // Eliminar botones secundarios previos para evitar duplicacion
        if (layoutDatosPuntoUser.childCount > 3) {
            layoutDatosPuntoUser.removeViews(3, layoutDatosPuntoUser.childCount - 3)
        }

        // Si soy el creador de la comunidad a la que pertenece este punto, puedo eliminarlo
        if (soyElCreador) {
            val btnEliminar = MaterialButton(this).apply {
                text = "Eliminar Punto"
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 16, 0, 0)
                }
                setOnClickListener {
                    eliminarPuntoDeBaseDatos(puntoId)
                }
            }
            layoutDatosPuntoUser.addView(btnEliminar)
        }
    }

    private fun eliminarPuntoDeBaseDatos(puntoId: Int) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_PUNTOS, "${DatabaseHelper.COLUMN_PUNTO_ID} = ?", arrayOf(puntoId.toString()))
        Toast.makeText(this, "Punto eliminado del sistema", Toast.LENGTH_SHORT).show()

        tvSeleccionaPuntoUser.visibility = View.VISIBLE
        layoutDatosPuntoUser.visibility = View.GONE
        cargarPuntosEnMapa()
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