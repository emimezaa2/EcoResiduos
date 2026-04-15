package com.meza.ecoresiduos.bot

import android.content.Context
import com.meza.ecoresiduos.db.DatabaseHelper
import java.util.Locale

class EcoBotEngine(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val prefs = context.getSharedPreferences("SesionEco", Context.MODE_PRIVATE)

    fun procesarMensaje(mensajeUsuario: String): String {
        val msj = mensajeUsuario.lowercase(Locale.getDefault()).trim()
        val userId = prefs.getInt("user_id", -1)
        val userName = prefs.getString("user_name", "Usuario")

        // 1. SALUDOS
        if (msj == "hola" || msj == "hey" || msj == "buenos dias" || msj == "buenas") {
            return "¡Hola, $userName! Soy EcoBot 🤖. Pregúntame sobre tus 'kilos', 'tickets pendientes', o pregúntame '¿Cómo está el punto de tu eleccion?' para ver contenedores."
        }
        if (msj.length < 3) {
            return "Por favor, sé un poco más específico. ¿Necesitas ayuda con puntos de recolección o tus estadísticas?"
        }

        // 2. ESTADO EN TIEMPO REAL DE UN PUNTO ESPECÍFICO
        if (msj.contains("como esta el punto") || msj.contains("estado de") || msj.contains("capacidad de")) {
            val puntoABuscar = msj.replace("como esta el punto", "")
                .replace("estado de", "")
                .replace("capacidad de", "")
                .replace("?", "")
                .trim()
            return if (puntoABuscar.isNotEmpty()) consultarEstadoRealPunto(puntoABuscar)
            else "¿De qué punto te gustaría saber el estado? Intenta con: '¿Cómo está el punto Parque Central?'"
        }

        // 3. PUNTOS DISPONIBLES EN GENERAL
        if (msj.contains("punto") || msj.contains("donde") || msj.contains("contenedor") || msj.contains("reciclar")) {
            return consultarPuntosDisponibles()
        }

        // 4. RANKING DE COMUNIDAD
        if (msj.contains("top") || msj.contains("ranking") || msj.contains("mejores") || msj.contains("ganando")) {
            return consultarRankingRealTime()
        }

        // 5. KILOS PERSONALES
        if (msj.contains("kilo") || msj.contains("llevo") || msj.contains("desechado") || msj.contains("impacto")) {
            return siUsuarioValido(userId) { consultarKilosPersonales(userId) }
        }

        // 6. TICKETS PENDIENTES
        if (msj.contains("ticket") || msj.contains("pendiente") || msj.contains("falta") || msj.contains("validar")) {
            return siUsuarioValido(userId) { consultarTicketsPendientes(userId) }
        }

        // 7. ECO-TIPS Y DATOS CURIOSOS
        if (msj.contains("consejo") || msj.contains("tip") || msj.contains("dato curioso")) {
            return darEcoTip()
        }

        // 8. CREADOR
        if (msj.contains("quien te creo") || msj.contains("tu creador") || msj.contains("tu papa")) {
            return "Fui desarrollado por el Ingeniero en Sistemas Arturo Emiliano Meza Legorreta para el proyecto EcoResiduos. 😎"
        }

        // 9. AYUDA GENERAL DE BASURA
        if (msj.contains("que puedo") || msj.contains("tipo") || msj.contains("basura") || msj.contains("residuo")) {
            return "Puedes registrar dos tipos de residuos:\n🌱 Orgánico: Restos de comida.\n📦 Seco: Cartón, plástico, vidrio."
        }

        // FALLBACK
        return "No estoy seguro de entender. Pregúntame: '¿Cómo está el punto [Nombre]?' o '¿Quiénes son los mejores?'"
    }

    private fun consultarEstadoRealPunto(nombreBusqueda: String): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_PUNTO_NOMBRE}, ${DatabaseHelper.COLUMN_PUNTO_CAPACIDAD}, ${DatabaseHelper.COLUMN_PUNTO_ESTADO} " +
                    "FROM ${DatabaseHelper.TABLE_PUNTOS} WHERE LOWER(${DatabaseHelper.COLUMN_PUNTO_NOMBRE}) LIKE ?", arrayOf("%$nombreBusqueda%")
        )

        var respuesta = ""
        if (cursor.moveToFirst()) {
            val nombreReal = cursor.getString(0)
            val capacidad = cursor.getInt(1)
            val estado = cursor.getString(2)

            respuesta = "📊 **Reporte en Tiempo Real**\nContenedor: $nombreReal\nNivel: $capacidad%\nEstatus: $estado"

            if (capacidad >= 90) respuesta += "\n\n⚠️ ¡Está casi lleno! Busca otro punto."
            else if (estado == "Mantenimiento") respuesta += "\n\n🛠️ Actualmente en mantenimiento."
            else respuesta += "\n\n✅ ¡Tiene espacio disponible!"
        }
        cursor.close()
        return if (respuesta.isNotEmpty()) respuesta else "No encontré ningún punto llamado '$nombreBusqueda'."
    }

    private fun consultarPuntosDisponibles(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_PUNTO_NOMBRE} FROM ${DatabaseHelper.TABLE_PUNTOS} WHERE ${DatabaseHelper.COLUMN_PUNTO_ESTADO} = 'Disponible'", null)
        val puntos = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do { puntos.add("- " + cursor.getString(0)) } while (cursor.moveToNext())
        }
        cursor.close()
        return if (puntos.isNotEmpty()) "Puntos operativos con espacio:\n${puntos.joinToString("\n")}" else "No hay contenedores disponibles ahora mismo."
    }

    private fun consultarRankingRealTime(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_USER_NAME}, ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} " +
                    "WHERE ${DatabaseHelper.COLUMN_USER_ROLE} != 'admin' AND ${DatabaseHelper.COLUMN_USER_KILOS} > 0 ORDER BY ${DatabaseHelper.COLUMN_USER_KILOS} DESC LIMIT 3", null
        )
        val top = mutableListOf<String>()
        var i = 1
        if (cursor.moveToFirst()) {
            do {
                val medalla = when(i) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
                top.add("$medalla ${cursor.getString(0)}: ${String.format("%.1f", cursor.getDouble(1))} kg")
                i++
            } while (cursor.moveToNext())
        }
        cursor.close()
        return if (top.isNotEmpty()) "🏆 **Líderes de EcoResiduos** 🏆\n\n${top.joinToString("\n")}" else "El ranking está vacío. ¡Sé el primero!"
    }

    private fun consultarKilosPersonales(userId: Int): String {
        val db = dbHelper.readableDatabase
        var kilos = 0.0
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) kilos = cursor.getDouble(0)
        cursor.close()
        return if (kilos > 0) "¡Has reciclado un total de ${String.format("%.1f", kilos)} kg! ¡Sigue así!" else "Aún no tienes kilos validados."
    }

    private fun consultarTicketsPendientes(userId: Int): String {
        val db = dbHelper.readableDatabase
        var pendientes = 0
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? AND ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Pendiente'", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) pendientes = cursor.getInt(0)
        cursor.close()
        return if (pendientes > 0) "Tienes $pendientes ticket(s) esperando ser validados." else "No tienes ningún ticket pendiente."
    }

    private fun darEcoTip(): String {
        val tips = listOf(
            "🌱 Tip: Los residuos orgánicos pueden convertirse en abono.",
            "📦 Tip: Aplastar cajas ayuda a que el contenedor rinda 3 veces más.",
            "🧴 Tip: Enjuagar envases evita malos olores en los puntos.",
            "♻️ Tip: Reciclar 1 tonelada de papel salva 17 árboles."
        )
        return tips.random()
    }

    private fun siUsuarioValido(userId: Int, accion: () -> String): String {
        return if (userId != -1) accion() else "Error: Necesitas iniciar sesión."
    }
}