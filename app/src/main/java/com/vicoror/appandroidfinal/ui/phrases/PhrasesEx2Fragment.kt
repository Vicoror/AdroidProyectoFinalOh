package com.vicoror.appandroidfinal.ui.phrases

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.DialogAlertBinding
import com.vicoror.appandroidfinal.databinding.FragmentPhrasesEx2Binding
import com.vicoror.appandroidfinal.utils.NetworkChecker
import com.vicoror.appandroidfinal.viewModel.PhrasesViewModel
import kotlin.math.min
import kotlin.random.Random

class PhrasesEx2Fragment : Fragment() {

    private var _binding: FragmentPhrasesEx2Binding? = null
    private val binding get() = _binding!!

    private val viewModel: PhrasesViewModel by viewModels(ownerProducer = { requireActivity() })
    private val args: PhrasesEx2FragmentArgs by navArgs()

    private var phrases: List<com.vicoror.appandroidfinal.data.model.Phrase> = emptyList()
    private var currentIndex = 0
    private var selectedBlock = 0
    private lateinit var mode: String
    private var dialogShown = false

    // Para controlar la frase actual y opciones
    private var currentCorrectAnswer: String = ""
    private var isShowingFrench = true

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    //private lateinit var sharedPreferences: android.content.SharedPreferences
    private val optionButtons = mutableListOf<com.google.android.material.button.MaterialButton>()

    companion object {
        private const val PREFS_NAME = "PhrasesProgress"
        private const val KEY_EX2_BLOCK_PROGRESS_PREFIX = "ex2_block_progress_"
        private const val KEY_EX2_BLOCK_COMPLETED_PREFIX = "ex2_block_completed_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recibir datos del fragment anterior
        selectedBlock = args.selectedBlock
        mode = args.mode

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhrasesEx2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mode = args.mode
        Log.d("PHRASES_EX2", "Modo actual: $mode")

        setupTopBar()
        setupButtons()
        setupObservers()

        // Verificar conexión
        if (!NetworkChecker.isOnline(requireContext())) {
            binding.frasePrincipal.text = getString(R.string.sin_conex)
            binding.msgCompletado.text = getString(R.string.conectate_con)
            binding.msgCompletado.visibility = View.VISIBLE
        } else {
            // Cargar datos según el modo
            loadData()
        }

        // Ocultar elementos iniciales
        binding.btnRepetir.visibility = View.GONE
        binding.txtBloqueCompletado.visibility = View.GONE
    }

    private fun loadSavedProgress(phrases: List<com.vicoror.appandroidfinal.data.model.Phrase>) {
        val savedProgress = getBlockProgress(selectedBlock)

        // Nueva corrección:
        // Si el progreso guardado es igual o mayor que el total de frases,
        // reiniciamos a 0 para evitar que dispare "bloque completado" incorrectamente.
        if (savedProgress >= phrases.size) {
            currentIndex = 0
            println(" Progreso inválido ($savedProgress), reiniciando a 0")
            return
        }

        // Solo cargar si hay frases y el progreso es válido
        if (phrases.isNotEmpty() && savedProgress > 0 && savedProgress < phrases.size) {
            currentIndex = savedProgress
            println(" Progreso cargado: frase $currentIndex/${phrases.size}")
        }
    }


    private fun saveBlockProgress(blockIndex: Int, progress: Int) {
        sharedPreferences.edit()
            .putInt(prefKey("$KEY_EX2_BLOCK_PROGRESS_PREFIX$blockIndex"), progress)
            .apply()
    }

    private fun getBlockProgress(blockIndex: Int): Int {
        return sharedPreferences.getInt(prefKey("$KEY_EX2_BLOCK_PROGRESS_PREFIX$blockIndex"), 0)
    }

    private fun markBlockAsCompleted(blockIndex: Int) {
        sharedPreferences.edit()
            .putBoolean(prefKey("$KEY_EX2_BLOCK_COMPLETED_PREFIX$blockIndex"), true)
            .apply()
    }

    private fun isBlockCompleted(blockIndex: Int = selectedBlock): Boolean {
        return sharedPreferences.getBoolean(prefKey("$KEY_EX2_BLOCK_COMPLETED_PREFIX$blockIndex"), false)
    }

