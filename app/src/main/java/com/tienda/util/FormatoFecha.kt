package com.tienda.util

import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FormatoFecha {

    companion object {

        fun formatearFechaMX(fecha: String): String {
            // Ej: si guardas "2025-09-15 14:30:00"
            return try {
                val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val outFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
                outFmt.format(inFmt.parse(fecha)!!)
            } catch (e: Exception) {
                // Si no matchea el formato, regresa la cadena tal cual para no perderla
                fecha
            }
        }

        // Timestamp en formato SQL
         fun obtenerTimestamp(): String {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("America/Mexico_City")
            return fmt.format(Date())
        }

    }
}