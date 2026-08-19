package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstatdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.sharedcomponents.components.GenericBackButton
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AccountStatDetailsScreen(
    viewModel: AccountStatDetailsViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Account Stat Details") },
                navigationIcon = { GenericBackButton() }
            )
        }
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            item(contentType = "chart") {
                CircleCard(
                    circleInfo = state.circleInfo,
                    favoritesCount = state.favoritesCount,
                    modifier = Modifier.animateItem()
                )
            }

            items(
                items = state.favorites,
                key = { it.name },
                contentType = { _ -> "list" }
            ) {
                OutlinedCard {
                    ListItem(
                        headlineContent = { Text(it.name) },
                        supportingContent = {
                            AppUsageProgress(
                                totalUsageTime = state.favoritesCount,
                                timeUsage = it.count,
                                progressColor = it.color,
                                modifier = Modifier.animateItem()
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleCard(
    circleInfo: List<CircleInfo>,
    modifier: Modifier = Modifier,
    favoritesCount: Long,
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedCircle(
                    data = circleInfo,
                    modifier = Modifier.size(300.dp)
                )
                Column {
                    Text(
                        text = "Total Favorites",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "$favoritesCount favorites",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var showMore by remember { mutableStateOf(false) }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                maxLines = if (showMore) Int.MAX_VALUE else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                circleInfo.forEach {
                    CircleInformationContent(
                        circleInfo = it,
                        duration = it.key.toString()
                    )
                }
            }

            TextButton(
                onClick = { showMore = !showMore },
            ) {
                Text(if (showMore) "Show less" else "Show more")
            }
        }
    }
}

@Composable
fun CircleInformationContent(
    circleInfo: CircleInfo,
    duration: String,
) {
    MaterialTheme(
        rememberDynamicColorScheme(
            circleInfo.color,
            isDark = isSystemInDarkTheme(),
        )
    ) {
        ElevatedAssistChip(
            onClick = {},
            label = {
                Text(
                    text = circleInfo.label.orEmpty(),
                )
            },
            trailingIcon = {
                Text(text = duration)
            }
        )
    }
}

@Composable
private fun AppUsageProgress(
    totalUsageTime: Long,
    timeUsage: Long,
    usageToString: (Long) -> String = { it.toString() },
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        // Optional: Add stiffness to control the speed of the bounce
        stiffness = Spring.StiffnessLow
    ),
    progressColor: Color = MaterialTheme.colorScheme.primaryFixedDim,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        val fraction = if (totalUsageTime > 0) {
            (timeUsage.toFloat() / totalUsageTime.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        var startAnimation by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(500.milliseconds)
            startAnimation = true
        }

        // Extract the animated value so both the Box and Spacer can share it
        val animatedFraction by animateFloatAsState(
            targetValue = if (startAnimation) fraction else 0f,
            animationSpec = animationSpec,
            label = "weightAnimation"
        )

        // Calculate safe weights for both elements.
        // The spring can bounce below 0 or above 1, so both need coerceAtLeast(0.001f)
        val boxWeight = animatedFraction.coerceAtLeast(0.001f)
        val spacerWeight = (1f - animatedFraction).coerceAtLeast(0.001f)

        Box(
            modifier = Modifier
                .weight(boxWeight)
                .height(4.dp)
                .background(progressColor, CircleShape)
        )

        Text(text = usageToString(timeUsage))

        Spacer(modifier = Modifier.weight(spacerWeight))
    }
}