    private fun resetBlockProgress(blockIndex: Int) {
        sharedPreferences.edit().apply {
            putInt(prefKey("$KEY_EX2_BLOCK_PROGRESS_PREFIX$blockIndex"), 0)
            putBoolean(prefKey("$KEY_EX2_BLOCK_COMPLETED_PREFIX$blockIndex"), false)
            apply()
        }
    }

    private fun prefKey(key: String): String {
        return "${mode}_$key"
    }

    override fun onPause() {
        super.onPause()
        // Guardar progreso cuando el fragment pierde foco
        saveBlockProgress(selectedBlock, currentIndex)
    }

    private fun setupTopBar() {
            binding.topBarExitButton.setOnClickListener {
                saveBlockProgress(selectedBlock, currentIndex)

                // Navegar de regreso a PhrasesFragment con el modo actual
                val action = PhrasesEx2FragmentDirections
                    .actionPhrasesEx2FragmentToPhrasesFragment(mode = mode)

                findNavController().navigate(action)
            }

        val macarons = listOf(
            R.drawable.macaron_c5a4da,
            R.drawable.macaron_fffa9c,
            R.drawable.macaron_8cd5c9,
            R.drawable.macaron_ff8a80,
            R.drawable.macaron_d4e9f8
        )

        binding.topBarLives.removeAllViews()

        for (m in macarons) {
            val img = ImageView(requireContext()).apply {
                setImageResource(m)
                val sizeInDp = 35
                val scale = resources.displayMetrics.density
                val sizeInPx = (sizeInDp * scale + 0.5f).toInt()
                layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    marginEnd = 12
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
            binding.topBarLives.addView(img)
        }

        binding.topBarLives.gravity = android.view.Gravity.CENTER
    }

    private fun setupButtons() {
        optionButtons.clear()
        optionButtons.add(binding.fraseOp1)
        optionButtons.add(binding.fraseOp2)
        optionButtons.add(binding.fraseOp3)

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                optionTapped(index)
            }
        }

