package com.vicoror.appandroidfinal.utils

object Utils {

    // 🔑 PARA PREFS / PROGRESO
    fun normalizeTenseKey(raw: String): String {
        return when (raw.lowercase()) {
            "présent", "presente", "present" -> "present"
            "futur simple", "futur" -> "futur"
            "imparfait" -> "imparfait"
            "passé composé", "passe compose", "passecompose" -> "passecompose"
            else -> "present"
        }
    }

    // 📦 PARA JSON / FIRESTORE
    fun mapToJsonName(raw: String): String {
        return when (raw.lowercase()) {
            "présent", "presente", "present" -> "Presente"
            "futur simple", "futur" -> "Futur"
            "imparfait" -> "Imparfait"
            "passé composé", "passe compose", "passecompose" -> "PasseCompose"
            else -> "Presente"
        }
    }
}
