package android.content

import android.net.Uri

/**
 * Data holder only: desktop has no activities, so [Context.startActivity] always throws
 * [ActivityNotFoundException], as Android does when nothing handles an intent.
 */
open class Intent() : Cloneable {
    private var action: String? = null
    private var data: Uri? = null
    private var component: ComponentName? = null
    private var flags: Int = 0
    private val extras = mutableMapOf<String, Any?>()

    constructor(action: String?) : this() {
        this.action = action
    }

    constructor(action: String?, uri: Uri?) : this(action) {
        this.data = uri
    }

    fun getAction(): String? = action
    fun setAction(action: String?): Intent = apply { this.action = action }

    fun getData(): Uri? = data
    fun setData(data: Uri?): Intent = apply { this.data = data }

    fun getComponent(): ComponentName? = component
    fun setComponent(component: ComponentName?): Intent = apply { this.component = component }

    fun getFlags(): Int = flags
    fun setFlags(flags: Int): Intent = apply { this.flags = flags }
    fun addFlags(flags: Int): Intent = apply { this.flags = this.flags or flags }

    fun putExtra(name: String, value: String?): Intent = apply { extras[name] = value }
    fun putExtra(name: String, value: Long): Intent = apply { extras[name] = value }
    fun putExtra(name: String, value: Int): Intent = apply { extras[name] = value }
    fun putExtra(name: String, value: Boolean): Intent = apply { extras[name] = value }

    fun getStringExtra(name: String): String? = extras[name] as? String
    fun getLongExtra(name: String, defaultValue: Long): Long = extras[name] as? Long ?: defaultValue
    fun getIntExtra(name: String, defaultValue: Int): Int = extras[name] as? Int ?: defaultValue
    fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean = extras[name] as? Boolean ?: defaultValue

    public override fun clone(): Any = Intent(action, data).also {
        it.component = component
        it.flags = flags
        it.extras.putAll(extras)
    }

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
    }
}

open class ComponentName(private val packageName: String, private val className: String) : Cloneable, Comparable<ComponentName> {
    constructor(context: Context, className: String) : this(context.packageName, className)

    fun getPackageName(): String = packageName
    fun getClassName(): String = className

    override fun compareTo(other: ComponentName): Int =
        compareValuesBy(this, other, ComponentName::packageName, ComponentName::className)

    public override fun clone(): Any = ComponentName(packageName, className)
}

open class ActivityNotFoundException(message: String?) : RuntimeException(message) {
    constructor() : this(null)
}
