package com.vicoror.appandroidfinal.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.data.model.PhrasesResponse

class PhrasesViewModel(application: Application) : AndroidViewModel(application) {

    private val _phrases = MutableLiveData<List<List<com.vicoror.appandroidfinal.data.model.Phrase>>>()
    val phrases: LiveData<List<List<com.vicoror.appandroidfinal.data.model.Phrase>>> = _phrases

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    companion object {
        private const val TAG = "PhrasesViewModel"
    }

    init {
        Log.d(TAG, "ViewModel inicializado")
        loadPhrasesFromAssets()
    }

    fun loadPhrasesFromAssets() {
        _isLoading.value = true
        _error.value = null

        val context = getApplication<Application>().applicationContext
        try {
            Log.d(TAG, "=== CARGANDO FRASES DESDE res/raw ===")

            // Cargar desde res/raw
            val inputStream = context.resources.openRawResource(R.raw.frases)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            Log.d(TAG, "✅ JSON cargado: ${jsonString.length} caracteres")

            // Parsear JSON
            val response = Gson().fromJson(jsonString, PhrasesResponse::class.java)
            val allPhrases = response.frases

            Log.d(TAG, "🎉 ${allPhrases.size} frases cargadas exitosamente")

            // Crear bloques de 10 frases
            val bloques = allPhrases.chunked(10)
            Log.d(TAG, "📦 ${bloques.size} bloques creados")

            _phrases.value = bloques

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR cargando frases: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            _error.value = "Error cargando frases: ${e.message}"
            _phrases.value = emptyList()
        } finally {
            _isLoading.postValue(false)
        }
    }

    fun reloadPhrases() {
        loadPhrasesFromAssets()
    }
}