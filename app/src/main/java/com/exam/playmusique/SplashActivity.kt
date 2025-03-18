package com.exam.playmusique

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.exam.playmusique.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Appliquer l’animation de pulsation à l’icône
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_pulse)
        binding.splashIcon.startAnimation(pulseAnimation)

        // Animation du fond (bleu ciel → vert moyen)
        binding.splashRoot.animate()
            .setDuration(2000) // 2 secondes
            .withStartAction { binding.splashRoot.setBackgroundColor(getColor(R.color.splash_background_start)) }
            .withEndAction { binding.splashRoot.setBackgroundColor(getColor(R.color.splash_background_end)) }

        // Passer à MainActivity après 2 secondes
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}