package com.vicoror.appandroidfinal.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vicoror.appandroidfinal.databinding.FragmentMacaronManagerBinding
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

// ==================== CLASE MACARON MANAGER ====================

class MacaronManager(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MacaronManager? = null

        fun getInstance(context: Context): MacaronManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MacaronManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        // 🔥 CONSTANTES DEFINIDAS AQUÍ
        private const val PREF_NAME = "macaron_prefs"
        private const val KEY_MACARON_COUNT = "macaron_count"
        private const val KEY_MACARON_DEPLETED_AT = "macaron_depleted_at"
        private const val KEY_LAST_RECOVERY_MESSAGE = "last_recovery_message_date"
        private const val KEY_INITIAL_SETUP = "initial_setup_done"
        private const val KEY_LAST_FULL_RECOVERY = "last_full_recovery_date"

        // Constantes públicas
        const val MAX_MACARONS = 5
        const val RECOVERY_HOURS = 12L
        const val RECOVERY_MESSAGE_COOLDOWN_HOURS = 24L

        private const val TAG = "MacaronManager"
    }

    private val prefs: SharedPreferences
    private var _currentMacaronCount = 0

    init {
        val appContext = context.applicationContext
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        setupInitialMacarons()
        _currentMacaronCount = loadFromPrefs()
    }

    private fun setupInitialMacarons() {
        val alreadySetup = prefs.getBoolean(KEY_INITIAL_SETUP, false)

        if (!alreadySetup) {
            prefs.edit()
                .putBoolean(KEY_INITIAL_SETUP, true)
                .putInt(KEY_MACARON_COUNT, MAX_MACARONS)
                .apply()
            Log.d(TAG, "Macarones iniciales configurados: $MAX_MACARONS")
        }
    }

    // En MacaronManager.kt
    fun showRecoveryAlertDialog(context: Context) {
        val timeLeft = timeUntilRecovery() ?: return

        val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60

        AlertDialog.Builder(context)
            .setTitle("⏰ Plus de Macarons")
            .setMessage("Vous retrouverez 5 macarons dans ${hours}h ${minutes}m")
            .setPositiveButton("D'accord") { dialog, _ ->
                dialog.dismiss()
                markRecoveryMessageShown()
            }
            .setCancelable(false)
            .show()
    }

    var currentMacaronCount: Int
        get() = _currentMacaronCount
        set(value) {
            _currentMacaronCount = value
            saveToPrefs(value)  // 🔥 Guardar en SharedPreferences
            Log.d(TAG, "Macarones actualizados: $value")
            notifyMacaronCountChanged()
        }

    private fun loadFromPrefs(): Int {
        return prefs.getInt(KEY_MACARON_COUNT, MAX_MACARONS)
    }

    private fun saveToPrefs(value: Int) {
        prefs.edit().putInt(KEY_MACARON_COUNT, value).apply()
    }

    suspend fun consumeMacaron(): Boolean {
        // 1. Restaurar si es necesario
        restoreMacaronsIfNeeded()

        // 2. Verificar que hay macarones
        if (_currentMacaronCount <= 0) {
            Log.w(TAG, "Intento de consumir sin macarones")
            return false
        }

        // 3. Guardar valor anterior para log
        val previous = _currentMacaronCount

        // 4. Descontar SOLO 1
        _currentMacaronCount = previous - 1
        saveToPrefs(_currentMacaronCount)

        // 5. Si se agotaron, guardar timestamp
        if (_currentMacaronCount == 0) {
            macaronDepletedAt = Date()
        }

        Log.d(TAG, "🍪 Consumido: $previous → $_currentMacaronCount")
        return true
    }

    private var macaronDepletedAt: Date?
        get() {
            val timestamp = prefs.getLong(KEY_MACARON_DEPLETED_AT, -1)
            return if (timestamp != -1L) Date(timestamp) else null
        }
        set(value) {
            val timestamp = value?.time ?: -1L
            prefs.edit().putLong(KEY_MACARON_DEPLETED_AT, timestamp).apply()
        }

    fun restoreMacaronsIfNeeded(forceRestore: Boolean = false): Boolean {
        if (forceRestore || shouldRestoreAllMacarons()) {
            currentMacaronCount = MAX_MACARONS
            saveLastFullRecoveryDate()
            macaronDepletedAt = null
            Log.d(TAG, "Macarones restaurados a: $MAX_MACARONS")
            return true
        }
        return false
    }

    private fun shouldRestoreAllMacarons(): Boolean {
        if (currentMacaronCount > 0 && currentMacaronCount < MAX_MACARONS) {
            val lastRecovery = getLastFullRecoveryDate()
            if (lastRecovery != null) {
                val hoursSinceLastRecovery =
                    (Date().time - lastRecovery.time) / (1000 * 60 * 60)
                return hoursSinceLastRecovery >= RECOVERY_HOURS
            }
            return false
        }

        if (currentMacaronCount == 0) {
            val depletedAt = macaronDepletedAt ?: return false
            val hoursSinceDepletion =
                (Date().time - depletedAt.time) / (1000 * 60 * 60)
            return hoursSinceDepletion >= RECOVERY_HOURS
        }

        return false
    }

    fun timeUntilRecovery(): Long? {
        if (currentMacaronCount > 0 && currentMacaronCount < MAX_MACARONS) {
            val lastRecovery = getLastFullRecoveryDate()
            if (lastRecovery != null) {
                val nextRecoveryTime = lastRecovery.time + (RECOVERY_HOURS * 60 * 60 * 1000)
                return maxOf(0, nextRecoveryTime - Date().time)
            }
        }

        if (currentMacaronCount == 0) {
            val depletedAt = macaronDepletedAt ?: return null
            val recoveryTime = depletedAt.time + (RECOVERY_HOURS * 60 * 60 * 1000)
            return maxOf(0, recoveryTime - Date().time)
        }

        return null
    }

    fun shouldShowRecoveryMessage(): Boolean {
        if (currentMacaronCount > 0) return false

        val lastMessageDate = getLastRecoveryMessageDate()
        val now = Date()

        return if (lastMessageDate != null) {
            val hoursSinceLastMessage =
                (now.time - lastMessageDate.time) / (1000 * 60 * 60)
            hoursSinceLastMessage >= RECOVERY_MESSAGE_COOLDOWN_HOURS
        } else {
            true
        }
    }

    fun markRecoveryMessageShown() {
        prefs.edit()
            .putLong(KEY_LAST_RECOVERY_MESSAGE, Date().time)
            .apply()
    }

    fun canPlay(): Boolean {
        restoreMacaronsIfNeeded()
        return currentMacaronCount > 0
    }

    fun getRecoveryMessage(): String {
        return if (currentMacaronCount == 0) {
            val timeLeft = timeUntilRecovery() ?: return "Sin macarones"
            val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
            "Plus de macarons! Recharge dans ${hours}h ${minutes}m"
        } else {
            "Vous avez $currentMacaronCount macarons"
        }
    }

    private fun saveLastFullRecoveryDate(date: Date = Date()) {
        prefs.edit()
            .putLong(KEY_LAST_FULL_RECOVERY, date.time)
            .apply()
    }

    private fun getLastFullRecoveryDate(): Date? {
        val timestamp = prefs.getLong(KEY_LAST_FULL_RECOVERY, -1)
        return if (timestamp != -1L) Date(timestamp) else null
    }

    private fun getLastRecoveryMessageDate(): Date? {
        val timestamp = prefs.getLong(KEY_LAST_RECOVERY_MESSAGE, -1)
        return if (timestamp != -1L) Date(timestamp) else null
    }

    private fun notifyMacaronCountChanged() {
        Log.d(TAG, "Contador de macarones cambiado: $currentMacaronCount")
    }
}

