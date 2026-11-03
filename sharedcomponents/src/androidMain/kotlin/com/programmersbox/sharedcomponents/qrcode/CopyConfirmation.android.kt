package com.programmersbox.sharedcomponents.qrcode

import android.os.Build

// Android 13+ shows its own clipboard preview after every copy
internal actual val systemConfirmsCopy: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
