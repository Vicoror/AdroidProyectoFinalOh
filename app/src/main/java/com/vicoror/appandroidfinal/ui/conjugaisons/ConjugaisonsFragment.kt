package com.vicoror.appandroidfinal.ui.conjugaisons

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.navigation.fragment.findNavController
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.data.model.Frase
import com.vicoror.appandroidfinal.databinding.FragmentConjugaisonsBinding
import com.vicoror.appandroidfinal.utils.MacaronManager
import com.vicoror.appandroidfinal.viewModel.ConjugaisonsViewModel


class ConjugaisonsFragment : Fragment() {

    companion object {
        const val TAG = "ConjugaisonsFragment"
        const val ARG_SELECTED_TENSE = "selectedTense"
        const val ARG_JSON_FILE_NAME = "jsonFileName"
    }

    private var _binding: FragmentConjugaisonsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConjugaisonsViewModel by viewModels()

    private lateinit var selectedTense: String
    private lateinit var jsonFileName: String
    private var selectedBlockIndex = 0
    private val totalMacarons = 12
    private val FRASES_POR_BLOQUE = 10
    private lateinit var macaronManager: MacaronManager

    // Lista de FRASES (que contienen conjugaciones)
    private var frases: List<Frase> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = requireArguments()

        selectedTense = args.getString(ARG_SELECTED_TENSE)!!
        jsonFileName  = args.getString(ARG_JSON_FILE_NAME)!!

        Log.d(TAG, "Tiempo verbal seleccionado: $selectedTense")
        Log.d(TAG, "Colección Firestore: $jsonFileName")
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConjugaisonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        macaronManager = MacaronManager(requireContext())

        updateLivesLabel()

        setupUI()
        setupObservers()
        loadFrases()
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

    private fun setupUI() {
        binding.tenseTitleLabel.text = getTenseTitle(selectedTense)

        binding.btnBackContainer.setOnClickListener {
            // Navegar directamente a MenuConjugaisonFragment
            findNavController().navigate(R.id.menuConjugaisonsFragment)
        }
    }
    private fun getTenseTitle(tense: String): String = when (tense) {
        "Présent" -> "Présent de l'indicatif"
        "Passé composé" -> "Passé composé"
        "Futur simple" -> "Futur simple"
        "Imparfait" -> "Imparfait de l'indicatif"
        else -> tense
    }

    private fun setupObservers() {
        // Observar FRASES, no conjugaciones
        viewModel.frases.observe(viewLifecycleOwner) { frasesList ->
            Log.d(TAG, "✅ Frases cargadas: ${frasesList.size}")

            if (frasesList.isNotEmpty()) {
                binding.shimmerContainer.stopShimmer()
                binding.shimmerContainer.visibility = View.GONE
                binding.macaronContainer.visibility = View.VISIBLE

                frases = frasesList
                Log.d(TAG, "Primeras 3 frases: ${frases.take(3).map { it.verbo }}")

                setupMacaronStack()
            } else {
                Log.e(TAG, "❌ No se cargaron frases")
                showError("No se encontraron conjugaciones")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.shimmerContainer.startShimmer()
                binding.shimmerContainer.visibility = View.VISIBLE
            } else {
                binding.shimmerContainer.stopShimmer()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Error: $it")
                showError(it)
            }
        }
    }

    private fun loadFrases() {
        Log.d(TAG, "📥 Cargando TODAS las frases de: $jsonFileName")

        // CAMBIO: Usar loadTodasLasFrases en lugar de loadFrasesDelBloque
        viewModel.loadTodasLasFrases(jsonFileName)
    }

    private fun setupMacaronStack() {
        val container = binding.macaronContainer
        container.removeAllViews()

        Log.d(TAG, "🔢 Total frases disponibles: ${frases.size}")
        Log.d(TAG, "🎯 Configurando $totalMacarons macarrones")
        Log.d(TAG, "🎯 setupMacaronStack() - frases.size=${frases.size}")

        if (frases.isEmpty()) {
            Log.e(TAG, "⚠️ frases está vacío, no se pueden crear macarrones")
            return
        }

        // Dividir frases en bloques (5 por bloque como antes)
        val frasesPorBloque = frases.chunked(FRASES_POR_BLOQUE)
        val bloquesDisponibles = frasesPorBloque.size


        Log.d(TAG, "📦 Bloques disponibles: $bloquesDisponibles (${frases.size} frases ÷ $FRASES_POR_BLOQUE)")

        // Configurar contenedor
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_HORIZONTAL
        container.setPadding(0, 25, 0, 25)

        val density = resources.displayMetrics.density
        val rowHeightPx = (120f * density).toInt()
        val jarSizePx = (100f * density).toInt()

        // Obtener progreso guardado
        val lastCompletedBlock = getLastBlockCompleted()
        Log.d(TAG, "📊 Último bloque completado: $lastCompletedBlock")

        // Crear 12 macarrones siempre
        for (blockIndex in 0 until totalMacarons) {
            // Determinar posición (0, 1, 2, 3) para el patrón
            val position = blockIndex % 4

            // Crear una fila nueva para cada bloque
            val row = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    rowHeightPx
                ).apply {
                    bottomMargin = (10f * density).toInt()
                }
                orientation = LinearLayout.HORIZONTAL
                // Aquí aplicamos la gravedad basada en la posición
                gravity = when (position) {
                    0 -> Gravity.CENTER_HORIZONTAL
                    1 -> Gravity.END
                    2 -> Gravity.CENTER_HORIZONTAL
                    3 -> Gravity.START
                    else -> Gravity.CENTER_HORIZONTAL
                }
            }

