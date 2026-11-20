package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.NativeCatalogs
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** One "Where extensions come from" row. */
data class CatalogInfo(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val builtIn: Boolean,
)

private val OtakuWorldRow = CatalogInfo(Icons.Default.AutoAwesome, "OtakuWorld catalog", "Built in, ready to browse", true)
private val CustomRow = CatalogInfo(Icons.Default.Power, "Custom repos", "Paste a repo link", false)

/**
 * Maps registered catalog [names] to the spec's copy: the built-in catalog first, one row per recognised
 * catalog (unknown names get a generic row with their own name), and "Custom repos" only with a custom bridge.
 */
fun onboardingCatalogs(names: List<String>, hasCustomBridge: Boolean): List<CatalogInfo> {
    val mapped = names.map { name ->
        val key = name.lowercase()
        when {
            "mihon" in key || "tachiyomi" in key ->
                CatalogInfo(Icons.AutoMirrored.Filled.MenuBook, "Mihon & Tachiyomi repos", "Most manga extensions work here", false)

            "aniyomi" in key -> CatalogInfo(Icons.Default.PlayCircle, "Aniyomi repos", "Anime extensions", false)
            "cloudstream" in key -> CatalogInfo(Icons.Default.Cloud, "CloudStream", ".cs3 plugins", false)
            else -> CatalogInfo(Icons.Default.Extension, name, "Extensions", false)
        }
    }.distinctBy { it.title }
    return listOf(OtakuWorldRow) + mapped + listOfNotNull(CustomRow.takeIf { hasCustomBridge })
}

/** The catalogs this app registers, driven by the same sources [ExtensionListViewModel][com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionListViewModel] uses. */
@Composable
fun rememberOnboardingCatalogs(): List<CatalogInfo> {
    val nativeCatalogs = koinInject<NativeCatalogs>()
    val otakuWorldCatalog = koinInject<OtakuWorldCatalog>()
    val sources by koinInject<SourceRepository>().sources.collectAsState()
    val hasCustomBridge = nativeCatalogs.catalogs.isNotEmpty() ||
        sources.any { it.catalog is KmpExternalCustomApiServicesCatalog && it.name == "Custom Tachiyomi Bridge" }
    val names = nativeCatalogs.catalogs.map { it.name } +
        sources.map { it.catalog }
            .filterIsInstance<KmpExternalApiServicesCatalog>()
            .filter { it.hasRemoteSources }
            .map { it.name }
    return remember(names, hasCustomBridge) {
        onboardingCatalogs(names.filter { it != otakuWorldCatalog.name }.distinct(), hasCustomBridge)
    }
}

private val HowItWorks = listOf(
    "Find" to "Settings › Sources & Extensions › Get extensions lists what's available.",
    "Install" to "Tap Install on one. Android may ask once to allow installs from this app.",
    "Browse" to "Its titles show up in Recent, All and Search. Switch sources from the source picker.",
)

/** Step 4: explain only. No buttons; the catalog list is [catalogs]. */
fun OnboardingScope.sourcesStep(appName: String, catalogs: List<CatalogInfo>) {
    val spec = OnboardingStepSpec(
        id = "sources",
        title = "Plug in sources",
        body = "$appName doesn't come with any titles. Extensions connect it to sites that have them, and you choose which to add.",
        icon = Icons.Default.Power,
        shape = HeroShape.Arch,
    )
    step(spec) { SourcesContent(spec, catalogs) }
}

@Composable
private fun SourcesContent(spec: OnboardingStepSpec, catalogs: List<CatalogInfo>) {
    val reduced = onboardingReducedMotion()
    var current by remember { mutableIntStateOf(0) }
    LaunchedEffect(reduced) {
        if (!reduced) {
            while (true) {
                delay(1800)
                current = (current + 1) % HowItWorks.size
            }
        }
    }

    Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
        StepLayout(spec) {
            SectionLabel("How it works")
            HowItWorks.forEachIndexed { index, (title, sub) ->
                HowRow(index + 1, title, sub, active = index == current, reduced = reduced)
            }

            SectionLabel("Where extensions come from")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    catalogs.forEach { CatalogRow(it) }
                }
            }

            Text(
                "You'll add extensions after setup, from Settings › Sources & Extensions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HowRow(number: Int, title: String, sub: String, active: Boolean, reduced: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val rowTarget = if (active) scheme.secondaryContainer else scheme.surfaceContainer
    val badgeTarget = if (active) scheme.primary else scheme.surfaceContainerHighest
    val cornerTarget = if (active) 16.dp else 8.dp
    val rowAnimated by animateColorAsState(rowTarget)
    val badgeAnimated by animateColorAsState(badgeTarget)
    val cornerAnimated by animateDpAsState(cornerTarget)
    val rowColor = if (reduced) rowTarget else rowAnimated
    val badgeColor = if (reduced) badgeTarget else badgeAnimated
    val corner = if (reduced) cornerTarget else cornerAnimated

    Surface(shape = RoundedCornerShape(16.dp), color = rowColor, modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(corner), color = badgeColor, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) scheme.onPrimary else scheme.onSurface,
                    )
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CatalogRow(info: CatalogInfo) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(info.icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(info.title, style = MaterialTheme.typography.titleSmall)
            Text(
                info.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (info.builtIn) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    "Built in",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
