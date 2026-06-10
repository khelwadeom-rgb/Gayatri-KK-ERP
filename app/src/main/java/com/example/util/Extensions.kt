package com.example.util

fun Double.format(digits: Int = 2): String = String.format("%.${digits}f", this)