            // Contenedor para el macarrón con márgenes
            val containerView = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(jarSizePx, jarSizePx).apply {
                    // Aplicar márgenes según la posición
                    when (position) {
                        1 -> setMargins(0, 0, (25f * density).toInt(), 0)
                        3 -> setMargins((25f * density).toInt(), 0, 0, 0)
                        else -> setMargins(0, 0, 0, 0)
                    }
                }
            }

            // Determinar estado del bloque
            val tieneContenido = blockIndex < bloquesDisponibles
            val estaDesbloqueado = isBlockUnlocked(blockIndex)
            val estaCompletado = isBlockCompleted(blockIndex)

            Log.d(TAG, "Macarrón ${blockIndex + 1}: " +
                    "Desbloqueado=$estaDesbloqueado, " +
                    "Completado=$estaCompletado, " +
                    "Contenido=$tieneContenido")

            val macaron = createMacaronView(
                jarSizePx = jarSizePx,
                blockIndex = blockIndex,
                isUnlocked = estaDesbloqueado,
                isCompleted = estaCompletado,
                hasContent = tieneContenido
            )

            containerView.addView(macaron)
            row.addView(containerView)
            container.addView(row)
        }

        binding.viewMsgPrincipal.bringToFront()
        Log.d(TAG, "✅ Macaron stack creado con $totalMacarons macarrones")
    }

    private fun createMacaronView(
        jarSizePx: Int,
        blockIndex: Int,
        isUnlocked: Boolean,
        isCompleted: Boolean,
        hasContent: Boolean
    ): ImageView {
        return ImageView(requireContext()).apply {
            // Determinar imagen basada en estado
            val imageResource = when {
                isCompleted -> R.drawable.macaron_c5a4da
                isUnlocked && hasContent -> R.drawable.macaron_8cd5c9
                else -> R.drawable.macaron_8cd5c9  // Usar versión gris si bloqueado
            }

            setImageResource(imageResource)
            layoutParams = FrameLayout.LayoutParams(jarSizePx, jarSizePx)
            scaleType = ImageView.ScaleType.FIT_CENTER

            // Configurar interactividad
            val esInteractivo = isUnlocked && hasContent
            isClickable = esInteractivo
            isEnabled = esInteractivo
            alpha = if (esInteractivo) 1.0f else 0.4f

            // Añadir número del bloque
            val numero = blockIndex + 1

            // Configurar clic
            if (esInteractivo) {
                setOnClickListener {
                    onMacaronClicked(blockIndex)
                }
            }

            contentDescription = "Bloque $numero"
        }
    }

    private fun onMacaronClicked(blockIndex: Int) {
        Log.d(TAG, "🎯 Macarrón clickeado: Bloque $blockIndex")

        // Guardar índice seleccionado
        selectedBlockIndex = blockIndex

        // Obtener frases del bloque usando el mismo tamaño que los macarrones
        val frasesPorBloque = frases.chunked(FRASES_POR_BLOQUE)
        val frasesDelBloque = frasesPorBloque.getOrNull(blockIndex) ?: emptyList()

        Log.d(TAG, "📝 Frases en bloque $blockIndex: ${frasesDelBloque.size}")

        if (frasesDelBloque.isNotEmpty()) {
            // Mostrar overlay con información
            showBlockOverlay(blockIndex, frasesDelBloque)
        } else {
            showError("Este bloque no tiene contenido disponible")
        }
    }


    private fun showBlockOverlay(blockIndex: Int, frasesDelBloque: List<Frase>) {
        binding.labelMsgPrincipal.text =
            getString(R.string.vamos_con_las_conjugaciones_del_bloque, blockIndex + 1, selectedTense)

        binding.btnMsgPrincipal.text =
            getString(R.string.continuar_al_bloque, blockIndex + 1)

        // Configurar clic en continuar CON VERIFICACIÓN DE MACARONES
        binding.btnMsgPrincipal.setOnClickListener {
            // ✅ VERIFICAR SI HAY MACARONES
            if (!macaronManager.canPlay()) {
                macaronManager.showRecoveryAlertDialog(requireContext())
                return@setOnClickListener
            }

            navigateToConjugationPractice(blockIndex, frasesDelBloque)
        }

        binding.btnCerrarMsg.setOnClickListener {
            hideMessageOverlay()
        }

        // Mostrar overlay
        binding.overlayMsg.visibility = View.VISIBLE
        binding.viewMsgPrincipal.visibility = View.VISIBLE
        binding.overlayMsg.bringToFront()
        binding.viewMsgPrincipal.bringToFront()
    }
    private fun navigateToConjugationPractice(blockIndex: Int, frasesDelBloque: List<Frase>) {
        val bundle = Bundle().apply {
            putString("selectedTense", selectedTense)
            putInt("blockIndex", blockIndex)
        }

        val action =
            ConjugaisonsFragmentDirections
                .actionConjugaisonsFragmentToNivel1ConjugaisonsFragment(
                    selectedTense = selectedTense,   // clave lógica ("present")
                    jsonFileName = jsonFileName,     // colección ("Presente")
                    blockIndex = blockIndex
                )

        findNavController().navigate(action)


        hideMessageOverlay()
    }

    private fun hideMessageOverlay() {
        binding.overlayMsg.visibility = View.GONE
        binding.viewMsgPrincipal.visibility = View.GONE
    }

    // ---------------- PROGRESS ----------------

    private fun isBlockCompleted(blockIndex: Int): Boolean {
        val prefs = requireContext()
            .getSharedPreferences("conjugaisons_progress", 0)
        val progress = prefs.getInt("block_${selectedTense}_$blockIndex", 0)

        val isCompleted = progress >= 10
        Log.d(TAG, "🔍 isBlockCompleted($blockIndex) - progress=$progress, threshold=10, returning=$isCompleted")

        return isCompleted
    }

    private fun isBlockUnlocked(blockIndex: Int): Boolean {
        try {
            val prefs = requireContext()
                .getSharedPreferences("conjugaisons_progress", 0)

            val lastCompleted = prefs.getInt("last_block_$selectedTense", -1)

            // IMPORTANTE: Calcular bloques con contenido BASADO EN frases cargadas
            val bloquesConContenido = if (frases.isNotEmpty()) {
                frases.size / FRASES_POR_BLOQUE
            } else {
                0
            }

            // La lógica correcta:
            // 1. El bloque 0 siempre está desbloqueado (o si lastCompleted = -1)
            // 2. Los siguientes bloques se desbloquean secuencialmente
            val isUnlocked = if (blockIndex == 0) {
                true  // Bloque 0 siempre desbloqueado
            } else {
                blockIndex <= lastCompleted + 1 && blockIndex < bloquesConContenido
            }

            Log.d(TAG, "🔓 isBlockUnlocked($blockIndex) - " +
                    "lastCompleted=$lastCompleted, " +
                    "bloquesConContenido=$bloquesConContenido, " +
                    "frases.size=${frases.size}, " +
                    "returning=$isUnlocked")

            return isUnlocked

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en isBlockUnlocked: ${e.message}")
            return false
        }
    }

    private fun getLastBlockCompleted(): Int {
        val prefs = requireContext()
            .getSharedPreferences("conjugaisons_progress", 0)
        val lastBlock = prefs.getInt("last_block_$selectedTense", -1)

        Log.d(TAG, "🔍 getLastBlockCompleted() - returning=$lastBlock")

        return lastBlock
    }

    private fun showError(message: String) {
        // Implementar según tu UI
        Log.e(TAG, "Error: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🎯 ConjugaisonsFragment onResume")

        // DEBUG: Ver estado del ViewModel
        viewModel.debugFrases()

        Handler(Looper.getMainLooper()).postDelayed({
            if (frases.isNotEmpty()) {
                Log.d(TAG, "🔄 Refrescando vista con ${frases.size} frases")
                setupMacaronStack()
            }
        }, 100)
    }
    private fun cleanupInconsistentData() {
        val prefs = requireContext().getSharedPreferences("conjugaisons_progress", 0)
        val editor = prefs.edit()
        // Lista de todas las claves que podrían estar causando conflicto
        val problematicKeys = prefs.all.keys.filter { key ->
            key.contains("Présent") || key.contains("Presente") || key.contains("present")
        }

        Log.d(TAG, "🧹 Limpiando datos inconsistentes:")
        problematicKeys.forEach { key ->
            Log.d(TAG, "   • Eliminando: $key = ${prefs.all[key]}")
            editor.remove(key)
        }

        // Establecer un estado limpio
        editor.putInt("last_block_Présent", -1)  // Empezar desde -1
        editor.putInt("block_Présent_0", 0)      // Bloque 0 con progreso 0

        editor.apply()

        Log.d(TAG, "✅ Datos limpiados. Estado inicial establecido.")
    }

    private fun updateLivesLabel() {
        val current = macaronManager.currentMacaronCount

        binding.livesLabel.text = current.toString()

    }
}