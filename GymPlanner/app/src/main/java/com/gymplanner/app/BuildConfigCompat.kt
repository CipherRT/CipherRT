package com.gymplanner.app

import android.content.Context

object BuildConfigCompat {
    /** True when this version was installed over an older one (not a fresh install). */
    fun wasUpdated(app: Context): Boolean = runCatching {
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        info.lastUpdateTime > info.firstInstallTime
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    fun versionCode(app: Context): Int =
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionCode }.getOrDefault(0)

    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
}
