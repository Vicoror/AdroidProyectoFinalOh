package com.vicoror.appandroidfinal.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.data.model.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Cambia ViewModel por AndroidViewModel
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: UserResponse) : LoginState()  // HomeFragment
        object NeedsSurvey : LoginState()  // SurveyFragment (usuario nuevo)
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val auth: FirebaseAuth = Firebase.auth

    // Contexto de la aplicación
    private val appContext = application.applicationContext

    init {
        checkCurrentUser()  // ← Esto se ejecuta cuando se crea el ViewModel
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser  // ← Verifica si hay sesión guardada
        if (currentUser != null) {
            // Si hay usuario, navega directo al Home
            val user = UserResponse(currentUser)
            _loginState.value = LoginState.Success(user)
        }
    }

    /**
     * LÓGICA SIMPLIFICADA:
     * 1. Intenta login
     * 2. Si falla -> Registro automático (sin verificar tipo de error)
     */
    fun loginOrRegister(email: String, password: String) {
        // Validaciones
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error(appContext.getString(R.string.email_y_contrase_a_requeridos))
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = LoginState.Error(appContext.getString(R.string.formato_de_email_inv_lido))
            return
        }

        if (password.length < 6) {
            _loginState.value = LoginState.Error(appContext.getString(R.string.la_contrase_a_debe_tener_al_menos_6_caracteres))
            return
        }

        _loginState.value = LoginState.Loading
        Log.d("AuthViewModel", "Procesando: $email")

        viewModelScope.launch {
            // PRIMERO: Intentar LOGIN
            val loginSuccess = attemptLogin(email, password)

            if (!loginSuccess) {
                // Si login falló, intentar REGISTRO
                attemptRegister(email, password)
            }
        }
    }

    /**
     * Intenta hacer login
     * @return true si fue exitoso, false si falló
     */
    private suspend fun attemptLogin(email: String, password: String): Boolean {
        return try {
            Log.d("AuthViewModel", "Intentando login...")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()

            if (authResult.user != null) {
                // LOGIN EXITOSO - Usuario existe
                val user = UserResponse(authResult.user!!)
                Log.d("AuthViewModel", "✓ Login exitoso. Usuario existente")
                _loginState.value = LoginState.Success(user)
                true
            } else {
                false
            }

        } catch (e: Exception) {
            Log.d("AuthViewModel", "✗ Login falló: ${e.message}")
            false  // Retorna false para intentar registro
        }
    }

    /**
     * Intenta registrar usuario nuevo
     */
    private suspend fun attemptRegister(email: String, password: String) {
        try {
            Log.d("AuthViewModel", "Intentando registro automático...")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()

            if (authResult.user != null) {
                // REGISTRO EXITOSO - Usuario nuevo
                val user = UserResponse(authResult.user!!)
                Log.d("AuthViewModel", "✓ Registro automático exitoso")
                _loginState.value = LoginState.NeedsSurvey  // Va a encuesta
            }

        } catch (e: Exception) {
            Log.e("AuthViewModel", "✗ Registro falló: ${e.message}")

            // Analizar error específico del registro
            val errorMsg = when {
                e is FirebaseAuthUserCollisionException ->
                    appContext.getString(R.string.el_email_ya_est_registrado_colisi_n_inesperada)

                e.message?.contains("email already in use") == true ->
                    appContext.getString(R.string.el_email_ya_est_registrado_intenta_recuperar_tu_contrase_a)

                e.message?.contains("weak password") == true ->
                    appContext.getString(R.string.la_contrase_a_es_muy_d_bil_usa_al_menos_6_caracteres)

                e.message?.contains("invalid email") == true ->
                    appContext.getString(R.string.formato_de_email_inv_lido)

                else -> {
                    // Si llegamos aquí, es un usuario existente con contraseña incorrecta
                    appContext.getString(R.string.credenciales_incorrectas_verifica_tu_email_y_contrase_a)
                }
            }

            _loginState.value = LoginState.Error(errorMsg)
        }
    }

    /**
     * Versión ALTERNATIVA más simple: Siempre intenta registro primero
     * Si falla por "email ya existe", entonces intenta login
     */
    fun loginOrRegisterV2(email: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                // 1. PRIMERO intentar REGISTRO (más probable para usuario nuevo)
                Log.d("AuthViewModel", "Intentando registro primero...")
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                if (authResult.user != null) {
                    // REGISTRO EXITOSO - Usuario NUEVO
                    val user = UserResponse(authResult.user!!)
                    Log.d("AuthViewModel", "✓ Usuario nuevo registrado")
                    _loginState.value = LoginState.NeedsSurvey  // Va a encuesta
                }

            } catch (registerException: Exception) {
                // Si el registro falla, probablemente porque el usuario YA EXISTE
                Log.d("AuthViewModel", "Registro falló: ${registerException.message}")

                // Verificar si es error de "email ya existe"
                val isEmailAlreadyExists = when {
                    registerException is FirebaseAuthUserCollisionException -> true
                    registerException.message?.contains("email already in use") == true -> true
                    else -> false
                }

                if (isEmailAlreadyExists) {
                    // Usuario EXISTE -> Intentar LOGIN
                    Log.d("AuthViewModel", "Usuario existe, intentando login...")

                    try {
                        val loginResult = auth.signInWithEmailAndPassword(email, password).await()

                        if (loginResult.user != null) {
                            // LOGIN EXITOSO - Usuario EXISTENTE
                            val user = UserResponse(loginResult.user!!)
                            Log.d("AuthViewModel", "✓ Login exitoso para usuario existente")
                            _loginState.value = LoginState.Success(user)  // Va a home
                        }

                    } catch (loginException: Exception) {
                        // Error en login (contraseña incorrecta)
                        Log.e("AuthViewModel", "Login falló: ${loginException.message}")
                        _loginState.value = LoginState.Error(
                            appContext.getString(R.string.credenciales_incorrectas_verifica_tu_email_y_contrase_a)
                        )
                    }

                } else {
                    // Otro error en registro
                    val errorMsg = when {
                        registerException.message?.contains("weak password") == true ->
                            appContext.getString(R.string.la_contrase_a_es_muy_d_bil_usa_al_menos_6_caracteres)
                        else ->
                            appContext.getString(R.string.error_desconocido_intenta_nuevamente)
                    }
                    _loginState.value = LoginState.Error(errorMsg)
                }
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}