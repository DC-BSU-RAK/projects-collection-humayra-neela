package com.aura.gridgage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/* Adapter for power station recommendations with images */
class DeviceAdapter(
    private val devices: List<PowerDevice>,
    private val onSelect: (PowerDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var selectedPosition = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_device_name)
        val tvInfo: TextView = view.findViewById(R.id.tv_device_info)
        val tvVoltage: TextView = view.findViewById(R.id.tv_device_voltage)
        val ivImage: ImageView = view.findViewById(R.id.iv_device_image)
        val rbSelect: RadioButton = view.findViewById(R.id.rb_select)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_power_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device.name
        holder.tvInfo.text = "${device.capacityWh}Wh | ${device.maxOutputW}W Output"
        holder.tvVoltage.text = "Voltage: ${device.voltage}V"

        // Set device image
        val imageRes = getDeviceImageResource(device.name)
        holder.ivImage.setImageResource(imageRes)
        
        // Remove tint for actual product images
        holder.ivImage.imageTintList = null

        val isSelected = selectedPosition == position
        holder.rbSelect.isChecked = isSelected

        // Interaction visuals
        holder.itemView.animate()
            .scaleX(if (isSelected) 1.02f else 1.0f)
            .scaleY(if (isSelected) 1.02f else 1.0f)
            .alpha(if (isSelected || selectedPosition == -1) 1.0f else 0.7f)
            .setDuration(200)
            .start()

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = holder.adapterPosition
            if (oldPos != -1) notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            onSelect(device)
        }
    }

    private fun getDeviceImageResource(name: String): Int {
        // Map names to resource names (Android resource names are lowercase, no spaces)
        val resName = name.lowercase().replace(" ", "_")
        return try {
            val field = R.drawable::class.java.getField(resName)
            field.getInt(null)
        } catch (e: Exception) {
            // Fallback to default if not found
            R.drawable.ic_save
        }
    }

    override fun getItemCount() = devices.size
}