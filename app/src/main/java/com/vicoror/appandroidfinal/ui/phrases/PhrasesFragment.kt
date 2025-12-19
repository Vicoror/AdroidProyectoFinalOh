package com.vicoror.appandroidfinal.ui.phrases

import android.R.attr.mode
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentPhrasesBinding
import com.vicoror.appandroidfinal.utils.MacaronManager
import com.vicoror.appandroidfinal.viewModel.PhrasesViewModel
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.apply

class PhrasesFragment : Fragment() {

    private var _binding: FragmentPhrasesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhrasesViewModel by viewModels(ownerProducer = { requireActivity() })

    private var mode: String = "phrases"
    private val sidePadding = 16
    private val rowHeight = 120
    private val jarSize = 100
    private val totalMacarons = 12

    private lateinit var macaronManager: MacaronManager

    // Variable para guardar el bloque seleccionado
    private var selectedBlockIndex: Int = -1

    private val args: PhrasesFragmentArgs by navArgs()


    // SharedPreferences para persistencia
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "PhrasesFragment"
        private const val PREFS_NAME = "PhrasesProgress"

        // Usar un formato consistente para todas las keys
        private fun keyLastBlockCompleted(mode: String) = "${mode}_last_completed_block"
        private fun keyBlockCompleted(mode: String, blockIndex: Int) = "${mode}_block_completed_$blockIndex"
        private fun keyEx3BlockCompleted(mode: String, blockIndex: Int) = "${mode}_ex3_block_completed_$blockIndex"
        private fun keyBlockProgress(mode: String, blockIndex: Int) = "${mode}_block_progress_$blockIndex"
        private fun keyTotalPracticed(mode: String) = "${mode}_total_practiced"
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

        // 1. Inicializar manager
        macaronManager = MacaronManager(requireContext())

        // 2. Mostrar vidas iniciales
        updateLivesLabel()

        mode = args.mode

        if (mode == "verbs") {
            binding.titleLabel.text = "Verbes"
        } else {
            binding.titleLabel.text = "Phrases"
        }

        Log.d(TAG, "=== PHRASES FRAGMENT INICIADO ===   MODE = $mode")

        viewModel.loadJson(mode)
        setupClickListeners()
        observeViewModel()
        binding.viewMsgPrincipal.bringToFront()

