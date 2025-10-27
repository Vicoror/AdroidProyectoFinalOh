package com.vicoror.appandroidfinal.ui.phrases

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentPhrasesBinding
import com.vicoror.appandroidfinal.viewModel.PhrasesViewModel

class PhrasesFragment : Fragment() {

    private var _binding: FragmentPhrasesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhrasesViewModel by viewModels(ownerProducer = { requireActivity() })

    private val sidePadding = 16
    private val rowHeight = 120
    private val jarSize = 100
    private val totalMacarons = 12

    // ✅ Variable para guardar el bloque seleccionado
    private var selectedBlockIndex: Int = -1

    // ✅ SharedPreferences para persistencia
    private lateinit var sharedPreferences: android.content.SharedPreferences

    companion object {
        private const val TAG = "PhrasesFragment"
        private const val PREFS_NAME = "PhrasesProgress"
        private const val KEY_LAST_BLOCK_COMPLETED = "last_block_completed"
        private const val KEY_BLOCK_PROGRESS_PREFIX = "block_progress_"
        private const val KEY_TOTAL_PRACTICED = "total_phrases_practiced"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhrasesBinding.inflate(inflater, container, false)

        // ✅ Inicializar SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "=== PHRASES FRAGMENT INICIADO ===")

        setupClickListeners()
        observeViewModel()
        binding.viewMsgPrincipal.bringToFront()
    }

    private fun setupClickListeners() {
        // ✅ Botón principal modificado para navegar al bloque seleccionado
        binding.btnMsgPrincipal.setOnClickListener {
            if (selectedBlockIndex != -1) {
                Log.d(TAG, "Navegando al bloque: $selectedBlockIndex")

                val bundle = Bundle().apply {
                    putInt("selectedBlockIndex", selectedBlockIndex)
                }

                findNavController().navigate(
                    R.id.action_phrasesFragment_to_phrasesEx1Fragment,
                    bundle
                )
            } else {
                Log.w(TAG, "No hay bloque seleccionado")
                hideMessageView()
            }
        }

        binding.phrasesRoot.setOnClickListener {
            if (binding.viewMsgPrincipal.visibility == View.VISIBLE) {
                hideMessageView()
                // ✅ Resetear el índice cuando se cierra sin continuar
                selectedBlockIndex = -1
            }
        }

        binding.viewMsgPrincipal.setOnClickListener {
            // No hacer nada - evita cerrar al tocar dentro
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Observando ViewModel...")

        viewModel.phrases.observe(viewLifecycleOwner) { bloques ->
            Log.d(TAG, "ViewModel devolvió: ${bloques?.size ?: 0} bloques")

            if (bloques != null && bloques.isNotEmpty()) {
                Log.d(TAG, "✅ Bloques cargados: ${bloques.size}")
                // SIEMPRE crear 12 macarons independientemente de los datos
                Log.d(TAG, "✅ Creando 12 macarons (como en iOS)")
                setupMacaronStack()
            } else {
                Log.e(TAG, "❌ No hay bloques disponibles")
            }
        }
    }

    private fun setupMacaronStack() {
        val container = binding.macaronContainer
        container.removeAllViews()

        Log.d(TAG, "Configurando Macaron Stack con 12 macarons")

        val density = resources.displayMetrics.density
        val sidePaddingPx = (sidePadding * density).toInt()
        val rowHeightPx = (rowHeight * density).toInt()
        val jarSizePx = (jarSize * density).toInt()

        container.orientation = LinearLayout.VERTICAL
        container.setPadding(sidePaddingPx, 20, sidePaddingPx, 40)
        container.gravity = android.view.Gravity.CENTER_HORIZONTAL

        // ✅ Obtener progreso guardado
        val lastCompletedBlock = getLastBlockCompleted()
        Log.d(TAG, "Último bloque completado: $lastCompletedBlock")

        for (i in 1..totalMacarons) {
            val position = (i - 1) % 4

            val rowView = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    rowHeightPx
                )
                orientation = LinearLayout.HORIZONTAL

                gravity = when (position) {
                    0 -> android.view.Gravity.CENTER_HORIZONTAL
                    1 -> android.view.Gravity.END
                    2 -> android.view.Gravity.CENTER_HORIZONTAL
                    3 -> android.view.Gravity.START
                    else -> android.view.Gravity.CENTER_HORIZONTAL
                }
            }

            val containerView = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(jarSizePx, jarSizePx).apply {
                    when (position) {
                        1 -> setMargins(0, 0, 25, 0)
                        3 -> setMargins(25, 0, 0, 0)
                        else -> setMargins(0, 0, 0, 0)
                    }
                }
            }

