package com.programmersbox.animeworldtv.compose

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.programmersbox.anime_sources.Sources
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import java.lang.reflect.Type

@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    OtakuMaterialTheme(navController = navController) {
        NavHost(
            navController = navController,
            startDestination = Screen.MainScreen()
        ) {
            composable(Screen.MainScreen()) { MainView() }
            composable(Screen.DetailScreen() + "/{model}") { DetailView() }
        }
    }
}

sealed class Screen(protected val route: String) {

    object MainScreen : Screen("main")
    object DetailScreen : Screen("detail") {
        fun navigateToDetails(navController: NavController, model: ItemModel) = navController.navigate(
            route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
        ) { launchSingleTop = true }

    }

    operator fun invoke() = route
}

class ApiServiceSerializer : JsonSerializer<ApiService> {
    override fun serialize(src: ApiService, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src.serviceName)
    }
}

class ApiServiceDeserializer : JsonDeserializer<ApiService> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ApiService? {
        return try {
            Sources.valueOf(json.asString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}