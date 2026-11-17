package com.programmersbox.kmpuiviews.domain.updatecheck

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.uc_explain_blocked
import otakuworld.kmpuiviews.generated.resources.uc_explain_not_found
import otakuworld.kmpuiviews.generated.resources.uc_explain_offline
import otakuworld.kmpuiviews.generated.resources.uc_explain_rate_limited
import otakuworld.kmpuiviews.generated.resources.uc_explain_server_error
import otakuworld.kmpuiviews.generated.resources.uc_explain_timed_out

/** Update-check failures common enough to explain with fixed text instead of AI. */
enum class KnownError(val resource: StringResource) {
    TimedOut(Res.string.uc_explain_timed_out),
    RateLimited(Res.string.uc_explain_rate_limited),
    Blocked(Res.string.uc_explain_blocked),
    NotFound(Res.string.uc_explain_not_found),
    ServerError(Res.string.uc_explain_server_error),
    Offline(Res.string.uc_explain_offline),
}

private val offlineRegex = Regex(
    "UnknownHostException|Unable to resolve host|ConnectException|NoRouteToHostException|" +
            "UnresolvedAddressException|Network is unreachable"
)

/** The known error for a failure, or null when it needs an AI explanation. Only the first detail line is matched. */
fun knownError(kind: FailureKind, httpCode: Int?, detail: String): KnownError? = when (kind) {
    FailureKind.TimedOut -> KnownError.TimedOut
    FailureKind.Http -> when (httpCode) {
        429 -> KnownError.RateLimited
        403, 503 -> KnownError.Blocked
        404, 410 -> KnownError.NotFound
        in 500..599 -> KnownError.ServerError
        else -> null
    }
    FailureKind.CouldNotRead ->
        KnownError.Offline.takeIf { offlineRegex.containsMatchIn(detail.lineSequence().firstOrNull().orEmpty()) }
}

fun ItemFailure.knownError(): KnownError? = knownError(kind, httpCode, detail)

suspend fun KnownError.explanation(sourceName: String): String = getString(resource, sourceName)
