package com.meza.ecoresiduos.db // Ajusta a tu paquete

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EcoResiduos.db"
        private const val DATABASE_VERSION = 3 // Incrementamos versión por los cambios

        // --- TABLA USUARIOS ---
        const val TABLE_USERS = "usuarios"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_NAME = "nombre"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_ROLE = "role" // 'admin' o 'user'
        const val COLUMN_USER_KILOS = "kilos_totales"

        // --- TABLA PUNTOS DE RECOLECCIÓN (Nueva) ---
        const val TABLE_PUNTOS = "puntos_recoleccion"
        const val COLUMN_PUNTO_ID = "id_punto"
        const val COLUMN_PUNTO_NOMBRE = "nombre_punto"
        const val COLUMN_PUNTO_LAT = "latitud"
        const val COLUMN_PUNTO_LON = "longitud"
        const val COLUMN_PUNTO_CAPACIDAD = "capacidad" // 0 a 100
        const val COLUMN_PUNTO_ESTADO = "estado" // 'Disponible', 'Lleno', 'En Recolección'

        // --- TABLA REPORTES (Actualizada con Foto y Punto) ---
        const val TABLE_REPORTS = "reportes"
        const val COLUMN_REPORT_ID = "id_reporte"
        const val COLUMN_REPORT_USER_ID = "user_id"
        const val COLUMN_REPORT_PUNTO_ID = "punto_id" // Relación con el punto
        const val COLUMN_REPORT_TIPO = "tipo_residuo"
        const val COLUMN_REPORT_PESO = "peso"
        const val COLUMN_REPORT_FECHA = "fecha"
        const val COLUMN_REPORT_STATUS = "status" // 'Pendiente', 'Aprobado', 'Rechazado'
        const val COLUMN_REPORT_FOTO_PATH = "foto_path" // Ruta de la imagen en el storage
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla Usuarios
        val createUsers = ("CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USER_NAME TEXT, " +
                "$COLUMN_USER_EMAIL TEXT UNIQUE, " +
                "$COLUMN_USER_PASSWORD TEXT, " +
                "$COLUMN_USER_ROLE TEXT, " +
                "$COLUMN_USER_KILOS REAL DEFAULT 0.0)")
        db.execSQL(createUsers)

        // Crear tabla Puntos de Recolección
        val createPuntos = ("CREATE TABLE $TABLE_PUNTOS (" +
                "$COLUMN_PUNTO_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_PUNTO_NOMBRE TEXT, " +
                "$COLUMN_PUNTO_LAT REAL, " +
                "$COLUMN_PUNTO_LON REAL, " +
                "$COLUMN_PUNTO_CAPACIDAD INTEGER DEFAULT 0, " +
                "$COLUMN_PUNTO_ESTADO TEXT DEFAULT 'Disponible')")
        db.execSQL(createPuntos)

        // Crear tabla Reportes
        val createReports = ("CREATE TABLE $TABLE_REPORTS (" +
                "$COLUMN_REPORT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_REPORT_USER_ID INTEGER, " +
                "$COLUMN_REPORT_PUNTO_ID INTEGER, " +
                "$COLUMN_REPORT_TIPO TEXT, " +
                "$COLUMN_REPORT_PESO REAL, " +
                "$COLUMN_REPORT_FECHA TEXT, " +
                "$COLUMN_REPORT_STATUS TEXT DEFAULT 'Pendiente', " +
                "$COLUMN_REPORT_FOTO_PATH TEXT, " + // Guardamos la ruta del archivo
                "FOREIGN KEY($COLUMN_REPORT_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID), " +
                "FOREIGN KEY($COLUMN_REPORT_PUNTO_ID) REFERENCES $TABLE_PUNTOS($COLUMN_PUNTO_ID))")
        db.execSQL(createReports)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Para desarrollo: borramos y recreamos (¡Cuidado! Esto borra datos actuales)
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PUNTOS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
        }
    }
}