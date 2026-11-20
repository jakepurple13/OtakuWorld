package com.programmersbox.kmpuiviews.presentation.onboarding.frame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.Onboarding
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import kotlinx.coroutines.launch
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape as Hero

/**
 * The shared story frame: progress, morphing hero, title/body, the step pager, and Back/Next.
 * The last page of [scope] is "done": Skip is hidden there and Next calls [onFinish].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StoryStepsScaffold(
    scope: OnboardingScope,
    appName: String,
    startLabel: String,
    onFinish: () -> Unit,
    onSkipConfirmed: () -> Unit,
    userScrollEnabled: Boolean = true,
    pagerState: PagerState = rememberPagerState { scope.size },
    modifier: Modifier = Modifier,
) {
    val reduced = onboardingReducedMotion()
    val animationScope = rememberCoroutineScope()
    var showSkip by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val heroSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.Dp>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val page = pagerState.currentPage
    val lastPage = scope.size - 1
    val isDone = page == lastPage
    val spec = scope[page].spec

    fun goTo(target: Int) {
        if (target !in 0..lastPage) return
        animationScope.launch {
            if (reduced) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
        }
    }

    BackHandler(enabled = page > 0) { goTo(page - 1) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> (page > 0).also { if (it) goTo(page - 1) }
                    Key.DirectionRight -> (page < lastPage).also { if (it) goTo(page + 1) }
                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .safeDrawingPadding(),
    ) {
        val heroSize = if (maxWidth >= 840.dp) 104.dp else 64.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                OnboardingProgress(
                    count = scope.size,
                    current = page,
                    modifier = Modifier.weight(1f),
                )
                if (!isDone) {
                    TextButton(
                        onClick = { showSkip = true },
                        modifier = Modifier.testTag(OnboardingTags.Skip),
                    ) { Text("Skip") }
                }
            }

            val heroHeight by animateDpAsState(
                if (spec.shape == Hero.Hidden) 0.dp else heroSize,
                if (reduced) snap() else heroSpec,
                label = "heroHeight",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Row(Modifier.height(heroHeight), verticalAlignment = Alignment.CenterVertically) {
                    HeroShape(shape = spec.shape, icon = spec.icon, size = heroSize)
                }
                AnimatedContent(
                    targetState = spec.title to spec.body,
                    transitionSpec = {
                        // Content still crossfades under reduced motion.
                        fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
                    },
                    label = "stepText",
                ) { (title, body) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        if (body.isNotBlank()) {
                            Text(
                                body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            Onboarding(
                onboardingScope = scope,
                state = pagerState,
                userScrollEnabled = userScrollEnabled,
                modifier = Modifier.weight(1f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                TextButton(
                    onClick = { goTo(page - 1) },
                    enabled = page > 0,
                    modifier = Modifier.testTag(OnboardingTags.Back),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Text("Back", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = { if (isDone) onFinish() else goTo(page + 1) },
                    modifier = Modifier
                        .animateContentSize()
                        .testTag(OnboardingTags.Next),
                ) {
                    val label = when {
                        isDone -> startLabel
                        page == 0 -> "Let's go"
                        else -> "Next"
                    }
                    AnimatedContent(
                        targetState = label,
                        transitionSpec = {
                            if (reduced) EnterTransition.None togetherWith ExitTransition.None
                            else fadeIn() togetherWith fadeOut()
                        },
                        label = "nextLabel",
                    ) { Text(it) }
                }
            }
        }
    }

    if (showSkip) {
        SkipSetupDialog(
            appName = appName,
            onKeepGoing = { showSkip = false },
            onSkip = {
                showSkip = false
                onSkipConfirmed()
            },
        )
    }
}
