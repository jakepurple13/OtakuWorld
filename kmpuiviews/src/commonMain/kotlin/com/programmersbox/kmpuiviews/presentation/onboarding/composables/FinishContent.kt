package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import otakuworld.kmpuiviews.generated.resources.Res

@Composable
internal fun FinishContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .size(150.dp)
        ) {
            val lottie by rememberLottieComposition {
                LottieCompositionSpec.JsonString(
                    Res.readBytes("files/successfully_done.json")
                        .decodeToString()
                )
            }

            Image(
                painter = rememberLottiePainter(
                    composition = lottie,
                    iterations = 1,
                    enableExpressions = true
                ),
                null,
                modifier = Modifier.matchParentSize()
            )
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "All done!",
                style = MaterialTheme.typography.headlineMedium,
            )

            Text("Remember you can change all of these settings at any time.")

            Text("Press finish to start using the app!")
        }
    }
}