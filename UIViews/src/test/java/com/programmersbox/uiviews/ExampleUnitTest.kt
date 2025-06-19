package com.programmersbox.uiviews

import com.programmersbox.models.ApiService
import com.programmersbox.models.InfoModel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() = runBlocking {

        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        val timeNormal = measureNanoTime {
            val f = list.map { "#$it" }
            println(f)
        }

        println(timeNormal)

        val timeFast = measureNanoTime {
            val f = list.map { "#$it" }
            println(f)
        }

        println(timeFast)

    }
}

object TestItems {

    val TEST_SOURCE = object : ApiService {
        override val baseUrl: String get() = ""
    }

    val TEST_INFOMODEL = InfoModel(
        title = "Hello",
        description = "Hello World".repeat(50),
        url = "",
        imageUrl = "",
        chapters = emptyList(),
        genres = listOf("Comedy", "Fantasy", "SciFi"),
        alternativeNames = emptyList(),
        source = TEST_SOURCE
    )

}

/*
@ExperimentalMaterialApi
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light Header")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Header")
@Composable
fun PreviewHeader() {

    MaterialTheme {

        var b by remember { mutableStateOf(false) }

        DetailsHeader(
            model = TestItems.TEST_INFOMODEL,
            logo = MainLogo(R.drawable.baseline_list_black_18dp),
            swatchInfo = TestItems.TEST_SWATCH,
            isFavorite = b
        ) { b = it }

    }

}*/
