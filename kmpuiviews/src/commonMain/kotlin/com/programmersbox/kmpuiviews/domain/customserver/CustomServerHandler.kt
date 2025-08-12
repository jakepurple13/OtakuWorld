package com.programmersbox.kmpuiviews.domain.customserver

import com.programmersbox.kmpuiviews.utils.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.providers.digest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface CustomServerHandle {
    val client: HttpClient
    val appConfig: AppConfig
}

class CustomServerHandler(
    override val appConfig: AppConfig,
    override val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json)
        }

        install(Auth) {
            basic {

            }

            bearer {

            }

            digest {

            }
        }

        /*defaultRequest {
            //TODO: Make sure this can change as needed
            url("http://0.0.0.0:8080")
        }*/
    },
) : CustomServerHandle,
    FavoriteHandler by FakeFavoriteHandler(),
    ListHandler by FakeListHandler()
/*FavoriteHandler by FavoriteHandlerImpl(appConfig, client),
ListHandler by ListHandlerImpl(client)*/ {

    /*val d = client.post("http://0.0.0.0:8080/otaku/favorites") {
        contentType(ContentType.Application.Json)
    }*/
}

internal suspend fun <T> runCatchLog(defaultValue: T, block: suspend () -> T) = runCatching { block() }
    .getOrDefault(defaultValue)