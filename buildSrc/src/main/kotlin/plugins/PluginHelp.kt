package plugins

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.LibraryProductFlavor
import com.android.build.gradle.internal.dsl.ProductFlavor
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType

internal val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun DependencyHandlerScope.implementation(dependency: Any) = add("implementation", dependency)

enum class ProductFlavorTypes(
    val nameType: String
) {
    NoFirebase("noFirebase"),
    Full("full");

    operator fun invoke(receiver: NamedDomainObjectContainer<ProductFlavor>, block: ProductFlavor.() -> Unit = {}) {
        with(receiver) {
            create(nameType) {
                dimension = "version"
                block()
            }
        }
    }

    fun librarySetup(receiver: NamedDomainObjectContainer<LibraryProductFlavor>, block: LibraryProductFlavor.() -> Unit = {}) {
        with(receiver) {
            create(nameType) {
                dimension = "version"
                block()
            }
        }
    }

    fun applicationSetup(receiver: NamedDomainObjectContainer<ApplicationProductFlavor>, block: ApplicationProductFlavor.() -> Unit = {}) {
        with(receiver) {
            create(nameType) {
                dimension = "version"
                block()
            }
        }
    }

    companion object {
        val dimension = "version"
    }
}