// ==================== FRAGMENT ====================

class MacaronManagerFragment : Fragment() {

    private var _binding: FragmentMacaronManagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var macaronManager: MacaronManager
    private var recoveryTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMacaronManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        macaronManager = MacaronManager.getInstance(requireContext())
        setupUI()
        updateUI()
        startRecoveryTimer()
    }

    private fun setupUI() {
        binding.btnConsumeMacaron.setOnClickListener {
            lifecycleScope.launch {
                val consumed = macaronManager.consumeMacaron()
                if (consumed) {
                    Log.d("MacaronManager", "Macarrón consumido. Restantes: ${macaronManager.currentMacaronCount}")
                    updateUI()
                } else {
                    Log.d("MacaronManager", "No hay macarrones disponibles")
                }
            }
        }

        binding.btnRestoreMacarons.setOnClickListener {
            lifecycleScope.launch {
                macaronManager.restoreMacaronsIfNeeded(forceRestore = true)
                updateUI()
            }
        }
    }

    private fun updateUI() {
        val currentCount = macaronManager.currentMacaronCount
        val canPlay = macaronManager.canPlay()

        binding.tvMacaronCount.text = "Macarons: $currentCount/${MacaronManager.MAX_MACARONS}"

        binding.tvMacaronStatus.text = if (canPlay) {
            "✅ Puedes jugar"
        } else {
            "⏳ Esperando recuperación"
        }

        if (macaronManager.shouldShowRecoveryMessage()) {
            showRecoveryMessage()
        }

        updateTimeRemaining()

        binding.btnConsumeMacaron.isEnabled = canPlay
        binding.btnConsumeMacaron.alpha = if (canPlay) 1.0f else 0.5f
    }

    private fun updateTimeRemaining() {
        val timeLeft = macaronManager.timeUntilRecovery()

        if (timeLeft != null && timeLeft > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60

            binding.tvTimeRemaining.text =
                "Tiempo restante: ${hours}h ${minutes}m ${seconds}s"
            binding.tvTimeRemaining.visibility = View.VISIBLE
        } else {
            binding.tvTimeRemaining.visibility = View.GONE
        }
    }

    private fun showRecoveryMessage() {
        val message = macaronManager.getRecoveryMessage()
        binding.tvRecoveryMessage.text = message
        binding.tvRecoveryMessage.visibility = View.VISIBLE
        macaronManager.markRecoveryMessageShown()
    }

    private fun startRecoveryTimer() {
        recoveryTimer?.cancel()

        recoveryTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimeRemaining()
            }

            override fun onFinish() {
                // No debería llegar aquí
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recoveryTimer?.cancel()
        recoveryTimer = null
        _binding = null
    }
}