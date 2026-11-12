package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.navigation3.runtime.NavKey
import java.lang.reflect.Modifier

actual fun createNavKey(fqcn: String, args: List<NavArg>): NavKey? = runCatching {
    val cls = Class.forName(fqcn)
    cls.declaredFields
        .firstOrNull { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
        ?.let { return@runCatching it.get(null) as? NavKey }

    val ctor = cls.declaredConstructors.firstOrNull { !it.isSynthetic && it.parameterCount == args.size }
        ?: return@runCatching null
    val values = args.zip(ctor.parameterTypes).map { (arg, type) ->
        if (arg.nullable) null else sampleValue(type) ?: return@runCatching null
    }
    ctor.isAccessible = true
    ctor.newInstance(*values.toTypedArray()) as? NavKey
}.getOrNull()

private fun sampleValue(type: Class<*>): Any? = when (type) {
    String::class.java -> "sample"
    java.lang.Long.TYPE, java.lang.Long::class.java -> 1L
    java.lang.Integer.TYPE, java.lang.Integer::class.java -> 1
    java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> false
    java.lang.Float.TYPE, java.lang.Float::class.java -> 1f
    java.lang.Double.TYPE, java.lang.Double::class.java -> 1.0
    else -> null
}
