package com.programmersbox.kmpuiviews.tabs

/**
 * An app singleton that hands state to the next screen, such as the chapter a reader opens.
 * [TabsController] saves one copy per tab and restores it when the tab becomes active, so a parked
 * reader tab reads its own chapter. Register each one in Koin as a [TabHandoff].
 */
interface TabHandoff {
    fun capture(): Any?
    fun restore(state: Any?)
}
