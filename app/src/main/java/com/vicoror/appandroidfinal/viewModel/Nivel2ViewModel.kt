package com.vicoror.appandroidfinal.viewModel

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.FirebaseFirestore
import com.vicoror.appandroidfinal.data.model.Conjugacion
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConjugacionesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _conjugaciones = MutableLiveData<List<Conjugacion>>()
    val conjugaciones: LiveData<List<Conjugacion>> = _conjugaciones

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Carga todas las conjugaciones de Firestore y luego filtra por bloque
     */
    fun loadConjugaciones() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val snapshot = db.collection("Conjugaciones")
                    .orderBy("num_conju")
                    .get()
                    .await()

                val todasConjugaciones = snapshot.documents.mapNotNull { doc ->
                    try {
                        Conjugacion(doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                }

                _conjugaciones.value = todasConjugaciones

                Log.d(
                    "ConjugacionesVM",
                    "✅ Total cargadas: ${todasConjugaciones.size}"
                )

            } catch (e: Exception) {
                _error.value = "Error cargando conjugaciones: ${e.localizedMessage}"
                _conjugaciones.value = emptyList()
                Log.e("ConjugacionesVM", "❌ Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * Obtiene la respuesta correcta para una conjugación según el tiempo
     */
    fun getRespuestaCorrecta(conjugacion: Conjugacion, tense: String): String {
        return when (tense.lowercase()) {
            "presente", "présent" -> conjugacion.resp_present
            "futur", "futur simple" -> conjugacion.resp_futur
            "imparfait" -> conjugacion.resp_impar
            "passecompose", "passé composé" -> conjugacion.resp_passec
            else -> conjugacion.resp_present
        }
    }

    /**
     * Obtiene solo el verbo conjugado (sin sujeto)
     */
    fun getVerboConjugado(conjugacion: Conjugacion, tense: String): String {
        return when (tense.lowercase()) {
            "presente", "présent" -> conjugacion.verb_present
            "futur", "futur simple" -> conjugacion.verb_futur
            "imparfait" -> conjugacion.verb_impar
            "passecompose", "passé composé" -> conjugacion.verb_passec
            else -> conjugacion.verb_present
        }
    }

    fun clear() {
        _conjugaciones.value = emptyList()
        _error.value = null
    }
}