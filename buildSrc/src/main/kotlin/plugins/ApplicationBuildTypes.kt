package plugins

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.BuildType
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.extra

enum class ApplicationBuildTypes(
    val buildTypeName: String
) {

    Release("release") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            getByName(buildTypeName) {
                isMinifyEnabled = false
                block()
            }
        }
    },
    Debug("debug") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            getByName(buildTypeName) {
                extra["enableCrashlytics"] = false
                block()
            }
        }
    },
    Beta("beta") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            create(buildTypeName) {
                initWith(getByName(Debug.buildTypeName))
                matchingFallbacks.addAll(values().filter { it != Beta }.map(ApplicationBuildTypes::buildTypeName))
                if(this is ApplicationBuildType) {
                    isDebuggable = false
                }
                block()
            }
        }
    };

    protected abstract fun <T: BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit)
    fun <T: BuildType> setup(container: NamedDomainObjectContainer<T>, block: T.() -> Unit = {}) = container.setupBuildType(block)
}