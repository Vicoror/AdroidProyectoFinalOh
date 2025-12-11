package com.vicoror.appandroidfinal.data.model

// MANTENGO TU ESTRUCTURA EXACTA, solo agrego constructor alternativo
data class UserResponse(
    val id: Int,
    val correo: String,
    val password: String
) {
    // Constructor alternativo desde FirebaseUser
    constructor(firebaseUser: com.google.firebase.auth.FirebaseUser) : this(
        id = 0, // Firebase usa String UID, pero mantengo Int para compatibilidad
        correo = firebaseUser.email ?: "",
        password = "" // No almacenamos contraseña
    )
}