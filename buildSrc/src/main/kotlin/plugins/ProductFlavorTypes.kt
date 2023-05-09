package plugins

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.LibraryProductFlavor
import com.android.build.gradle.internal.dsl.ProductFlavor
import org.gradle.api.NamedDomainObjectContainer

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
        val dimension = PRODUCT_FLAVOR_DIMENSION
    }
}

private const val PRODUCT_FLAVOR_DIMENSION = "version"
