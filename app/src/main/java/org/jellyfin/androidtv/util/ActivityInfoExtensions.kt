package org.jellyfin.androidtv.util

import android.content.ComponentName
import android.content.pm.ActivityInfo

/**
 * Get the [ComponentName] for this [ActivityInfo].
 */
val ActivityInfo.componentName get() = ComponentName(packageName, name)
