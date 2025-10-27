package com.vicoror.appandroidfinal.ui.phrases

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentPhrasesEx1Binding
import com.vicoror.appandroidfinal.utils.NetworkChecker
import com.vicoror.appandroidfinal.viewModel.PhrasesViewModel
import kotlin.math.abs

class PhrasesEx1Fragment : Fragment() {

    private var _binding: FragmentPhrasesEx1Binding? = null
    private val binding get() = _binding!!

    private val viewModel: PhrasesViewModel by viewModels(ownerProducer = { requireActivity() })
    private var selectedBlockIndex: Int = 0

    private var phrases: List<com.vicoror.appandroidfinal.data.model.Phrase> = emptyList()
    private var currentIndex = 0

    private var startX = 0f
    private val SWIPE_THRESHOLD = 100f

    private lateinit var sharedPreferences: android.content.SharedPreferences

    companion object {
        private const val PREFS_NAME = "PhrasesProgress"
        private const val KEY_LAST_BLOCK_COMPLETED = "last_block_completed"
        private const val KEY_BLOCK_PROGRESS_PREFIX = "block_progress_"
        private const val KEY_TOTAL_PRACTICED = "total_phrases_practiced"
        private const val KEY_BLOCK_COMPLETION_PREFIX = "block_completed_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedBlockIndex = arguments?.getInt("selectedBlockIndex", 0) ?: 0
        Log.d("PhrasesEx1", "onCreate - Bloque seleccionado: $selectedBlockIndex")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhrasesEx1Binding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!NetworkChecker.isOnline(requireContext())) {
            binding.fraseFrLabel.text = getString(R.string.esperando)
            binding.fraseEsLabel.text = getString(R.string.cargando_frases)
        } else {
            setupObservers()
            mostrarFraseActual()
        }

        setupTopBar()
        setupSwipeGestures()

        // ✅ Botón siguiente
        binding.btnSuivant.setOnClickListener {
            if (!NetworkChecker.isOnline(requireContext())) {
                binding.fraseFrLabel.text = getString(R.string.sin_conex)
                binding.fraseEsLabel.text = getString(R.string.conectate_con)
            } else {
                siguienteFrase()
            }
        }

        // ✅ Nuevo: Botón anterior
        binding.btnAnterior?.setOnClickListener {
            anteriorFrase()
        }

        view.setBackgroundColor(resources.getColor(R.color.purplewhite, null))

