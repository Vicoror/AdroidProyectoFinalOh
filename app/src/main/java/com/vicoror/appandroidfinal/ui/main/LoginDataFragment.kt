package com.vicoror.appandroidfinal.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentLoginDatosBinding
import com.vicoror.appandroidfinal.viewModel.AuthViewModel
import kotlinx.coroutines.launch

class LoginDatosFragment : Fragment() {

    private var _binding: FragmentLoginDatosBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private var hasNavigated = false // Flag para evitar navegar varias veces

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginDatosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = binding.etEmail
        val etPassword = binding.etPassword
        val btnNext = binding.btnNext

        fun mostrarMensaje(texto: String, isError: Boolean = true) {
            val color = if (isError) R.color.warningred else R.color.principal
            Snackbar.make(binding.root, texto, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .setBackgroundTint(resources.getColor(color, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        fun validarCampos(): Boolean {
            val correo = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrarMensaje(getString(R.string.valCorreo))
                return false
            }

            if (pass.length < 4) {
                mostrarMensaje(getString(R.string.valPassw))
                return false
            }
            return true
        }

        fun actualizarEstadoBoton() {
            btnNext.isEnabled =
                etEmail.text.toString().trim().isNotEmpty() &&
                        etPassword.text.toString().trim().isNotEmpty()
        }

        fun hideKeyboard() {
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }

        // Observar loginState
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginState.collect { state ->
                    if (hasNavigated) return@collect

                    when (state) {
                        is AuthViewModel.LoginState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            btnNext.isEnabled = false
                            Log.d("LoginState", "Cargando...")
                        }

                        is AuthViewModel.LoginState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            btnNext.isEnabled = true
                            mostrarMensaje("¡Bienvenid@ ${state.user.correo}!", false)
                            hideKeyboard()
                            hasNavigated = true
                            binding.root.post {
                                if (isAdded)
                                    findNavController().navigate(R.id.action_loginDatosFragment_to_homeFragment)
                            }
                        }

                        is AuthViewModel.LoginState.NotAuthenticated -> {
                            binding.progressBar.visibility = View.GONE
                            btnNext.isEnabled = true
                            mostrarMensaje("Usuario no autenticado, completa la encuesta", false)
                            hideKeyboard()
                            hasNavigated = true
                            binding.root.post {
                                if (isAdded)
                                    findNavController().navigate(R.id.action_loginDatosFragment_to_surveyFragment)
                            }
                        }

                        is AuthViewModel.LoginState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            btnNext.isEnabled = true
                            mostrarMensaje(state.message)
                        }

                        is AuthViewModel.LoginState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            btnNext.isEnabled = true
                        }
                    }
                }
            }
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = actualizarEstadoBoton()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etEmail.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)

        btnNext.setOnClickListener {
            if (validarCampos()) {
                val correo = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                authViewModel.login(correo, password)
            }
        }

        actualizarEstadoBoton()
        authViewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
