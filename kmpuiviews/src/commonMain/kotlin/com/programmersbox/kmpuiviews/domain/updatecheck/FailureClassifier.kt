package com.programmersbox.kmpuiviews.domain.updatecheck

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.TimeoutCancellationException

data class FailureInfo(
    val kind: FailureKind,
    val httpCode: Int?,
    val detail: String,
)

private val httpCodeRegex = Regex("""HTTP\D{0,8}([1-5]\d{2})""", RegexOption.IGNORE_CASE)

fun classifyFailure(t: Throwable): FailureInfo {
    val chain = generateSequence(t) { it.cause }.take(10).toList()
    val detail = buildString {
        append("${t::class.simpleName}: ${t.message}")
        t.stackTraceToString().lineSequence()
            .filter { it.trimStart().startsWith("at ") }
            .take(5)
            .forEach { append("\n").append(it.trim()) }
    }
    if (chain.any { it is TimeoutCancellationException }) {
        return FailureInfo(FailureKind.TimedOut, null, detail)
    }
    val code = chain.firstNotNullOfOrNull { (it as? ResponseException)?.response?.status?.value }
        ?: chain.firstNotNullOfOrNull { e ->
            e.message?.let { httpCodeRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        }
    return if (code != null) {
        FailureInfo(FailureKind.Http, code, detail)
    } else {
        FailureInfo(FailureKind.CouldNotRead, null, detail)
    }
}
