package com.vicoror.appandroidfinal.viewModel

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.FirebaseFirestore
import com.vicoror.appandroidfinal.data.model.Frase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ConjugaisonsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _frases = MutableLiveData<List<Frase>>()
    val frases: LiveData<List<Frase>> = _frases

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    companion object {
        private const val TAG = "ConjugaisonsVM"
    }

    /**
     * 🔥 Carga TODAS las frases de una colección
     * Usado por: ConjugaisonsFragment
     */
    fun loadTodasLasFrases(collectionName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val snapshot = db.collection(collectionName)
                    .orderBy("num_frase")
                    .get()
                    .await()

                val todas = snapshot.documents.mapNotNull { doc ->
                    try {
                        Frase(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                }

                // ⚠️ IMPORTANTE: Guarda TODAS las frases, no solo un bloque
                _frases.value = todas

                Log.d(
                    TAG,
                    "✅ Cargadas ${todas.size} frases de $collectionName"
                )

            } catch (e: Exception) {
                _error.value = "Error cargando frases: ${e.localizedMessage}"
                _frases.value = emptyList()
                Log.e(TAG, "❌ Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🔥 Carga frases de UN SOLO bloque específico
     * Usado por: Nivel1ConjugaisonsFragment
     */
    fun loadFrasesDelBloque(
        collectionName: String,
        blockIndex: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // PRIMERO: Cargar todas las frases
                val snapshot = db.collection(collectionName)
                    .orderBy("num_frase")
                    .get()
                    .await()

                val todas = snapshot.documents.mapNotNull { doc ->
                    try {
                        Frase(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                }

                Log.d(TAG, "📊 Total frases en colección: ${todas.size}")

                // SEGUNDO: Dividir en bloques de 10 y obtener el bloque específico
                val bloques = todas.chunked(10)

                if (bloques.isEmpty()) {
                    _frases.value = emptyList()
                    Log.w(TAG, "⚠️ No hay bloques disponibles")
                } else if (blockIndex < bloques.size) {
                    _frases.value = bloques[blockIndex]
                    Log.d(TAG, "🎯 Cargado bloque $blockIndex: ${bloques[blockIndex].size} frases")
                } else {
                    _frases.value = emptyList()
                    Log.w(TAG, "⚠️ Bloque $blockIndex no disponible (solo ${bloques.size} bloques)")
                }

            } catch (e: Exception) {
                _error.value = "Error cargando bloque: ${e.localizedMessage}"
                _frases.value = emptyList()
                Log.e(TAG, "❌ Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🔥 Versión MEJORADA: Carga todas las frases y luego filtra el bloque
     * Más eficiente porque no hace dos llamadas a Firestore
     */
    fun loadFrasesDelBloqueMejorado(
        collectionName: String,
        blockIndex: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val snapshot = db.collection(collectionName)
                    .orderBy("num_frase")
                    .get()
                    .await()

                val todas = snapshot.documents.mapNotNull { doc ->
                    try {
                        Frase(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                }

                Log.d(TAG, "📊 Colección $collectionName: ${todas.size} frases totales")

                // Calcular rango del bloque
                val frasesPorBloque = 10
                val startIndex = blockIndex * frasesPorBloque
                val endIndex = minOf(startIndex + frasesPorBloque, todas.size)

                if (startIndex < todas.size) {
                    val frasesDelBloque = todas.subList(startIndex, endIndex)
                    _frases.value = frasesDelBloque
                    Log.d(TAG, "🎯 Bloque $blockIndex: frases $startIndex-$endIndex (${frasesDelBloque.size} frases)")
                } else {
                    _frases.value = emptyList()
                    Log.w(TAG, "⚠️ Bloque $blockIndex fuera de rango (0-${todas.size / frasesPorBloque})")
                }

            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
                _frases.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🔥 Carga un número específico de frases (para paginación)
     */
    fun loadFrasesConLimite(
        collectionName: String,
        limit: Int = 10,
        startAfter: Int = 0
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val snapshot = db.collection(collectionName)
                    .orderBy("num_frase")
                    .limit(limit.toLong())
                    .startAfter(startAfter.toDouble())
                    .get()
                    .await()

                val frasesLimitadas = snapshot.documents.mapNotNull { doc ->
                    try {
                        Frase(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                }

                _frases.value = frasesLimitadas
                Log.d(TAG, "📄 Cargadas ${frasesLimitadas.size} frases (límite: $limit)")

            } catch (e: Exception) {
                _error.value = e.localizedMessage
                _frases.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpia los datos del ViewModel
     */
    fun clear() {
        _frases.value = emptyList()
        _error.value = null
        Log.d(TAG, "🧹 ViewModel limpiado")
    }

    /**
     * Obtiene el número total de bloques disponibles
     * Útil para ConjugaisonsFragment
     */
    fun getTotalBloques(frases: List<Frase>, frasesPorBloque: Int = 10): Int {
        return if (frases.isEmpty()) 0 else (frases.size + frasesPorBloque - 1) / frasesPorBloque
    }

    /**
     * DEBUG: Muestra información sobre las frases cargadas
     */
    fun debugFrases() {
        val frasesActuales = _frases.value
        if (frasesActuales != null) {
            Log.d(TAG, "🔍 DEBUG - Frases en ViewModel:")
            Log.d(TAG, "   • Total: ${frasesActuales.size}")
            Log.d(TAG, "   • Primeras 3 verbos: ${frasesActuales.take(3).map { it.verbo }}")
            Log.d(TAG, "   • IDs: ${frasesActuales.take(3).map { it.id }}")
        } else {
            Log.d(TAG, "🔍 DEBUG - No hay frases cargadas")
        }
    }
}