package com.vicoror.appandroidfinal.ui.phrases

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.text.Normalizer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentPhrasesEx3Binding
import com.vicoror.appandroidfinal.utils.MacaronRainFragment
import com.vicoror.appandroidfinal.utils.NetworkChecker
import com.vicoror.appandroidfinal.viewModel.PhrasesViewModel
import java.util.*
import kotlin.math.min
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import com.vicoror.appandroidfinal.utils.MacaronManager
import kotlinx.coroutines.launch


class PhrasesEx3Fragment : Fragment() {

    private var _binding: FragmentPhrasesEx3Binding? = null
    private val binding get() = _binding!!

    private val viewModel: PhrasesViewModel by viewModels(ownerProducer = { requireActivity() })
    private val args: PhrasesEx3FragmentArgs by navArgs()

    private var phrases: List<com.vicoror.appandroidfinal.data.model.Phrase> = emptyList()
    private var currentIndex = 0
    private var selectedBlock = 0
    private lateinit var mode: String
    private var dialogShown = false

    // Control de intentos y estado
    private var intentosFallidos = 0
    private var pistaMostrada = false
    private var respuestaMostrada = false

    // SharedPreferences
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var macaronManager: MacaronManager

    companion object {
        private const val PREFS_NAME = "PhrasesProgress"
        private const val KEY_EX3_BLOCK_PROGRESS_PREFIX = "ex3_block_progress_"
        private const val KEY_EX3_BLOCK_COMPLETED_PREFIX = "ex3_block_completed_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedBlock = args.selectedBlock
        mode = args.mode
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhrasesEx3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        macaronManager = MacaronManager.getInstance(requireContext())

        setupTopBar()
        setupButtons()
        setupObservers()

        if (!NetworkChecker.isOnline(requireContext())) {
            binding.frasePrincipal.text = getString(R.string.sin_conex)
            binding.msgCompletado.text = getString(R.string.conectate_con)
            binding.msgCompletado.visibility = View.VISIBLE
        } else {
            loadData()
        }

        binding.btnRepetir.visibility = View.GONE
        binding.txtBloqueCompletado.visibility = View.GONE
        binding.msgCompletado.visibility = View.GONE

        binding.scrollView.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        binding.root.setOnClickListener {
            hideKeyboard()
        }

    }

    private fun setupTopBar() {
        binding.topBarExitButton.setOnClickListener {
            saveBlockProgress(selectedBlock, currentIndex)
            val action = PhrasesEx3FragmentDirections
                .actionPhrasesEx3FragmentToPhrasesFragment(mode = mode)
            findNavController().navigate(action)
        }

        updateTopBarMacarons()  // ← LLAMA A FUNCIÓN SEPARADA
    }

    private fun updateTopBarMacarons() {
        val macarons = listOf(
            R.drawable.macaron_c5a4da,
            R.drawable.macaron_fffa9c,
            R.drawable.macaron_8cd5c9,
            R.drawable.macaron_ff8a80,
            R.drawable.macaron_d4e9f8
        )

        binding.topBarLives.removeAllViews()

        val currentCount = macaronManager.currentMacaronCount  // 🔥 USA macaronManager

        macarons.forEachIndexed { index, resId ->
            val img = ImageView(requireContext()).apply {
                setImageResource(resId)
                val sizeInDp = 35
                val scale = resources.displayMetrics.density
                val sizeInPx = (sizeInDp * scale + 0.5f).toInt()
                layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    marginEnd = 12
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true

                // 🔥 DIFUMINADO: izquierda a derecha
                alpha = if (index < currentCount) 1f else 0.25f
            }
            binding.topBarLives.addView(img)
        }

        binding.topBarLives.gravity = android.view.Gravity.CENTER
    }
    private fun setupButtons() {
        binding.btnPista.setOnClickListener { pistaTapped() }
        binding.btnRespuesta.setOnClickListener { verRespuestaTapped() }
        binding.btnValider.setOnClickListener { validarTapped() }
        binding.btnRepetir.setOnClickListener { reiniciarEjercicio3() }
    }