        Log.d("PhrasesEx1", "onViewCreated - Bloque seleccionado: $selectedBlockIndex")
    }

    private fun setupObservers() {
        Log.d("PhrasesEx1", "Configurando observadores...")

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.fraseFrLabel.text = getString(R.string.cargando_frases)
                binding.fraseEsLabel.text = getString(R.string.por_favor_espera)
                binding.btnSuivant.visibility = View.GONE
            } else {
                binding.btnSuivant.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                binding.fraseFrLabel.text = getString(R.string.error)
                binding.fraseEsLabel.text = error
                Log.e("PhrasesEx1", "Error: $error")
            }
        }

        viewModel.phrases.observe(viewLifecycleOwner) { bloques ->
            Log.d("PhrasesEx1", "Datos recibidos: ${bloques?.size ?: 0} bloques")

            if (bloques != null && bloques.isNotEmpty()) {
                if (selectedBlockIndex < bloques.size) {
                    phrases = bloques[selectedBlockIndex]
                    Log.d("PhrasesEx1", "✅ Cargado bloque $selectedBlockIndex con ${phrases.size} frases")

                    val savedProgress = getBlockProgress(selectedBlockIndex)
                    if (savedProgress > 0 && savedProgress < phrases.size) {
                        currentIndex = savedProgress
                        Log.d("PhrasesEx1", "✅ Progreso cargado: frase $currentIndex")
                    }

                    if (phrases.isNotEmpty()) {
                        mostrarFraseActual()
                    } else {
                        binding.fraseFrLabel.text = getString(R.string.bloquevacio)
                        binding.fraseEsLabel.text = getString(R.string.nohayfrases)
                    }
                } else {
                    phrases = bloques.last()
                    Log.w("PhrasesEx1", "⚠ Índice $selectedBlockIndex no existe, usando último bloque")
                    mostrarFraseActual()
                }
            } else {
                binding.fraseFrLabel.text = getString(R.string.no_hay_frases_disponibles)
                binding.fraseEsLabel.text = getString(R.string.vuelve_a_la_pantalla_anterior)
            }
        }
    }

    private fun mostrarFraseActual() {
        if (currentIndex < phrases.size) {
            val frase = phrases[currentIndex]
            binding.fraseFrLabel.text = frase.fraseFr
            binding.fraseEsLabel.text =
                getString(R.string.categoria, frase.fraseEs, frase.categoria)
            binding.btnSuivant.text = getString(R.string.siguiente, currentIndex + 1, phrases.size)

            if (isBlockCompleted(selectedBlockIndex)) {
                binding.fraseEsLabel.append("\n\n✅ Bloque ya completado anteriormente")
            }
        } else {
            mostrarDialogoFinal()
        }
    }

    private fun siguienteFrase() {
        if (currentIndex < phrases.size - 1) {
            currentIndex++
            saveBlockProgress(selectedBlockIndex, currentIndex)
            incrementTotalPhrasesPracticed()
            animateTextChange()
            mostrarFraseActual()
        } else {
            saveBlockProgress(selectedBlockIndex, currentIndex)
            markBlockAsCompleted(selectedBlockIndex)
            saveLastBlockCompleted(selectedBlockIndex)
            incrementTotalPhrasesPracticed()
            mostrarDialogoFinal()
        }
    }

    // ✅ Nuevo diálogo final
    private fun mostrarDialogoFinal() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("¡Bloque completado!")
            .setMessage("¿Deseas repetir el ejercicio o volver al menú principal?")
            .setPositiveButton("Repetir") { _, _ ->
                currentIndex = 0
                saveBlockProgress(selectedBlockIndex, 0)
                animateTextChange()
                mostrarFraseActual()
            }
            .setNegativeButton("Volver al menú") { _, _ ->
                findNavController().navigate(R.id.action_phrasesEx1Fragment_to_phrasesFragment)
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    private fun anteriorFrase() {
        if (currentIndex > 0) {
            currentIndex--
            saveBlockProgress(selectedBlockIndex, currentIndex)
            animateTextChange()
            mostrarFraseActual()
        }
    }

    private fun getLastBlockCompleted(): Int {
        return sharedPreferences.getInt(KEY_LAST_BLOCK_COMPLETED, -1)
    }

    private fun saveLastBlockCompleted(blockIndex: Int) {
        val currentLastBlock = getLastBlockCompleted()
        if (blockIndex > currentLastBlock) {
            sharedPreferences.edit().putInt(KEY_LAST_BLOCK_COMPLETED, blockIndex).apply()
        }
    }

    private fun getBlockProgress(blockIndex: Int): Int {
        return sharedPreferences.getInt("$KEY_BLOCK_PROGRESS_PREFIX$blockIndex", 0)
    }

    private fun saveBlockProgress(blockIndex: Int, progress: Int) {
        sharedPreferences.edit().putInt("$KEY_BLOCK_PROGRESS_PREFIX$blockIndex", progress).apply()
    }

    private fun isBlockCompleted(blockIndex: Int): Boolean {
        return sharedPreferences.getBoolean("$KEY_BLOCK_COMPLETION_PREFIX$blockIndex", false)
    }

    private fun markBlockAsCompleted(blockIndex: Int) {
        sharedPreferences.edit().putBoolean("$KEY_BLOCK_COMPLETION_PREFIX$blockIndex", true).apply()
    }

    private fun incrementTotalPhrasesPracticed() {
        val current = sharedPreferences.getInt(KEY_TOTAL_PRACTICED, 0)
        sharedPreferences.edit().putInt(KEY_TOTAL_PRACTICED, current + 1).apply()
    }

    private fun animateTextChange() {
        binding.fraseFrLabel.alpha = 0f
        binding.fraseEsLabel.alpha = 0f
        binding.fraseFrLabel.animate().alpha(1f).setDuration(300).start()
        binding.fraseEsLabel.animate().alpha(1f).setDuration(300).start()
    }

    private fun setupSwipeGestures() {
        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val endX = event.x
                    val diffX = endX - startX
                    if (abs(diffX) > SWIPE_THRESHOLD) {
                        if (diffX > 0) anteriorFrase() else siguienteFrase()
                        return@setOnTouchListener true
                    }
                    false
                }

                else -> false
            }
        }
    }

    private fun setupTopBar() {
        binding.topBarExitButton.setOnClickListener {
            findNavController().popBackStack()
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
            val img = androidx.appcompat.widget.AppCompatImageView(requireContext()).apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