        binding.btnRepetir.setOnClickListener {
            reiniciarEjercicio2()
        }
    }

    private fun setupObservers() {
        viewModel.phrases.observe(viewLifecycleOwner) { bloques ->
            if (bloques != null && bloques.isNotEmpty()) {
                if (selectedBlock < bloques.size) {
                    phrases = bloques[selectedBlock]
                    val tipo = if (mode == "verbs") "verbos" else "frases"
                    println("Cargando $tipo para Ex2 del bloque $selectedBlock: ${phrases.size} elementos")

                    loadSavedProgress(phrases)

                    // Mostrar según estado
                    if (isBlockCompleted()) {
                        // Si ya estaba completado, mostrar estado completado
                        binding.msgCompletado.visibility = View.VISIBLE
                        binding.msgCompletado.text = getString(R.string.bloque_ya_completado_anteriormente)
                        binding.progressEjercicio.progress = 100
                        binding.btnRepetir.visibility = View.VISIBLE
                        // Ocultar botones de opciones
                        optionButtons.forEach { it.visibility = View.GONE }
                    } else if (phrases.isNotEmpty()) {
                        // Si no está completado, mostrar frase actual
                        mostrarFraseActual()
                        updateProgress()
                    } else {
                        binding.frasePrincipal.text = getString(R.string.bloquevacio)
                        binding.msgCompletado.text = getString(R.string.nohayfrases)
                        binding.msgCompletado.visibility = View.VISIBLE
                    }
                } else {
                    phrases = bloques.last()
                    println("⚠ Índice $selectedBlock no existe, usando último bloque con ${phrases.size} elementos")
                    loadSavedProgress(phrases)
                    mostrarFraseActual()
                    updateProgress()
                }
            } else {
                binding.frasePrincipal.text = getString(R.string.no_hay_frases_disponibles)
                binding.msgCompletado.text = getString(R.string.vuelve_a_la_pantalla_anterior)
                binding.msgCompletado.visibility = View.VISIBLE
            }
        }
    }

    private fun loadData() {
        // Cargar datos según el modo
        if (viewModel.phrases.value == null || viewModel.phrases.value?.isEmpty() == true) {
            viewModel.loadJson(mode)  // Usar "verbs" o "phrases"
        } else {
            // Ya están cargadas, mostrar primera frase
            val bloques = viewModel.phrases.value
            if (bloques != null && bloques.isNotEmpty()) {
                val blockIndex = if (selectedBlock < bloques.size) selectedBlock else bloques.lastIndex
                phrases = bloques[blockIndex]
                if (phrases.isNotEmpty()) {
                    loadSavedProgress(phrases)
                    mostrarFraseActual()
                    updateProgress()
                }
            }
        }
    }

    private fun mostrarFraseActual() {

        if (isBlockCompleted()) {
            mostrarDialogoFinal()
        }

        // --- Corrección: asegurar que currentIndex siempre esté en rango ---
        if (currentIndex < 0) currentIndex = 0
        if (phrases.isNotEmpty() && currentIndex >= phrases.size) {
            // Si está fuera de rango, significa que ya terminó correctamente
            mostrarDialogoFinal()
            return
        }

        // --- Si no hay frases ---
        if (phrases.isEmpty()) {
            binding.frasePrincipal.text = getString(R.string.no_hay_frases_disponibles)
            optionButtons.forEach { btn ->
                btn.text = "-"
                btn.isEnabled = false
            }
            return
        }

        // Obtener frase actual
        val frase = phrases[currentIndex]

        // Aleatorizar idioma mostrado
        isShowingFrench = Random.nextBoolean()

        val preguntaText = if (isShowingFrench) frase.fraseFr else frase.fraseEs
        val respuestaCorrecta = if (isShowingFrench) frase.fraseEs else frase.fraseFr

        currentCorrectAnswer = respuestaCorrecta
        binding.frasePrincipal.text = preguntaText

        // Construir lista de opciones
        val pool = mutableListOf<String>()

        phrases.forEach { item ->
            pool.add(if (isShowingFrench) item.fraseEs else item.fraseFr)
        }

        // Remover correcta
        pool.remove(currentCorrectAnswer)

        // Mezclar y tomar dos incorrectas
        pool.shuffle()
        val incorrectas = pool.take(2)

        // Lista final de opciones
        val opciones = mutableListOf<String>().apply {
            addAll(incorrectas)
            add(currentCorrectAnswer)
            shuffle()
        }

        // Asignar opciones
        optionButtons.forEachIndexed { index, button ->
            if (index < opciones.size) {
                val title = opciones[index]
                button.text = title
                button.tag = if (title == currentCorrectAnswer) "correct" else "incorrect"
                button.isEnabled = true
                resetButtonStyle(button)
            } else {
                button.text = "-"
                button.tag = "incorrect"
                button.isEnabled = false
            }
        }

        // Actualizar progreso
        updateProgress()
    }

    private fun resetButtonStyle(button: com.google.android.material.button.MaterialButton) {
        button.setTextColor(Color.WHITE)
        button.strokeColor = null
        button.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            R.color.principallight
        )
    }

    private fun highlightCorrectButton() {
        optionButtons.forEach { button ->
            if (button.tag == "correct") {
                // Cambiar estilo del botón correcto a verde
                button.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.warningblue
                )
            }
        }
    }

    private fun optionTapped(selectedIndex: Int) {
        val selectedButton = optionButtons[selectedIndex]
        val isCorrect = selectedButton.tag == "correct"

        // Deshabilitar todos los botones después de seleccionar
        optionButtons.forEach { it.isEnabled = false }

        // Resaltar el botón correcto
        highlightCorrectButton()

        if (isCorrect) {
            mostrarModalCorrecta(correctText = selectedButton.text.toString())
        } else {
            mostrarModalIncorrecta(correctText = currentCorrectAnswer)
        }
    }

    private fun mostrarModalCorrecta(correctText: String) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_correct, null)

        // Configurar el texto de respuesta correcta
        val textAnswer = view.findViewById<android.widget.TextView>(R.id.textAnswerCorrect)
        textAnswer?.text = correctText

        // Configurar botón continuar
        val btnContinue = view.findViewById<android.widget.Button>(R.id.btnContinueCorrect)
        btnContinue?.setOnClickListener {
            dialog.dismiss()
            avanzarSiguienteFrase()
        }

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.isDraggable = false
        dialog.behavior.isFitToContents = true

        dialog.setContentView(view)
        dialog.show()

        dialog.setOnDismissListener {

        }
    }

    private fun mostrarModalIncorrecta(correctText: String) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_incorrect, null)

        // Configurar el texto de respuesta correcta
        val textAnswer = view.findViewById<android.widget.TextView>(R.id.textAnswerIncorrect)
        textAnswer?.text = correctText

        // Configurar botón continuar
        val btnContinue = view.findViewById<android.widget.Button>(R.id.btnContinueIncorrect)
        btnContinue?.setOnClickListener {
            dialog.dismiss()
            avanzarSiguienteFrase()
        }

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.isDraggable = false
        dialog.behavior.isFitToContents = true

        dialog.setContentView(view)
        dialog.show()

        dialog.setOnDismissListener {

        }
    }

    private fun avanzarSiguienteFrase() {

        if (currentIndex >= phrases.size) return
        currentIndex++

        // Guardar progreso
        if (currentIndex < phrases.size) {
            saveBlockProgress(selectedBlock, currentIndex)
        }

        if (hasUserFinishedBlock()) {
            markBlockAsCompleted(selectedBlock)
            saveBlockProgress(selectedBlock, 0) // Reiniciar progreso
            mostrarDialogoFinal()
            return
        }

        mostrarFraseActual()

    }

    private fun updateProgress() {
        if (phrases.isEmpty()) {
            binding.progressEjercicio.progress = 0
            return
        }
        val progress = ((currentIndex + 1).toFloat() / phrases.size.toFloat() * 100).toInt()

        binding.progressEjercicio.progress = min(progress, 100)
    }

    private fun mostrarDialogoFinal() {
        if (dialogShown) return
        dialogShown = true

        val dialogBinding = DialogAlertBinding.inflate(layoutInflater)

        // PERSONALIZAR LOS TEXTOS
        dialogBinding.dialogTitle.text = getString(R.string.ejercicio_2_completado)
        dialogBinding.dialogMessage.text =
            getString(R.string.deseas_repetir_el_ejercicio_o_ir_al_ejercicio_3)
        dialogBinding.btnNext.text = getString(R.string.ir_al_ejercicio_3)
        dialogBinding.btnRepeat.text = getString(R.string.repetir_ejercicio_2)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Opcional: Evitar que se cierre al tocar fuera
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()

        // --- BOTÓN REPETIR (SOLO EJERCICIO 2) ---
        dialogBinding.btnRepeat.setOnClickListener {
            dialog.dismiss()
            dialogShown = false

            // Reiniciar solo el ejercicio 2
            reiniciarEjercicio2()
        }

        // --- BOTÓN IR A EJERCICIO 3 ---
        dialogBinding.btnNext.setOnClickListener {
            dialog.dismiss()

            // Navegar al ejercicio 3
            val bundle = Bundle().apply {
                putInt("selectedBlock", selectedBlock)
                putString("mode", mode)
            }
            findNavController().navigate(
                R.id.action_phrasesEx2Fragment_to_phrasesEx3Fragment,
                bundle
            )
        }
    }

    private fun reiniciarEjercicio2() {
        currentIndex = 0

        // Limpiar estado de completado
        resetBlockProgress(selectedBlock)

        // Resetear el estado de completado
        dialogShown = false

        // Ocultar elementos de "completado"
        binding.btnRepetir.visibility = View.GONE
        binding.msgCompletado.visibility = View.GONE
        binding.txtBloqueCompletado.visibility = View.GONE

        // Restaurar botones de opciones
        optionButtons.forEach { button ->
            button.visibility = View.VISIBLE
            button.isEnabled = true
            resetButtonStyle(button)
        }

        // Resetear progreso visual
        updateProgress()

        // Mostrar primera frase
        mostrarFraseActual()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Guardar progreso antes de destruir
        saveBlockProgress(selectedBlock, currentIndex)
        _binding = null
    }

    private fun hasUserFinishedBlock(): Boolean {
        return currentIndex >= phrases.size
    }

}