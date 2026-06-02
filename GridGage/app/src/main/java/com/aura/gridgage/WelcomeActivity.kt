package com.aura.gridgage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/* Welcome screen with animations and background */
class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Init sound
        SoundManager.init(this)
        SoundManager.startMusic()

        val btnEnter = findViewById<Button>(R.id.btn_enter)
        val btnInfo = findViewById<ImageButton>(R.id.btn_info)
        val btnMute = findViewById<ImageButton>(R.id.btn_mute)
        val appLogo = findViewById<ImageView>(R.id.app_logo)
        val welcomeLayout = findViewById<LinearLayout>(R.id.welcome_text_layout)

        // Start animations
        startWelcomeAnimations(appLogo, welcomeLayout, btnEnter)
        setupElectricityRain()

        // Start button
        btnEnter.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Info panel
        btnInfo.setOnClickListener {
            showInfoDialog()
        }

        // Music toggle
        btnMute.setOnClickListener {
            val isMuted = SoundManager.toggleMute(this)
            btnMute.setImageResource(
                if (isMuted) R.drawable.ic_mute
                else R.drawable.ic_volume_up
            )
        }
    }

    private fun startWelcomeAnimations(logo: ImageView, text: LinearLayout, button: Button) {
        // Zoom In effect for the whole text layout
        val zoomIn = ScaleAnimation(0.2f, 1.0f, 0.2f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 1500
            interpolator = android.view.animation.OvershootInterpolator()
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000
        }
        val set = AnimationSet(true).apply {
            addAnimation(zoomIn)
            addAnimation(fadeIn)
        }
        text.startAnimation(set)

        // Blink animation for "GridGage" text
        val tvGridGage = findViewById<TextView>(R.id.tv_gridgage_title)
        val blink = AlphaAnimation(1.0f, 0.2f).apply {
            duration = 1200
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
            startOffset = 1500 // Start after zoom
        }
        tvGridGage.startAnimation(blink)

        // Logo Animation: Fade in and Scale up
        val logoAnim = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 1200 })
            addAnimation(ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, 
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply { duration = 1000 })
        }
        logo.startAnimation(logoAnim)

        // Button Animation: Pulse
        val pulseAnim = ScaleAnimation(1.0f, 1.08f, 1.0f, 1.08f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 1200
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        button.startAnimation(pulseAnim)
    }

    private fun setupElectricityRain() {
        val container = findViewById<android.widget.FrameLayout>(R.id.animation_container)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val rainRunnable = object : Runnable {
            override fun run() {
                if (container.childCount < 8) {
                    createRaindrop(container)
                }
                handler.postDelayed(this, (400..800).random().toLong())
            }
        }
        handler.post(rainRunnable)
    }

    private fun createRaindrop(container: android.widget.FrameLayout) {
        val drop = ImageView(this)
        drop.setImageResource(R.drawable.ic_electricity_bolt)
        drop.alpha = 0.6f
        
        val size = (12..24).random()
        val params = android.widget.FrameLayout.LayoutParams(
            (size * resources.displayMetrics.density).toInt(),
            (size * 2 * resources.displayMetrics.density).toInt()
        )
        
        val screenWidth = resources.displayMetrics.widthPixels
        params.leftMargin = (0..screenWidth).random()
        params.topMargin = -100
        drop.layoutParams = params
        
        container.addView(drop)
        
        val duration = (2000..4000).random().toLong()
        val anim = android.view.animation.TranslateAnimation(
            0f, 0f, 0f, resources.displayMetrics.heightPixels.toFloat() + 200
        )
        anim.duration = duration
        anim.interpolator = android.view.animation.LinearInterpolator()
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                container.post { container.removeView(drop) }
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        
        drop.startAnimation(anim)
    }

    private fun showInfoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null)
        val tvContent = dialogView.findViewById<TextView>(R.id.tv_info_content)
        tvContent.text = android.text.Html.fromHtml(getString(R.string.info_content), android.text.Html.FROM_HTML_MODE_LEGACY)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btn_close_info).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        SoundManager.startMusic()
        val btnMute = findViewById<ImageButton>(R.id.btn_mute)
        btnMute.setImageResource(
            if (SoundManager.isMuted()) R.drawable.ic_mute
            else R.drawable.ic_volume_up
        )
    }
}