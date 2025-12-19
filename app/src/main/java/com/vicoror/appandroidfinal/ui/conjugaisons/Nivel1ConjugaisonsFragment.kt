package com.vicoror.appandroidfinal.ui.conjugaisons

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.data.model.Frase
import com.vicoror.appandroidfinal.databinding.FragmentNivel1ConjugaisonsBinding
import com.vicoror.appandroidfinal.ui.conjugaisons.ConjugaisonsFragment.Companion.TAG
import com.vicoror.appandroidfinal.utils.MacaronManager
import com.vicoror.appandroidfinal.viewModel.ConjugaisonsViewModel
import kotlinx.coroutines.launch

class Nivel1ConjugaisonsFragment : Fragment() {

    private lateinit var binding: FragmentNivel1ConjugaisonsBinding
    private val viewModel: ConjugaisonsViewModel by viewModels()

    private lateinit var selectedTense: String
    private var blockIndex: Int = 0

    private var frases: List<Frase> = emptyList()
    private var currentQuestionIndex = 0
    private var bonnesReponses = 0
    private var shuffledOpciones: List<String> = emptyList()
    private var correctAnswerIndex = 0
    private var answerSubmitted = false

    private var timer: CountDownTimer? = null
    private var timeRemaining = 100_000L // 100 segundos
    private var isTimerPaused = false

    private var currentLives = 5 // vidas temporales
    private lateinit var jsonFileName: String

