package com.vicoror.appandroidfinal.ui.conjugaisons

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.data.model.Conjugacion
import com.vicoror.appandroidfinal.databinding.FragmentNivel2ConjugaisonsBinding
import com.vicoror.appandroidfinal.utils.MacaronManager
import com.vicoror.appandroidfinal.utils.MacaronRainFragment
import com.vicoror.appandroidfinal.viewModel.ConjugacionesViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class Nivel2ConjugaisonsFragment : Fragment() {

    companion object {
        private const val TAG = "Nivel2Conjugaisons"
        private const val MAX_INTENTOS = 2
    }

    private lateinit var binding: FragmentNivel2ConjugaisonsBinding
    private val viewModel: ConjugacionesViewModel by viewModels()

    private lateinit var selectedTense: String
    private var blockIndex = 0

    private var conjugaciones: List<Conjugacion> = emptyList()
    private var currentQuestionIndex = 0
    private var bonnesReponses = 0
    private var intentosActuales = 0
    private var respuestaVerificada = false
    private var respuestaUsuario = ""

    private lateinit var jsonFileName: String

    private var timer: CountDownTimer? = null
    private var timeRemaining = 100_000L // 100 segundos
    private var isTimerPaused = false

    private lateinit var macaronManager: MacaronManager

    private val args: Nivel2ConjugaisonsFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedTense = args.selectedTense
        jsonFileName = args.jsonFileName
        blockIndex = args.blockIndex

        Log.d(TAG, "Nivel2: jsonFileName=$jsonFileName, selectedTense=$selectedTense, blockIndex=$blockIndex")
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNivel2ConjugaisonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ PRIMERO inicializar macaronManager
        macaronManager = MacaronManager.getInstance(requireContext())

        // ✅ LUEGO setupUI (que usará macaronManager)
        setupUI()
        setupObservers()
        setupListeners()

        // Cargar conjugaciones
        viewModel.loadConjugaciones()
    }
    private fun setupUI() {
        binding.bonnesLabel.text = "Bonnes Réponses: 0"
        updateLivesView()
    }

    private fun setupObservers() {
        viewModel.conjugaciones.observe(viewLifecycleOwner) { todas ->

            if (todas.isNotEmpty()) {

                val blockSize = 10
                val startIndex = blockIndex * blockSize
                val endIndex = minOf(startIndex + blockSize, todas.size)

                if (startIndex >= todas.size) {
                    Toast.makeText(
                        requireContext(),
                        "Bloque no disponible",
                        Toast.LENGTH_SHORT
                    ).show()
                    requireActivity().onBackPressed()
                    return@observe
                }

                // 🔑 AQUÍ está la clave
                conjugaciones = todas.subList(startIndex, endIndex)

                Log.d(
                    "Nivel2",
                    "🧱 Nivel2 bloque $blockIndex → ${conjugaciones.size} conjugaciones ($startIndex-$endIndex)"
                )

                currentQuestionIndex = 0
                bonnesReponses = 0
                intentosActuales = 0
                respuestaVerificada = false

                mostrarPregunta(currentQuestionIndex)
                startTimer()
                updateLivesView()

            } else {
                Toast.makeText(
                    requireContext(),
                    "No hay conjugaciones disponibles",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().onBackPressed()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            // opcional
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnExit.setOnClickListener {
            exitButtonTapped()  }
        binding.btnPro.setOnClickListener { proButtonTapped() }
        binding.btnSuivant.setOnClickListener { nextQuestion() }
        binding.btnReponse.setOnClickListener { showAnswer() }

        // Configurar TextWatcher para el campo de respuesta
        binding.respuestaTextField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                respuestaUsuario = s.toString()
                // Habilitar/deshabilitar botón suivant basado en si hay texto
                binding.btnSuivant.isEnabled = s?.isNotEmpty() == true || respuestaVerificada
            }
        })

    }

    // ---------------- FUNCIONES DE NORMALIZACIÓN ----------------

    private fun normalizarRespuesta(respuesta: String): String {
        // Limitar a 60 caracteres
        val limited = if (respuesta.length > 60) respuesta.substring(0, 60) else respuesta

        // Trim espacios
        val trimmed = limited.trim()

        // Eliminar caracteres peligrosos
        val safeText = eliminarCaracteresPeligrosos(trimmed)

        return safeText.lowercase()
    }

    private fun eliminarCaracteresPeligrosos(text: String): String {
        // Permitir letras, espacios, acentos y signos de puntuación comunes
        val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "áéíóúàèìòùäëïöüâêîôûñç ,.!?;:-'\"()"

        return text.filter { char ->
            allowedChars.contains(char, ignoreCase = true) || char == ' '
        }
    }

    private fun esRespuestaValida(respuestaUsuario: String, conjugacion: Conjugacion): Boolean {
        val usuarioNormalizado = normalizarRespuesta(respuestaUsuario)

        // Obtener ambas posibles respuestas (con y sin sujeto)
        val respuestaCompleta = viewModel.getRespuestaCorrecta(conjugacion, selectedTense)
        val verboConjugado = viewModel.getVerboConjugado(conjugacion, selectedTense)

        val respNormalizada = normalizarRespuesta(respuestaCompleta)
        val verbNormalizada = normalizarRespuesta(verboConjugado)

        // Aceptar cualquiera de las dos formas
        return usuarioNormalizado == respNormalizada || usuarioNormalizado == verbNormalizada
    }

    // ---------------- FUNCIONES DE PREGUNTAS ----------------

    private fun mostrarPregunta(index: Int) {
        if (index >= conjugaciones.size) {
            ejercicioCompletado()
            return
        }

        val conjugacion = conjugaciones[index]

        // Mostrar verbo y sujeto
        binding.verbeLabel.text = "Verbe: ${conjugacion.verbo}"
        binding.sujetLabel.text = "Sujet: ${conjugacion.sujeto}"

        // 1. QUITAR EL FOCO primero
        binding.respuestaTextField.clearFocus()
        binding.textInputLayout.clearFocus()

        // 2. Limpiar campo de respuesta
        binding.respuestaTextField.text?.clear()
        respuestaUsuario = ""

        // 3. RESETEAR COLOR DEL TextInputLayout
        resetearColorTextField()

        // 4. Resetear estado
        intentosActuales = 0
        respuestaVerificada = false
        binding.btnReponse.visibility = View.GONE

        // 5. Actualizar botón suivant
        binding.btnSuivant.text = "Suivant ${index + 1}/${conjugaciones.size}"
        binding.btnSuivant.isEnabled = false

        // 6. Ocultar teclado si está visible
        hideKeyboard()
    }

    private fun resetearColorTextField() {
        // Forzar el estado normal (sin foco)
        binding.textInputLayout.isEnabled = false
        binding.textInputLayout.isEnabled = true

        // Restaurar colores originales
        binding.textInputLayout.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.principal)
        binding.textInputLayout.defaultHintTextColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.principal)
        )
        binding.textInputLayout.hintTextColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.principal)
        )

        // También resetear el color del texto
        binding.respuestaTextField.setTextColor(ContextCompat.getColor(requireContext(), R.color.textdark))
    }



    private fun verificarRespuesta() {
        if (currentQuestionIndex >= conjugaciones.size) return

        if (!macaronManager.canPlay()) {
            // Bloquear interacción
            binding.respuestaTextField.isEnabled = false
            binding.btnSuivant.isEnabled = false
            binding.btnReponse.isEnabled = false

            // Mostrar mensaje
            val timeLeft = macaronManager.timeUntilRecovery()
            val mensaje = if (timeLeft != null) {
                val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
                "¡Sin macarones! Espera ${hours}h ${minutes}m"
            } else {
                "¡Sin macarones disponibles!"
            }

            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
            return
        }

        // ✅ Verificar disponibilidad de macarones primero
        if (!::macaronManager.isInitialized) {
            macaronManager = MacaronManager.getInstance(requireContext())
        }

        if (!macaronManager.canPlay()) {
            macaronManager.showRecoveryAlertDialog(requireContext())
            return
        }

        val conjugacion = conjugaciones[currentQuestionIndex]

        if (respuestaUsuario.isEmpty()) {
            Toast.makeText(requireContext(), "Escribe una respuesta", Toast.LENGTH_SHORT).show()
            return
        }

        val esCorrecta = esRespuestaValida(respuestaUsuario, conjugacion)

        if (esCorrecta) {
            // Solo sumar si es la primera vez que es correcta
            if (intentosActuales == 0) {
                bonnesReponses++
                binding.bonnesLabel.text = "Bonnes Réponses: $bonnesReponses"
            }
            mostrarRespuestaCorrecta()
            respuestaVerificada = true
            binding.btnSuivant.text = "Continuer →"
            binding.btnSuivant.isEnabled = true
        } else {
            intentosActuales++

            if (intentosActuales == 1) {
                // Primer intento incorrecto - consumir macaron
                lifecycleScope.launch {
                    val consumido = macaronManager.consumeMacaron()
                    if (consumido) {
                        updateLivesView()
                        mostrarMensajeMacaronConsumido()
                    }
                }
            }

            mostrarRespuestaIncorrecta()

            if (intentosActuales >= MAX_INTENTOS) {
                binding.btnReponse.visibility = View.VISIBLE
                respuestaVerificada = true
                binding.btnSuivant.isEnabled = true
            }
        }
    }

    private fun mostrarMensajeMacaronConsumido() {
        Snackbar.make(
            binding.root,
            "Macarón usado. Te quedan: ${macaronManager.currentMacaronCount}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun mostrarRespuestaCorrecta() {
        // Cambiar color del borde a verde
        binding.respuestaTextField.setBackgroundResource(R.color.warningblue)
        Toast.makeText(requireContext(), "Correct! 🎉", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarRespuestaIncorrecta() {
        // Cambiar color del borde a rojo
        binding.respuestaTextField.setBackgroundResource(R.color.warningred)
        Toast.makeText(requireContext(), "Incorrect, intenta de nuevo", Toast.LENGTH_SHORT).show()
        if (macaronManager.currentMacaronCount <= 0) {
            Toast.makeText(requireContext(), "¡Se acabaron los macarones!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAnswer() {
        if (currentQuestionIndex >= conjugaciones.size) return

        val conjugacion = conjugaciones[currentQuestionIndex]
        val respuestaCorrecta = viewModel.getRespuestaCorrecta(conjugacion, selectedTense)

        // Mostrar respuesta en el campo de texto
        binding.respuestaTextField.setText(respuestaCorrecta)

        // Marcar como verificada
        respuestaVerificada = true
        binding.btnSuivant.text = "Continuer →"
        binding.btnSuivant.isEnabled = true
        binding.btnReponse.visibility = View.GONE

        Toast.makeText(requireContext(), "Respuesta: $respuestaCorrecta", Toast.LENGTH_LONG).show()
    }

    private fun nextQuestion() {
        // PRIMER CLIC: Verificar respuesta si no se ha hecho y hay respuesta
        if (!respuestaVerificada && respuestaUsuario.isNotEmpty()) {
            verificarRespuesta()
            return
        }

        // SEGUNDO CLIC O CONTINUAR: Avanzar a siguiente pregunta
        if (respuestaVerificada) {
            currentQuestionIndex++
            mostrarPregunta(currentQuestionIndex)
        } else {
            Toast.makeText(requireContext(), "Responde primero", Toast.LENGTH_SHORT).show()
        }
    }

// ---------------- TIMER ----------------

    private fun startTimer() {
        timer?.cancel()

        timer = object : CountDownTimer(timeRemaining, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                binding.timerLabel.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                timeExpired()
            }
        }.start()
    }

    private fun pauseTimer() {
        timer?.cancel()
        timer = null
        binding.btnPro.text = "▶️"
    }

    private fun resumeTimer() {
        startTimer()
        binding.btnPro.text = "∞"
    }

    private fun proButtonTapped() {
        if (timer == null) {
            // Está pausado, reanudar
            resumeTimer()
            isTimerPaused = false
        } else {
            // Está corriendo, pausar
            pauseTimer()
            isTimerPaused = true
        }
    }

    private fun timeExpired() {
        timer?.cancel()
        timer = null

        Toast.makeText(requireContext(), "Tiempo agotado", Toast.LENGTH_SHORT).show()

        timeRemaining = 100_000L
        currentQuestionIndex = 0
        bonnesReponses = 0
        binding.bonnesLabel.text = "Bonnes Réponses: 0"

        mostrarPregunta(0)
        startTimer()
    }
    // ---------------- MACARONS ----------------

    private fun updateLivesView() {
        binding.livesStack.removeAllViews()

        val macarons = listOf(
            R.drawable.macaron_c5a4da,
            R.drawable.macaron_fffa9c,
            R.drawable.macaron_8cd5c9,
            R.drawable.macaron_ff8a80,
            R.drawable.macaron_d4e9f8
        )

        val totalMacarons = macarons.size
        val currentCount = macaronManager.currentMacaronCount

        macarons.forEachIndexed { index, resId ->
            val image = ImageView(requireContext()).apply {
                setImageResource(resId)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(30),
                    dpToPx(30)
                ).apply {
                    setMargins(0, 0, dpToPx(6), 0)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER

                alpha = if (index < currentCount) 1f else 0.25f
            }
            binding.livesStack.addView(image)
        }
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()

    // ---------------- EJERCICIO COMPLETADO ----------------

    private fun ejercicioCompletado() {
        timer?.cancel()

        // Guardar progreso
        marcarBloqueCompletado()

        // DEBUG: Verificar que se guardó correctamente
        val prefs = requireContext().getSharedPreferences("conjugaisons_progress", 0)
        val lastCompleted = prefs.getInt("last_block_$selectedTense", -1)
        Log.d(TAG, "✅ DEBUG - Último bloque completado después de guardar: $lastCompleted")

        // Mostrar lluvia de macarrones
        mostrarLluviaMacaronsYRegresar()
    }

    private fun marcarBloqueCompletado() {
        val prefs = requireContext().getSharedPreferences("conjugaisons_progress", 0)

        // DEBUG: Mostrar todas las claves
        val allKeys = prefs.all.keys
        Log.d(TAG, "🔑 Todas las claves en SharedPreferences:")
        allKeys.forEach { key ->
            Log.d(TAG, "   • $key = ${prefs.all[key]}")
        }

        // ⚠️ CORRECCIÓN CRÍTICA: Usar SIEMPRE selectedTense
        val keyToUse = selectedTense  // "Futur simple", "Présent", etc.

        Log.d(TAG, "🔑 Usando formato de clave: '$keyToUse'")

        // Guardar usando el tiempo verbal correcto
        prefs.edit()
            .putInt("block_${keyToUse}_$blockIndex", 10)
            .putInt("last_block_$keyToUse", blockIndex)
            .apply()

        // Verificar
        val newProgress = prefs.getInt("block_${keyToUse}_$blockIndex", 0)
        val newLastBlock = prefs.getInt("last_block_$keyToUse", -1)

        Log.d(TAG, "✅ Guardado: block_${keyToUse}_$blockIndex = $newProgress")
        Log.d(TAG, "✅ Guardado: last_block_$keyToUse = $newLastBlock")

        // Debug adicional
        Log.d(TAG, "🔍 Verificando guardado:")
        Log.d(TAG, "   • block_${keyToUse}_$blockIndex = ${prefs.getInt("block_${keyToUse}_$blockIndex", -999)}")
        Log.d(TAG, "   • last_block_$keyToUse = ${prefs.getInt("last_block_$keyToUse", -999)}")
    }


    private fun mostrarLluviaMacaronsYRegresar() {
        // Validar que el fragment esté activo
        if (!isAdded || isDetached) return

        val macaronFragment = MacaronRainFragment()

        // Ocultar elementos
        binding.topBar.visibility = View.GONE
        binding.scrollContent.visibility = View.GONE
        hideKeyboard()

        // Asegurar que el contenedor esté listo
        binding.macaronContainer.post {
            binding.macaronContainer.bringToFront()
            binding.macaronContainer.visibility = View.VISIBLE

            childFragmentManager.beginTransaction()
                .replace(R.id.macaronContainer, macaronFragment, "MACARON_RAIN")
                .commitAllowingStateLoss() // Usar commitAllowingStateLoss para evitar crashes
        }


        // Navegar después de un delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded || isDetached) return@postDelayed

            try {
                // Limpiar
                childFragmentManager.findFragmentByTag("MACARON_RAIN")?.let {
                    childFragmentManager.beginTransaction()
                        .remove(it)
                        .commitAllowingStateLoss()
                }

                binding.macaronContainer.visibility = View.GONE

                // Navegar al fragment principal
                navigateToConjugaisonsFragment()

            } catch (e: Exception) {
                Log.e(TAG, "Error en postDelayed: ${e.message}")
                safeNavigateBack()
            }
        }, 6000)
    }


    private fun safeNavigateBack() {
        try {
            if (isAdded && !isDetached) {
                findNavController().navigateUp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en safeNavigateBack: ${e.message}")
            try {
                requireActivity().onBackPressed()
            } catch (e2: Exception) {
                Log.e(TAG, "Error crítico: ${e2.message}")
            }
        }
    }

    private fun navigateToConjugaisonsFragment() {
        try {
            // ⚠️ CORRECCIÓN: Usa jsonFileName (no selectedTense)
            val bundle = Bundle().apply {
                putString("selectedTense", selectedTense)  // "Présent"
                putString("jsonFileName", jsonFileName)    // "Presente" ← ¡CORRECTO!
            }

            // También puedes usar Safe Args si los tienes configurados
            val action = Nivel2ConjugaisonsFragmentDirections
                .actionNivel2ConjugaisonsFragmentToConjugaisonsFragment(
                    selectedTense = selectedTense,
                    jsonFileName = jsonFileName  // ← Pasa la variable correcta
                )

            findNavController().navigate(action)

        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar: ${e.message}")
            safeNavigateBack()
        }
    }
    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Opción 1: Usar el EditText directamente
        imm.hideSoftInputFromWindow(binding.respuestaTextField.windowToken, 0)

        // Opción 2: También intentar con currentFocus
        val currentFocus = requireActivity().currentFocus
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
    private fun isNivel2Unlocked(): Boolean {
        val prefs = requireContext().getSharedPreferences("conjugaisons_progress", 0)

        return prefs.getBoolean(
            "unlocked_${selectedTense}_$blockIndex",
            blockIndex == 0 // el primer bloque siempre desbloqueado
        )
    }

    private fun exitButtonTapped() {
        timer?.cancel()

        try {
            // DEBUG: Verificar valores
            Log.d(TAG, "🚪 Saliendo a ConjugaisonsFragment:")
            Log.d(TAG, "   • selectedTense: $selectedTense")
            Log.d(TAG, "   • jsonFileName: $jsonFileName")

            // Corregir jsonFileName si es necesario
            val coleccionCorregida = when {
                jsonFileName == selectedTense -> {
                    // Si son iguales (ej: "Présent"), usar la colección correcta
                    when (selectedTense) {
                        "Présent" -> "Presente"
                        "Futur simple" -> "Futur"
                        "Imparfait" -> "Imparfait"
                        "Passé composé" -> "PasseCompose"
                        else -> jsonFileName
                    }
                }
                else -> jsonFileName
            }

            Log.d(TAG, "   • colección corregida: $coleccionCorregida")

            // Navegar
            val action = Nivel2ConjugaisonsFragmentDirections
                .actionNivel2ConjugaisonsFragmentToConjugaisonsFragment(
                    selectedTense = selectedTense,
                    jsonFileName = coleccionCorregida
                )

            findNavController().navigate(action)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error navegando: ${e.message}")
            // Fallback seguro
            findNavController().navigate(R.id.menuConjugaisonsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}