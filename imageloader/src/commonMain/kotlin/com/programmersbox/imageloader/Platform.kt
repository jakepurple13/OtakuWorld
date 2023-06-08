package com.programmersbox.imageloader

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
