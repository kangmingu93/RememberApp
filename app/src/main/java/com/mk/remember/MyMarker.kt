package com.mk.remember

import android.graphics.Bitmap

data class MyMarker (
    val bitmap: Bitmap,
    val fileName: String,
    val lat: Double,
    val lng: Double
)