package com.programmersbox.jsextensionloader

object SemVerCompare {

    fun isNewer(currentVersion: String, candidateVersion: String): Boolean = try {
        val current = currentVersion.split(".").map { it.trim().toInt() }
        val candidate = candidateVersion.split(".").map { it.trim().toInt() }
        val length = maxOf(current.size, candidate.size)
        var result = false
        for (i in 0 until length) {
            val c = current.getOrElse(i) { 0 }
            val n = candidate.getOrElse(i) { 0 }
            if (n > c) {
                result = true
                break
            }
            if (n < c) {
                result = false
                break
            }
        }
        result
    } catch (e: NumberFormatException) {
        false
    }
}
