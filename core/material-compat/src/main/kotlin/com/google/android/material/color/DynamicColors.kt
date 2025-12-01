package com.google.android.material.color

import android.os.Build

@Suppress("unused")
object DynamicColors {
    @JvmStatic
    fun isDynamicColorAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
