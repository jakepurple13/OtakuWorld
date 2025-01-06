package com.programmersbox.uiviews.utils

import androidx.lifecycle.ViewModel
import com.programmersbox.sharedutils.FirebaseConnection
import com.programmersbox.sharedutils.FirebaseDb

class FireListenerClosable(
    private val fireListener: FirebaseConnection.FirebaseListener = FirebaseDb.FirebaseListener(),
) : AutoCloseable, FirebaseConnection.FirebaseListener by fireListener {
    override fun close() {
        unregister()
    }
}

fun ViewModel.fireListener(
    key: String = "default",
    itemListener: FirebaseConnection.FirebaseListener = FirebaseDb.FirebaseListener(),
): FireListenerClosable {
    val fireListener = FireListenerClosable(itemListener)
    addCloseable("firebase_listener_$key", fireListener)
    return fireListener
}