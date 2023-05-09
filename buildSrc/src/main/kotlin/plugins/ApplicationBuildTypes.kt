package plugins

import com.android.build.gradle.internal.dsl.BuildType
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.extra

enum class ApplicationBuildTypes(
    val buildTypeName: String
) {

    Release("release") {
        override fun NamedDomainObjectContainer<BuildType>.setupBuildType(block: BuildType.() -> Unit) {
            getByName(buildTypeName) {
                isMinifyEnabled = false
                block()
            }
        }
    },
    Debug("debug") {
        override fun NamedDomainObjectContainer<BuildType>.setupBuildType(block: BuildType.() -> Unit) {
            getByName(buildTypeName) {
                extra["enableCrashlytics"] = false
                block()
            }
        }
    },
    Beta("beta") {
        override fun NamedDomainObjectContainer<BuildType>.setupBuildType(block: BuildType.() -> Unit) {
            create(buildTypeName) {
                initWith(getByName(Debug.buildTypeName))
                matchingFallbacks.addAll(values().filter { it != Beta }.map(ApplicationBuildTypes::buildTypeName))
                isDebuggable = false
                block()
            }
        }
    };

    protected abstract fun NamedDomainObjectContainer<BuildType>.setupBuildType(block: BuildType.() -> Unit)
    fun setup(container: NamedDomainObjectContainer<BuildType>, block: BuildType.() -> Unit = {}) = container.setupBuildType(block)
}