import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

// Native JVM splash shown before any app code runs. Nucleus adds the -splash arg for both the
// packaged app and the Gradle `run` task from nativeDistributions.splashImage.
const val DESKTOP_SPLASH_IMAGE = "splash.png"

/**
 * Renders `splash/splash.png` + `splash@2x.png` (the app icon) into a splash card (icon, [title],
 * empty progress track) in `build/appResources/common`, which must be the Nucleus
 * `appResourcesRootDir`. At runtime `NativeSplash` (kmpuiviews) animates the progress bar on top.
 */
fun Project.registerDesktopSplash(title: String) {
    val prepareSplash = tasks.register<RenderDesktopSplash>("prepareSplash") {
        this.title.set(title)
        iconDir.set(layout.projectDirectory.dir("splash"))
        outputDir.set(layout.buildDirectory.dir("appResources/common"))
    }

    tasks.named { it == "prepareAppResources" || it == "run" }.configureEach {
        dependsOn(prepareSplash)
    }
}

/**
 * Splash card layout in 1x pixels. NativeSplash.kt in kmpuiviews draws the bar at
 * [BAR_X]/[BAR_Y]/[BAR_WIDTH]/[BAR_HEIGHT]; keep the two in sync.
 */
private object SplashCard {
    const val WIDTH = 360
    const val HEIGHT = 300
    const val CORNER = 24f
    const val ICON_SIZE = 112
    const val ICON_Y = 60
    const val TITLE_BASELINE = 214
    const val TITLE_SIZE = 24f
    const val BAR_X = 32
    const val BAR_Y = 236
    const val BAR_WIDTH = 296
    const val BAR_HEIGHT = 4

    // Black card with the Material 3 baseline dark scheme.
    val surface = Color(0x000000)
    val outline = Color(0x49454F)
    val onSurface = Color(0xE6E0E9)
    val track = Color(0x4A4458)
}

abstract class RenderDesktopSplash : DefaultTask() {
    @get:Input
    abstract val title: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iconDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun render() {
        System.setProperty("java.awt.headless", "true")
        val icons = iconDir.get().asFile
        val out = outputDir.get().asFile
        listOf("splash.png" to 1, "splash@2x.png" to 2).forEach { (name, scale) ->
            val icon = ImageIO.read(icons.resolve(name))
            ImageIO.write(renderCard(icon, scale), "png", out.resolve(name))
        }
    }

    private fun renderCard(icon: BufferedImage, scale: Int): BufferedImage = with(SplashCard) {
        val image = BufferedImage(WIDTH * scale, HEIGHT * scale, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.scale(scale.toDouble(), scale.toDouble())

        val card = RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1f, HEIGHT - 1f, CORNER, CORNER)
        g.color = surface
        g.fill(card)
        g.color = outline
        g.stroke = BasicStroke(1f)
        g.draw(card)

        g.drawImage(icon, (WIDTH - ICON_SIZE) / 2, ICON_Y, ICON_SIZE, ICON_SIZE, null)

        g.color = onSurface
        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(TITLE_SIZE)
        val titleWidth = g.fontMetrics.stringWidth(title.get())
        g.drawString(title.get(), (WIDTH - titleWidth) / 2f, TITLE_BASELINE.toFloat())

        g.color = track
        g.fill(
            RoundRectangle2D.Float(
                BAR_X.toFloat(), BAR_Y.toFloat(), BAR_WIDTH.toFloat(), BAR_HEIGHT.toFloat(),
                BAR_HEIGHT.toFloat(), BAR_HEIGHT.toFloat(),
            )
        )
        g.dispose()
        image
    }
}
