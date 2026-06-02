package com.aura.gridgage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/* Appliance data model */
@Parcelize
data class Appliance(
    val name: String,
    val watts: Int,
    var count: Int = 0
) : Parcelable
