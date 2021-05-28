package com.programmersbox.uiviews.utils

fun tryThis(block: () -> Unit) = try {
    block()
} catch (e: Exception) {
    e.printStackTrace()
}