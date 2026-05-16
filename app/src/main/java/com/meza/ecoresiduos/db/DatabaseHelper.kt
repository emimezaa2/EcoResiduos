package com.meza.ecoresiduos.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EcoResiduos.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_USERS = "usuarios"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_NAME = "nombre"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_ROLE = "role"
        const val COLUMN_USER_KILOS = "kilos_totales"
        const val COLUMN_USER_COMUNIDAD_ID = "comunidad_id"

        const val TABLE_PUNTOS = "puntos_recoleccion"
        const val COLUMN_PUNTO_ID = "id_punto"
        const val COLUMN_PUNTO_NOMBRE = "nombre_punto"
        const val COLUMN_PUNTO_LAT = "latitud"
        const val COLUMN_PUNTO_LON = "longitud"
        const val COLUMN_PUNTO_CAPACIDAD = "capacidad"
        const val COLUMN_PUNTO_ESTADO = "estado"
        const val COLUMN_PUNTO_COMUNIDAD_ID = "comunidad_id" // Relación directa con comunidades

        const val TABLE_REPORTS = "reportes"
        const val COLUMN_REPORT_ID = "id_reporte"
        const val COLUMN_REPORT_USER_ID = "user_id"
        const val COLUMN_REPORT_PUNTO_ID = "punto_id"
        const val COLUMN_REPORT_TIPO = "tipo_residuo"
        const val COLUMN_REPORT_PESO = "peso"
        const val COLUMN_REPORT_FECHA = "fecha"
        const val COLUMN_REPORT_STATUS = "status"
        const val COLUMN_REPORT_FOTO_PATH = "foto_path"

        const val TABLE_COMMUNITIES = "communities"
        const val COLUMN_COM_ID = "id"
        const val COLUMN_COM_NOMBRE = "nombre"
        const val COLUMN_COM_TIPO = "tipo"
        const val COLUMN_COM_CREADOR = "creador_id"
        const val COLUMN_COM_PUNTOS = "puntos_totales"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsers = ("CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USER_NAME TEXT, " +
                "$COLUMN_USER_EMAIL TEXT UNIQUE, " +
                "$COLUMN_USER_PASSWORD TEXT, " +
                "$COLUMN_USER_ROLE TEXT, " +
                "$COLUMN_USER_KILOS REAL DEFAULT 0.0, " +
                "$COLUMN_USER_COMUNIDAD_ID INTEGER DEFAULT -1)")
        db?.execSQL(createUsers)

        val createPuntos = ("CREATE TABLE $TABLE_PUNTOS (" +
                "$COLUMN_PUNTO_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_PUNTO_NOMBRE TEXT, " +
                "$COLUMN_PUNTO_LAT REAL, " +
                "$COLUMN_PUNTO_LON REAL, " +
                "$COLUMN_PUNTO_CAPACIDAD INTEGER DEFAULT 0, " +
                "$COLUMN_PUNTO_ESTADO TEXT DEFAULT 'Disponible', " +
                "$COLUMN_PUNTO_COMUNIDAD_ID INTEGER DEFAULT -1)") // Columna reincorporada
        db?.execSQL(createPuntos)

        val createReports = ("CREATE TABLE $TABLE_REPORTS (" +
                "$COLUMN_REPORT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_REPORT_USER_ID INTEGER, " +
                "$COLUMN_REPORT_PUNTO_ID INTEGER, " +
                "$COLUMN_REPORT_TIPO TEXT, " +
                "$COLUMN_REPORT_PESO REAL, " +
                "$COLUMN_REPORT_FECHA TEXT, " +
                "$COLUMN_REPORT_STATUS TEXT DEFAULT 'Pendiente', " +
                "$COLUMN_REPORT_FOTO_PATH TEXT, " +
                "FOREIGN KEY($COLUMN_REPORT_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID), " +
                "FOREIGN KEY($COLUMN_REPORT_PUNTO_ID) REFERENCES $TABLE_PUNTOS($COLUMN_PUNTO_ID))")
        db?.execSQL(createReports)

        val createCommunitiesTable = """
            CREATE TABLE $TABLE_COMMUNITIES (
                $COLUMN_COM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COM_NOMBRE TEXT,
                $COLUMN_COM_TIPO TEXT,
                $COLUMN_COM_CREADOR INTEGER,
                $COLUMN_COM_PUNTOS REAL DEFAULT 0.0
            )
        """.trimIndent()
        db?.execSQL(createCommunitiesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_PUNTOS")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_COMMUNITIES")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
        }
    }
}