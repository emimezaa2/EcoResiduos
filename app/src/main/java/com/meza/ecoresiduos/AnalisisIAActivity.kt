package com.meza.ecoresiduos.user

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.meza.ecoresiduos.R

// Estructura estricta para la base de datos
data class InfoResiduo(val nombre: String, val categoria: String)

class AnalisisIAActivity : AppCompatActivity() {

    private lateinit var tvResultadosIA: TextView
    private lateinit var ivFotoBasura: ImageView
    private lateinit var btnRegistrarIA: MaterialButton
    private lateinit var btnTomarFoto: MaterialButton

    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap

            // Mostrar la foto y quitar el filtro
            ivFotoBasura.setImageBitmap(imageBitmap)
            ivFotoBasura.imageTintList = null
            ivFotoBasura.scaleType = ImageView.ScaleType.CENTER_CROP

            procesarBasuraConIA(imageBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis_ia)

        tvResultadosIA = findViewById(R.id.tvResultadosIA)
        ivFotoBasura = findViewById(R.id.ivFotoBasura)
        btnRegistrarIA = findViewById(R.id.btnRegistrarIA)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)

        findViewById<TextView>(R.id.btnBackIA).setOnClickListener { finish() }

        btnTomarFoto.setOnClickListener {
            abrirCamara()
        }

        btnRegistrarIA.setOnClickListener {
            val intent = Intent(this, UserReporteActivity::class.java)

            // Truco Tech Lead: Leemos lo que dice la pantalla para saber qué mandarle al formulario
            val resultadoPantalla = tvResultadosIA.text.toString()
            if (resultadoPantalla.contains("Orgánico") && !resultadoPantalla.contains("Inorgánico")) {
                intent.putExtra("TIPO_DETECTADO_IA", "Orgánico")
            } else if (resultadoPantalla.contains("Inorgánico")) {
                intent.putExtra("TIPO_DETECTADO_IA", "Inorgánico")
            }

            startActivity(intent)
            finish()
        }

        abrirCamara()
    }

    private fun abrirCamara() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            tomarFotoLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cámara no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarBasuraConIA(bitmap: Bitmap) {
        tvResultadosIA.text = "Analizando con Inteligencia Artificial...\nPor favor espera."
        btnRegistrarIA.isEnabled = false

        val image = InputImage.fromBitmap(bitmap, 0)

        // Exigencia del 75%
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.75f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isEmpty()) {
                    tvResultadosIA.text = "️ Material no identificado claramente.\nIntenta acercar la cámara a un objeto con volumen."
                    return@addOnSuccessListener
                }

                // DICCIONARIO RESTRINGIDO (Solo Orgánico e Inorgánico)
                val diccionario = mapOf(
                    "Plastic" to InfoResiduo("Plástico", "Inorgánico"),
                    "Bottle" to InfoResiduo("Botella", "Inorgánico"),
                    "Cardboard" to InfoResiduo("Cartón", "Inorgánico"),
                    "Paper" to InfoResiduo("Papel", "Inorgánico"),
                    "Can" to InfoResiduo("Lata / Metal", "Inorgánico"),
                    "Tin can" to InfoResiduo("Lata de Aluminio", "Inorgánico"),
                    "Glass" to InfoResiduo("Vidrio", "Inorgánico"),
                    "Mobile phone" to InfoResiduo("Celular", "Inorgánico"),
                    "Gadget" to InfoResiduo("Dispositivo", "Inorgánico"),
                    "Tableware" to InfoResiduo("Cerámica / Loza", "Inorgánico"),
                    "Cup" to InfoResiduo("Vaso / Taza", "Inorgánico"),
                    "Fruit" to InfoResiduo("Fruta", "Orgánico"),
                    "Vegetable" to InfoResiduo("Vegetal", "Orgánico"),
                    "Food" to InfoResiduo("Comida", "Orgánico"),
                    "Plant" to InfoResiduo("Planta / Hoja", "Orgánico")
                )

                // Tomamos SOLO el primer resultado (el de mayor confianza) para un diseño limpio
                val labelPrincipal = labels[0]
                val info = diccionario[labelPrincipal.text]

                // Si la IA detecta algo raro que no está en la lista, asumimos por default que es Inorgánico
                val nombreResiduo = info?.nombre ?: "Objeto Detectado"
                val categoriaResiduo = info?.categoria ?: "Inorgánico"

                // Variable mutable para forzar a 0% si rompe la regla de negocio
                var confianza = (labelPrincipal.confidence * 100).toInt()

                // APLICACIÓN DE LA REGLA DE NEGOCIO PEDIDA
                val reporte: String
                if (categoriaResiduo == "Inorgánico") {
                    confianza = 0 // Forzamos a cero por ciento seguro
                    btnRegistrarIA.isEnabled = false // Bloqueamos el botón de guardar

                    reporte = """
                        RESULTADO DEL ANÁLISIS:
                        
                        $nombreResiduo
                        Clasificación: $categoriaResiduo
                        Seguridad: $confianza%
                        
                        No se puede reciclar.
                        Solo aceptamos material Orgánico.
                    """.trimIndent()
                } else {
                    btnRegistrarIA.isEnabled = true // Habilitamos si cumple con ser orgánico

                    reporte = """
                        RESULTADO DEL ANÁLISIS:
                        
                        $nombreResiduo
                        Clasificación: $categoriaResiduo
                        Seguridad: $confianza%
                    """.trimIndent()
                }

                tvResultadosIA.text = reporte

                // Centramos el texto programáticamente para que se vea como en tu captura
                tvResultadosIA.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
            .addOnFailureListener {
                tvResultadosIA.text = " Error en el motor de IA. Intenta de nuevo."
                tvResultadosIA.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
    }
}