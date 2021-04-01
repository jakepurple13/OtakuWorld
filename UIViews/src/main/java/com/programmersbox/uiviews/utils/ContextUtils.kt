package com.programmersbox.uiviews.utils

import android.content.Context
import com.google.gson.*
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.uiviews.GenericInfo
import java.lang.reflect.Type

var Context.currentService: ApiService? by sharedPrefObjectDelegate(null)

class ChapterModelSerializer : JsonSerializer<ChapterModel> {
    override fun serialize(src: ChapterModel, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val json = JsonObject()
        json.addProperty("name", src.name)
        json.addProperty("uploaded", src.uploaded)
        json.addProperty("source", src.source.serviceName)
        json.addProperty("url", src.url)
        return json
    }
}

class ChapterModelDeserializer(private val genericInfo: GenericInfo) : JsonDeserializer<ChapterModel> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChapterModel? {
        return json.asJsonObject.let {
            genericInfo.toSource(it["source"].asString)?.let { it1 ->
                ChapterModel(
                    name = it["name"].asString,
                    uploaded = it["uploaded"].asString,
                    source = it1,
                    url = it["url"].asString
                )
            }
        }
    }
}