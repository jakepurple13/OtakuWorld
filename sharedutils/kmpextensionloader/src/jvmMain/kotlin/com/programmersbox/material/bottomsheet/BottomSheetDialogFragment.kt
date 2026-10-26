package com.google.android.material.bottomsheet

import androidx.appcompat.app.AppCompatDialogFragment

/**
 * Supertype of CloudStream plugins' settings sheets (Anichi, AnimePahe). Desktop never creates one; the
 * sheet's own BottomSheetDialog/BottomSheetBehavior are only touched inside its methods and stay missing.
 */
open class BottomSheetDialogFragment : AppCompatDialogFragment()
