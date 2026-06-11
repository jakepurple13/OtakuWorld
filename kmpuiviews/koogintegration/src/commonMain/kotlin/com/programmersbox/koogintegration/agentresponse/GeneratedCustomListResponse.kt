package com.programmersbox.koogintegration.agentresponse

import ai.koog.agents.core.tools.annotations.LLMDescription
import androidx.compose.runtime.Stable
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@LLMDescription("A response containing a newly generated list of favorites matching the requested genre or description.")
@Stable
data class GeneratedCustomListResponse(
    @property:LLMDescription("A small description from the LLM. This should contain a small response of why some items were chosen.")
    val response: String,
    @property:LLMDescription("The generated name for the custom list.")
    val listName: String,
    @property:LLMDescription("The generated description for the custom list.")
    val listDescription: String,
    @property:LLMDescription("The items from the user's favorites that fit this list.")
    val items: List<CustomListInfo>,
) : AgentResponse() {

    // Helper function to easily map this response into your relational CustomList model
    fun toCustomList(): CustomList {
        // Generate a single UUID to link the parent list to its children
        val listUuid = Uuid.random().toString()

        val customListItem = CustomListItem(
            uuid = listUuid,
            name = listName,
            description = listDescription,
            // time and useBiometric use your default values
        )

        // Ensure all items are assigned the parent list's UUID
        val linkedItems = items.map { it.copy(uuid = listUuid) }

        return CustomList(
            item = customListItem,
            list = linkedItems
        )
    }
}

fun DbModel.toCustomListInfo(parentUuid: String = ""): CustomListInfo {
    return CustomListInfo(
        uniqueId = Uuid.random().toString(),
        uuid = parentUuid, // This gets overwritten by the GeneratedCustomListResponse mapper later
        title = this.title,
        description = this.description,
        url = this.url,
        imageUrl = this.imageUrl,
        source = this.source
    )
}