        setupTabBar()
    }

    private fun setupTabBar() {
        // Tab Home
        binding.tabHome.setOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        findNavController().popBackStack(R.id.homeFragment, false)
    }
    private fun setupClickListeners() {
        // BOTÓN CONTINUAR
        binding.btnMsgPrincipal.setOnClickListener {
            // ✅ VERIFICAR SI HAY MACARONES ANTES DE NAVEGAR
            if (!macaronManager.canPlay()) {
                macaronManager.showRecoveryAlertDialog(requireContext())
                return@setOnClickListener
            }

            if (selectedBlockIndex != -1) {

                Log.d(TAG, "Navegando al bloque: $selectedBlockIndex con mode: $mode")

                // Usar Safe Args para pasar TODOS los parámetros
                val action = PhrasesFragmentDirections
                    .actionPhrasesFragmentToPhrasesEx1Fragment(
                        selectedBlockIndex = selectedBlockIndex,
                        mode = mode  // ← ¡IMPORTANTE! Pasar el mode aquí
                    )

                findNavController().navigate(action)
            } else {
                Log.w(TAG, "No hay bloque seleccionado")
                hideMessageView()
            }
        }

        // LA X PARA CERRAR EL MENSAJE
        binding.btnCerrarMsg.setOnClickListener {
            hideMessageView()
            selectedBlockIndex = -1
        }

        // CERRAR TOCANDO FUERA (si usas overlay)
        binding.overlayMsg?.setOnClickListener {
            hideMessageView()
            selectedBlockIndex = -1
        }

        // EVITAR QUE TOQUES DENTRO Y SE CIERRE
        binding.viewMsgPrincipal.setOnClickListener {
            // no cerrar, solo consumir clic
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
        container.gravity = Gravity.CENTER_HORIZONTAL

        //Obtener progreso guardado
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
                    0 -> Gravity.CENTER_HORIZONTAL
                    1 -> Gravity.END
                    2 -> Gravity.CENTER_HORIZONTAL
                    3 -> Gravity.START
                    else -> Gravity.CENTER_HORIZONTAL
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

            val jar = createMacaronView(
                jarSizePx,
                i,
                lastCompletedBlock,
                mode
            )
            containerView.addView(jar)
            rowView.addView(containerView)
            container.addView(rowView)


            Log.d(TAG, "Macaron $i en posición $position")
        }

        binding.viewMsgPrincipal.bringToFront()
        Log.d(TAG, " Macaron Stack creado")
    }

    private fun createMacaronView(
        jarSizePx: Int,
        index: Int,
        lastCompletedBlock: Int,
        currentMode: String
    ): ImageView {
        return ImageView(requireContext()).apply {
            val blockIndex = index - 1

            // Verificar si el bloque está completado (usando las nuevas funciones)
            val isBlockCompleted = isBlockCompleted(blockIndex)
            val isBlockUnlocked = isBlockUnlocked(blockIndex)

            // Un bloque está activo si está completado o desbloqueado
            val isActive = isBlockCompleted || isBlockUnlocked || index == 1

            val image = when {
                isBlockCompleted -> if (currentMode == "verbs")
                    R.drawable.macaron_fffa9c
                else
                    R.drawable.macaron_c5a4da

                isActive -> if (currentMode == "verbs")
                    R.drawable.macaron_ff8a80
                else
                    R.drawable.macaron

                else -> if (currentMode == "verbs")
                    R.drawable.macaron_ff8a80
                else
                    R.drawable.macaron
            }

            setImageResource(image)

            layoutParams = FrameLayout.LayoutParams(jarSizePx, jarSizePx).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER

            isClickable = isActive
            isEnabled = isActive
            alpha = if (isActive) 1.0f else 0.4f

            if (isActive) {
                setOnClickListener { onMacaronTapped(index) }
            }

            // Mostrar indicador de progreso si el bloque está completado
            if (isBlockCompleted) {
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

        // Guardar el índice seleccionado
        selectedBlockIndex = index - 1

        // Mostrar mensaje
        binding.labelMsgPrincipal.text = getString(R.string.vamos_con_las_frases_del_bloque, index)

        //Cambiar texto del botón para que sea más específico
        binding.btnMsgPrincipal.text = getString(R.string.continuar_al_bloque, index)

        // Mostrar overlay
        binding.overlayMsg.apply {
            alpha = 0f
            visibility = View.VISIBLE
            bringToFront()
            animate().alpha(1f).setDuration(300).start()
        }
        // Mostrar tarjeta
        binding.viewMsgPrincipal.apply {
            alpha = 0f
            visibility = View.VISIBLE
            bringToFront()   // ← ESTO ES LO QUE FALTABA
            animate().alpha(1f).setDuration(300).start()
        }

    }

    //MÉTODOS DE SHAREDPREFERENCES

    private fun getLastBlockCompleted(): Int {
        // Usar la key consistente
        val key = keyLastBlockCompleted(mode)
        return sharedPreferences.getInt(key, -1)
    }

    private fun saveLastBlockCompleted(blockIndex: Int) {
        val key = keyLastBlockCompleted(mode)
        sharedPreferences.edit().putInt(key, blockIndex).apply()
        Log.d(TAG, "Guardado último bloque completado: $blockIndex para modo: $mode")
    }

    private fun getBlockProgress(blockIndex: Int): Int {
        val key = keyBlockProgress(mode, blockIndex)
        return sharedPreferences.getInt(key, 0)
    }

    private fun saveBlockProgress(blockIndex: Int, progress: Int) {
        val key = keyBlockProgress(mode, blockIndex)
        sharedPreferences.edit().putInt(key, progress).apply()
    }

    private fun incrementTotalPhrasesPracticed() {
        val key = keyTotalPracticed(mode)
        val current = sharedPreferences.getInt(key, 0)
        sharedPreferences.edit().putInt(key, current + 1).apply()
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
                binding.overlayMsg.alpha = 1f
                //Resetear el índice cuando se oculta el mensaje
                binding.overlayMsg.visibility = View.GONE
                selectedBlockIndex = -1
            }
            .start()

    }

    private fun isBlockCompleted(blockIndex: Int): Boolean {
        // Verificar si el bloque está completado (EXERCISE 3)
        val ex3Key = keyEx3BlockCompleted(mode, blockIndex)
        val isEx3Completed = sharedPreferences.getBoolean(ex3Key, false)

        // También puedes verificar otros ejercicios si quieres
        val blockKey = keyBlockCompleted(mode, blockIndex)
        val isAnyCompleted = sharedPreferences.getBoolean(blockKey, false)

        return isEx3Completed || isAnyCompleted
    }

    private fun isBlockUnlocked(blockIndex: Int): Boolean {
        // Bloque 0 siempre desbloqueado
        if (blockIndex == 0) return true

        // Verificar si el bloque anterior está completado
        return isBlockCompleted(blockIndex - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateLivesLabel() {
        val current = macaronManager.currentMacaronCount

        binding.livesLabel.text = current.toString()
    }

}