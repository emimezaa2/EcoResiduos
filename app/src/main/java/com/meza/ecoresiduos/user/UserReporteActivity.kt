package com.meza.ecoresiduos.user

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// 1. CLASE AUXILIAR PARA EL SPINNER (Para mostrar el nombre pero guardar el ID)
data class PuntoMenu(val id: Int, val nombre: String) {
    override fun toString(): String = nombre
}

class UserReporteActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivEvidencia: ImageView
    private lateinit var placeholderFoto: LinearLayout
    private lateinit var spinnerPuntos: Spinner

    private var fotoBitmap: Bitmap? = null
    private var fotoPath: String = ""

    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            fotoBitmap = bitmap
            ivEvidencia.setImageBitmap(bitmap)
            placeholderFoto.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_reporte)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<TextView>(R.id.btnBackReporte)
        val tvDisplayPeso = findViewById<TextView>(R.id.tvDisplayPeso)
        val sliderPeso = findViewById<Slider>(R.id.sliderPeso)
        val toggleTipo = findViewById<MaterialButtonToggleGroup>(R.id.toggleTipo)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarReporte)
        val cardFoto = findViewById<MaterialCardView>(R.id.cardFoto)

        ivEvidencia = findViewById(R.id.ivEvidencia)
        placeholderFoto = findViewById(R.id.placeholderFoto)
        spinnerPuntos = findViewById(R.id.spinnerPuntos)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Actualizar número del Slider
        sliderPeso.addOnChangeListener { _, value, _ ->
            tvDisplayPeso.text = "${value} kg"
        }

        // Permiso y Cámara
        cardFoto.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            } else {
                tomarFotoLauncher.launch(null)
            }
        }

        // Cargar los contenedores que el Admin creó en el mapa
        cargarPuntosDisponibles()

        btnFinalizar.setOnClickListener {
            if (fotoBitmap == null) {
                Toast.makeText(this, "Por favor toma una foto de la evidencia.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val puntoSeleccionado = spinnerPuntos.selectedItem as? PuntoMenu
            if (puntoSeleccionado == null || puntoSeleccionado.id == -1) {
                Toast.makeText(this, "Por favor selecciona un punto válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val peso = sliderPeso.value.toDouble()
            val tipoResiduo = if (toggleTipo.checkedButtonId == R.id.btnOrganico) "Orgánico" else "Seco"

            val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)

            if (userId != -1) {
                // 2. Guardar la foto físicamente y obtener la ruta
                fotoPath = guardarImagenEnAlmacenamiento(fotoBitmap!!)

                // 3. Mandar todos los datos cruzados a la BD
                guardarReporteEnBD(userId, puntoSeleccionado.id, peso, tipoResiduo, fotoPath)
            }
        }
    }

    private fun cargarPuntosDisponibles() {
        val db = dbHelper.readableDatabase
        val listaPuntos = mutableListOf<PuntoMenu>()

        // Solo mostramos los contenedores que NO están en mantenimiento
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_PUNTO_ID}, ${DatabaseHelper.COLUMN_PUNTO_NOMBRE} FROM ${DatabaseHelper.TABLE_PUNTOS} WHERE ${DatabaseHelper.COLUMN_PUNTO_ESTADO} != 'Mantenimiento'", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val nombre = cursor.getString(1)
                listaPuntos.add(PuntoMenu(id, nombre))
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (listaPuntos.isEmpty()) {
            listaPuntos.add(PuntoMenu(-1, "Sin puntos disponibles en el área"))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaPuntos)
        spinnerPuntos.adapter = adapter
    }

    private fun guardarImagenEnAlmacenamiento(bitmap: Bitmap): String {
        // Guardamos la foto en el almacenamiento seguro de la app
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "eco_evidencia_${timeStamp}.jpg"
        val file = File(filesDir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) // 85% de calidad para no saturar memoria
        }
        return file.absolutePath
    }

    private fun guardarReporteEnBD(userId: Int, puntoId: Int, peso: Double, tipo: String, rutaFoto: String) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_REPORT_USER_ID, userId)
            put(DatabaseHelper.COLUMN_REPORT_PUNTO_ID, puntoId) // <- Vinculado al punto del Admin!
            put(DatabaseHelper.COLUMN_REPORT_PESO, peso)
            put(DatabaseHelper.COLUMN_REPORT_TIPO, tipo)
            put(DatabaseHelper.COLUMN_REPORT_FOTO_PATH, rutaFoto) // <- Guardando la foto
            put(DatabaseHelper.COLUMN_REPORT_STATUS, "Pendiente")

            // Fecha automática
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            put(DatabaseHelper.COLUMN_REPORT_FECHA, fechaHoy)
        }

        val newRowId = db.insert(DatabaseHelper.TABLE_REPORTS, null, values)

        if (newRowId != -1L) {
            Toast.makeText(this, "Registro procesado y foto guardada.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}