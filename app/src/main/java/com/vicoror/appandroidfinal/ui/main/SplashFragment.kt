package com.vicoror.appandroidfinal.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentSplashBinding


class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivLogo.animate().setDuration(1500).rotation(360f).start()

        binding.root.postDelayed({
            checkAuthAndNavigate()
        }, 2000)
    }

    private fun checkAuthAndNavigate() {
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null && isAdded) {
            // USUARIO YA LOGUEADO → Ir directo a HOME
            findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
        } else if (isAdded) {
            // No logueado → Ir a LOGIN
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}