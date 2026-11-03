package com.programmersbox.favoritesdatabase

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Creates a database file exactly as Room would at [version] of [databaseClass], using the
 * exported schema JSON, then runs [seed] statements. Used to test real migrations.
 */
fun createDatabaseAtVersion(
    file: File,
    databaseClass: String,
    version: Int,
    seed: List<String> = emptyList(),
) {
    val schema = Json.parseToJsonElement(
        File("schemas/$databaseClass/$version.json").readText()
    ).jsonObject["database"]!!.jsonObject

    val statements = buildList {
        schema["entities"]!!.jsonArray.forEach { entity ->
            val e = entity.jsonObject
            val table = e["tableName"]!!.jsonPrimitive.content
            add(e["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            (e["indices"] as? JsonArray)?.forEach { index ->
                add(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
            (e["contentSyncTriggers"] as? JsonArray)?.forEach { add(it.jsonPrimitive.content) }
        }
        (schema["views"] as? JsonArray)?.forEach { view ->
            val v = view.jsonObject as JsonObject
            add("CREATE VIEW `${v["viewName"]!!.jsonPrimitive.content}` AS ${v["createSql"]!!.jsonPrimitive.content}")
        }
        schema["setupQueries"]!!.jsonArray.forEach { add(it.jsonPrimitive.content) }
        addAll(seed)
        add("PRAGMA user_version = $version")
    }

    BundledSQLiteDriver().open(file.absolutePath).use { connection ->
        statements.forEach { connection.execSQL(it) }
    }
}
