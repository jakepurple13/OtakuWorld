package android.content

/**
 * Android's Application extends ContextWrapper, so extension code calling e.g.
 * `application.getSharedPreferences(...)` links against `ContextWrapper`, not `Context`.
 */
abstract class ContextWrapper : Context()
