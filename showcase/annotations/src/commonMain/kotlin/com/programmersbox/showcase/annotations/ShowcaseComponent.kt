package com.programmersbox.showcase.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ShowcaseComponent(
    val name: String,
    val description: String,
    val group: String,
)
