package com.meza.ecoresiduos.user

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper
import java.io.File

class UserImpactoActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var containerLotes: LinearLayout
    private lateinit var tvTotalKilos: TextView
    private lateinit var tvTotalEntregas: TextView

    // ==========================================
    // 1. LANZADOR PARA GUARDAR EL PDF
    // ==========================================
    private val crearArchivoPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            escribirPDF(uri)
        } else {
            Toast.makeText(this, "Descarga cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_impacto)

        dbHelper = DatabaseHelper(this)
        containerLotes = findViewById(R.id.containerLotes)
        tvTotalKilos = findViewById(R.id.tvTotalKilos)
        tvTotalEntregas = findViewById(R.id.tvTotalEntregas)

        findViewById<TextView>(R.id.btnBackImpacto).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ==========================================
        // 2. CLIC EN EL BOTÓN DE DESCARGA
        // ==========================================
        findViewById<ImageView>(R.id.btnDescargarPDF).setOnClickListener {
            crearArchivoPdfLauncher.launch("Mi_Reporte_EcoResiduos.pdf")
        }

        cargarDatos()
    }

    // ==========================================
    // 3. LA MAGIA: DIBUJAR Y GUARDAR EL PDF
    // ==========================================
    private fun escribirPDF(uri: Uri) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4 estándar
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = Color.parseColor("#10B981") // Verde EcoResiduos
            }

            // Título
            canvas.drawText("Reporte de Impacto Ambiental - EcoResiduos", 40f, 60f, titlePaint)

            // Totales
            paint.textSize = 16f
            paint.color = Color.BLACK
            canvas.drawText("Total Kilos Reciclados: ${tvTotalKilos.text}", 40f, 100f, paint)
            canvas.drawText("Aportaciones Realizadas: ${tvTotalEntregas.text}", 40f, 130f, paint)

            // Línea separadora
            paint.strokeWidth = 2f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(40f, 150f, 555f, 150f, paint)

            // Historial (Título)
            paint.textSize = 18f
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            canvas.drawText("Detalle de Registros Recientes:", 40f, 190f, paint)

            var yPosition = 230f
            paint.textSize = 14f
            paint.isFakeBoldText = false

            // Consultar la Base de Datos para el PDF
            val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? ORDER BY ${DatabaseHelper.COLUMN_REPORT_ID} DESC LIMIT 15", arrayOf(userId.toString()))

            if (cursor.moveToFirst()) {
                do {
                    val peso = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_PESO))
                    val tipo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_TIPO))
                    val fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_FECHA))
                    val status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_STATUS))

                    // Escribir cada línea
                    val textoLinea = "• $fecha  |  $peso kg de $tipo  |  Estatus: $status"
                    canvas.drawText(textoLinea, 40f, yPosition, paint)
                    yPosition += 30f // Salto de línea

                } while (cursor.moveToNext())
            } else {
                canvas.drawText("No hay registros en el historial.", 40f, yPosition, paint)
            }
            cursor.close()

            pdfDocument.finishPage(page)

            // Guardar físicamente el archivo en el celular
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            Toast.makeText(this, "✅ PDF generado y guardado en tu celular", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Error al crear PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // CÓDIGO ORIGINAL QUE YA TENÍAS (Cargar Datos y Tarjetas)
    // ==========================================
    private fun cargarDatos() {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) return

        val db = dbHelper.readableDatabase
        val cursorStats = db.rawQuery("SELECT SUM(${DatabaseHelper.COLUMN_REPORT_PESO}), COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ?", arrayOf(userId.toString()))

        if (cursorStats.moveToFirst()) {
            tvTotalKilos.text = String.format("%.1f kg", cursorStats.getDouble(0))
            tvTotalEntregas.text = cursorStats.getInt(1).toString()
        }
        cursorStats.close()

        containerLotes.removeAllViews()
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? ORDER BY ${DatabaseHelper.COLUMN_REPORT_ID} DESC", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val peso = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_PESO))
                val tipo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_TIPO))
                val fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_FECHA))
                val status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_STATUS))
                val fotoPath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORT_FOTO_PATH))

                crearTarjetaRegistro(peso, tipo, fecha, status, fotoPath)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun crearTarjetaRegistro(peso: Double, tipo: String, fecha: String, status: String, foto: String) {
        val card = MaterialCardView(this).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
            radius = 48f
            cardElevation = 0f
            strokeWidth = 2
            setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0")))
            setCardBackgroundColor(Color.WHITE)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER_VERTICAL
        }

        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120)
            scaleType = ImageView.ScaleType.CENTER_CROP
            val file = File(foto)
            if (file.exists()) {
                setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } else {
                setImageResource(R.drawable.ic_menu_reporte)
            }
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val paramsCol = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            paramsCol.marginStart = 24
            layoutParams = paramsCol
        }

        val t1 = TextView(this).apply {
            text = "$peso kg de $tipo"
            textSize = 15f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.parseColor("#0F172A"))
        }

        val t2 = TextView(this).apply {
            text = fecha
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
        }

        col.addView(t1)
        col.addView(t2)

        val badge = TextView(this).apply {
            text = status
            textSize = 10f
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(20, 8, 20, 8)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 100f
                setColor(if (status == "Pendiente") Color.parseColor("#F59E0B") else Color.parseColor("#10B981"))
            }
        }

        layout.addView(img)
        layout.addView(col)
        layout.addView(badge)
        card.addView(layout)
        containerLotes.addView(card)
    }
}