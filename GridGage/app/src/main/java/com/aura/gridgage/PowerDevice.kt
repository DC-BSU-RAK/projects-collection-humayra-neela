package com.aura.gridgage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PowerDevice(
    val id: Int,
    val name: String,
    val capacityWh: Int,
    val maxOutputW: Int,
    val voltage: Int,
    val imageResId: Int = R.drawable.ic_report_image
) : Parcelable
