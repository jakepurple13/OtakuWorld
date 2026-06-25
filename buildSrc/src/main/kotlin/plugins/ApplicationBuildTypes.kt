package plugins

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.BuildType
import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.extra

enum class ApplicationBuildTypes(
    val buildTypeName: String
) {

    Release("release") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            getByName(buildTypeName) {
                isMinifyEnabled = false
                isShrinkResources = false
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
                matchingFallbacks.addAll(listOf(Release.buildTypeName, Debug.buildTypeName))
                if(this is ApplicationBuildType) {
                    isDebuggable = false
                    isShrinkResources = false
                    isMinifyEnabled = false
                }
                block()
            }
        }
    },
    ReleaseMinified("releaseMinified") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            create(buildTypeName) {
                isMinifyEnabled = true
                isShrinkResources = true
                matchingFallbacks.add(Release.buildTypeName)

                if (this is ApplicationBuildType) {
                    isDebuggable = false
                }

                block()
            }
        }
    },

    BetaMinified("betaMinified") {
        override fun <T : BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit) {
            create(buildTypeName) {
                isMinifyEnabled = true
                isShrinkResources = true
                matchingFallbacks.addAll(listOf(Release.buildTypeName, Debug.buildTypeName))
                if (this is ApplicationBuildType) {
                    isDebuggable = false
                }
                // Note: signingConfigs is likely out-of-scope here!
                // We'll rely on the `block()` to configure this from the consumer side.

                block()
            }
        }
    }
    ;


    protected abstract fun <T: BuildType> NamedDomainObjectContainer<T>.setupBuildType(block: T.() -> Unit)
    fun <T: BuildType> setup(container: NamedDomainObjectContainer<T>, block: T.() -> Unit = {}) = container.setupBuildType(block)
}