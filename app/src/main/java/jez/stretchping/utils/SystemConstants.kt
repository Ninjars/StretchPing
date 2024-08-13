package jez.stretchping.utils

import android.os.Build
import javax.inject.Inject

class SystemConstants @Inject constructor() {
    val isDynamicThemeEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
