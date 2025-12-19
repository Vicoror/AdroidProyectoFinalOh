package com.vicoror.appandroidfinal.ui.conjugaisons

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentMenuConjugaisonsBinding
import com.vicoror.appandroidfinal.databinding.ItemTenseBinding


class MenuConjugaisonsFragment : Fragment() {

    private var _binding: FragmentMenuConjugaisonsBinding? = null
    private val binding get() = _binding!!

    private val options = listOf(
        "Présent",
        "Passé composé",
        "Futur simple",
        "Imparfait"
        // "Plus-que-parfait (PRO)",
        // "Conditionnel présent (PRO)",
        // "Conditionnel passé (PRO)",
        // "Subjonctif (PRO)"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuConjugaisonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

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

    private fun setupRecyclerView() {
        binding.rvTenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTenses.adapter = TensesAdapter(options) { selectedTense, position ->
            onTenseSelected(selectedTense, position)
        }
        binding.rvTenses.setHasFixedSize(true)
    }

    private fun onTenseSelected(selectedTense: String, position: Int) {
        // Animación de selección
        val view = binding.rvTenses.layoutManager?.findViewByPosition(position)
        view?.let {
            ObjectAnimator.ofFloat(it, "scaleX", 0.96f, 1.0f).apply {
                duration = 150
                interpolator = AccelerateDecelerateInterpolator()
            }.start()

            ObjectAnimator.ofFloat(it, "scaleY", 0.96f, 1.0f).apply {
                duration = 150
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }

        // Si es opción PRO
        if (selectedTense.contains("(PRO)")) {
            showProModal()
            return
        }

        // Mapear a nombre de archivo/colección
        val jsonName = when (selectedTense) {
            "Présent" -> "Presente"
            "Passé composé" -> "PasseCompose"
            "Futur simple" -> "Futur"
            "Imparfait" -> "Imparfait"
            else -> ""
        }

        // Navegar a ConjugaisonsFragment
        val action = MenuConjugaisonsFragmentDirections.actionMenuConjugaisonsFragmentToConjugaisonsFragment(
            selectedTense = selectedTense,
            jsonFileName = jsonName
        )
        findNavController().navigate(action)
    }

    private fun showProModal() {
        // Implementar modal para funciones PRO
        // TODO: Mostrar diálogo o bottom sheet
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter para la lista de tiempos verbales
class TensesAdapter(
    private val tenses: List<String>,
    private val onItemClick: (String, Int) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<TensesAdapter.TenseViewHolder>() {

    class TenseViewHolder(
        private val binding: ItemTenseBinding,
        private val onItemClick: (String, Int) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(tense: String, position: Int) {
            binding.tvTenseName.text = tense

            // Configurar fondo degradado
            if (tense.contains("(PRO)")) {
                setGoldGradient()
            } else {
                setGreenGradient()
            }

            binding.root.setOnClickListener {
                onItemClick(tense, position)
            }
        }

        private fun setGreenGradient() {
            val greenColor = ContextCompat.getColor(binding.root.context, R.color.warningblue)
            binding.root.background = createGradientDrawable(greenColor)
        }

        private fun setGoldGradient() {
            val goldColor = ContextCompat.getColor(binding.root.context, R.color.pro)
            binding.root.background = createGradientDrawable(goldColor)
        }

        private fun createGradientDrawable(startColor: Int): GradientDrawable {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.orientation = GradientDrawable.Orientation.TL_BR
            gradientDrawable.colors = intArrayOf(startColor, Color.WHITE)
            gradientDrawable.cornerRadius = 32f
            return gradientDrawable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TenseViewHolder {
        val binding = ItemTenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TenseViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: TenseViewHolder, position: Int) {
        holder.bind(tenses[position], position)
    }

    override fun getItemCount(): Int = tenses.size
}