package com.meza.ecoresiduos.user

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.meza.ecoresiduos.R
import com.meza.ecoresiduos.db.DatabaseHelper

class UserRecompensasActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var totalCoins: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_recompensas)

        dbHelper = DatabaseHelper(this)

        findViewById<TextView>(R.id.btnBackRecompensas).setOnClickListener { finish() }

        calcularEcoCoins()

        // Lógica de los botones de canje
        findViewById<Button>(R.id.btnCanjearCafe).setOnClickListener {
            intentarCanjear(150, "2x1 en Cafetería")
        }

        findViewById<Button>(R.id.btnCanjearBus).setOnClickListener {
            intentarCanjear(300, "Boleto de Transporte")
        }
    }

    private fun calcularEcoCoins() {
        val prefs = getSharedPreferences("SesionEco", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId != -1) {
            val db = dbHelper.readableDatabase
            // Obtenemos todos los kilos validados ("Aprobado" o "Pendiente", tú decides. Usaremos todos por ahora)
            val cursorStats = db.rawQuery("SELECT SUM(${DatabaseHelper.COLUMN_REPORT_PESO}) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ?", arrayOf(userId.toString()))

            var totalKilos = 0.0
            if (cursorStats.moveToFirst()) {
                totalKilos = cursorStats.getDouble(0)
            }
            cursorStats.close()

            // REGLA DE NEGOCIO: 1 Kilo = 10 Eco-Coins
            totalCoins = (totalKilos * 10).toInt()

            findViewById<TextView>(R.id.tvEcoCoins).text = totalCoins.toString()
        }
    }

    private fun intentarCanjear(costo: Int, recompensa: String) {
        if (totalCoins >= costo) {
            // Caso de Éxito
            AlertDialog.Builder(this)
                .setTitle("🎉 ¡Felicidades!")
                .setMessage("Has canjeado exitosamente: $recompensa.\n\nSe enviará un código QR a tu correo electrónico.")
                .setPositiveButton("Aceptar", null)
                .show()
        } else {
            // Caso de Fracaso
            val faltan = costo - totalCoins
            Toast.makeText(this, "Te faltan $faltan Eco-Coins. ¡Sigue reciclando!", Toast.LENGTH_LONG).show()
        }
    }
}