    private fun setupObservers() {
        viewModel.phrases.observe(viewLifecycleOwner) { bloques ->
            if (bloques != null && bloques.isNotEmpty()) {
                if (selectedBlock < bloques.size) {
                    phrases = bloques[selectedBlock]
                    println("Cargando frases para Ex3 del bloque $selectedBlock: ${phrases.size} elementos")

                    loadSavedProgress()

                    if (isBlockCompleted()) {
                        binding.msgCompletado.visibility = View.VISIBLE
                        binding.msgCompletado.text = getString(R.string.bloque_ya_completado_anteriormente)
                        binding.progressEjercicio.progress = 100
                        binding.btnRepetir.visibility = View.VISIBLE
                    } else if (phrases.isNotEmpty()) {
                        mostrarFraseActual()
                        updateProgress()
                    } else {
                        binding.frasePrincipal.text = getString(R.string.bloquevacio)
                        binding.msgCompletado.text = getString(R.string.nohayfrases)
                        binding.msgCompletado.visibility = View.VISIBLE
                    }
                } else {
                    phrases = bloques.last()
                    println("⚠ Índice $selectedBlock no existe, usando último bloque")
                    loadSavedProgress()
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
        if (viewModel.phrases.value == null || viewModel.phrases.value?.isEmpty() == true) {
            viewModel.loadJson(mode)
        } else {
            val bloques = viewModel.phrases.value
            if (bloques != null && bloques.isNotEmpty()) {
                val blockIndex = if (selectedBlock < bloques.size) selectedBlock else bloques.lastIndex
                phrases = bloques[blockIndex]
                if (phrases.isNotEmpty()) {
                    loadSavedProgress()
                    mostrarFraseActual()
                    updateProgress()
                }
            }
        }
    }

    private fun mostrarFraseActual() {
        if (currentIndex >= phrases.size) {
            mostrarDialogoFinal()
            return
        }

        val frase = phrases[currentIndex]

        // Mostrar frase en español
        binding.frasePrincipal.text = frase.fraseEs

        // Limpiar input y poner placeholder
        binding.inputEdit.text?.clear()
        binding.inputEdit.hint = getString(R.string.crivez_la_traduction_en_fran_ais)

        // Reset estado
        intentosFallidos = 0
        pistaMostrada = false
        respuestaMostrada = false

        // Mostrar/ocultar botones
        binding.btnRespuesta.visibility = View.GONE
        binding.btnPista.isEnabled = true
        binding.btnRespuesta.isEnabled = true

        updateProgress()
    }

    private fun updateProgress() {
        if (phrases.isEmpty()) {
            binding.progressEjercicio.progress = 0
            return
        }
        val progress = ((currentIndex + 1).toFloat() / phrases.size.toFloat() * 100).toInt()
        binding.progressEjercicio.progress = min(progress, 100)
    }

    // MARK: - Normalización de texto (Ignorando Signos de Puntuación)
    private fun normalizarTexto(texto: String): String {
        // 1) Normalizar Unicode (NFKC) para convertir variantes (NBSP, comillas tipográficas, etc.)
        var s = Normalizer.normalize(texto, Normalizer.Form.NFKC)

        // 2) Pasar a minúsculas y trim
        s = s.trim().lowercase(Locale.getDefault())

        // 3) Normalizar espacios no rompibles y otros whitespace raros a espacio normal
        s = s.replace('\u00A0', ' ') // NBSP -> espacio normal
        s = s.replace(Regex("\\s+"), " ")

        // 4) Eliminar apóstrofes (ASCII ' y tipográficos ’ ) — los unimos: "C'est" -> "cest"
        s = s.replace(Regex("[\\u0027\\u2019\\u2018]"), "")

        // 5) Reemplazar el resto de puntuación por un espacio para no unir palabras (.,!?;:()... => " ")
        s = s.replace(Regex("\\p{Punct}"), " ")

        // 6) Eliminar cualquier carácter que no sea letra (incluye acentos) ni espacio
        s = s.replace(Regex("[^\\p{L}\\s]"), "")

        // 7) Colapsar múltiples espacios y trim final
        s = s.split("\\s+".toRegex()).joinToString(" ").trim()

        return s
    }


    private fun validarRespuesta(respuestaUsuario: String, fraseCorrecta: String): Boolean {
        val respuestaNormalizada = normalizarTexto(respuestaUsuario)
        val fraseNormalizada = normalizarTexto(fraseCorrecta)

        println("Respuesta usuario normalizada: '$respuestaNormalizada'")
        println("Frase correcta normalizada: '$fraseNormalizada'")

        return respuestaNormalizada == fraseNormalizada
    }

    // MARK: - Acciones de Botones
    private fun pistaTapped() {
        if (currentIndex >= phrases.size) return

        val frase = phrases[currentIndex]
        val primeraLetra = frase.fraseFr.take(1)

        // Mostrar primera letra como pista
        AlertDialog.Builder(requireContext())
            .setTitle("Pista")
            .setMessage("La frase comienza con: '$primeraLetra'")
            .setPositiveButton("Merci!", null)
            .show()

        pistaMostrada = true
        binding.btnPista.isEnabled = false

        // Animación del botón
        binding.btnPista.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(200)
            .withEndAction {
                binding.btnPista.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun verRespuestaTapped() {
        if (currentIndex >= phrases.size) return

        val frase = phrases[currentIndex]
        binding.inputEdit.setText(frase.fraseFr)
        respuestaMostrada = true
        binding.btnRespuesta.isEnabled = false

        // Animación del botón
        binding.btnRespuesta.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(200)
            .withEndAction {
                binding.btnRespuesta.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun validarTapped() {
        if (currentIndex >= phrases.size) return

        // 🔥 VERIFICAR SI HAY MACARONES AL INICIO
        if (!macaronManager.canPlay()) {
            macaronManager.showRecoveryAlertDialog(requireContext())
            return  // ⛔ NO PERMITIR JUGAR
        }

        val respuestaUsuario = binding.inputEdit.text?.toString()?.trim() ?: ""
        val fraseCorrecta = phrases[currentIndex].fraseFr

        if (respuestaUsuario.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.champ_vide))
                .setMessage(getString(R.string.veuillez_crire_votre_r_ponse_avant_de_valider))
                .setPositiveButton(getString(R.string.d_accord), null)
                .show()
            return
        }

        hideKeyboard()

        if (validarRespuesta(respuestaUsuario, fraseCorrecta)) {
            // Respuesta correcta
            intentosFallidos = 0
            respuestaMostrada = false
            mostrarModalCorrecta(correctText = fraseCorrecta)
        } else {
            // Respuesta incorrecta
            intentosFallidos++

            // 🔥 PERDER VIDA SOLO EN PRIMER ERROR
            if (intentosFallidos == 1) {
                lifecycleScope.launch {
                    val consumido = macaronManager.consumeMacaron()
                    if (consumido) {
                        updateTopBarMacarons()  // 🔥 ACTUALIZAR BARRA

                        // Verificar si se acabaron los macarones
                        if (macaronManager.currentMacaronCount <= 0) {
                            Toast.makeText(
                                requireContext(),
                                "¡Se te acabaron los macarones!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            if (intentosFallidos >= 2 && !respuestaMostrada) {
                binding.btnRespuesta.visibility = View.VISIBLE
            }

            if (intentosFallidos >= 3) {
                // Tercer intento fallido
                mostrarModalIncorrecta(correctText = fraseCorrecta)
            } else {
                // Primer o segundo intento fallido
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.incorrect))
                    .setMessage(
                        getString(
                            R.string.essaye_encore_vous_avez_tentatives_restantes,
                            3 - intentosFallidos
                        ))
                    .setPositiveButton("D'accord", null)
                    .show()
            }
        }
    }
    private fun mostrarModalCorrecta(correctText: String) {

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_correct, null)

        val textAnswer = view.findViewById<android.widget.TextView>(R.id.textAnswerCorrect)
        textAnswer?.text = correctText

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

        if (!macaronManager.canPlay()) {
            macaronManager.showRecoveryAlertDialog(requireContext())
            return
        }
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_incorrect, null)

        val textAnswer = view.findViewById<android.widget.TextView>(R.id.textAnswerIncorrect)
        textAnswer?.text = correctText

        val btnContinue = view.findViewById<android.widget.Button>(R.id.btnContinueIncorrect)
        btnContinue?.setOnClickListener {
            dialog.dismiss()
            // No avanzar, quedarse en la misma frase
            // Solo resetear intentos para poder intentar de nuevo
            intentosFallidos = 0
            binding.inputEdit.text?.clear()
        }

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.isDraggable = false
        dialog.behavior.isFitToContents = true

        dialog.setContentView(view)
        dialog.show()
    }

    private fun avanzarSiguienteFrase() {
        currentIndex++
        saveBlockProgress(selectedBlock, currentIndex)

        if (currentIndex >= phrases.size) {
            markBlockAsCompleted(selectedBlock)
            mostrarDialogoFinal()
            binding.progressEjercicio.progress = 100
        } else {
            mostrarFraseActual()
        }
    }

    private fun mostrarDialogoFinal() {
        if (dialogShown) return
        dialogShown = true

        // Mostrar directamente la lluvia de macarons
        mostrarLluviaMacaronsYRegresar()
    }

    private fun mostrarLluviaMacaronsYRegresar() {
        val macaronFragment = MacaronRainFragment()

        // MOSTRAR EL CONTENEDOR
        binding.macaronContainer.visibility = View.VISIBLE

        childFragmentManager.beginTransaction()
            .replace(R.id.macaronContainer, macaronFragment, "MACARON_RAIN")
            .commit()

        // quitar animación y navegar después
        Handler(Looper.getMainLooper()).postDelayed({

            // Quitar el fragment visual
            val fragment = childFragmentManager.findFragmentByTag("MACARON_RAIN")
            fragment?.let {
                childFragmentManager.beginTransaction()
                    .remove(it)
                    .commit()
            }

            binding.macaronContainer.visibility = View.GONE

            // navegar CON EL PARÁMETRO MODE
            try {
                if (isAdded) {
                    // Usar Safe Args para pasar el modo
                    val action = PhrasesEx3FragmentDirections
                        .actionPhrasesEx3FragmentToPhrasesFragment(mode = mode)

                    findNavController().navigate(action)
                }
            } catch (e: Exception) {
                Log.e("PHRASES_EX3", "Error al navegar: ${e.message}")
                // Fallback: intentar navegar sin Safe Args
                try {
                    findNavController().navigate(R.id.action_phrasesEx3Fragment_to_phrasesFragment)
                } catch (_: Exception) {
                    activity?.onBackPressed()
                }
            }
        }, 6000)
    }


    // Ocultar teclado desde cualquier vista
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val view = requireActivity().currentFocus ?: View(requireContext())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }


    // Función para actualizar el progreso en PhrasesFragment
    private fun updateCompletedBlockInPhrasesFragment() {
        // Marcar el bloque actual como completado
        val lastCompletedBlock = getLastBlockCompleted()
        if (selectedBlock > lastCompletedBlock) {
            saveLastBlockCompleted(selectedBlock)
            Log.d("PhrasesEx3", "✅ Bloque $selectedBlock completado")
        }

        // También marcar en shared preferences específico de Ex3
        markBlockAsCompleted(selectedBlock)
    }

    // Agregar estas funciones para manejar el progreso
    private fun getLastBlockCompleted(): Int {
        return sharedPreferences.getInt("${mode}_last_completed_block", -1)
    }

    private fun saveLastBlockCompleted(blockIndex: Int) {
        sharedPreferences.edit()
            .putInt("${mode}_last_completed_block", blockIndex)
            .apply()
    }

    private fun reiniciarEjercicio3() {
        currentIndex = 0
        resetBlockProgress(selectedBlock)
        dialogShown = false

        binding.btnRepetir.visibility = View.GONE
        binding.msgCompletado.visibility = View.GONE
        binding.txtBloqueCompletado.visibility = View.GONE

        binding.btnPista.isEnabled = true
        binding.btnRespuesta.isEnabled = true
        binding.btnRespuesta.visibility = View.GONE

        mostrarFraseActual()
    }

    // MARK: - SharedPreferences
    private fun loadSavedProgress() {
        val savedProgress = getBlockProgress(selectedBlock)
        if (phrases.isNotEmpty() && savedProgress > 0 && savedProgress < phrases.size) {
            currentIndex = savedProgress
            println("Progreso cargado para Ex3: índice $currentIndex")
        }
    }

    private fun saveBlockProgress(blockIndex: Int, progress: Int) {
        sharedPreferences.edit()
            .putInt("${mode}_$KEY_EX3_BLOCK_PROGRESS_PREFIX$blockIndex", progress)
            .apply()
    }

    private fun getBlockProgress(blockIndex: Int): Int {
        return sharedPreferences.getInt("${mode}_$KEY_EX3_BLOCK_PROGRESS_PREFIX$blockIndex", 0)
    }

    private fun markBlockAsCompleted(blockIndex: Int) {
        sharedPreferences.edit()
            .putBoolean("${mode}_$KEY_EX3_BLOCK_COMPLETED_PREFIX$blockIndex", true)
            .apply()
    }

    private fun isBlockCompleted(blockIndex: Int = selectedBlock): Boolean {
        return sharedPreferences.getBoolean("${mode}_$KEY_EX3_BLOCK_COMPLETED_PREFIX$blockIndex", false)
    }

    private fun resetBlockProgress(blockIndex: Int) {
        sharedPreferences.edit().apply {
            putInt("${mode}_$KEY_EX3_BLOCK_PROGRESS_PREFIX$blockIndex", 0)
            putBoolean("${mode}_$KEY_EX3_BLOCK_COMPLETED_PREFIX$blockIndex", false)
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        saveBlockProgress(selectedBlock, currentIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveBlockProgress(selectedBlock, currentIndex)
        _binding = null
    }
}