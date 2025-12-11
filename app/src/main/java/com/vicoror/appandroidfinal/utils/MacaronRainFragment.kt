package com.vicoror.appandroidfinal.utils

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.vicoror.appandroidfinal.R
import kotlin.random.Random

class MacaronRainFragment : Fragment() {

    // Lista de drawables de macarons
    private val macaronDrawables = listOf(
        R.drawable.macaron_8cd5c9,
        R.drawable.macaron_b3f8bc,
        R.drawable.macaron_c5a4da,
        R.drawable.macaron_d4e9f8,
        R.drawable.macaron_ff8a80,
        R.drawable.macaron_ffc278,
        R.drawable.macaron_fffa9c,
        R.drawable.macaron
    )

    private lateinit var container: ViewGroup
    private lateinit var textLabel: TextView
    private var handler: Handler? = null
    private val macaronViews = mutableListOf<ImageView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el layout XML
        return inflater.inflate(R.layout.fragment_macaron_rain, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar vistas desde XML
        container = view.findViewById(R.id.container)
        textLabel = view.findViewById(R.id.textLabel)

        // 2. Configurar el texto
        setupTextLabel()

        // 3. Iniciar animaciones DESPUÉS de que la vista esté medida
        view.post {
            // 4. Iniciar lluvia de macarons
            startMacaronRain()

            // 5. Animar texto
            startTextAnimation()
        }
    }

    private fun setupTextLabel() {
        // Configurar propiedades del texto
        textLabel.apply {
            // Hacer visible
            visibility = View.VISIBLE
            alpha = 0f // Comienza invisible

            // Fuente
            try {
                typeface = resources.getFont(R.font.poetsen_one_regular)
            } catch (e: Exception) {
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // Color
            val principalLight = try {
                ContextCompat.getColor(requireContext(), R.color.principallight)
            } catch (e: Exception) {
                Color.parseColor("#D9ACFF")
            }
            setTextColor(principalLight)

            // Sombra
            setShadowLayer(6f, 2f, 3f, Color.argb(77, 0, 0, 0))

            // Traer al frente
            bringToFront()
        }
    }

    private fun startTextAnimation() {
        // Animación de aparición
        textLabel.animate()
            .alpha(1f)
            .setDuration(1200)
            .start()

        // Animación de pulso
        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        val animator = android.animation.ValueAnimator.ofFloat(1.0f, 1.08f)
        animator.duration = 1200
        animator.repeatCount = android.animation.ValueAnimator.INFINITE
        animator.repeatMode = android.animation.ValueAnimator.REVERSE

        animator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            textLabel.scaleX = scale
            textLabel.scaleY = scale
        }

        animator.start()
    }

    private fun startMacaronRain() {
        // Limpiar macarons anteriores si los hay
        clearMacarons()

        // Crear macarons iniciales
        repeat(15) {
            createMacaron()
        }

        // Programar creación continua
        handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                createMacaron()
                handler?.postDelayed(this, 250) // Cada 250ms
            }
        }
        handler?.post(runnable)

        // Auto-detener después de 6 segundos
        handler?.postDelayed({
            stopMacaronRain()
        }, 6000)
    }

    private fun createMacaron() {
        val drawableRes = macaronDrawables.random()

        val imageView = ImageView(requireContext()).apply {
            // Configurar para vector drawables
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }

            // Establecer imagen
            setImageResource(drawableRes)

            // Tamaño aleatorio
            val size = Random.nextInt(50, 80)
            layoutParams = ViewGroup.LayoutParams(size, size)

            // Posición inicial (arriba, posición X aleatoria)
            val startX = Random.nextFloat() * (container.width - size)
            x = startX
            y = -size.toFloat()

            // Configuración de imagen
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true

            // Guardar referencia
            macaronViews.add(this)
        }

        // Agregar a la vista
        container.addView(imageView)

        // Animar caída
        animateMacaron(imageView)

        // Asegurar que texto esté al frente
        textLabel.bringToFront()
    }

    private fun animateMacaron(imageView: ImageView) {
        // Duración aleatoria para variedad
        val duration = Random.nextLong(4000, 7000)

        // Posición final (abajo de la pantalla)
        val finalY = container.height.toFloat() + imageView.height

        // Animación de caída con rotación
        imageView.animate()
            .translationY(finalY)
            .rotation(Random.nextFloat() * 360f) // Rotación aleatoria
            .setDuration(duration)
            .setInterpolator(android.view.animation.LinearInterpolator())
            .withEndAction {
                // Remover de la vista cuando termine
                container.removeView(imageView)
                macaronViews.remove(imageView)
            }
            .start()
    }

    private fun clearMacarons() {
        // Remover todos los macarons de la vista
        macaronViews.forEach { view ->
            container.removeView(view)
        }
        macaronViews.clear()
    }

    private fun stopMacaronRain() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMacaronRain()
        clearMacarons()
    }
}