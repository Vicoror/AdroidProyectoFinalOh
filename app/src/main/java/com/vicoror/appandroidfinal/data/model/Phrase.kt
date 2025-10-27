package com.vicoror.appandroidfinal.data.model

data class Phrase(
    val numfrase: Int,
    val nivel: String,
    val tipo: String,
    val fraseFr: String,
    val fraseEs: String,
    val categoria: String
)

data class PhrasesResponse(
    val frases: List<Phrase>
)