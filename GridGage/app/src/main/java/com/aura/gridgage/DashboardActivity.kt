package com.aura.gridgage

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Locale

/* Advanced Power Station Dashboard - EcoFlow Inspired Visuals */
class DashboardActivity : AppCompatActivity() {

    private lateinit var device: PowerDevice
    private lateinit var appliances: MutableList<Appliance>
    
    private var isConnectedToMainline = false
    private var batteryLevel = 100.0
    private var chargeLimitWatts = 0
    private var isAcOutputOn = true
    private var isSystemOn = true

    // UI Elements
    private lateinit var tvInputWatts: TextView
    private lateinit var tvOutputWatts: TextView
    private lateinit var tvBatteryPercent: TextView
    private lateinit var tvRuntime: TextView
    private lateinit var tvChargeLimitValue: TextView
    private lateinit var tvAcToggleLabel: TextView
    private lateinit var tvInputLabel: TextView
    private lateinit var tvOverloadAlert: TextView
    
    private lateinit var btnPowerToggle: MaterialButton
    private lateinit var btnManageAppliances: MaterialButton
    private lateinit var cardInputAc: View
    private lateinit var ivInputAc: ImageView
    private lateinit var cardOutput: View
    private lateinit var btnToggleMainline: MaterialButton
    private lateinit var chargeLimitContainer: View
    private lateinit var sbMainlineLimit: SeekBar
    private lateinit var inputControlPanel: View
    private lateinit var batteryWaveFill: View
    private lateinit var batteryWaveContainer: View
    
    private lateinit var lineInput: View
    private lateinit var lineOutput: View
    private lateinit var dotInput: View
    private lateinit var dotOutput: View

    private var lastBatteryUpdateMillis = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            simulatePowerDynamics()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        device = intent.getParcelableExtra("EXTRA_DEVICE") ?: return
        val selected = intent.getParcelableArrayListExtra<Appliance>("SELECTED_APPLIANCES") ?: mutableListOf()
        
        appliances = getMasterApplianceList()
        selected.forEach { sel ->
            appliances.find { it.name == sel.name }?.count = sel.count
        }

        initViews()
        setupListeners()
        startAnimations()
        