    private lateinit var macaronManager: MacaronManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { bundle ->
            selectedTense = bundle.getString("selectedTense") ?: ""
            jsonFileName = bundle.getString("jsonFileName") ?: ""
            blockIndex = bundle.getInt("blockIndex", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNivel1ConjugaisonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Nivel1: jsonFileName = $jsonFileName, selectedTense = $selectedTense, blockIndex = $blockIndex")
        macaronManager = MacaronManager.getInstance(requireContext())
        // Obtener vidas actuales
        currentLives = macaronManager.currentMacaronCount
        binding.shimmerContainer.startShimmer()
        binding.shimmerContainer.visibility = View.VISIBLE
        binding.scrollContent.visibility = View.GONE
        setupCallbacks()
        setupObservers()

        // Mostrar vidas iniciales
        updateLivesView()
        updateLivesLabel()  // Si tienes TextView aparte

        // Ahora sí funcionará:
        viewModel.loadFrasesDelBloque(jsonFileName, blockIndex)
    }

    // ---------------- OBSERVERS ----------------

    private fun setupObservers() {
        viewModel.frases.observe(viewLifecycleOwner) { frasesBloque ->
            // SIEMPRE ocultar el shimmer cuando lleguen datos
            binding.shimmerContainer.stopShimmer()
            binding.shimmerContainer.visibility = View.GONE
            binding.scrollContent.visibility = View.VISIBLE

            if (frasesBloque.isNotEmpty()) {
                // Mostrar el contenido
                binding.scrollContent.visibility = View.VISIBLE

                frases = frasesBloque
                currentQuestionIndex = 0
                bonnesReponses = 0
                showQuestion(0)
                startTimer()
                updateLivesView()
            } else {
                // Si no hay datos, también mostrar contenido pero vacío
                binding.scrollContent.visibility = View.VISIBLE
                showError("No hay frases disponibles")
            }
        }

        // También observa errores del ViewModel si los tienes
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                binding.shimmerContainer.stopShimmer()
                binding.shimmerContainer.visibility = View.GONE
                binding.scrollContent.visibility = View.VISIBLE
                showError(it)
            }
        }
    }
    private fun showError(message: String) {
        // Detener y ocultar shimmer
        binding.shimmerContainer.stopShimmer()
        binding.shimmerContainer.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        // Mostrar mensaje de error
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        // Opcional: puedes agregar un botón para regresar
    }

    // ---------------- CALLBACKS ----------------

    private fun setupCallbacks() {
        binding.btnExit.setOnClickListener { exitButtonTapped() }
        binding.btnPro.setOnClickListener { proButtonTapped() }

        binding.btnOpcion1.setOnClickListener { opcionSelected(0) }
        binding.btnOpcion2.setOnClickListener { opcionSelected(1) }
        binding.btnOpcion3.setOnClickListener { opcionSelected(2) }

        binding.btnSuivant.setOnClickListener { nextQuestion() }
    }

    // ---------------- TIMER ----------------

    private fun startTimer() {
        timer?.cancel()

        timer = object : CountDownTimer(timeRemaining, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                if (isTimerPaused) return
                timeRemaining = millisUntilFinished
                binding.timerLabel.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                timeExpired()
            }
        }.start()
    }

    private fun timeExpired() {
        timer?.cancel()
        timer = null

        Toast.makeText(requireContext(), "Tiempo agotado", Toast.LENGTH_SHORT).show()

        timeRemaining = 100_000L
        currentQuestionIndex = 0
        bonnesReponses = 0
        binding.bonnesLabel.text = "Bonnes Réponses: 0"

        showQuestion(0)
        startTimer()
    }

    // ---------------- QUESTIONS ----------------

    private fun showQuestion(index: Int) {
        if (index >= frases.size) {
            exerciseCompleted()
            return
        }

        val frase = frases[index]

        val opciones = listOf(
            frase.opc_1,
            frase.opc_2,
            frase.opc_correcta
        )
            .filter { it.isNotBlank() }
            .distinct()
            .shuffled()

        if (opciones.size < 3) {
            Log.e("Nivel1", "Opciones inválidas en frase ${frase.fraseFr}")
            return
        }

        shuffledOpciones = opciones
        correctAnswerIndex = opciones.indexOf(frase.opc_correcta)
        answerSubmitted = false

        // 🔥🔥🔥 AÑADE ESTA LÍNEA AQUÍ 🔥🔥🔥
        vidaYaDescontadaEnEstaPregunta = false  // RESET PARA NUEVA PREGUNTA

        binding.verboLabel.text = frase.verbo
        binding.fraseFrLabel.text = prepareFraseWithSpace(frase.fraseFr, frase.opc_correcta)
        binding.fraseEsLabel.text = frase.fraseEs

        binding.btnOpcion1.text = opciones[0]
        binding.btnOpcion2.text = opciones[1]
        binding.btnOpcion3.text = opciones[2]

        binding.btnSuivant.text = "Suivant ${index + 1}/${frases.size}"

        setOpcionesEnabled(true)
        resetOpcionesColors()
    }

    private fun prepareFraseWithSpace(frase: String, correct: String): String {
        var fraseModificada = frase

        // Lista de formas posibles con apóstrofes
        val possibleForms = listOf(
            correct,                    // "homme"
            "l'$correct",              // "l'homme"
            "j'$correct",              // "j'aime"
            "t'$correct",              // "t'aime"
            "s'$correct",              // "s'appelle"
            "c'$correct",              // "c'est"
            "d'$correct",              // "d'accord"
            "qu'$correct",             // "qu'il"
            "n'$correct",              // "n'est"
            "m'$correct"               // "m'appelle"
        )

        // Reemplazar cada forma posible (solo palabras completas)
        for (form in possibleForms) {
            // Usar regex para coincidir solo con palabras completas
            // \b es el límite de palabra en regex
            val regex = "\\b${Regex.escape(form)}\\b".toRegex(RegexOption.IGNORE_CASE)
            fraseModificada = regex.replace(fraseModificada, "_____")
        }

        return fraseModificada
    }

    // ---------------- ANSWERS ----------------

    private fun opcionSelected(index: Int) {
        if (answerSubmitted) return

        // 🔥 AÑADE ESTA VERIFICACIÓN AL INICIO
        if (!macaronManager.canPlay()) {
            macaronManager.showRecoveryAlertDialog(requireContext())
            return  // 🔥 SALIR, no permitir seleccionar opción
        }

        answerSubmitted = true
        setOpcionesEnabled(false)

        if (shuffledOpciones[index] == frases[currentQuestionIndex].opc_correcta) {
            bonnesReponses++
            binding.bonnesLabel.text = "Bonnes Réponses: $bonnesReponses"
            showCorrectAnswer(index)
        } else {
            showWrongAnswer(index, correctAnswerIndex)
        }
    }

    private fun nextQuestion() {
        if (!answerSubmitted) {
            Toast.makeText(requireContext(), "Seleccione una respuesta", Toast.LENGTH_SHORT).show()
            return
        }
        currentQuestionIndex++
        showQuestion(currentQuestionIndex)
    }

    private fun exerciseCompleted() {
        timer?.cancel()
        // GUARDAR PROGRESO ANTES de mostrar el diálogo
        marcarNivel1Completado()
        desbloquearNivel2DelMismoBloque()
        // Crear y mostrar el diálogo
        showCompletionDialog()
    }

    private fun showCompletionDialog() {
        // Inflar el layout del diálogo
        val dialogView = layoutInflater.inflate(R.layout.dialog_alert, null)

        // Configurar el mensaje con el resultado
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)
        messageTextView.text = getString(
            R.string.resultado_ejercicio,
            bonnesReponses,
            frases.size
        )

        // Crear el AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false) // Evitar que se cierre tocando fuera
            .create()

        // Configurar botón "Repetir"
        dialogView.findViewById<Button>(R.id.btnRepeat).setOnClickListener {
            dialog.dismiss()
            restartExercise()
        }

        // Configurar botón "Ir al ejercicio 2"
        dialogView.findViewById<Button>(R.id.btnNext).setOnClickListener {
            dialog.dismiss()
            navigateToNivel2()
        }

        // Mostrar el diálogo
        dialog.show()

        // Opcional: Personalizar la ventana del diálogo
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun restartExercise() {
        // Reiniciar el ejercicio actual
        currentQuestionIndex = 0
        bonnesReponses = 0
        binding.bonnesLabel.text = "Bonnes Réponses: 0"
        timeRemaining = 100_000L
        showQuestion(0)
        startTimer()
    }

    private fun navigateToNivel2() {

        val action =
            Nivel1ConjugaisonsFragmentDirections
                .actionNivel1ConjugaisonsFragmentToNivel2ConjugaisonsFragment(
                    selectedTense = selectedTense,
                    jsonFileName = jsonFileName,
                    blockIndex = blockIndex
                )

        findNavController().navigate(action)

    }


    // ---------------- UI ----------------

    private fun setOpcionesEnabled(enabled: Boolean) {
        binding.btnOpcion1.isEnabled = enabled
        binding.btnOpcion2.isEnabled = enabled
        binding.btnOpcion3.isEnabled = enabled
    }

    private fun resetOpcionesColors() {
        val color = resources.getColor(
            com.vicoror.appandroidfinal.R.color.principalextralight, null
        )
        binding.btnOpcion1.setBackgroundColor(color)
        binding.btnOpcion2.setBackgroundColor(color)
        binding.btnOpcion3.setBackgroundColor(color)
    }

    private fun showCorrectAnswer(index: Int) {
        val color = resources.getColor(
            com.vicoror.appandroidfinal.R.color.warningblue, null
        )
        listOf(binding.btnOpcion1, binding.btnOpcion2, binding.btnOpcion3)[index]
            .setBackgroundColor(color)
    }

    private fun showWrongAnswer(selected: Int, correct: Int) {
        // Mostrar colores primero
        listOf(binding.btnOpcion1, binding.btnOpcion2, binding.btnOpcion3)[selected]
            .setBackgroundColor(resources.getColor(R.color.warningred, null))
        listOf(binding.btnOpcion1, binding.btnOpcion2, binding.btnOpcion3)[correct]
            .setBackgroundColor(resources.getColor(R.color.warningblue, null))

        // Descontar vida SOLO UNA VEZ por pregunta
        if (!vidaYaDescontadaEnEstaPregunta) {
            vidaYaDescontadaEnEstaPregunta = true

            lifecycleScope.launch {
                // 🔥 VERIFICACIÓN CRÍTICA: ¿El fragment sigue activo?
                if (!isAdded || isDetached) return@launch

                // Verificar si hay macarones disponibles
                if (macaronManager.canPlay()) {
                    val consumido = macaronManager.consumeMacaron()
                    if (consumido) {
                        // 🔥 VERIFICAR DE NUEVO ANTES DE UI
                        if (!isAdded || isDetached) return@launch

                        // Actualizar UI
                        updateLivesView()
                        updateLivesLabel()

                        // 🔥 VERIFICAR SI SE ACABARON LOS MACARONES
                        if (macaronManager.currentMacaronCount <= 0) {
                            // Bloquear botones de opciones
                            setOpcionesEnabled(false)

                            // 🔥 VERIFICAR PARA Toast
                            if (isAdded && !isDetached) {
                                Toast.makeText(
                                    requireContext(),
                                    "¡Se te acabaron los macarones!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else {
                    // Si no hay macarones, mostrar mensaje Y bloquear
                    if (isAdded && !isDetached) {
                        showNoLivesMessage()
                        setOpcionesEnabled(false)
                    }
                }
            }
        }
    }

    // Añade esta propiedad a tu clase
    private var vidaYaDescontadaEnEstaPregunta = false

    // Y resetea en cada nueva pregunta
    private fun setupNextQuestion() {
        vidaYaDescontadaEnEstaPregunta = false
        // Resto de tu lógica...
    }


    private fun exitButtonTapped() {
        timer?.cancel()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun proButtonTapped() {
        isTimerPaused = !isTimerPaused
        binding.btnPro.text = if (isTimerPaused) "▶️" else "∞"
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

        // 🔥 Usa siempre macaronManager para consistencia
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

                // 🔥 CORRECTO: derecha a izquierda
                alpha = if (index < currentCount) 1f else 0.25f
            }
            binding.livesStack.addView(image)
        }
    }
    private fun updateLivesLabel() {
    }

    private fun showNoLivesMessage() {
        val message = macaronManager.getRecoveryMessage()
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    private fun marcarNivel1Completado() {
        val prefs = requireContext().getSharedPreferences("conjugaisons_progress", 0)
        val editor = prefs.edit()

        // Marcar que el Nivel1 de este bloque está completado
        editor.putInt("nivel1_${selectedTense}_$blockIndex", 5) // 5 preguntas completadas

        // Actualizar último bloque completado
        val lastBlock = prefs.getInt("last_block_$selectedTense", -1)
        if (blockIndex > lastBlock) {
        }

        editor.apply()
        Log.d("Nivel1", "✅ Nivel1 bloque $blockIndex marcado como completado para $selectedTense")
    }

     fun desbloquearNivel2DelMismoBloque() {
        val prefs = requireContext().getSharedPreferences("conjugaisons_progress", 0)
        val editor = prefs.edit()

        // Desbloquear el Nivel2 del MISMO bloque (no el siguiente)
        // Esto es diferente a lo que haces en Nivel2
        editor.putBoolean("unlocked_nivel2_${selectedTense}_$blockIndex", true)

        editor.apply()
        Log.d("Nivel1", "🔓 Nivel2 del bloque $blockIndex desbloqueado para $selectedTense")
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
}
