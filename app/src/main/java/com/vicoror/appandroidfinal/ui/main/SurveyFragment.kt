package com.vicoror.appandroidfinal.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentSurveyBinding

class SurveyFragment : Fragment() {

    private var _binding: FragmentSurveyBinding? = null
    private val binding get() = _binding!!
    private var selectedRadioButtonId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSurveyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioButtons = listOf(
            binding.rbViajar, binding.rbEstudios, binding.rbTrabajo,
            binding.rbHobby, binding.rbExamen, binding.rbOtro
        )

        val inputOtroLayout = binding.inputOtro
        val inputOtroEdit = binding.etOtro
        val btnEnter = binding.btnEnter

        inputOtroEdit.isEnabled = false
        btnEnter.isEnabled = false
        // Color inicial opaco
        btnEnter.alpha = 0.5f

        fun actualizarBoton() {
            btnEnter.isEnabled = if (selectedRadioButtonId == binding.rbOtro.id) {
                !inputOtroEdit.text.isNullOrEmpty() && (inputOtroEdit.text?.length ?: 0) <= 30
            } else {
                selectedRadioButtonId != -1
            }
            // Cambia el alpha según esté habilitado o no
            btnEnter.alpha = if (btnEnter.isEnabled) 1.0f else 0.5f
        }

        radioButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                radioButtons.forEach { rb -> if (rb != radioButton) rb.isChecked = false }

                radioButton.isChecked = true
                selectedRadioButtonId = radioButton.id

                when (radioButton.id) {
                    binding.rbOtro.id -> {
                        inputOtroEdit.isEnabled = true
                        inputOtroLayout.visibility = TextInputLayout.VISIBLE
                    }
                    else -> {
                        inputOtroEdit.isEnabled = false
                        inputOtroLayout.visibility = TextInputLayout.GONE
                        inputOtroEdit.text?.clear()
                    }
                }
                actualizarBoton()
            }
        }

        inputOtroEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) > 30) {
                    inputOtroLayout.error = getString(R.string.max30)
                } else {
                    inputOtroLayout.error = null
                }
                actualizarBoton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnEnter.setOnClickListener {
            val seleccion = when (selectedRadioButtonId) {
                binding.rbViajar.id -> getString(R.string.opc1)
                binding.rbEstudios.id -> getString(R.string.opc2)
                binding.rbTrabajo.id -> getString(R.string.opc3)
                binding.rbHobby.id -> getString(R.string.opc5)
                binding.rbExamen.id -> getString(R.string.opc4)
                binding.rbOtro.id -> inputOtroEdit.text.toString()
                else -> ""
            }

            Toast.makeText(requireContext(),
                getString(R.string.opcion_sel, seleccion), Toast.LENGTH_SHORT).show()

            // Navegar a HomeFragment
            findNavController().navigate(R.id.action_surveyFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
