package android.view

/** SDK superclass of KeyEvent and MotionEvent. The Parcelable interface is left out: no extension references it. */
abstract class InputEvent

/** Parameter type of [android.content.DialogInterface.OnKeyListener]. Desktop never creates one. */
open class KeyEvent : InputEvent()
