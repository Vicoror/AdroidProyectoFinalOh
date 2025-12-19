package com.vicoror.appandroidfinal.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.vicoror.appandroidfinal.databinding.ActivityMainBinding
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.utils.NetworkChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var networkDialog: AlertDialog? = null
    private var isCheckingNetwork = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, 0)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment

        // INICIAR MONITOREO EN TIEMPO REAL
        startNetworkMonitoring()
    }

    private fun startNetworkMonitoring() {
        if (isCheckingNetwork) return

        isCheckingNetwork = true
        lifecycleScope.launch {
            while (isCheckingNetwork) {
                val isOnline = NetworkChecker.isOnline(this@MainActivity)

                if (!isOnline && networkDialog?.isShowing != true) {
                    // Mostrar diálogo si no hay conexión y el diálogo no está visible
                    showNetworkDialog()
                } else if (isOnline && networkDialog?.isShowing == true) {
                    // Ocultar diálogo si hay conexión y el diálogo está visible
                    networkDialog?.dismiss()
                    networkDialog = null
                }

                delay(10000)
            }
        }
    }

    private fun showNetworkDialog() {
        if (isFinishing || isDestroyed) return
        if (networkDialog?.isShowing == true) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.fragment_error, null)

        networkDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Configurar botones según tu layout
        dialogView.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            checkConnectionAndDismiss()
        }

        dialogView.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            // Ocultar diálogo pero seguir monitoreando
            networkDialog?.dismiss()
            networkDialog = null
        }

        // Actualizar mensajes según el estado actual
        updateDialogMessages()

        networkDialog?.show()
    }

    private fun checkConnectionAndDismiss() {
        if (NetworkChecker.isOnline(this)) {
            networkDialog?.dismiss()
            networkDialog = null
        } else {
            // Actualizar mensaje para indicar que sigue sin conexión
            updateDialogMessages()
        }
    }

    private fun updateDialogMessages() {
        val title = networkDialog?.findViewById<TextView>(R.id.titleNetworkDialog)
        val message = networkDialog?.findViewById<TextView>(R.id.messageNetworkDialog)

        title?.text = getString(R.string.no_hay_internet)
        message?.text = getString(R.string.con_datos)
    }

    override fun onResume() {
        super.onResume()
        // Verificar conexión inmediatamente al volver a la app
        if (!NetworkChecker.isOnline(this) && networkDialog?.isShowing != true) {
            showNetworkDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isCheckingNetwork = false
        networkDialog?.dismiss()
    }
}