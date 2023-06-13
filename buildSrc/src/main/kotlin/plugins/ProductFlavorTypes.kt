package plugins

import com.android.build.api.dsl.ProductFlavor
import org.gradle.api.NamedDomainObjectContainer

enum class ProductFlavorTypes(
    val nameType: String
) {
    NoFirebase("noFirebase"),
    Full("full");

    operator fun <T: ProductFlavor> invoke(receiver: NamedDomainObjectContainer<T>, block: T.() -> Unit = {}) {
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
