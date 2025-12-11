package com.vicoror.appandroidfinal.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.data.model.Phrase
import com.vicoror.appandroidfinal.data.model.PhrasesResponse
import com.vicoror.appandroidfinal.data.model.Verbes

class PhrasesViewModel(application: Application) : AndroidViewModel(application) {

    private val _phrases = MutableLiveData<List<List<com.vicoror.appandroidfinal.data.model.Phrase>>>()
    val phrases: LiveData<List<List<com.vicoror.appandroidfinal.data.model.Phrase>>> = _phrases
    data class VerbesResponse(
        @SerializedName("verbos") val verbos: List<Verbes>
    )

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    companion object {
        private const val TAG = "PhrasesViewModel"
    }

    fun loadJson(mode: String) {
        _isLoading.value = true
        _error.value = null

        val context = getApplication<Application>().applicationContext

        try {
            val rawId = if (mode == "verbs") R.raw.verbos else R.raw.frases

            val inputStream = context.resources.openRawResource(rawId)
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            if (mode == "verbs") {

                // Parsear verbos
                val response = Gson().fromJson(jsonString, VerbesResponse::class.java)
                val allVerbs = response.verbos

                // Convertir verbos → Phrase para reutilizar TU app sin cambiar nada
                val phrasesConverted = allVerbs.map {
                    Phrase(
                        numfrase = it.numfrase,
                        nivel = it.nivel,
                        tipo = "",
                        fraseFr = it.fraseFr,
                        fraseEs = it.fraseEs,
                        categoria = "",
                        sonidoPh = it.audioVerbos
                    )
                }

                val bloques = phrasesConverted.chunked(10)
                _phrases.value = bloques

            } else {
                // Parsear frases normales
                val response = Gson().fromJson(jsonString, PhrasesResponse::class.java)
                _phrases.value = response.frases.chunked(10)
            }

        } catch (e: Exception) {
            _error.value = e.message
            _phrases.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }



    fun loadPhrasesFromAssets() {
    }

    fun reloadPhrases() {
        loadPhrasesFromAssets()
    }
}