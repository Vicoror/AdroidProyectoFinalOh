package com.vicoror.appandroidfinal.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.vicoror.appandroidfinal.R
import com.vicoror.appandroidfinal.databinding.FragmentLoginBinding
import com.vicoror.appandroidfinal.BuildConfig


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var auth: FirebaseAuth

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 1001

    // Facebook Login
    private lateinit var callbackManager: CallbackManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        // Configurar Google Sign-In
        setupGoogleSignIn()

        // Configurar Facebook Login
        //setupFacebookLogin()
        setupTerminosTextView()
        // Botón login normal (email/password)
        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_loginDatosFragment)
        }

        // Botón Google
        binding.google.setOnClickListener {
            signInWithGoogle()
        }

        // Botón Facebook
      /*  binding.facebook.setOnClickListener {
            signInWithFacebook()
        }*/

        // Verificar si ya hay usuario logueado
        checkCurrentUser()
    }

    /**
     * Configurar Google Sign-In
     */

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    /**
     * Configurar Facebook Login
     */
   /* private fun setupFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d("FacebookLogin", "Facebook login success")
                    handleFacebookAccessToken(loginResult.accessToken)
                }

                override fun onCancel() {
                    Log.d("FacebookLogin", "Facebook login canceled")
                }

                override fun onError(error: FacebookException) {
                    Log.e("FacebookLogin", "Facebook login error", error)
                }
            })
    }*/

    /**
     * Iniciar sesión con Google
     */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    /**
     * Iniciar sesión con Facebook
     */
    private fun signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )
    }

    /**
     * Manejar resultado de login de Google y Facebook
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook
       // callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google sign in failed", e)
            }
        }
    }

    /**
     * Autenticar con Firebase usando credenciales de Google
     */
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleSignIn", "signInWithCredential:success")
                    val user = auth.currentUser
                    checkIfNewUser(user)
                } else {
                    Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                }
            }
    }

    /**
     * Autenticar con Firebase usando credenciales de Facebook
     */
   /* private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("FacebookLogin", "signInWithCredential:success")
                    val user = auth.currentUser
                    checkIfNewUser(user)
                } else {
                    Log.w("FacebookLogin", "signInWithCredential:failure", task.exception)
                }
            }
    }*/

    /**
     * Verificar si ya hay usuario logueado
     */
    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya logueado → HomeFragment (ya pasó por encuesta)
            Log.d("LoginFragment", "Usuario ya logueado, yendo a Home")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    /**
     * Verificar si el usuario es NUEVO o EXISTENTE
     */
    private fun checkIfNewUser(user: FirebaseUser?) {
        if (user == null) return

        // Obtener metadata del usuario
        val metadata = user.metadata

        // Verificar si es usuario NUEVO (primer login)
        // creationTimestamp == lastSignInTimestamp significa que es la primera vez
        val isNewUser = metadata?.creationTimestamp == metadata?.lastSignInTimestamp

        if (isNewUser) {
            // Usuario NUEVO → SurveyFragment
            Log.d("LoginFragment", "🔵 Usuario NUEVO detectado, yendo a Survey")
            findNavController().navigate(R.id.action_loginFragment_to_surveyFragment)
        } else {
            // ✅ Usuario EXISTENTE → HomeFragment
            Log.d("LoginFragment", "🟢 Usuario EXISTENTE, yendo a Home")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    private fun setupTerminosTextView() {
        val tvTerminos = binding.tvTerminos
        val textoCompleto = getString(R.string.tvTerminos)
        val terminosText = "Términos de servicio"

        // Encontrar posición de "Términos de servicio"
        val startIndex = textoCompleto.indexOf(terminosText)
        val endIndex = startIndex + terminosText.length

        if (startIndex != -1) {
            val spannable = SpannableString(textoCompleto)

            // 1. SUBRAYAR
            spannable.setSpan(UnderlineSpan(), startIndex, endIndex, 0)

            // 2. HACER CLICKABLE (¡IMPORTANTE! usa ClickableSpan)
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        mostrarModalTerminos()  // Esto SÍ se ejecutará
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        // Cambiar color para indicar que es clickable
                        ds.color = ContextCompat.getColor(requireContext(), R.color.principallight)
                        ds.isUnderlineText = true  // Mantener subrayado
                    }
                },
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 3. Asignar al TextView
            tvTerminos.text = spannable
            tvTerminos.movementMethod = LinkMovementMethod.getInstance()  // ¡IMPORTANTE!

        } else {
            // Fallback
            val styledText = Html.fromHtml(getString(R.string.tvTerminos), Html.FROM_HTML_MODE_COMPACT)
            tvTerminos.text = styledText
            tvTerminos.setOnClickListener {
                mostrarModalTerminos()
            }
        }
    }

    private fun mostrarModalTerminos() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.modal_terminos, null)

        // ✅ ASIGNAR EL TEXTO (esto es lo que falta)
        val tvContenido = view.findViewById<TextView>(R.id.tvContenido)
        val textoHtml = Html.fromHtml(getString(R.string.terminos_contenido), Html.FROM_HTML_MODE_COMPACT)
        tvContenido.text = textoHtml  // ¡IMPORTANTE!
        tvContenido.movementMethod = LinkMovementMethod.getInstance()

        dialog.setContentView(view)
        dialog.show()

        // Ajustar altura
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
        dialog.behavior.isDraggable = true
    }

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}