            val jar = createMacaronView(jarSizePx, i, lastCompletedBlock)
            containerView.addView(jar)
            rowView.addView(containerView)
            container.addView(rowView)

            Log.d(TAG, "Macaron $i en posición $position")
        }

        binding.viewMsgPrincipal.bringToFront()
        Log.d(TAG, "✅ Macaron Stack creado")
    }

    private fun createMacaronView(jarSizePx: Int, index: Int, lastCompletedBlock: Int): ImageView {
        return ImageView(requireContext()).apply {
            setImageResource(R.drawable.macaron)
            layoutParams = FrameLayout.LayoutParams(jarSizePx, jarSizePx).apply {
                gravity = android.view.Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER

            // ✅ Determinar si el macaron está activo basado en el progreso
            val isBlockCompleted = (index - 1) <= lastCompletedBlock
            val isNextBlock = index == lastCompletedBlock + 2
            val isActive = isBlockCompleted || isNextBlock || index == 1

            isClickable = isActive
            isEnabled = isActive
            alpha = if (isActive) 1.0f else 0.4f

            if (isActive) {
                setOnClickListener { onMacaronTapped(index) }
            }

            // ✅ Mostrar indicador de progreso si el bloque está completado
            if (isBlockCompleted) {
                // Puedes cambiar el color o agregar un checkmark aquí
                alpha = 0.8f
            }

            contentDescription = if (isBlockCompleted) {
                "Macaron $index de 12 - Completado"
            } else if (isActive) {
                "Macaron $index de 12 - Disponible"
            } else {
                "Macaron $index de 12 - Bloqueado"
            }
        }
    }

    private fun onMacaronTapped(index: Int) {
        Log.d(TAG, "Macaron $index tapped")

        // Animación
        val macaron = findMacaronView(index)
        macaron?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(150)?.withEndAction {
            macaron.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
        }

        // ✅ Guardar el índice seleccionado
        selectedBlockIndex = index - 1

        // Mostrar mensaje
        binding.labelMsgPrincipal.text = getString(R.string.vamos_con_las_frases_del_bloque, index)

        // ✅ Cambiar texto del botón para que sea más específico
        binding.btnMsgPrincipal.text = getString(R.string.continuar_al_bloque, index)

        binding.viewMsgPrincipal.apply {
            alpha = 0f
            visibility = View.VISIBLE
            bringToFront()
            animate().alpha(1f).setDuration(300).start()
        }
    }

    // ✅ MÉTODOS DE SHAREDPREFERENCES
    private fun getLastBlockCompleted(): Int {
        return sharedPreferences.getInt(KEY_LAST_BLOCK_COMPLETED, -1)
    }

    private fun saveLastBlockCompleted(blockIndex: Int) {
        sharedPreferences.edit().putInt(KEY_LAST_BLOCK_COMPLETED, blockIndex).apply()
        Log.d(TAG, "Bloque $blockIndex marcado como completado")
    }

    private fun getBlockProgress(blockIndex: Int): Int {
        return sharedPreferences.getInt("$KEY_BLOCK_PROGRESS_PREFIX$blockIndex", 0)
    }

    private fun saveBlockProgress(blockIndex: Int, progress: Int) {
        sharedPreferences.edit().putInt("$KEY_BLOCK_PROGRESS_PREFIX$blockIndex", progress).apply()
    }

    private fun incrementTotalPhrasesPracticed() {
        val current = sharedPreferences.getInt(KEY_TOTAL_PRACTICED, 0)
        sharedPreferences.edit().putInt(KEY_TOTAL_PRACTICED, current + 1).apply()
        Log.d(TAG, "Frases practicadas total: ${current + 1}")
    }

    private fun findMacaronView(index: Int): ImageView? {
        val container = binding.macaronContainer
        if (index - 1 < container.childCount) {
            val rowView = container.getChildAt(index - 1) as? LinearLayout
            val containerView = rowView?.getChildAt(0) as? FrameLayout
            return containerView?.getChildAt(0) as? ImageView
        }
        return null
    }

    private fun hideMessageView() {
        binding.viewMsgPrincipal.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.viewMsgPrincipal.visibility = View.GONE
                // ✅ Resetear el índice cuando se oculta el mensaje
                selectedBlockIndex = -1
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}