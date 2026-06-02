package com.aura.gridgage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/* Adapter for selecting appliance quantities */
class ApplianceAdapter(
    private val appliances: List<Appliance>,
    private val onUpdate: () -> Unit
) : RecyclerView.Adapter<ApplianceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_appliance_name)
        val tvWatts: TextView = view.findViewById(R.id.tv_appliance_watts)
        val tvCount: TextView = view.findViewById(R.id.tv_count)
        val btnPlus: ImageButton = view.findViewById(R.id.btn_plus)
        val btnMinus: ImageButton = view.findViewById(R.id.btn_minus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appliance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appliances[position]
        holder.tvName.text = item.name
        holder.tvWatts.text = "${item.watts} W"
        holder.tvCount.text = item.count.toString()

        holder.btnPlus.setOnClickListener {
            item.count++
            holder.tvCount.text = item.count.toString()
            onUpdate()
        }

        holder.btnMinus.setOnClickListener {
            if (item.count > 0) {
                item.count--
                holder.tvCount.text = item.count.toString()
                onUpdate()
            }
        }
    }

    override fun getItemCount() = appliances.size
}