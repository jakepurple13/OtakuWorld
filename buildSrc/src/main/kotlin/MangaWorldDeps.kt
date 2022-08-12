object MangaWorldDeps {
    const val swiperefresh = "com.google.accompanist:accompanist-swiperefresh:${Deps.accompanist}"
    const val coilGif = "io.coil-kt:coil-gif:${Deps.coil}"

    const val subsamplingImageView = "com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0"

    const val subsamplingCompose = "xyz.quaver:subsampledimage:0.0.1-alpha22-SNAPSHOT"

    const val piasy = "1.8.1"
    val piasyLibs = arrayOf(
        "com.github.piasy:BigImageViewer:$piasy",
        "com.github.piasy:GlideImageLoader:$piasy",
        "com.github.piasy:ProgressPieIndicator:$piasy"
    )
}