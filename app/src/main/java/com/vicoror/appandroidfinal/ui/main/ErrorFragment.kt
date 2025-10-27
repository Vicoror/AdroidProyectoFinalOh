package com.vicoror.appandroidfinal.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentErrorBinding
import com.vicoror.appandroidfinal.utils.NetworkChecker

private const val ARG_ERROR_MESSAGE = "error_message"

class ErrorFragment : Fragment() {

    private var _binding: FragmentErrorBinding? = null
    private val binding get() = _binding!!
    private var errorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            errorMessage = it.getString(ARG_ERROR_MESSAGE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Usar los mismos IDs de tu layout
        binding.titleNetworkDialog.text = getString(R.string.error_conexion)
        binding.messageNetworkDialog.text = errorMessage ?: getString(R.string.no_hay_internet)

        binding.btnRetry.setOnClickListener {
            if (NetworkChecker.isOnline(requireContext())) {
                // Regresar al fragment anterior si hay conexión
                parentFragmentManager.popBackStack()
            } else {
                binding.messageNetworkDialog.text = getString(R.string.con_datos)
            }
        }

        binding.btnContinue.setOnClickListener {
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(message: String) = ErrorFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ERROR_MESSAGE, message)
            }
        }
    }
}