        updateUI()
        handler.post(updateRunnable)
    }

    private fun initViews() {
        findViewById<TextView>(R.id.tv_device_name).text = device.name
        
        tvInputWatts = findViewById(R.id.tv_input_watts)
        tvOutputWatts = findViewById(R.id.tv_output_watts)
        tvBatteryPercent = findViewById(R.id.tv_battery_percent)
        tvRuntime = findViewById(R.id.tv_runtime)
        tvChargeLimitValue = findViewById(R.id.tv_charge_limit_value)
        tvAcToggleLabel = findViewById(R.id.tv_ac_toggle_label)
        tvInputLabel = findViewById(R.id.tv_input_label)
        tvOverloadAlert = findViewById(R.id.tv_overload_alert)
        
        btnPowerToggle = findViewById(R.id.btn_power_toggle)
        btnManageAppliances = findViewById(R.id.btn_manage_appliances)

        cardInputAc = findViewById(R.id.card_input_ac)
        ivInputAc = findViewById(R.id.iv_input_ac)
        cardOutput = findViewById(R.id.card_output)
        inputControlPanel = findViewById(R.id.input_control_panel)
        btnToggleMainline = findViewById(R.id.btn_toggle_mainline)
        chargeLimitContainer = findViewById(R.id.charge_limit_container)
        sbMainlineLimit = findViewById(R.id.sb_mainline_limit)
        batteryWaveFill = findViewById(R.id.battery_wave_fill)
        batteryWaveContainer = findViewById(R.id.battery_wave_container)
        
        lineInput = findViewById(R.id.line_input)
        lineOutput = findViewById(R.id.line_output)
        dotInput = findViewById(R.id.dot_input)
        dotOutput = findViewById(R.id.dot_output)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        
        val btnMute = findViewById<ImageButton>(R.id.btn_mute_dash)
        updateMuteButton(btnMute)
        btnMute.setOnClickListener {
            SoundManager.toggleMute(this)
            updateMuteButton(btnMute)
        }

        btnPowerToggle.setOnClickListener {
            isSystemOn = !isSystemOn
            updateUI()
        }

        cardInputAc.setOnClickListener {
            if (isSystemOn) {
                inputControlPanel.visibility = if (inputControlPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }

        cardOutput.setOnClickListener {
            if (isSystemOn) {
                isAcOutputOn = !isAcOutputOn
                updateUI()
            }
        }

        btnManageAppliances.setOnClickListener {
            if (isSystemOn) {
                showManageAppliancesDialog()
            }
        }

        btnToggleMainline.setOnClickListener {
            if (isSystemOn) {
                isConnectedToMainline = !isConnectedToMainline
                if (!isConnectedToMainline) {
                    chargeLimitWatts = 0
                    sbMainlineLimit.progress = 0
                    tvChargeLimitValue.text = "Off"
                }
                updateUI()
            }
        }

        sbMainlineLimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isConnectedToMainline) return
                
                if (progress == 0) {
                    chargeLimitWatts = 0
                    tvChargeLimitValue.text = "Off"
                    tvChargeLimitValue.setTextColor(Color.parseColor("#88FFFFFF"))
                } else {
                    chargeLimitWatts = progress + 500
                    tvChargeLimitValue.text = "${chargeLimitWatts}W"
                    tvChargeLimitValue.setTextColor(Color.parseColor("#2DDE98"))
                }
                updateUI()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateUI() {
        val currentOutput = if (isSystemOn && isAcOutputOn) appliances.sumOf { it.watts * it.count } else 0
        val isOverloaded = currentOutput > device.maxOutputW
        
        // Power Button Visuals
        btnPowerToggle.iconTint = ColorStateList.valueOf(
            Color.parseColor(if (isSystemOn) "#FF3B30" else "#44FFFFFF")
        )
        findViewById<View>(R.id.battery_section).alpha = if (isSystemOn) 1.0f else 0.4f
        
        // Output Update
        tvOutputWatts.text = "${currentOutput}W"
        tvOutputWatts.setTextColor(if (isOverloaded) Color.parseColor("#FF3B30") else Color.WHITE)
        
        tvAcToggleLabel.text = if (isAcOutputOn) "ON" else "OFF"
        tvAcToggleLabel.setTextColor(Color.parseColor(
            if (!isSystemOn) "#44FFFFFF" 
            else if (isAcOutputOn) "#2DDE98" 
            else "#FF3B30"
        ))

        // Input Update
        tvInputWatts.text = "${chargeLimitWatts}W"
        tvInputWatts.setTextColor(if (chargeLimitWatts > 0) Color.parseColor("#2DDE98") else Color.WHITE)
        
        tvInputLabel.text = if (isConnectedToMainline) "CONNECTED" else "TAP TO CONNECT"
        tvInputLabel.setTextColor(if (isConnectedToMainline) Color.parseColor("#2DDE98") else Color.parseColor("#88FFFFFF"))
        cardInputAc.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(if (isConnectedToMainline) "#2DDE98" else "#1AFFFFFF")
        )
        ivInputAc.imageTintList = ColorStateList.valueOf(
            Color.parseColor(if (isConnectedToMainline) "#000000" else "#FFFFFF")
        )

        // Overload Logic
        if (isOverloaded && isSystemOn) {
            tvOverloadAlert.visibility = View.VISIBLE
            tvRuntime.text = "OVERLOAD"
            tvRuntime.setTextColor(Color.RED)
        } else if (!isSystemOn) {
            tvOverloadAlert.visibility = View.GONE
            tvRuntime.text = "SYSTEM OFF"
            tvRuntime.setTextColor(Color.GRAY)
        } else {
            tvOverloadAlert.visibility = View.GONE
            calculateRuntime(chargeLimitWatts, currentOutput)
        }

        // Mainline Button Visuals
        btnToggleMainline.text = if (isConnectedToMainline) "DISCONNECT FROM MAINLINE" else "CONNECT TO MAINLINE"
        btnToggleMainline.strokeColor = ColorStateList.valueOf(Color.parseColor(if (isConnectedToMainline) "#FF3B30" else "#2DDE98"))
        
        chargeLimitContainer.alpha = if (isConnectedToMainline) 1.0f else 0.5f
        sbMainlineLimit.isEnabled = isConnectedToMainline
        sbMainlineLimit.alpha = if (isConnectedToMainline) 1.0f else 0.5f

        // Flow Animations
        updateFlowAnimations(chargeLimitWatts, currentOutput)
        
        // Controls state
        btnManageAppliances.alpha = if (isSystemOn) 1.0f else 0.5f
    }

    private fun calculateRuntime(input: Int, output: Int) {
        val net = input - output
        if (net > 0) {
            val hoursToFull = ((100 - batteryLevel) * device.capacityWh / 100.0) / net
            if (batteryLevel >= 100) {
                tvRuntime.setTextColor(Color.BLACK)
                tvRuntime.text = "Fully Charged"
            } else {
                val h = hoursToFull.toInt()
                val m = ((hoursToFull - h) * 60).toInt()
                tvRuntime.setTextColor(Color.BLACK)
                tvRuntime.text = String.format(Locale.US, "Charging: %02dh %02dm", h, m)
            }
        } else if (net < 0) {
            val hoursRemaining = (batteryLevel * device.capacityWh / 100.0) / (-net)
            val h = hoursRemaining.toInt()
            val m = ((hoursRemaining - h) * 60).toInt()
            tvRuntime.setTextColor(Color.BLACK)
            tvRuntime.text = if (h >= 99) "99h 59m+" else String.format(Locale.US, "%02dh %02dm left", h, m)
        } else {
            tvRuntime.setTextColor(Color.BLACK)
            tvRuntime.text = if (chargeLimitWatts > 0) "Stable (Bypass)" else "99h 59m"
        }
    }

    private fun updateFlowAnimations(input: Int, output: Int) {
        // Input Flow (Top to Battery)
        if (input > 0 && isSystemOn) {
            dotInput.visibility = View.VISIBLE
            animateDot(dotInput, lineInput, true)
        } else {
            dotInput.visibility = View.GONE
            dotInput.clearAnimation()
        }

        // Output Flow (Battery to Bottom)
        if (output > 0 && isSystemOn && isAcOutputOn) {
            dotOutput.visibility = View.VISIBLE
            animateDot(dotOutput, lineOutput, true)
        } else {
            dotOutput.visibility = View.GONE
            dotOutput.clearAnimation()
        }
    }

    private fun animateDot(dot: View, line: View, downwards: Boolean) {
        if (dot.animation != null) return
        line.post {
            val distance = line.height.toFloat()
            if (distance <= 0) return@post
            
            // Move from top of line to bottom of line
            val anim = TranslateAnimation(
                0f, 0f, 
                -distance / 2, 
                distance / 2
            )
            anim.duration = 800
            anim.repeatCount = Animation.INFINITE
            anim.interpolator = LinearInterpolator()
            dot.startAnimation(anim)
        }
    }

    private fun simulatePowerDynamics() {
        if (!isSystemOn) return

        val now = System.currentTimeMillis()
        val currentOutput = if (isAcOutputOn) appliances.sumOf { it.watts * it.count } else 0
        val isOverloaded = currentOutput > device.maxOutputW
        val netPower = chargeLimitWatts - currentOutput

        // Logic: 1% change at a time based on specific intervals
        var intervalMs = 10000L // Default 10s for 1% drop
        if (isOverloaded) intervalMs = 2000L // 2s if overloaded
        
        if (chargeLimitWatts > 0) {
            // Charging speed depends on input (500W to 18000W)
            intervalMs = (5000000 / chargeLimitWatts).toLong().coerceIn(1000L, 10000L)
        }

        if (now - lastBatteryUpdateMillis >= intervalMs) {
            if (netPower > 0 && batteryLevel < 100.0) {
                batteryLevel += 1.0
                lastBatteryUpdateMillis = now
            } else if (netPower < 0 && batteryLevel > 0.0) {
                batteryLevel -= 1.0
                lastBatteryUpdateMillis = now
            }
        }

        if (batteryLevel > 100.0) batteryLevel = 100.0
        if (batteryLevel < 0.0) {
            batteryLevel = 0.0
            isAcOutputOn = false
        }

        tvBatteryPercent.text = batteryLevel.toInt().toString()
        
        // Sync wave animation height and color
        syncWaveHeight()
        
        updateUI()
    }

    private fun syncWaveHeight() {
        batteryWaveContainer.post {
            val totalHeight = batteryWaveContainer.height.toFloat()
            val fillHeight = (batteryLevel.toFloat() / 100f) * totalHeight
            val translationY = totalHeight - fillHeight
            
            // Update color based on level
            val color = when {
                batteryLevel <= 20.0 -> Color.parseColor("#FF3B30") // Red
                batteryLevel <= 45.0 -> Color.parseColor("#F5D142") // Light Yellow
                else -> Color.parseColor("#2DDE98") // Green
            }
            batteryWaveFill.setBackgroundColor(color)

            batteryWaveFill.animate()
                .translationY(translationY)
                .setDuration(500)
                .start()
            
            // Wavy liquid effect
            if (isSystemOn && (chargeLimitWatts > 0 || appliances.any { it.count > 0 && isAcOutputOn })) {
                if (batteryWaveFill.animation == null) {
                    val waveAnim = TranslateAnimation(-20f, 20f, 0f, 0f)
                    waveAnim.duration = 1200
                    waveAnim.repeatMode = Animation.REVERSE
                    waveAnim.repeatCount = Animation.INFINITE
                    waveAnim.interpolator = LinearInterpolator()
                    batteryWaveFill.startAnimation(waveAnim)
                }
            } else {
                batteryWaveFill.clearAnimation()
            }
        }
    }

    private fun startAnimations() {
        val glow = findViewById<View>(R.id.battery_glow)
        val anim = AlphaAnimation(0.2f, 0.5f)
        anim.duration = 2000
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        glow.startAnimation(anim)
        
        // Ensure glow respects system state
        handler.post(object : Runnable {
            override fun run() {
                glow.visibility = if (isSystemOn) View.VISIBLE else View.INVISIBLE
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun showManageAppliancesDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_appliances, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_manage_appliances)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ApplianceAdapter(appliances) { updateUI() }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .create()
        dialogView.findViewById<Button>(R.id.btn_done_manage).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun getMasterApplianceList(): MutableList<Appliance> {
        return mutableListOf(
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
    }

    private fun updateMuteButton(btn: ImageButton) {
        btn.setImageResource(if (SoundManager.isMuted()) R.drawable.ic_mute else R.drawable.ic_volume_up)
    }

    override fun onResume() {
        super.onResume()
        SoundManager.startMusic()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
}