package com.vicoror.appandroidfinal.data.model

data class Phrase(
    val numfrase: Int,
    val nivel: String,
    val tipo: String,
    val fraseFr: String,
    val fraseEs: String,
    val categoria: String,
    val sonidoPh: String
)

data class PhrasesResponse(
    val frases: List<Phrase>
)

data class Verbes(
    val numfrase: Int,
    val nivel: String,
    val fraseFr: String,
    val fraseEs: String,
    val audioVerbos: String
)

data class VerbesResponse(
    val verbes: List<Verbes>
)
