package com.vicoror.appandroidfinal.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vicoror.appandroidfinal.data.model.ApiService
import com.vicoror.appandroidfinal.data.model.RetrofitClient
import com.vicoror.appandroidfinal.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    // Estados de login
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        object NotAuthenticated : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val apiService: ApiService = RetrofitClient.apiService

    fun login(email: String, password: String) {
        _loginState.value = LoginState.Loading
        Log.d("AuthViewModel", "Intentando login: $email / $password")

        viewModelScope.launch {
            try {
                val response = apiService.getAllUsers()
                Log.d("AuthViewModel", "Código de respuesta HTTP: ${response.code()}")

                if (response.isSuccessful) {
                    val users = response.body()
                    Log.d("AuthViewModel", "Usuarios obtenidos del servidor: $users")

                    val user = users?.find { it.correo == email && it.password == password }
                    Log.d("AuthViewModel", "Usuario encontrado: $user")

                    if (user != null) {
                        _loginState.value = LoginState.Success(user)
                        Log.d("AuthViewModel", "Login exitoso: ${user.correo}")
                    } else {
                        _loginState.value = LoginState.NotAuthenticated
                        Log.d("AuthViewModel", "Usuario no autenticado")
                    }
                } else {
                    _loginState.value = LoginState.Error("Error ${response.code()} al obtener usuarios")
                    Log.e("AuthViewModel", "Respuesta no exitosa: ${response.code()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error de conexión: ${e.message}")
                Log.e("AuthViewModel", "Excepción login: ${e.message}")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
