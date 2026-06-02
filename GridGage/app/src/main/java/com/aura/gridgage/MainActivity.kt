package com.aura.gridgage

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/* Home screen: Select appliances and calculate load */
class MainActivity : AppCompatActivity() {

    private val appliances = listOf(
        Appliance("Ceiling Fan", 75),
        Appliance("LED Bulb", 12),
        Appliance("Desktop PC", 300),
        Appliance("Laptop", 65),
        Appliance("Television", 150),
        Appliance("Refrigerator", 250),
        Appliance("Air Conditioner", 1500),
        Appliance("Smartphone", 15),
        Appliance("Microwave", 1200),
        Appliance("Coffee Maker", 800)
    )

    private lateinit var tvTotalWatts: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalWatts = findViewById(R.id.tv_total_watts)
        val rvAppliances = findViewById<RecyclerView>(R.id.rv_appliances)
        val btnFind = findViewById<Button>(R.id.btn_find_stations)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_welcome)
        val btnMute = findViewById<ImageButton>(R.id.btn_mute)

        // Setup appliance list
        rvAppliances.layoutManager = LinearLayoutManager(this)
        rvAppliances.adapter = ApplianceAdapter(appliances) {
            updateTotalLoad()
        }

        // Navigate back to welcome
        btnBack.setOnClickListener {
            finish()
        }

        // Find stations
        btnFind.setOnClickListener {
            val total = appliances.sumOf { it.watts * it.count }
            val intent = Intent(this, RecommendationsActivity::class.java)
            intent.putExtra("TOTAL_LOAD", total)
            intent.putParcelableArrayListExtra("SELECTED_APPLIANCES", ArrayList(appliances.filter { it.count > 0 }))
            startActivity(intent)
        }

        // Music toggle
        updateMuteButton(btnMute)
        btnMute.setOnClickListener {
            SoundManager.toggleMute(this)
            updateMuteButton(btnMute)
        }

        updateTotalLoad()
        setupElectricityRain()
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
        val drop = android.widget.ImageView(this)
        drop.setImageResource(R.drawable.ic_electricity_drop)
        drop.alpha = 0.4f
        
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

    private fun updateTotalLoad() {
        val total = appliances.sumOf { it.watts * it.count }
        tvTotalWatts.text = "$total W"
    }

    private fun updateMuteButton(btn: ImageButton) {
        btn.setImageResource(
            if (SoundManager.isMuted()) R.drawable.ic_mute
            else R.drawable.ic_volume_up
        )
    }

    override fun onResume() {
        super.onResume()
        SoundManager.startMusic()
        val btnMute = findViewById<ImageButton>(R.id.btn_mute)
        updateMuteButton(btnMute)
    }
}