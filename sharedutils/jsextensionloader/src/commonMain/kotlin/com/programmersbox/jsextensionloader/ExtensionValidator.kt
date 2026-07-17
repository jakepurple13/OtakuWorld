package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import kotlinx.serialization.json.Json

private val validatorJson = Json { ignoreUnknownKeys = true }

class ExtensionValidationException(val missing: List<String>) :
    Exception("Extension is missing required function(s): ${missing.joinToString()}")

object ExtensionValidator {

    private val requiredFunctions = listOf(
        "getPopularRequest", "getPopularParse",
        "getLatestRequest", "getLatestParse",
        "searchRequest", "searchParse",
        "getDetailRequest", "getDetailParse",
        "getContentRequest", "getContentParse",
    )

    fun validate(quickJs: QuickJs): List<String> {
        val probe = requiredFunctions.joinToString(
            separator = ",",
            prefix = "JSON.stringify({",
            postfix = "})",
        ) { "\"$it\": typeof $it" }
        val resultJson = quickJs.evaluate(probe, "extension-validate.js") as String
        val types: Map<String, String> = validatorJson.decodeFromString(resultJson)
        return requiredFunctions.filter { types[it] != "function" }
    }
}
