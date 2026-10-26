package androidx.fragment.app

/**
 * Only reachable through [FragmentActivity.getSupportFragmentManager], which desktop never gets to call.
 * The SDK's FragmentResultOwner interface is left out: no extension references it.
 */
abstract class FragmentManager {
    abstract fun isDestroyed(): Boolean
    abstract fun findFragmentByTag(tag: String?): Fragment?
    abstract fun beginTransaction(): FragmentTransaction
}

/** Only reachable through [FragmentManager.beginTransaction]. */
abstract class FragmentTransaction {
    abstract fun add(fragment: Fragment, tag: String?): FragmentTransaction
    abstract fun remove(fragment: Fragment): FragmentTransaction
    abstract fun commitAllowingStateLoss(): Int
}
