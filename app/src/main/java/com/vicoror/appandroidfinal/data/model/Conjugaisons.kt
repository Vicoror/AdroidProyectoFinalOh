package com.vicoror.appandroidfinal.data.model

data class Frase(
    val id: String = "",
    val num_frase: String = "",
    val fraseFr: String = "",
    val fraseEs: String = "",
    val verbo: String = "",
    val opc_1: String = "",
    val opc_2: String = "",
    val opc_correcta: String = ""
) {
    constructor(map: Map<String, Any>) : this(
        num_frase = map["num_frase"]?.toString() ?: "",
        id = map["num_frase"]?.toString() ?: "",
        fraseFr = map["fraseFr"] as? String ?: "",
        fraseEs = map["fraseEs"] as? String ?: "",
        verbo = map["verbo"] as? String ?: "",
        opc_1 = map["opc_1"] as? String ?: "",
        opc_2 = map["opc_2"] as? String ?: "",
        opc_correcta = map["opc_correcta"] as? String ?: ""
    )
}
data class Conjugacion(
    val id: String = "",
    val num_conju: Int = 0,
    val sujeto: String = "",
    val verbo: String = "",

    // Respuestas completas
    val resp_present: String = "",
    val resp_futur: String = "",
    val resp_impar: String = "",
    val resp_passec: String = "",

    // Verbos conjugados (sin sujeto)
    val verb_present: String = "",
    val verb_futur: String = "",
    val verb_impar: String = "",
    val verb_passec: String = ""
) {
    // Constructor para Firestore
    constructor(map: Map<String, Any>) : this(
        id = map["num_conju"]?.toString() ?: "",
        num_conju = (map["num_conju"] as? Long)?.toInt() ?: 0,
        sujeto = map["sujeto"] as? String ?: "",
        verbo = map["verbo"] as? String ?: "",
        resp_present = map["resp_present"] as? String ?: "",
        resp_futur = map["resp_futur"] as? String ?: "",
        resp_impar = map["resp_impar"] as? String ?: "",
        resp_passec = map["resp_passec"] as? String ?: "",
        verb_present = map["verb_present"] as? String ?: "",
        verb_futur = map["verb_futur"] as? String ?: "",
        verb_impar = map["verb_impar"] as? String ?: "",
        verb_passec = map["verb_passec"] as? String ?: ""
    )

    // Método para obtener la conjugación según el tiempo
    fun getConjugacionByTense(tense: String): String {
        return when (tense.lowercase()) {
            "presente", "présent", "present" -> resp_present
            "futur", "futur simple" -> resp_futur
            "imparfait", "imparfait" -> resp_impar
            "passecompose", "passé composé", "passe compose" -> resp_passec
            else -> resp_present
        }
    }

    // Método para obtener solo el verbo conjugado (sin sujeto)
    fun getVerboConjugadoByTense(tense: String): String {
        return when (tense.lowercase()) {
            "presente", "présent", "present" -> verb_present
            "futur", "futur simple" -> verb_futur
            "imparfait", "imparfait" -> verb_impar
            "passecompose", "passé composé", "passe compose" -> verb_passec
            else -> verb_present
        }
    }

    // Para mostrar en UI
    fun getDisplayText(tense: String): String {
        return "$sujeto ${getVerboConjugadoByTense(tense)}"
    }
}