package com.aura.gridgage

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/* Suggests power stations based on calculated load */
class RecommendationsActivity : AppCompatActivity() {

    private var selectedDevice: PowerDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations)

        val totalLoad = intent.getIntExtra("TOTAL_LOAD", 0)
        val selectedAppliances = intent.getParcelableArrayListExtra<Appliance>("SELECTED_APPLIANCES") ?: arrayListOf()
        
        // Real-world power station examples
        val allDevices = listOf(
            PowerDevice(1, "Jackery Explorer 240", 240, 200, 110),
            PowerDevice(2, "EcoFlow RIVER 2", 256, 300, 110),
            PowerDevice(3, "Anker 521 PowerHouse", 256, 200, 110),
            PowerDevice(4, "Jackery Explorer 500", 518, 500, 110),
            PowerDevice(5, "Bluetti EB55", 537, 700, 110),
            PowerDevice(6, "EcoFlow DELTA 2", 1024, 1800, 120),
            PowerDevice(7, "Jackery Explorer 1000", 1002, 1000, 110),
            PowerDevice(8, "Anker 757 PowerHouse", 1229, 1500, 110),
            PowerDevice(9, "Bluetti AC200P", 2000, 2000, 110),
            PowerDevice(10, "EcoFlow DELTA Pro", 3600, 3600, 120)
        )

        // Filter compatible devices
        val recommended = allDevices.filter { it.maxOutputW >= totalLoad }
            .sortedBy { it.maxOutputW }

        val rv = findViewById<RecyclerView>(R.id.rv_recommendations)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)
        val btnGetDevice = findViewById<Button>(R.id.btn_get_device)
        val btnMute = findViewById<ImageButton>(R.id.btn_mute)

        if (recommended.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = DeviceAdapter(recommended) { device ->
                selectedDevice = device
                findViewById<View>(R.id.card_action).visibility = View.VISIBLE
                btnGetDevice.text = "GET ${device.name.uppercase()}"
                
                // Haptic feedback or small animation could go here
                btnGetDevice.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).withEndAction {
                    btnGetDevice.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
            }
        }

        // Back button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // Home button (Clear stack and go to Main)
        btnGetDevice.setOnClickListener {
            selectedDevice?.let { device ->
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("EXTRA_DEVICE", device)
                intent.putParcelableArrayListExtra("SELECTED_APPLIANCES", selectedAppliances)
                startActivity(intent)
            }
        }

        // Music toggle
        updateMuteButton(btnMute)
        btnMute.setOnClickListener {
            SoundManager.toggleMute(this)
            updateMuteButton(btnMute)
        }
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