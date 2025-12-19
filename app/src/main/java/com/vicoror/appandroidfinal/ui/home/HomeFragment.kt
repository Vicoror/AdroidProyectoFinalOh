package com.vicoror.appandroidfinal.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // 👉 MODO FRASES
        binding.btnfrases.setOnClickListener {
            val bundle = Bundle().apply {
                putString("mode", "phrases")
            }
            findNavController().navigate(
                R.id.action_homeFragment_to_phrasesFragment,
                bundle
            )
        }

        // 👉 MODO VERBES
        binding.btnVerbes.setOnClickListener {
            val bundle = Bundle().apply {
                putString("mode", "verbs")
            }
            findNavController().navigate(
                R.id.action_homeFragment_to_phrasesFragment,
                bundle
            )
        }

        binding.btnConjug.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_menuConjugaisonsFragment
            )
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}