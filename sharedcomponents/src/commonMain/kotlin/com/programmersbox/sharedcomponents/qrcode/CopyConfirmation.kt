package com.programmersbox.sharedcomponents.qrcode

/** True when the platform itself confirms a clipboard copy, so the app shouldn't show its own message */
internal expect val systemConfirmsCopy: Boolean
