package com.meza.ecoresiduos.bot

import android.content.Context
import com.meza.ecoresiduos.db.DatabaseHelper
import java.util.Locale

class EcoBotEngine(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val prefs = context.getSharedPreferences("SesionEco", Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════
    //  MEMORIA DE SESIÓN
    // ═══════════════════════════════════════════════════════════
    private val historialTemas = mutableListOf<String>()   // temas tocados en esta sesión
    private val historialMsj   = mutableListOf<String>()   // últimos mensajes del usuario
    private var ultimoTema      = ""                        // último tema detectado
    private var contadorMensajes = 0                        // cuántas veces ha escrito

    // ═══════════════════════════════════════════════════════════
    //  DICCIONARIO DE SINÓNIMOS / VARIACIONES ORGÁNICAS
    // ═══════════════════════════════════════════════════════════
    private val palabrasOrganicas = listOf(
        "manzana", "naranja", "platano", "mango", "papaya", "pera", "uva", "melon",
        "sandia", "fresa", "limon", "toronja", "kiwi", "aguacate", "pina", "coco", "cereza",
        "ciruela", "higo", "guayaba", "mandarina", "durazno", "chabacano",
        "zanahoria", "lechuga", "tomate", "jitomate", "cebolla", "ajo", "papa", "calabaza",
        "chayote", "brocoli", "coliflor", "espinaca", "pepino", "chile", "nopal", "elote",
        "apio", "betabel", "rabano", "poro",
        "cascara", "sobra", "resto", "desperdicio", "alimento", "comida", "alimentos",
        "verdura", "fruta", "vegetal", "hortaliza", "hierba", "hoja", "semilla", "semillas",
        "hueso de", "bagazo", "pulpa", "pasto", "poda", "maleza", "deshecho", "organicos",
        "cascaron", "huevo", "concha", "marisco", "hueso", "comidita", "cenas", "desayuno",
        "podrido", "echado", "viejo", "caducado", "vencido", "fermentado", "descompuesto",
        "sobro", "sobrante", "tirar", "botar", "desechar", "basurita", "desperdiciar"
    )

    private val palabrasInorganicas = listOf(
        "plastico", "vidrio", "carton", "papel", "lata", "metal", "botella", "inorganico",
        "aluminio", "cobre", "bolsa", "pila", "bateria", "tela", "ropa", "unicel",
        "tecnologia", "electronico", "celular", "cable", "foco", "foquito", "lamina",
        "laton", "tubo", "panal", "cartonero", "periodico", "revista", "caja", "empaque",
        "envoltura", "alambre", "fierro", "metalico", "vidrios", "plasticos"
    )

    // ═══════════════════════════════════════════════════════════
    //  PROCESADOR PRINCIPAL
    // ═══════════════════════════════════════════════════════════
    fun procesarMensaje(mensajeUsuario: String): String {
        val msj = mensajeUsuario.lowercase(Locale.getDefault())
            .trim()
            .replace("á","a").replace("é","e").replace("í","i")
            .replace("ó","o").replace("ú","u").replace("ü","u")

        val userId   = prefs.getInt("user_id", -1)
        val userName = prefs.getString("user_name", "Usuario") ?: "Usuario"

        // Registrar mensaje en el historial
        contadorMensajes++
        historialMsj.add(msj)
        if (historialMsj.size > 8) historialMsj.removeAt(0)

        // Detección de intención
        val esOrganico   = palabrasOrganicas.any   { msj.contains(it) }
        val esInorganico = palabrasInorganicas.any { msj.contains(it) }

        // Preguntas de sí/no o confirmación con memoria de contexto
        val esConfirmacion = listOf("si", "claro", "exacto", "eso", "correcto", "ok", "va", "acepto").any { msj == it }
        val esNegacion     = listOf("no", "nop", "nel", "para nada", "nunca").any { msj == it }

        // ── BLOQUE 1: Mensajes muy cortos ──
        if (msj.length < 3) {
            return respuestaCorta(ultimoTema)
        }

        // ── BLOQUE 2: Confirmaciones contextuales ──
        if (esConfirmacion && ultimoTema.isNotEmpty()) {
            return manejarConfirmacion(ultimoTema, userName)
        }
        if (esNegacion && ultimoTema.isNotEmpty()) {
            return "Entendido, $userName. ¡Ningún problema! ¿Hay alguna otra función de la app que quieras que te explique? 🌱"
        }

        // ── BLOQUE 3: Saludos ──
        val saludos = listOf("hola", "hey", "buenos dias", "buenas tardes", "buenas noches",
            "buenas", "que tal", "saludos", "eco", "ey", "alo", "quiobole")
        if (saludos.any { msj == it || msj.startsWith(it) }) {
            return saludarConContexto(userName, userId)
        }

        // ── BLOQUE 4: Inorgánicos ──
        if (esInorganico && !esOrganico) {
            val materialDetectado = palabrasInorganicas.firstOrNull { msj.contains(it) } ?: "ese material"
            return explicarInorganico(materialDetectado, userName)
        }

        // ── BLOQUE 5: Orgánicos detectados ──
        if (esOrganico) {
            val materialDetectado = palabrasOrganicas.firstOrNull { msj.contains(it) } ?: "ese material"
            return confirmarOrganico(materialDetectado, msj, userName)
        }

        // ── BLOQUE 6: ¿Puedo tirar X? / ¿Se acepta X? ──
        val preguntaAceptacion = listOf("puedo tirar", "puedo echar", "puedo depositar", "puedo meter",
            "se acepta", "aceptan", "reciben", "sirve el", "va el", "va la", "es reciclable")
        if (preguntaAceptacion.any { msj.contains(it) }) {
            return resolverPreguntaAceptacion(msj, userName)
        }

        // ── BLOQUE 7: Ayuda / Menú Dinámico / ¿Qué puedo preguntar? ──
        val triggersAyuda = listOf("que puedo preguntar", "que haces", "comandos", "opciones",
            "ayuda", "ayudame", "menu", "lista", "que sabes", "instrucciones")
        if (triggersAyuda.any { msj.contains(it) }) {
            registrarTema("ayuda")
            val puntoEjemplo = obtenerPuntoEjemploReal()
            return buildMenuAyuda(userName, puntoEjemplo)
        }

        // ── BLOQUE 8: Tutorial / Funcionamiento de la App ──
        if (msj.contains("como funciona") || msj.contains("que hago") ||
            msj.contains("tutorial") || msj.contains("guia") || msj.contains("funcionamiento")) {
            registrarTema("tutorial")
            return buildTutorial()
        }

        // ── BLOQUE 9: Gestión de Comunidades ──
        val triggersComunidad = listOf("crear comunidad", "hacer comunidad", "nueva comunidad",
            "crear grupo", "hacer grupo", "nuevo grupo", "como hago un grupo", "fundar", "unirme a un grupo")
        if (triggersComunidad.any { msj.contains(it) }) {
            registrarTema("comunidad")
            return buildRespuestaComunidad()
        }

        // ── BLOQUE 10: Creación de Puntos de Recolección ──
        val triggersPuntos = listOf("crear punto", "hacer punto", "agregar punto", "anadir punto",
            "nuevo contenedor", "poner contenedor", "poner punto", "registrar punto")
        if (triggersPuntos.any { msj.contains(it) }) {
            registrarTema("puntos_crear")
            return buildRespuestaPuntos()
        }

        // ── BLOQUE 11: Estado de un punto específico ──
        val triggersEstado = listOf("como esta el punto", "estado de", "capacidad de",
            "esta lleno", "tiene espacio", "nivel de", "cuanto tiene", "estatus de")
        if (triggersEstado.any { msj.contains(it) }) {
            val nombreBusqueda = extraerNombrePunto(msj)
            registrarTema("estado_punto")
            return if (nombreBusqueda.isNotEmpty()) consultarEstadoRealPunto(nombreBusqueda)
            else "¿De cuál contenedor te interesa conocer la capacidad de llenado? Escribe por ejemplo: ¿Cómo está el punto Parque Central? 📍"
        }

        // ── BLOQUE 12: Mapa / puntos disponibles ──
        if (msj.contains("puntos") || msj.contains("donde") ||
            msj.contains("contenedor") || msj.contains("mapa") ||
            msj.contains("cerca") || msj.contains("disponible")) {
            registrarTema("mapa")
            return consultarPuntosDisponibles()
        }

        // ── BLOQUE 13: Ranking ──
        if (msj.contains("top") || msj.contains("ranking") || msj.contains("mejores") ||
            msj.contains("ganando") || msj.contains("lideres") || msj.contains("quien va primero") ||
            msj.contains("clasificacion")) {
            registrarTema("ranking")
            return consultarRankingRealTime()
        }

        // ── BLOQUE 14: Kilos / impacto personal ──
        if (msj.contains("kilo") || msj.contains("llevo") || msj.contains("desechado") ||
            msj.contains("mi impacto") || msj.contains("cuanto he") || msj.contains("mis puntos") ||
            msj.contains("mi progreso") || msj.contains("mi aportacion")) {
            registrarTema("kilos")
            return siUsuarioValido(userId) { consultarKilosPersonales(userId, userName) }
        }

        // ── BLOQUE 15: Tickets ──
        if (msj.contains("ticket") || msj.contains("pendiente") || msj.contains("falta") ||
            msj.contains("validar") || msj.contains("mis registros") || msj.contains("mis depositos")) {
            registrarTema("tickets")
            return siUsuarioValido(userId) { consultarTicketsPendientes(userId) }
        }

        // ── BLOQUE 16: Eco-Tips ──
        if (msj.contains("consejo") || msj.contains("tip") || msj.contains("dato curioso") ||
            msj.contains("sabias que") || msj.contains("aprende") || msj.contains("ensenanza")) {
            registrarTema("tip")
            return darEcoTip(historialTemas)
        }

        // ── BLOQUE 17: Creador / créditos ──
        if (msj.contains("quien te creo") || msj.contains("tu creador") ||
            msj.contains("tu papa") || msj.contains("desarrollador") ||
            msj.contains("quien te hizo") || msj.contains("quien te programo")) {
            return "Fui desarrollado por emimezaa para el proyecto EcoResiduos."
        }

        // ── BLOQUE 18: Qué se puede reciclar ──
        val queReciclar = listOf("que puedo reciclar", "que se recibe", "que acepta",
            "tipo de basura", "materiales", "que tiran", "que depositan", "que van")
        if (queReciclar.any { msj.contains(it) }) {
            registrarTema("materiales")
            return buildMaterialesAceptados()
        }

        // ── BLOQUE 19: Preguntas de impacto ambiental ──
        if (msj.contains("impacto") || msj.contains("medio ambiente") || msj.contains("planeta") ||
            msj.contains("carbono") || msj.contains("huella") || msj.contains("co2") ||
            msj.contains("calentamiento") || msj.contains("ecologia")) {
            registrarTema("impacto_ambiental")
            return buildImpactoAmbiental()
        }

        // ── BLOQUE 20: Compostaje ──
        if (msj.contains("composta") || msj.contains("compostar") || msj.contains("compostaje") ||
            msj.contains("abono") || msj.contains("fertilizante") || msj.contains("tierra")) {
            registrarTema("composta")
            return buildInfoComposta()
        }

        // ── BLOQUE 21: Frustración ──
        val frustacion = listOf("no entiendes", "no sirves", "eres malo", "que malo eres",
            "no funciona", "no entiendo", "no se", "estoy perdido", "tonto")
        if (frustacion.any { msj.contains(it) }) {
            return manejarFrustracion(userName)
        }

        // ── BLOQUE 22: Agradecimientos ──
        val gracias = listOf("gracias", "thank", "chido", "excelente", "perfecto",
            "genial", "bien", "ok gracias", "listo", "ya entendi")
        if (gracias.any { msj.contains(it) }) {
            return respuestaGracias(userName, contadorMensajes)
        }

        // ── FALLBACK: Respuesta divertida ──
        return respuestaFueraDeAlcance(msj, userName)
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTIÓN DE MEMORIA DE SESIÓN (CORREGIDA)
    // ═══════════════════════════════════════════════════════════
    private fun registrarTema(tema: String) {
        ultimoTema = tema
        if (!historialTemas.contains(tema)) historialTemas.add(tema)
        if (historialTemas.size > 10) historialTemas.removeAt(0)
    }

    private fun yaPreguntoPor(tema: String): Boolean {
        return historialTemas.contains(tema)
    }

    // ═══════════════════════════════════════════════════════════
    //  RESPUESTAS CONTEXTUALES
    // ═══════════════════════════════════════════════════════════
    private fun saludarConContexto(userName: String, userId: Int): String {
        return when {
            contadorMensajes == 1 -> {
                "¡Hola, $userName! 👋 Soy EcoBot, tu asistente de reciclaje orgánico. Escribe '¿qué puedo preguntar?' o 'ayuda' para ver todo lo que puedo resolver por ti de inmediato."
            }
            historialTemas.isNotEmpty() -> {
                val ultimoTemaLindo = traducirTema(ultimoTema)
                "¡Aquí andamos de nuevo, $userName! 😄 Hace un momento estábamos revisando sobre $ultimoTemaLindo. ¿Quieres continuar con ese hilo o resolvemos una nueva pregunta de la app?"
            }
            else -> {
                "¡Hola otra vez, $userName! 🌱 ¿Qué módulo o estadística de la app inspeccionamos ahora?"
            }
        }
    }

    private fun manejarConfirmacion(tema: String, userName: String): String {
        return when (tema) {
            "materiales" -> "¡Excelente! La regla es estricta: si es residuo alimenticio o materia vegetal, va para adentro. Si es plástico o metal, directo al rechazo. ¿Tienes alguna duda de un residuo en mente?"
            "tutorial"   -> "¡Perfecto! El flujo operativo es simple: registras con tu cámara o formulario -> depositas en el contenedor oficial -> el líder te valida el ticket. ¿Te gustaría saber cómo crear un grupo?"
            "comunidad"  -> "¡No pierdas tiempo! Ve al botón Centro Comunitario en tu Dashboard y crea tu red de reciclaje ahora mismo. 🚀"
            "ranking"    -> consultarRankingRealTime()
            else         -> "¡Entendido, $userName! Mis sistemas están listos. ¿Qué otra consulta tienes sobre EcoResiduos? 🌱"
        }
    }

    private fun respuestaCorta(ultimoTema: String): String {
        return if (ultimoTema.isNotEmpty()) {
            "Ups, ese mensaje fue muy breve. ¿Te quedaste con dudas sobre ${traducirTema(ultimoTema)}? Escríbeme la frase completa y lo resolvemos. 🤔"
        } else {
            "Hmm, ocupo un poco más de texto para activar mis circuitos. 😅 Escribe 'ayuda' para guiarte de forma automática."
        }
    }

    private fun respuestaGracias(userName: String, contador: Int): String {
        val opciones = listOf(
            "¡Para eso fui programado, $userName! 🌱 Cada gramo de desperdicio de comida que desvías de los vertederos es una victoria para el planeta.",
            "¡Con gusto! 💚 ¿Hay alguna otra pantalla de la app de la que quieras conocer su funcionamiento?",
            "¡De nada, $userName! Hacemos un excelente equipo de ingeniería ambiental. 🤝 ¡A seguir acumulando kilos!",
            "¡Un placer ayudarte! Mantengamos limpia la base de datos de la comunidad. 🌍"
        )
        return opciones[contador % opciones.size]
    }

    private fun manejarFrustracion(userName: String): String {
        return "¡Vaya! Lamento no haber sido del todo claro, $userName. 🤖 A veces mis algoritmos se saturan, pero no te preocupes: escribe '¿qué puedo preguntar?' para ver exactamente las instrucciones y frases que sé responder al 100%."
    }

    private fun respuestaFueraDeAlcance(msj: String, userName: String): String {
        val contexto = if (historialTemas.isNotEmpty()) {
            " Hace un momento hablábamos de ${traducirTema(historialTemas.last())}, pero esto se escapó de mis cables."
        } else ""

        val pool = listOf(
            "¡Error 404: Respuesta graciosa no encontrada! 🧠 Esa pregunta se sale por completo de mi base de datos ecológica.$contexto Mis ingenieros solo me enseñaron a optimizar el reciclaje orgánico. ¿Por qué no me preguntas por tus kilos mejor?",
            "¡Córrele que me asusto! 🤖 Mi procesador de lenguaje no computa esa duda.$contexto Si no se puede compostar o no tiene que ver con las pantallas de la app, prefiero ignorarlo con elegancia artificial. ¡Prueba preguntándome cómo crear una comunidad!",
            "Interesante pregunta... para un buscador convencional. 🌐 Aquí solo procesamos datos de residuos orgánicos, tickets y mapas de red.$contexto ¡Escribe 'ayuda' para regresar a mi zona de confort!",
            "Eso superó mis bytes asignados de memoria RAM, $userName. 💻 Si no es un residuo de comida o un contenedor GPS, mi sistema entra en hibernación. ¿Inspeccionamos el Ranking de líderes mejor?"
        )
        return pool[(msj.length + contadorMensajes) % pool.size]
    }

    // ═══════════════════════════════════════════════════════════
    //  DETECCIÓN INTELIGENTE DE MATERIALES
    // ═══════════════════════════════════════════════════════════
    private fun confirmarOrganico(material: String, msj: String, userName: String): String {
        registrarTema("materiales")
        val accionDetectada = when {
            msj.contains("composta") || msj.contains("compostar") -> "compostar"
            else -> "registrar y depositar"
        }

        val estadoDetectado = when {
            msj.contains("podrido") || msj.contains("echado") || msj.contains("descompuesto") ->
                " (y no te preocupes porque esté descompuesto, ¡entre más degradado esté, es mucho mejor para los nutrientes del suelo!)"
            msj.contains("vencido") || msj.contains("caducado") ->
                " (esté vencido o no, al sistema le interesa su origen biológico)"
            else -> ""
        }

        return "✅ ¡Confirmado, $userName! El elemento detectado pertenece a la categoría de materia orgánica$estadoDetectado. Es exactamente el residuo apto para la plataforma. Puedes proceder a $accionDetectada el material en tu contenedor más cercano. ¿Quieres consultar qué puntos están libres? 📍"
    }

    private fun explicarInorganico(material: String, userName: String): String {
        registrarTema("inorganico")
        val alternativa = when {
            material.contains("pila") || material.contains("bateria") ->
                "las pilas y baterías requieren un protocolo químico especializado; llévalas a contenedores de acopio en centros comerciales o farmacias"
            material.contains("vidrio") ->
                "el vidrio debe ser canalizado a centros de reciclaje industrial convencionales para su fundición"
            material.contains("plastico") || material.contains("botella") || material.contains("unicel") ->
                "los polímeros y plásticos van al contenedor seco de recolección municipal o a programas de acopio de PET"
            material.contains("carton") || material.contains("papel") || material.contains("caja") ->
                "el cartón y papel secos deben ser entregados a recolectores de celulosa convencionales"
            else ->
                "te recomendamos localizar un centro de transferencia de residuos inorgánicos reciclables en tu localidad"
        }

        return "⚠️ ¡Cuidado, $userName! El material ingresado es Inorgánico. En EcoResiduos tenemos una regla de negocio estricta: únicamente procesamos restos de comida y residuos biológicos orgánicos. Para ese elemento en específico, $alternativa. ¿Tienes algún desperdicio orgánico listo para registrar? 🌱"
    }

    private fun resolverPreguntaAceptacion(msj: String, userName: String): String {
        val organicoEncontrado = palabrasOrganicas.firstOrNull { msj.contains(it) }
        val inorganicoEncontrado = palabrasInorganicas.firstOrNull { msj.contains(it) }

        return when {
            organicoEncontrado != null -> confirmarOrganico(organicoEncontrado, msj, userName)
            inorganicoEncontrado != null -> explicarInorganico(inorganicoEncontrado, userName)
            else -> {
                registrarTema("materiales")
                "Buena pregunta 🤔 Para saber si un residuo pasa el filtro del sistema, aplica la regla de oro: ¿Vino de la tierra y corresponde a restos de comida o material vegetal? Si la respuesta es sí, ¡es totalmente bienvenido! Todo lo que sea plástico, cartón, metal o vidrio queda descartado. ¿Qué material específico traes en mente?"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILDERS DE RESPUESTAS LARGAS
    // ═══════════════════════════════════════════════════════════
    private fun buildMenuAyuda(userName: String, puntoEjemplo: String): String {
        val intro = if (yaPreguntoPor("ayuda")) "Aquí tienes la lista de comandos de nuevo, $userName:" else "¡Perfecto, $userName! Estas son las frases exactas que entiendo de forma nativa:"
        return "🤖 Menú de Comandos del EcoBot\n\n$intro\n\n" +
                "🔹 ¿Cómo funciona la app? — Guía operativa paso a paso.\n" +
                "🔹 ¿Cómo creo una comunidad? — Instrucciones para fundar un grupo.\n" +
                "🔹 ¿Cómo creo un punto? — Requisitos para añadir contenedores.\n" +
                "🔹 ¿Cómo está el punto $puntoEjemplo? — Consulta la capacidad de este contenedor en tiempo real.\n" +
                "🔹 Puntos disponibles — Lista de contenedores libres en el mapa.\n" +
                "🔹 ¿Qué puedo reciclar? — Catálogo general de residuos biológicos aceptados.\n" +
                "🔹 Mis kilos — Tu impacto total en kilogramos acumulados.\n" +
                "🔹 Tickets pendientes — Estado de tus reportes en espera de validación.\n" +
                "🔹 Ranking — Tabla de clasificación de la comunidad.\n" +
                "🔹 Consejo — Dato curioso del día.\n" +
                "🔹 ¿Qué es la composta? — Información educativa del proceso biológico."
    }

    private fun buildTutorial(): String {
        return "📱 Guía Operativa de EcoResiduos:\n\n" +
                "1. Registra: Dirígete a 'Nuevo Registro' o utiliza el Eco-Escáner IA para capturar el peso y datos de tu residuo alimenticio.\n" +
                "2. Deposita: Ubica un contenedor físico activo mediante el 'Mapa de Red' y vierte el material.\n" +
                "3. Valida: Presenta tu ticket físico o digital al creador/fundador de tu grupo para que lo autorice desde su panel.\n" +
                "4. Suma: En cuanto sea aprobado, ¡tus métricas de impacto se dispararán automáticamente en el Dashboard!"
    }

    private fun buildRespuestaComunidad(): String {
        return "👥 Flujo de Creación de Comunidades:\n\nPara fundar un nuevo grupo de reciclaje, haz clic en la tarjeta 'Centro Comunitario' de tu menú principal. En la parte superior encontrarás un control de texto llamado 'Nueva Comunidad'. Ingresa el nombre comercial o de tu colonia, presiona el botón 'Crear' y listo. El sistema te registrará inmediatamente como el líder administrador del grupo, y tus vecinos podrán buscarlo para unirse."
    }

    private fun buildRespuestaPuntos(): String {
        return "📍 Regla de Puntos de Recolección:\n\nPara asegurar la integridad geográfica de la red y evitar contenedores duplicados o fuera de servicio, la creación de nuevos nodos GPS en el mapa está reservada exclusivamente para los Administradores. Si tu comunidad organizada ya cuenta con un depósito físico oficial, ponte en contacto con nuestro equipo de soporte técnico para validar las coordenadas y plantarlo en el mapa."
    }

    private fun buildMaterialesAceptados(): String {
        return "🌱 Filtro de Materiales Aceptados:\n\n" +
                "SÍ SE PERMITE: Restos culinarios orgánicos, desperdicios vegetales de cocina, restos de poda, deshechos de jardinería húmedos y cascarones biológicos.\n\n" +
                "RECHAZO ABSOLUTO: Plásticos, cartones, papeles comerciales, metales, latas de aluminio, textiles, pilas o cualquier derivado sintético inorgánico.\n\n" +
                "Recuerda la premisa de la app: si es biodegradable de origen alimenticio o vegetal, ¡pasa el filtro!"
    }

    private fun buildImpactoAmbiental(): String {
        return "🌍 Métrica de Impacto Ambiental:\n\nCuando los restos de comida terminan compactados en un basurero municipal común, sufren una descomposición anaeróbica que libera gas metano, un compuesto letal para la atmósfera. Al desviarlos a los contenedores de EcoResiduos, esa materia se degrada de forma controlada, capturando el carbono en la tierra y cancelando las emisiones de gases de efecto invernadero. ¡Cada kilo cuenta!"
    }

    private fun buildInfoComposta(): String {
        return "♻️ El Proceso del Compostaje:\n\nLa composta es la degradación biológica controlada de la materia orgánica llevada a cabo por microorganismos del suelo. Todo el residuo alimenticio que recolectas en tus grupos es transformado de forma segura en abono orgánico de alta potencia, regresándole la fertilidad a los suelos agrícolas y áreas verdes urbanas. ¡Tus sobras nutren la tierra!"
    }

    // ═══════════════════════════════════════════════════════════
    //  DATOS EN TIEMPO REAL
    // ═══════════════════════════════════════════════════════════
    private fun obtenerPuntoEjemploReal(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_PUNTO_NOMBRE} FROM ${DatabaseHelper.TABLE_PUNTOS} LIMIT 1", null)
        var nombre = "Parque Central"
        if (cursor.moveToFirst()) nombre = cursor.getString(0)
        cursor.close()
        return nombre
    }

    private fun extraerNombrePunto(msj: String): String {
        return msj
            .replace("como esta el punto", "")
            .replace("estado de", "")
            .replace("capacidad de", "")
            .replace("esta lleno el punto", "")
            .replace("tiene espacio el punto", "")
            .replace("nivel de", "")
            .replace("estatus de", "")
            .replace("?", "")
            .trim()
    }

    private fun consultarEstadoRealPunto(nombreBusqueda: String): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_PUNTO_NOMBRE}, ${DatabaseHelper.COLUMN_PUNTO_CAPACIDAD}, ${DatabaseHelper.COLUMN_PUNTO_ESTADO} " +
                    "FROM ${DatabaseHelper.TABLE_PUNTOS} WHERE LOWER(${DatabaseHelper.COLUMN_PUNTO_NOMBRE}) LIKE ?", arrayOf("%$nombreBusqueda%")
        )
        var respuesta = ""
        if (cursor.moveToFirst()) {
            val nombre   = cursor.getString(0)
            val capacidad = cursor.getInt(1)
            val estado   = cursor.getString(2)
            val barra    = buildBarraCapacidad(capacidad)

            respuesta = "📊 Contenedor: $nombre\nNivel: $barra $capacidad%\nEstado: $estado\n\n" +
                    when {
                        capacidad >= 90 -> "⚠️ El contenedor está al límite. Te sugerimos redirigir tu depósito a otro punto de la red."
                        capacidad >= 70 -> "🟡 Volumen ocupado considerablemente. Operativo pero con espacio limitado."
                        estado == "Mantenimiento" -> "🛠️ Nodo bajo mantenimiento preventivo temporal. Por favor, selecciona otra ubicación por hoy."
                        else -> "✅ ¡Espacio óptimo disponible! Listo para recibir tus registros orgánicos."
                    }
        }
        cursor.close()
        return if (respuesta.isNotEmpty()) respuesta
        else "No detecté ningún punto registrado bajo el nombre '$nombreBusqueda'. ¿Deseas consultar la lista de puntos disponibles? 📍"
    }

    private fun buildBarraCapacidad(porcentaje: Int): String {
        val llenos = (porcentaje / 10)
        val vacios = 10 - llenos
        return "[${"█".repeat(llenos)}${"░".repeat(vacios)}]"
    }

    private fun consultarPuntosDisponibles(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_PUNTO_NOMBRE}, ${DatabaseHelper.COLUMN_PUNTO_CAPACIDAD} FROM ${DatabaseHelper.TABLE_PUNTOS} " +
                    "WHERE ${DatabaseHelper.COLUMN_PUNTO_ESTADO} = 'Disponible' ORDER BY ${DatabaseHelper.COLUMN_PUNTO_CAPACIDAD} ASC", null
        )
        val puntos = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                val nombre    = cursor.getString(0)
                val capacidad = cursor.getInt(1)
                val icono     = when {
                    capacidad < 50 -> "🟢"
                    capacidad < 80 -> "🟡"
                    else           -> "🔴"
                }
                puntos.add("$icono $nombre — ${capacidad}% de volumen ocupado")
            } while (cursor.moveToNext())
        }
        cursor.close()
        return if (puntos.isNotEmpty()) {
            "📍 Estatus de Contenedores Libres:\n\n${puntos.joinToString("\n")}\n\nSimbología de Red:\n🟢 = Vacío / Disponible  🟡 = Mitad de Capacidad  🔴 = Llenado Crítico"
        } else {
            "Actualmente todos los contenedores de tu zona se reportan al límite o en mantenimiento técnico. Consulta el mapa satelital en la app para más detalles. 🗺️"
        }
    }

    private fun consultarRankingRealTime(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_USER_NAME}, ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} " +
                    "WHERE ${DatabaseHelper.COLUMN_USER_ROLE} != 'admin' AND ${DatabaseHelper.COLUMN_USER_KILOS} > 0 ORDER BY ${DatabaseHelper.COLUMN_USER_KILOS} DESC LIMIT 5", null
        )
        val top = mutableListOf<String>()
        var i = 1
        if (cursor.moveToFirst()) {
            do {
                val medalla = when(i) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "  $i." }
                val kilos   = String.format("%.1f", cursor.getDouble(1))
                top.add("$medalla ${cursor.getString(0)}: $kilos kg")
                i++
            } while (cursor.moveToNext())
        }
        cursor.close()
        return if (top.isNotEmpty()) {
            "🏆 Cuadro de Honor de la Red:\n\n${top.joinToString("\n")}\n\n¿Estás listo para escalar posiciones? 👀"
        } else {
            "La tabla de clasificación global está en ceros. ¡Comienza a validar tus tickets y toma el liderato! 🚀"
        }
    }

    private fun consultarKilosPersonales(userId: Int, userName: String): String {
        val db = dbHelper.readableDatabase
        var kilos = 0.0
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_USER_KILOS} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_USER_ID} = ?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) kilos = cursor.getDouble(0)
        cursor.close()
        return if (kilos > 0) {
            val equivalencia = when {
                kilos >= 100 -> "¡un impacto masivo que equivale al peso de una persona adulta desviada de la contaminación!"
                kilos >= 50  -> "¡lo cual equivale a un costal industrial de sustrato orgánico recuperado para el campo!"
                kilos >= 10  -> "¡una aportación totalmente sólida y medible en tu comunidad urbana!"
                else         -> "¡un excelente primer paso para activar la economía circular!"
            }
            "💪 Llevas ${"%.1f".format(kilos)} kg de residuos orgánicos validados, $userName — $equivalencia 🌍"
        } else {
            "No registras kilogramos aprobados en esta sesión, $userName. ¡Ejecuta tu primer depósito orgánico y solicita la validación a tu líder de grupo! 🌱"
        }
    }

    private fun consultarTicketsPendientes(userId: Int): String {
        val db = dbHelper.readableDatabase
        var pendientes = 0
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTS} WHERE ${DatabaseHelper.COLUMN_REPORT_USER_ID} = ? AND ${DatabaseHelper.COLUMN_REPORT_STATUS} = 'Pendiente'", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) pendientes = cursor.getInt(0)
        cursor.close()
        return if (pendientes > 0) {
            "📋 Alertas: Tienes $pendientes ticket(s) en la cola de revisión de tu líder de grupo. Una vez verificado el depósito físico, tus kilos impactarán el tablero. ⏳"
        } else {
            "✅ ¡Historial limpio! No cuentas con registros pendientes en la cola de auditoría. ¡Buen trabajo! 🌿"
        }
    }

    private fun darEcoTip(historial: List<String>): String {
        val tips = listOf(
            "🌱 Tip: La materia orgánica recuperada y separada correctamente se transforma en nutrientes puros para los suelos, cerrando el ciclo de la vida de forma limpia.",
            "📦 Tip: Entregar tus residuos en contenedores herméticos reutilizables evita malos olores en tu cocina y reduce el uso de plásticos desechables.",
            "♻️ Tip: Al separar los residuos orgánicos evitas que generen gases tóxicos en los basureros tradicionales, ayudando directamente a mitigar el cambio climático.",
            "💧 Tip: El residuo alimenticio está compuesto en gran parte por agua. Separarlo reduce drásticamente el peso de los camiones recolectores, ahorrando toneladas de combustible fósil.",
            "🌍 Tip: El uso de composta natural reduce la necesidad de fertilizantes químicos industriales nocivos para los mantos acuíferos subterráneos.",
            "🌿 Tip: Los restos de podas y hojas secas son excelentes aliados en el proceso de compostaje al equilibrar de forma natural los niveles de humedad.",
            "🔬 Tip: El compostaje imita el proceso natural de reciclaje de los ecosistemas forestales. ¡Tus sobras de comida se convierten en tierra fértil!"
        )
        val indiceBase = (historial.size + contadorMensajes) % tips.size
        return tips[indiceBase]
    }

    private fun siUsuarioValido(userId: Int, accion: () -> String): String {
        return if (userId != -1) accion()
        else "Error de sesión: Se requiere autenticación activa para extraer esta métrica. Por favor ingresa al login de la app. 🔐"
    }

    private fun traducirTema(tema: String): String {
        return when(tema) {
            "materiales"        -> "materiales aceptados"
            "tutorial"          -> "funcionamiento de la app"
            "comunidad"         -> "creación de comunidades"
            "puntos_crear"      -> "puntos de recolección"
            "estado_punto"      -> "capacidad de contenedores"
            "mapa"              -> "los puntos disponibles"
            "ranking"           -> "la tabla de líderes"
            "kilos"             -> "tu impacto en kilogramos"
            "tickets"           -> "tus registros pendientes"
            "tip"               -> "consejos ecológicos"
            "composta"          -> "procesos de compostaje"
            "impacto_ambiental" -> "impacto ambiental global"
            "inorganico"        -> "materiales restringidos inorgánicos"
            else                -> tema
        }
    }

    fun reiniciarSesion() {
        historialTemas.clear()
        historialMsj.clear()
        ultimoTema = ""
        contadorMensajes = 0
    }
}